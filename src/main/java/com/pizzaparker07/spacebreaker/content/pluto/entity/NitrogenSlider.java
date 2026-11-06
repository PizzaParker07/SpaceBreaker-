package com.pizzaparker07.spacebreaker.content.pluto.entity;

import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Nitrogen Slider: a sleek, flat-bellied grazer of Pluto's great ice plain. On frost it shuffles along slowly on its
 * short flippers; on nitrogen ice it drops onto its belly and slides, gliding twice as fast over the near-frictionless
 * surface - which is also how it gets away when startled. It feeds on tholin.
 */
public class NitrogenSlider extends Animal {
    /** Speed factor while sliding on nitrogen ice. */
    public static final float SLIDE_BOOST = 2.2F;

    public NitrogenSlider(EntityType<? extends NitrogenSlider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(PlutoModule.PLUTONIAN_GROUND) || reason != MobSpawnType.NATURAL;
    }

    /** True while it lies on nitrogen ice and slides. */
    public boolean isSliding() {
        return onGround() && level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).is(PlutoModule.NITROGEN_ICE.get());
    }

    /** Multiplier on its walking speed: {@link #SLIDE_BOOST} while sliding, else 1. */
    public float slideFactor() {
        return isSliding() ? SLIDE_BOOST : 1F;
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * slideFactor();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(SaturnModule.THOLIN.get()), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
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
        return PlutoEntities.NITROGEN_SLIDER.get().create(level);
    }
}
