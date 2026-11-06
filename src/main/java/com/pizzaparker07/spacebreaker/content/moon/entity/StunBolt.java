package com.pizzaparker07.spacebreaker.content.moon.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A survey drone's stun bolt: a pulse of charged dust that hurts a little and slows its target. No fire in vacuum. */
public class StunBolt extends AbstractHurtingProjectile {
    public StunBolt(EntityType<? extends StunBolt> type, Level level) {
        super(type, level);
    }

    public StunBolt(Level level, LivingEntity owner, Vec3 direction) {
        super(MoonEntities.STUN_BOLT.get(), owner, direction, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), 4F) && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1), owner);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.ELECTRIC_SPARK;
    }

    @Override
    protected float getInertia() {
        return 1.0F; // no air drag
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
