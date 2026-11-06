package com.pizzaparker07.spacebreaker.content.pluto.entity;

import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Frost Tortoise: a slow, domed grazer of Charon's grey plains, its shell crusted with the same dark red frost as the
 * pole. Hit, it pulls head and legs into its shell and stays there a while; inside, it takes only a quarter of the
 * damage. Fed ammonia crystals, two tortoises breed.
 */
public class FrostTortoise extends Animal {
    private static final EntityDataAccessor<Integer> SHELL = SynchedEntityData.defineId(FrostTortoise.class, EntityDataSerializers.INT);
    /** Ticks it stays in its shell after a hit. */
    public static final int SHELL_TIME = 100;

    public FrostTortoise(EntityType<? extends FrostTortoise> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.11)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(PlutoModule.PLUTONIAN_GROUND) || reason != MobSpawnType.NATURAL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHELL, 0);
    }

    public boolean inShell() {
        return entityData.get(SHELL) > 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(PlutoModule.AMMONIA_CRYSTAL.get()), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !inShell() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F) {
            @Override
            public boolean canUse() {
                return !inShell() && super.canUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && inShell()) {
            getNavigation().stop();
            entityData.set(SHELL, entityData.get(SHELL) - 1);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasIn = inShell();
        boolean hit = super.hurt(source, wasIn ? amount * 0.25F : amount);
        if (hit && !level().isClientSide) {
            if (!wasIn) {
                playSound(SoundEvents.TURTLE_SHAMBLE, 1F, 0.6F);
            }
            entityData.set(SHELL, SHELL_TIME);
        }
        return hit;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(PlutoModule.AMMONIA_CRYSTAL.get());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return PlutoEntities.FROST_TORTOISE.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shell", entityData.get(SHELL));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SHELL, tag.getInt("Shell"));
    }
}
