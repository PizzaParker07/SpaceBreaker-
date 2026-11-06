package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Damage types of the space environment (data/spacebreaker/damage_type/*.json). No blood, ever. */
public final class SuitDamage {
    public static final ResourceKey<DamageType> VACUUM = key("vacuum");
    public static final ResourceKey<DamageType> HYPOTHERMIA = key("hypothermia");
    public static final ResourceKey<DamageType> HYPERTHERMIA = key("hyperthermia");
    public static final ResourceKey<DamageType> RADIATION = key("radiation");

    private SuitDamage() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Spacebreaker.id(name));
    }

    public static DamageSource source(LivingEntity entity, ResourceKey<DamageType> type) {
        return new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    public static void hurt(LivingEntity entity, ResourceKey<DamageType> type, float amount) {
        entity.hurt(source(entity, type), amount);
    }
}
