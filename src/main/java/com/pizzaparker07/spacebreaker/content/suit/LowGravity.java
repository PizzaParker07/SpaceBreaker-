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
