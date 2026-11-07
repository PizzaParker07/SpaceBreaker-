package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Wear on EVA suit pieces from the environment (crushing pressure, micrometeorites, ...), for any module to use.
 * Server side. Wear is plain item damage: the Suit Maintenance Station and an anvil mend it. A piece whose
 * durability runs out leaks (the suit is no longer sealed), so hazards that must never trap the player pass a
 * {@code maxFraction} below 1.
 */
public final class SuitWear {
    private SuitWear() {
    }

    /**
     * Adds {@code points} of wear to every worn suit piece, but never beyond {@code maxFraction} of its durability
     * (1.0 = may wear it out completely). Returns how many pieces took wear.
     */
    public static int wearAll(LivingEntity wearer, int points, double maxFraction) {
        int worn = 0;
        for (EquipmentSlot slot : SuitProtection.SLOTS) {
            if (wear(wearer.getItemBySlot(slot), points, maxFraction)) {
                worn++;
            }
        }
        return worn;
    }

    /** Adds wear to one worn piece ({@code slot}); true if it took any. */
    public static boolean wearPiece(LivingEntity wearer, EquipmentSlot slot, int points, double maxFraction) {
        return wear(wearer.getItemBySlot(slot), points, maxFraction);
    }

    static boolean wear(ItemStack stack, int points, double maxFraction) {
        if (!(stack.getItem() instanceof SpaceSuitItem) || !stack.isDamageableItem() || points <= 0) {
            return false;
        }
        int cap = (int) Math.floor(stack.getMaxDamage() * Math.min(1.0, maxFraction));
        // a fully worn piece keeps one point so it stays in the slot (and leaks) instead of vanishing
        cap = Math.min(cap, stack.getMaxDamage() - 1);
        int next = Math.min(cap, stack.getDamageValue() + points);
        if (next <= stack.getDamageValue()) {
            return false;
        }
        stack.setDamageValue(next);
        return true;
    }
}
