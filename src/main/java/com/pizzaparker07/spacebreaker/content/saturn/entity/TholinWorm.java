package com.pizzaparker07.spacebreaker.content.saturn.entity;

import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Tholin Worm: a slow, ringed burrower of Titan's dune seas. It ploughs through the organic sand, sifting out the
 * tholin grains it lives on and now and then leaving a clean pellet of tholin behind it (a slow, renewable source).
 * Shy: when a player comes close it sinks into the sand and waits, coming back up once things are quiet; a hit drives
 * it out again. Fed tholin sand, two worms breed.
 */
public class TholinWorm extends Animal {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(TholinWorm.class, EntityDataSerializers.BOOLEAN);
    /** Player distance that makes it burrow, and the quiet distance before it surfaces. */
    public static final double SHY = 6.0;
    public static final double QUIET = 10.0;
    private int calm;
    private int pelletTime;

    public TholinWorm(EntityType<? extends TholinWorm> type, Level level) {
        super(type, level);
        pelletTime = 6000 + random.nextInt(6000);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.14)
                .add(Attributes.ARMOR, 2.0);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SaturnModule.THOLIN_SAND.get()) || reason != MobSpawnType.NATURAL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    /** Ticks it has been calm while burrowed (it surfaces after 60). */
    public int calmTicks() {
        return calm;
    }

    public void setBurrowed(boolean burrowed) {
        if (burrowed != isBurrowed()) {
            entityData.set(BURROWED, burrowed);
            calm = 0;
            getNavigation().stop();
            playSound(SoundEvents.SAND_BREAK, 0.6F, 0.7F);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(SaturnModule.THOLIN_SAND.get()), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !isBurrowed() && super.canUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isNoAi()) {
            return;
        }
        Player near = level().getNearestPlayer(this, isBurrowed() ? QUIET : SHY);
        boolean threat = near != null && !near.isCreative() && !near.isSpectator() && !isFood(near.getMainHandItem());
        if (!isBurrowed() && threat && onGround() && getLastHurtByMob() == null) {
            setBurrowed(true);
        } else if (isBurrowed()) {
            getNavigation().stop();
            if (!threat && ++calm > 60) {
                setBurrowed(false);
            }
        }
        if (!isBaby() && isAlive() && --pelletTime <= 0) {
            spawnAtLocation(SaturnModule.THOLIN.get());
            playSound(SoundEvents.SAND_STEP, 0.5F, 1.2F);
            pelletTime = 6000 + random.nextInt(6000);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && isBurrowed()) {
            setBurrowed(false);
        }
        return hit;
    }

    @Override
    public boolean isPushable() {
        return !isBurrowed() && super.isPushable();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(SaturnModule.THOLIN_SAND.get().asItem());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return SaturnEntities.THOLIN_WORM.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Burrowed", isBurrowed());
        tag.putInt("PelletTime", pelletTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURROWED, tag.getBoolean("Burrowed"));
        if (tag.contains("PelletTime")) {
            pelletTime = tag.getInt("PelletTime");
        }
    }

    /** For tests: the ticks until the next pellet. */
    public void setPelletTime(int ticks) {
        pelletTime = ticks;
    }
}
