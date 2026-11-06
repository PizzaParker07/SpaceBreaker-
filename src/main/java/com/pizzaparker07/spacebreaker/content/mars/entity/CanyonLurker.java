package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Canyon Lurker: an ambush predator of the rift walls. Its carapace is layered like the sandstone around it; folded
 * up it looks like one more rock. When prey comes close it unfolds, lunges with its claws and fights; after a while
 * without prey it folds up and waits again.
 */
public class CanyonLurker extends Monster {
    private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(CanyonLurker.class, EntityDataSerializers.BOOLEAN);
    private int calm;

    public CanyonLurker(EntityType<? extends CanyonLurker> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return level.getDifficulty() != Difficulty.PEACEFUL
                && (below.is(MarsModule.MARS_STONE) || below.is(MarsModule.MARS_SOILS) || reason != MobSpawnType.NATURAL)
                && level.getBlockState(pos).isAir();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDDEN, true);
    }

    public boolean isHidden() {
        return entityData.get(HIDDEN);
    }

    public void setHidden(boolean hidden) {
        if (hidden != isHidden()) {
            entityData.set(HIDDEN, hidden);
            if (!hidden && level() instanceof ServerLevel server) {
                BlockState below = level().getBlockState(blockPosition().below());
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.4, getZ(), 30, 0.5, 0.3, 0.5, 0.2);
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> !isHidden() || p.distanceToSqr(this) < 25));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (isHidden()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            Player near = level().getNearestPlayer(this, 4.5);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                setHidden(false);
                setTarget(near);
                // the ambush: a lunge out of the rock face
                double dx = near.getX() - getX();
                double dz = near.getZ() - getZ();
                double d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                setDeltaMovement(dx / d * 0.7, 0.42, dz / d * 0.7);
            }
            return;
        }
        calm = getTarget() == null ? calm + 1 : 0;
        if (calm > 200 && onGround()) {
            setHidden(true);
            calm = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isHidden()) {
            setHidden(false);
        }
        return super.hurt(source, amount);
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
        setHidden(!tag.contains("Hidden") || tag.getBoolean("Hidden"));
    }
}
