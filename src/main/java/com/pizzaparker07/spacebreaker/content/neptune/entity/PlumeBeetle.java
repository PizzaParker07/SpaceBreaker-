package com.pizzaparker07.spacebreaker.content.neptune.entity;

import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Plume Beetle: a dark, round-shelled grazer of Triton's polar cap that lives off the dust the geysers rain down. When
 * a geyser throws it up, it spreads its wing cases and parachutes back down through the thin gas; a fall never hurts it.
 * Fed plume dust, two beetles breed.
 */
public class PlumeBeetle extends Animal {
    public PlumeBeetle(EntityType<? extends PlumeBeetle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ARMOR, 3.0);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(NeptuneModule.TRITON_GROUND) || reason != MobSpawnType.NATURAL;
    }

    /** True while it is airborne with its wing cases open (rising or drifting down). */
    public boolean isGliding() {
        return !onGround() && !isInWater() && getDeltaMovement().y < 0.6;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // wing cases spread: falls slowly
        if (!onGround() && getDeltaMovement().y < -0.08) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.7, 1));
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(NeptuneModule.PLUME_DUST.get()), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(NeptuneModule.PLUME_DUST.get().asItem());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return NeptuneEntities.PLUME_BEETLE.get().create(level);
    }
}
