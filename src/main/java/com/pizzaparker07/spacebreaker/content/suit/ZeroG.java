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
        if (player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            puffs(level, player, data.thrustDir);
        }
        ItemStack copy = mmu.copy();
        copy.set(SuitModule.GAS_FUEL.get(), fuel - 1);
        SuitTanks.setManeuveringUnit(torso, copy);
    }

    /** Puffs from the nozzles at the top and bottom of both thruster towers, blowing against the push. */
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
            level.sendParticles(ParticleTypes.POOF, nozzle.x, nozzle.y, nozzle.z, 0, -dir.x, -dir.y, -dir.z, 0.12);
        }
        if (player.tickCount % 6 == 0) {
            level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    0.06F, 1.9F + level.random.nextFloat() * 0.2F);
        }
    }
}
