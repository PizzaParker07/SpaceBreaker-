package com.pizzaparker07.spacebreaker.content.belt.entity;

import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Rock Mite: an eight-legged grazer of the asteroid belt, the size of a cat, that scrapes salts and clay from the rock
 * with two blunt palps and carries a crust of carbonate crystals on its back. It sits half-buried and still among the
 * pebbles until something comes close. Neutral: hit one and every mite around wakes and leaps at you - in a hundredth
 * of a g, a mite's jump carries it a long way.
 */
public class RockMite extends Monster {
    private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(RockMite.class, EntityDataSerializers.BOOLEAN);
    /** Wakes when a player comes this close (blocks), even unprovoked. */
    public static final double WAKE_RANGE = 3.0;
    private int calm;

    public RockMite(EntityType<? extends RockMite> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(BeltModule.CERES_GROUND) || reason != MobSpawnType.NATURAL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDDEN, false);
    }

    public boolean isHidden() {
        return entityData.get(HIDDEN);
    }

    public void setHidden(boolean hidden) {
        if (hidden != isHidden()) {
            entityData.set(HIDDEN, hidden);
            if (level() instanceof ServerLevel server) {
                BlockState ground = level().getBlockState(blockPosition().below());
                if (!ground.isAir()) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.15, getZ(), 14, 0.3, 0.05, 0.3, 0.05);
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return !isHidden() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isNoAi() || tickCount % 10 != 0) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null) {
            calm = 0;
            setHidden(false);
            return;
        }
        Player near = level().getNearestPlayer(this, WAKE_RANGE);
        if (near != null && !near.isCreative() && !near.isSpectator() && isHidden()) {
            // startled: pops out of the gravel and scuttles off
            setHidden(false);
            calm = 0;
            return;
        }
        if (!isHidden() && onGround() && ++calm > 30 && near == null) {
            getNavigation().stop();
            setHidden(true);
        }
        if (isHidden()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHidden = isHidden();
        boolean hit = super.hurt(source, amount * (wasHidden ? 0.6F : 1F));
        if (hit && !level().isClientSide) {
            setHidden(false);
            calm = 0;
            // the whole cluster wakes
            if (source.getEntity() instanceof LivingEntity attacker) {
                for (RockMite m : level().getEntitiesOfClass(RockMite.class, new AABB(blockPosition()).inflate(10), m -> m != this)) {
                    m.setHidden(false);
                    m.setTarget(attacker);
                }
            }
        }
        return hit;
    }

    @Override
    public boolean isPushable() {
        return !isHidden() && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hidden", isHidden());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHidden(tag.getBoolean("Hidden"));
    }
}
