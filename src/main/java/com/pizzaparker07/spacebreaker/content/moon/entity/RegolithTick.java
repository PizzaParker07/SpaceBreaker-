package com.pizzaparker07.spacebreaker.content.moon.entity;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Regolith Tick: a small, eight-legged scavenger of the lunar dust. It buries itself in soft soil and waits;
 * when something warm walks by it bursts out and makes long, floating hops in the low gravity.
 * Elder ticks (bigger, tougher) guard old lava tubes.
 */
public class RegolithTick extends Monster {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(RegolithTick.class, EntityDataSerializers.BOOLEAN);
    private static final int BURROW_AFTER = 80;
    private int idle;
    private int hopCooldown;

    public RegolithTick(EntityType<? extends RegolithTick> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.JUMP_STRENGTH, 0.62)
                .add(Attributes.SAFE_FALL_DISTANCE, 12.0);
    }

    public static AttributeSupplier.Builder elderAttributes() {
        return attributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    /** Natural spawning: on lunar soil or rock, never on peaceful, ignores light (there is no night shelter on the Moon). */
    public static boolean checkSpawn(EntityType<? extends RegolithTick> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return level.getDifficulty() != Difficulty.PEACEFUL
                && (below.is(MoonModule.LUNAR_SOILS) || reason == MobSpawnType.SPAWNER || reason == MobSpawnType.STRUCTURE)
                && level.getBlockState(pos).isAir();
    }

    public boolean isElder() {
        return getType() == MoonEntities.ELDER_REGOLITH_TICK.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    public void setBurrowed(boolean burrowed) {
        if (burrowed != isBurrowed()) {
            entityData.set(BURROWED, burrowed);
            if (level() instanceof ServerLevel server) {
                BlockState below = level().getBlockState(blockPosition().below());
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.1, getZ(),
                        burrowed ? 10 : 24, 0.3, 0.1, 0.3, 0.15);
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.55F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (isBurrowed()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            Player near = level().getNearestPlayer(this, isElder() ? 7 : 4.5);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                setBurrowed(false);
                setTarget(near);
                // burst out of the dust
                setDeltaMovement(getDeltaMovement().add(0, 0.55, 0));
            }
            return;
        }
        boolean calm = getTarget() == null && onGround() && getDeltaMovement().horizontalDistanceSqr() < 1e-3;
        idle = calm ? idle + 1 : 0;
        if (idle > BURROW_AFTER && tryBurrow()) {
            idle = 0;
        }
        // low-gravity hops while chasing or wandering
        if (hopCooldown > 0) {
            hopCooldown--;
        } else if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            jumpControl.jump();
            hopCooldown = 12 + random.nextInt(25);
        }
    }

    /** Buries the tick if it stands on soft lunar soil (not on rock). */
    public boolean tryBurrow() {
        if (getTarget() == null && level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ())).is(MoonModule.LUNAR_SOILS)) {
            setBurrowed(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBurrowed()) {
            setBurrowed(false);
            amount *= 0.5F; // the dust shields it
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isBurrowed() && super.isPushable();
    }

    @Override
    protected float getJumpPower() {
        return super.getJumpPower() * (isBurrowed() ? 0F : 1.1F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Burrowed", isBurrowed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURROWED, tag.getBoolean("Burrowed"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBurrowed() ? null : SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isElder() ? 0.6F : 0.85F);
    }
}
