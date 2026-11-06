package com.pizzaparker07.spacebreaker.content.venus;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Venusian atmosphere by altitude. The body's climate numbers describe the surface (464 °C, 92 bar); air gets
 * thinner and cooler with height (scale height ~40 blocks, lapse ~2.5 °C per block), so the cloud deck around
 * y 195-245 is close to Earth's sea level: 1 bar and room temperature, but full of sulfuric-acid droplets that eat
 * unprotected armour.
 */
public final class VenusClimate {
    /** Mean surface height of the plains. */
    public static final int SURFACE = 46;
    public static final int CLOUD_BASE = 195;
    public static final int CLOUD_TOP = 245;
    /** Pressure scale height in blocks: 92 bar at the surface, ~1 bar around y 225. */
    public static final double SCALE_HEIGHT = 39.0;
    public static final double LAPSE = 2.45;

    private VenusClimate() {
    }

    static boolean venus(Level level) {
        return level.dimension() == VenusModule.VENUS;
    }

    static void register() {
        Environment.addTemperatureModifier((level, pos) -> venus(level) ? -LAPSE * Math.max(0, pos.getY() - SURFACE) : 0);
        Environment.addPressureModifier((level, pos) -> venus(level) ? Math.exp(-Math.max(0, pos.getY() - SURFACE) / SCALE_HEIGHT) : 1);
        // the cloud deck and the haze below it hide the sun: only a dim orange glow reaches the ground
        Environment.addSunlightModifier((level, pos) -> venus(level) ? sunlight(pos.getY()) : 1);
    }

    /** Share of sunlight reaching height y (the bright cloud tops above, a dim twilight on the surface). */
    public static double sunlight(int y) {
        if (y >= CLOUD_TOP) {
            return 1.0;
        }
        if (y >= CLOUD_BASE) {
            return Mth.lerp((double) (y - CLOUD_BASE) / (CLOUD_TOP - CLOUD_BASE), 0.35, 1.0);
        }
        return 0.08 + 0.27 * Mth.clamp((double) (y - SURFACE) / (CLOUD_BASE - SURFACE), 0, 1);
    }

    public static boolean inAcidClouds(Level level, BlockPos pos) {
        return venus(level) && pos.getY() >= CLOUD_BASE && pos.getY() <= CLOUD_TOP;
    }

    /** Every few seconds in the acid clouds, each uncoated armour piece loses durability; bare skin stings. */
    static void acid(Player player) {
        if (!(player instanceof ServerPlayer sp) || player.isCreative() || player.isSpectator() || player.tickCount % 60 != 0
                || !inAcidClouds(player.level(), player.blockPosition()) || !player.level().canSeeSky(player.blockPosition())) {
            return;
        }
        boolean hurt = false;
        boolean bare = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.isEmpty()) {
                bare = true;
            } else if (!Boolean.TRUE.equals(s.get(VenusModule.ACID_COATED.get())) && s.isDamageableItem()) {
                s.hurtAndBreak(1, sp, slot);
                hurt = true;
            }
        }
        if (bare) {
            player.hurt(player.damageSources().magic(), 1F);
        }
        if ((hurt || bare) && player.tickCount % 600 == 0) {
            sp.displayClientMessage(Component.translatable("message.spacebreaker.acid_clouds"), true);
        }
    }
}
