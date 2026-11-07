package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * The two settings of the helmet lamps: a wide flood for working nearby and a long, narrow beam for lava tubes
 * and shadowed craters. Only the light changes (no extra power or oxygen). Stored on the helmet, so everyone
 * sees the same cone. Toggled with a key (client.suit.SuitKeys) through {@code LampModePayload}.
 */
public final class HelmetLamp {
    public static final double WIDE_RANGE = 20.0, LONG_RANGE = 40.0;

    private HelmetLamp() {
    }

    public static boolean longBeam(ItemStack helmet) {
        return helmet.getOrDefault(SuitModule.LONG_BEAM.get(), false);
    }

    public static boolean longBeam(LivingEntity entity) {
        return longBeam(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** How far the lamps reach. */
    public static double range(boolean longBeam) {
        return longBeam ? LONG_RANGE : WIDE_RANGE;
    }

    /** Radius in blocks of the pool of light on a surface {@code distance} blocks away. */
    public static double poolRadius(double distance, boolean longBeam) {
        return longBeam ? 1.6 + distance * 0.1 : 1.8 + distance * 0.3;
    }

    /** Pool brightness 0..1 at that distance: the long beam carries its light much further. */
    public static double strength(double distance, boolean longBeam) {
        return Math.max(0, 1 - distance / range(longBeam));
    }

    /** Server: switches the worn helmet's lamps; returns the new mode, or null without an EVA helmet. */
    public static Boolean toggle(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof SpaceSuitItem suit) || suit.getType() != ArmorItem.Type.HELMET) {
            return null;
        }
        boolean now = !longBeam(helmet);
        helmet.set(SuitModule.LONG_BEAM.get(), now);
        player.displayClientMessage(Component.translatable(now ? "message.spacebreaker.lamp.long" : "message.spacebreaker.lamp.wide")
                .withStyle(ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.3F, now ? 1.4F : 1.1F);
        return now;
    }
}
