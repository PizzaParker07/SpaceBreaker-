package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How fast the suit breathes its tanks down. A tank holds "breathing ticks" at the walking rate (1 per tick); the
 * wearer uses more when sprinting or working and less at rest, and the heavier suits cost a little more. The HUD
 * and the way-home warning use a running average over a few seconds, so the shown time does not flicker.
 *
 * <p>The tank capacities themselves live in {@link OxygenTankItem}; when tanks get gated behind later materials,
 * only their recipes change, not this rate.
 */
public final class OxygenUse {
    /** Rates relative to walking. */
    public static final double SPRINT = 1.5, WORK = 1.25, WALK = 1.0, REST = 0.8;
    /** The heavier suits (thermal, heated, radiation) breathe this much more. */
    public static final double HEAVY_SUIT = 1.1;
    /** The running average follows the activity over about this many ticks (5 s). */
    public static final int AVERAGE_TICKS = 100;
    /** Moving slower than this (blocks per tick, horizontally) counts as resting. */
    static final double REST_SPEED = 0.02;

    private OxygenUse() {
    }

    /** The activity part of the rate: sprinting, working (swinging or using an item), walking or resting. */
    public static double activity(boolean sprinting, boolean working, double horizontalSpeed) {
        if (sprinting) {
            return SPRINT;
        }
        if (working) {
            return WORK;
        }
        return horizontalSpeed > REST_SPEED ? WALK : REST;
    }

    /** The suit part of the rate: 1 for the Mk1 suit, {@link #HEAVY_SUIT} for the heavier tiers. */
    public static double suitFactor(Player player) {
        ItemStack torso = player.getItemBySlot(EquipmentSlot.CHEST);
        return torso.getItem() instanceof SpaceSuitItem suit && suit.tier() != SuitTier.MK1 ? HEAVY_SUIT : 1.0;
    }

    /** The breathing rate right now, in tank ticks per game tick. */
    public static double rate(Player player) {
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        boolean working = player.swinging || player.isUsingItem();
        return activity(player.isSprinting(), working, Math.sqrt(dx * dx + dz * dz)) * suitFactor(player);
    }

    /** Moves the running average one tick towards {@code rate}. */
    public static double follow(double average, double rate) {
        return average + (rate - average) / AVERAGE_TICKS;
    }

    /**
     * Takes this tick's oxygen from the tanks at {@code rate}, carrying fractions over in {@code data}.
     * Returns whether the wearer got air (the tanks were not empty).
     */
    public static boolean breathe(ItemStack torso, SurvivalData data, double rate) {
        data.oxygenOwed += rate;
        int whole = (int) data.oxygenOwed;
        if (whole == 0) {
            return SuitTanks.oxygen(torso) > 0;
        }
        data.oxygenOwed -= whole;
        return SuitTanks.drain(torso, whole) > 0;
    }

    /** Game ticks the tanks last at the given average rate. */
    public static int ticksLeft(int oxygen, double averageRate) {
        return (int) Math.round(oxygen / Math.max(0.1, averageRate));
    }
}
