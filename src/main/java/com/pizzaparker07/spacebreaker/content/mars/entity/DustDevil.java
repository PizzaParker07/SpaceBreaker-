package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Dust Devil: a living whirlwind that only forms in dust storms. It races over the ground toward anything that moves,
 * lifts it off its feet and sandblasts it (short blindness). It falls apart when the storm ends. Made of dust: it takes
 * no fall damage.
 */
public class DustDevil extends Monster {
    public DustDevil(EntityType<? extends DustDevil> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            Vec3 m = target.getDeltaMovement();
            target.setDeltaMovement(m.x * 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.75, m.z * 0.5 + (random.nextDouble() - 0.5) * 0.6);
            target.hurtMarked = true;
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
            }
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 4 == 0) {
                server.sendParticles(com.pizzaparker07.spacebreaker.content.mars.MarsParticles.DUST_MOTE.get(), getX(), getY() + 0.3, getZ(),
                        3, 0.5, 0.2, 0.5, 0.05);
            }
            // a whirlwind lives only as long as the storm that feeds it
            if (!isNoAi() && tickCount > 100 && MarsWeather.dusty(level()) && MarsWeather.stormStrength(level()) < 0.2F && random.nextInt(40) == 0) {
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.02);
                discard();
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELYTRA_FLYING;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SAND_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    /** Called by the storm spawner: finds a spot on open ground near a player. */
    public static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.below()).isSolid()
                && level.canSeeSky(pos);
    }
}
