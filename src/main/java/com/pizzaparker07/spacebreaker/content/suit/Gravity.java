package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Applies the local gravity of a celestial body to living entities through vanilla attributes:
 * gravity itself, jump strength (so jumps get higher, but not absurdly: height grows with 1/sqrt(g)),
 * and fall damage (impact energy scales with g, so safe fall height is 3 blocks / g).
 * Magnetic boots pin the wearer to the floor (Earth gravity) whenever a block is right below them.
 */
public final class Gravity {
    public static final ResourceLocation ID = Spacebreaker.id("planet_gravity");

    private Gravity() {
    }

    /** The gravity factor an entity actually feels here (0 = free fall, 1 = Earth). */
    public static double factorFor(LivingEntity entity) {
        double factor = Environment.gravityFactor(entity.level());
        if (factor < 1.0 && magnetised(entity)) {
            return 1.0;
        }
        return factor;
    }

    /** Magnetic boots on, not sneaking, and a solid block right underfoot. */
    public static boolean magnetised(LivingEntity entity) {
        if (!entity.getItemBySlot(EquipmentSlot.FEET).is(SuitModule.MAGNETIC_BOOTS.get()) || entity.isShiftKeyDown()) {
            return false;
        }
        Level level = entity.level();
        BlockPos feet = entity.blockPosition();
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = feet.below(dy);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() && entity.getY() - (p.getY() + 1) < 1.25) {
                return true;
            }
        }
        return false;
    }

    /** Sets (or clears, at factor 1) the gravity modifiers of an entity. Cheap when nothing changes. */
    public static void apply(LivingEntity entity, double factor) {
        if (Math.abs(factor - 1.0) < 1e-4) {
            clear(entity);
            return;
        }
        double f = Math.max(0.0, factor);
        set(entity, Attributes.GRAVITY, f - 1.0);
        set(entity, Attributes.JUMP_STRENGTH, Math.pow(f, 0.25) - 1.0);
        set(entity, Attributes.FALL_DAMAGE_MULTIPLIER, f - 1.0);
        set(entity, Attributes.SAFE_FALL_DISTANCE, f <= 0.01 ? 1000.0 : 1.0 / f - 1.0);
    }

    public static void clear(LivingEntity entity) {
        for (Holder<Attribute> a : java.util.List.of(Attributes.GRAVITY, Attributes.JUMP_STRENGTH, Attributes.FALL_DAMAGE_MULTIPLIER,
                Attributes.SAFE_FALL_DISTANCE)) {
            AttributeInstance inst = entity.getAttribute(a);
            if (inst != null && inst.getModifier(ID) != null) {
                inst.removeModifier(ID);
            }
        }
    }

    private static void set(LivingEntity entity, Holder<Attribute> attribute, double amount) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        AttributeModifier current = inst.getModifier(ID);
        if (current != null && Math.abs(current.amount() - amount) < 1e-6) {
            return;
        }
        inst.addOrUpdateTransientModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
