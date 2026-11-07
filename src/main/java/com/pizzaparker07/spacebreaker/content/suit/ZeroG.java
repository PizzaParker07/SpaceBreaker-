package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the Maneuvering Unit: while the wearer reports thrust (see ThrustPayload) and is not
 * standing on the ground, one tick of cold gas is burned per tick and small cold-gas puffs leave the
 * thruster towers against the push, seen by everyone. The movement itself is applied on the client
 * (client.suit.ZeroGControls), like all player movement in Minecraft.
 */
public final class ZeroG {
    private ZeroG() {
    }

    public static void tickThrust(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        SurvivalData data = LifeSupport.data(player);
        if (!data.thrusting || player.onGround() || player.isCreative()) {
            return;
        }
        ItemStack torso = SuitTanks.wornTorso(player);
        ItemStack mmu = torso.isEmpty() ? ItemStack.EMPTY : SuitTanks.maneuveringUnit(torso);
        int fuel = mmu.isEmpty() ? 0 : ManeuveringUnitItem.fuel(mmu);
        if (fuel <= 0) {
            data.thrusting = false;
            return;
        }
        player.resetFallDistance();
        if (player.level() instanceof ServerLevel level && player.tickCount % 3 == 0) {
            puffs(level, player, data.thrustDir);
        }
        ItemStack copy = mmu.copy();
        copy.set(SuitModule.GAS_FUEL.get(), fuel - 1);
        SuitTanks.setManeuveringUnit(torso, copy);
    }

    /** Small puffs of condensing cold gas (snowflake particles) from the nozzles at the top and bottom of both thruster towers, blowing against the push. */
    static void puffs(ServerLevel level, Player player, Vec3 push) {
        Vec3 dir = push.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : push.normalize();
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(0.3);
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.34);
        for (int i = 0; i < 4; i++) {
            Vec3 side = right.scale(i % 2 == 0 ? 1 : -1);
            double y = i < 2 ? 1.42 : 0.82;
            Vec3 nozzle = player.position().add(back).add(side).add(0, y, 0);
            // count 0: the offsets are the velocity of the one particle
            level.sendParticles(ParticleTypes.SNOWFLAKE, nozzle.x, nozzle.y, nozzle.z, 0, -dir.x, -dir.y, -dir.z, 0.18);
        }
        if (player.tickCount % 6 == 0) {
            level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    0.06F, 1.9F + level.random.nextFloat() * 0.2F);
        }
    }

    /** Climb speed the unit holds while jump is held, and the gentlest descent it holds while sneaking (blocks/tick). */
    public static final double CLIMB_SPEED = 0.25, GLIDE_SPEED = -0.1;
    /** Most the unit can change the vertical speed in one tick (cold gas is weak: on Earth it only softens a fall). */
    public static final double MAX_LIFT = 0.06;

    /**
     * New vertical speed with the Maneuvering Unit in gravity: jump climbs towards {@link #CLIMB_SPEED}, sneak
     * brakes a fall to a glide at {@link #GLIDE_SPEED}; the unit never pushes harder than {@link #MAX_LIFT} per
     * tick (times the booster), so on the Moon it hovers and glides, on Earth it only softens a fall.
     * Returns {@code vy} unchanged when the unit does nothing.
     */
    public static double lift(double vy, boolean up, boolean down, double boost) {
        double max = MAX_LIFT * boost;
        if (up && vy < CLIMB_SPEED * boost) {
            return vy + Math.min(max, CLIMB_SPEED * boost - vy);
        }
        if (down && !up && vy < GLIDE_SPEED) {
            return vy + Math.min(max, GLIDE_SPEED - vy);
        }
        return vy;
    }
}
