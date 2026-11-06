package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Combines the four worn pieces into one protection rating. A mixed suit is as good as its weakest piece;
 * upgrade modules widen the temperature range or add shielding; a piece whose technology the wearer has
 * not researched yet only protects like a Mk1 piece.
 */
public final class SuitProtection {
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Degrees added to both ends of the range per thermal regulator module. */
    public static final int THERMAL_MODULE_C = 15;
    /** Dose multiplier per radiation shield module. */
    public static final double SHIELD_MODULE_FACTOR = 0.75;

    /** @param minTempC coldest compensated temperature; @param maxTempC hottest; @param radiationFactor dose fraction */
    public record Rating(int minTempC, int maxTempC, double radiationFactor) {
        public boolean covers(double ambientC) {
            return ambientC >= minTempC && ambientC <= maxTempC;
        }

        public int shieldingPercent() {
            return (int) Math.round((1.0 - radiationFactor) * 100);
        }
    }

    private SuitProtection() {
    }

    /** The tier a piece actually provides for this wearer (Mk1 when its technology is unknown). */
    public static SuitTier effectiveTier(LivingEntity wearer, SpaceSuitItem item) {
        SuitTier tier = item.tier();
        if (tier.tech() != null && wearer instanceof Player p && !p.isCreative() && !Tech.knows(p, tier.tech())) {
            return SuitTier.MK1;
        }
        return tier;
    }

    /** The rating of a complete, intact suit, or null when it is not sealed. */
    @Nullable
    public static Rating rating(LivingEntity entity) {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        double rad = 0.0;
        int thermal = 0;
        int shields = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof SpaceSuitItem suit) || suit.getEquipmentSlot() != slot) {
                return null;
            }
            if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
                return null; // a ruined piece leaks
            }
            SuitTier t = effectiveTier(entity, suit);
            min = Math.max(min, t.minTempC());
            max = Math.min(max, t.maxTempC());
            rad = Math.max(rad, t.radiationFactor());
            SuitUpgrade upgrade = SuitUpgrade.of(stack);
            if (upgrade == SuitUpgrade.THERMAL_REGULATOR) {
                thermal++;
            } else if (upgrade == SuitUpgrade.RADIATION_SHIELD) {
                shields++;
            }
        }
        return new Rating(min - thermal * THERMAL_MODULE_C, max + thermal * THERMAL_MODULE_C, rad * Math.pow(SHIELD_MODULE_FACTOR, shields));
    }

    /** Whether the worn torso carries the given upgrade. */
    public static boolean torsoHas(LivingEntity entity, SuitUpgrade upgrade) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof SpaceSuitItem && SuitUpgrade.of(chest) == upgrade;
    }
}
