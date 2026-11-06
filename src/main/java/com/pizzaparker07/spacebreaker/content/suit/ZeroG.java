package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the Maneuvering Unit: while the wearer reports thrust (see ThrustPayload) and is not
 * standing on the ground, one tick of cold gas is burned per tick. The movement itself is applied on the
 * client (client.suit.ZeroGControls), like all player movement in Minecraft.
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
        ItemStack copy = mmu.copy();
        copy.set(SuitModule.GAS_FUEL.get(), fuel - 1);
        SuitTanks.setManeuveringUnit(torso, copy);
    }
}
