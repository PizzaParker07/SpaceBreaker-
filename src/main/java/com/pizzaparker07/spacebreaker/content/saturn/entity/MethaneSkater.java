package com.pizzaparker07.spacebreaker.content.saturn.entity;

import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Methane Skater: a long-legged skimmer of Titan's lakes. Its feet spread its weight over the surface of the cold
 * liquid methane (low surface tension, but low gravity too), so it walks on the lakes as easily as on the shore,
 * grazing the organic film that rains out of the sky. Harmless; startled, it skates off across the lake.
 */
public class MethaneSkater extends Animal {
    public MethaneSkater(EntityType<? extends MethaneSkater> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos).isAir() && (level.getBlockState(pos.below()).is(SaturnModule.SATURNIAN_GROUND)
                || level.getFluidState(pos.below()).is(FluidsModule.TAG_LCH4) || reason != MobSpawnType.NATURAL);
    }

    /** Stands on liquid methane like a water strider on a pond. */
    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidsModule.TAG_LCH4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(SaturnModule.THOLIN.get());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return SaturnEntities.METHANE_SKATER.get().create(level);
    }
}
