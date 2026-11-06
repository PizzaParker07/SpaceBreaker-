package com.pizzaparker07.spacebreaker.content.jupiter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Under-Ice Glow Serpent: a long, slender swimmer of Europa's dark ocean and its brine pockets. Bands of
 * bioluminescent spots ripple along its body - in water no sunlight has ever reached, light is how it finds its own kind.
 * Passive and shy: it slips away from anything bigger than itself.
 */
public class GlowSerpent extends WaterAnimal {
    public GlowSerpent(EntityType<? extends GlowSerpent> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.04F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder attributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0);
    }

    public static boolean checkSpawn(EntityType<? extends WaterAnimal> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6F, 1.2, 1.6));
        goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0, 20));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    /** Swim phase for the body wave (client animation). */
    public float swimPhase(float partialTick) {
        return (tickCount + partialTick) * (0.25F + (float) getDeltaMovement().length() * 3F);
    }
}
