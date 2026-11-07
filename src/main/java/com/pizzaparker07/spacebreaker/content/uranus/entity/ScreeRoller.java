package com.pizzaparker07.spacebreaker.content.uranus.entity;

import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Scree Roller: a plated grazer of Titania's canyon slopes. Hit, it curls into a hard ball and rolls away at speed down
 * the scree, taking a fraction of the damage while curled; after a while it unrolls. Fed coal (the carbon film it scrapes
 * off the ice), two rollers breed.
 */
public class ScreeRoller extends Animal {
    private static final EntityDataAccessor<Integer> ROLLED = SynchedEntityData.defineId(ScreeRoller.class, EntityDataSerializers.INT);
    /** Ticks it stays curled after a hit. */
    public static final int ROLL_TIME = 80;
    /** Speed factor while rolling. */
    public static final float ROLL_SPEED = 2.4F;

    public ScreeRoller(EntityType<? extends ScreeRoller> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ARMOR, 4.0);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(UranusModule.MIRANDAN_GROUND) || reason != MobSpawnType.NATURAL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLLED, 0);
    }

    public boolean isRolled() {
        return entityData.get(ROLLED) > 0;
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * (isRolled() ? ROLL_SPEED : 1F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(Items.COAL), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isRolled()) {
            entityData.set(ROLLED, entityData.get(ROLLED) - 1);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean was = isRolled();
        boolean hit = super.hurt(source, was ? amount * 0.3F : amount);
        if (hit && !level().isClientSide) {
            if (!was) {
                playSound(SoundEvents.ARMADILLO_ROLL, 1F, 0.8F);
            }
            entityData.set(ROLLED, ROLL_TIME);
        }
        return hit;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.COAL);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return UranusEntities.SCREE_ROLLER.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rolled", entityData.get(ROLLED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLLED, tag.getInt("Rolled"));
    }
}
