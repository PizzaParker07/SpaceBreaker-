package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Terminator Crawler: a long, heat-shielded arthropod that lives on the day-night boundary of Mercury, where the
 * ground is neither molten-hot nor deep-frozen. While the sun is low it roams and hunts; when the sun climbs high or
 * sinks far below the horizon it digs into the regolith and waits for the terminator to come back around.
 */
public class TerminatorCrawler extends Monster {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(TerminatorCrawler.class, EntityDataSerializers.BOOLEAN);
    /** Active while the sun is within this many degrees of the horizon. */
    public static final float TWILIGHT_DEG = 20F;

    public TerminatorCrawler(EntityType<? extends TerminatorCrawler> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(MercuryModule.MERCURY_REGOLITH.get()) || reason != MobSpawnType.NATURAL);
    }

    /** True while the sun is near the horizon: the crawler's waking hours. */
    public static boolean twilight(Level level) {
        return Math.abs(Environment.sunElevation(level) * Mth.RAD_TO_DEG) < TWILIGHT_DEG;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    public void setBurrowed(boolean b) {
        if (b != isBurrowed()) {
            entityData.set(BURROWED, b);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                        getX(), getY() + 0.2, getZ(), 24, 0.6, 0.1, 0.6, 0.1);
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !isBurrowed()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isNoAi() || tickCount % 20 != 0) {
            return;
        }
        boolean awake = twilight(level()) || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 200;
        if (!awake && !isBurrowed() && onGround()) {
            setTarget(null);
            setBurrowed(true);
        } else if (awake && isBurrowed()) {
            setBurrowed(false);
        }
        if (isBurrowed()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBurrowed()) {
            setBurrowed(false);
            amount *= 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isBurrowed() && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Burrowed", isBurrowed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBurrowed(tag.getBoolean("Burrowed"));
    }
}
