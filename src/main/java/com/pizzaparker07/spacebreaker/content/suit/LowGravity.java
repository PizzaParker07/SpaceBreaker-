package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Local gravity for the things that are not living entities (those use attributes, see {@link Gravity}): dropped
 * items, experience orbs, arrows and other projectiles, falling blocks and primed TNT. Vanilla pulls them down with
 * Earth gravity everywhere; on a body with less gravity each tick gives back the part of the pull that is too much,
 * so a dropped stone takes as long to land on the Moon as the player who threw it.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class LowGravity {
    /** One cache per side (the client and the integrated server tick on different threads). */
    private static final Level[] CACHED_LEVEL = new Level[2];
    private static final long[] CACHED_TIME = {Long.MIN_VALUE, Long.MIN_VALUE};
    private static final double[] CACHED_FACTOR = {1.0, 1.0};

    private LowGravity() {
    }

    /** The things this applies to (cheap class checks, done before anything else each tick). */
    public static boolean affected(Entity e) {
        return e instanceof ItemEntity || e instanceof ExperienceOrb || e instanceof Projectile
                || e instanceof FallingBlockEntity || e instanceof PrimedTnt;
    }

    /** Upward speed to give back after a vanilla tick that applied {@code gravity} at local factor {@code f}. */
    public static double restoredFall(double gravity, double f) {
        return f >= 1.0 ? 0.0 : gravity * (1.0 - Math.max(0.0, f));
    }

    /** Least share of air control left on the lightest worlds above microgravity. */
    public static final double MIN_AIR_CONTROL = 0.35;

    /**
     * Share of the walking keys' push that still acts while airborne at gravity factor {@code f}: with little weight
     * there is little to push against, so a long low-gravity hop mostly follows its take-off. Square root of the
     * factor, never below {@link #MIN_AIR_CONTROL}; full control on Earth and heavier worlds.
     */
    public static double airControl(double f) {
        return f >= 1.0 ? 1.0 : Math.max(MIN_AIR_CONTROL, Math.sqrt(Math.max(0.0, f)));
    }

    /** Vanilla's horizontal air drag per tick. */
    public static final double VANILLA_AIR_DRAG = 0.91;

    /**
     * Horizontal air drag per tick while airborne at gravity factor {@code f}: with less steering the vanilla drag
     * would eat the take-off speed, so most of it is given back (four fifths of what the steering lost, tuned on
     * measured hops: a Moon sprint hop keeps about nine tenths of its vanilla length), fading out on the
     * lightest worlds (below a factor of 0.15) so their hops stay short. Vanilla on Earth-like worlds and in
     * microgravity (own controls).
     */
    public static double airDrag(double f) {
        if (f >= 1.0 || ZeroG.microGravity(f)) {
            return VANILLA_AIR_DRAG;
        }
        double relief = Math.min(1.0, f / 0.15);
        return VANILLA_AIR_DRAG + (1.0 - VANILLA_AIR_DRAG) * 0.8 * (1.0 - airControl(f)) * relief;
    }

    /**
     * Mining speed multiplier on top of vanilla's "not on the ground: five times slower" while floating in
     * microgravity: the penalty becomes two times, and none at all on a tether (the line gives something to brace
     * against). Elsewhere, and when standing (magnetic boots), vanilla is kept.
     */
    public static float floatingMiningBoost(double f, boolean onGround, boolean tethered) {
        if (onGround || !ZeroG.microGravity(f)) {
            return 1F;
        }
        return tethered ? 5F : 2.5F;
    }

    @SubscribeEvent
    static void onBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        var p = event.getEntity();
        if (p.onGround()) {
            return;
        }
        float boost = floatingMiningBoost(Gravity.factorFor(p), false, Tether.isTethered(p));
        if (boost != 1F) {
            event.setNewSpeed(event.getNewSpeed() * boost);
        }
    }

    /**
     * Dust grains kicked up by a player landing after a fall of {@code distance} blocks at gravity factor {@code f}:
     * none on Earth-like worlds, for short steps or in microgravity; a light puff otherwise, at most ten.
     */
    public static int landingDust(double f, double distance) {
        if (f >= 1.0 || ZeroG.microGravity(f) || distance < 1.5) {
            return 0;
        }
        return (int) Math.min(10, 3 + distance);
    }

    @SubscribeEvent
    static void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player p)
                || !(p.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        int grains = landingDust(Gravity.factorFor(p), event.getDistance());
        net.minecraft.core.BlockPos below = p.getOnPos();
        var state = level.getBlockState(below);
        if (grains == 0 || state.isAir()) {
            return;
        }
        // slow, low grains that spread out (no air to hold them up), and a soft thud of the ground underfoot
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                p.getX(), p.getY() + 0.05, p.getZ(), grains, 0.35, 0.02, 0.35, 0.02);
        var sound = state.getSoundType(level, below, p);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), sound.getStepSound(), net.minecraft.sounds.SoundSource.PLAYERS,
                sound.getVolume() * 0.3F, sound.getPitch() * 0.7F);
    }

    /** Below this ambient pressure (kPa) wings find nothing to lean on. */
    public static final double VACUUM_KPA = 0.05;

    /**
     * Share of an elytra's lift at ambient pressure {@code kpa}: none in vacuum, the pressure against Earth's to the
     * power 0.35 in thin air (Mars, 0.6 kPa: about a sixth, a tenth of an Earth glide's reach), full in Earth-like
     * or denser air.
     */
    public static double elytraLift(double kpa) {
        if (kpa < VACUUM_KPA) {
            return 0.0;
        }
        return Math.min(1.0, Math.pow(kpa / 101.325, 0.35));
    }

    /** The local player's speed before this tick's movement, to tell the wings' share from a plain fall. */
    private static net.minecraft.world.phys.Vec3 glideStart;

    @SubscribeEvent
    static void beforePlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        var p = event.getEntity();
        glideStart = p.level().isClientSide && p.isLocalPlayer() && p.isFallFlying() ? p.getDeltaMovement() : null;
    }

    @SubscribeEvent
    static void afterPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        var p = event.getEntity();
        if (!p.isFallFlying()) {
            return;
        }
        double lift = elytraLift(Environment.pressureKpa(p.level(), p.blockPosition()));
        if (lift >= 1.0) {
            return;
        }
        if (lift <= 0.0) {
            p.stopFallFlying(); // nothing to glide on: the wings fold and the wearer simply falls
            return;
        }
        if (p.level().isClientSide && p.isLocalPlayer() && glideStart != null) {
            // keep only part of what the wings did this tick, the rest is a plain fall with vanilla drag
            var v0 = glideStart;
            var plain = new net.minecraft.world.phys.Vec3(v0.x * 0.99, (v0.y - p.getGravity()) * 0.98, v0.z * 0.99);
            p.setDeltaMovement(plain.add(p.getDeltaMovement().subtract(plain).scale(lift)));
        }
    }

    /** The gravity factor of a level, looked up once per level and tick. */
    static double factor(Level level) {
        int side = level.isClientSide ? 1 : 0;
        long time = level.getGameTime();
        if (level != CACHED_LEVEL[side] || time != CACHED_TIME[side]) {
            CACHED_LEVEL[side] = level;
            CACHED_TIME[side] = time;
            CACHED_FACTOR[side] = Environment.gravityFactor(level);
        }
        return CACHED_FACTOR[side];
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!affected(e) || e.isNoGravity() || e.onGround() || e.isInWater() || e.isInLava()) {
            return;
        }
        if (e.getX() == e.xo && e.getY() == e.yo && e.getZ() == e.zo) {
            return; // stuck (an arrow in a block, a block resting on another): leave it be
        }
        double f = factor(e.level());
        if (f >= 1.0) {
            return;
        }
        double back = restoredFall(e.getGravity(), f);
        if (back > 0) {
            e.setDeltaMovement(e.getDeltaMovement().add(0, back, 0));
        }
    }
}
