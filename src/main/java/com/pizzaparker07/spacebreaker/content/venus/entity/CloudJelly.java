package com.pizzaparker07.spacebreaker.content.venus.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Cloud Jelly: a passive gas bag of the Venusian cloud deck, kept aloft by a sac of light gas, trailing tentacles
 * that sieve sulfur aerosols out of the air. It drifts with the winds and pulses slowly.
 */
public class CloudJelly extends SkyFloater {
    public CloudJelly(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected double cruise() {
        return 0.06;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new DriftGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }
}
