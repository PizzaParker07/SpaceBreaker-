package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
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
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hollow Snail: a slow, harmless grazer of Mercury's bright hollows. It rasps the sulfide crust off hollow deposits
 * and leaves little lumps of sulfur behind, so a herd near a hollow is a small sulfur supply. Its pale shell reflects
 * the fierce sunlight. Lured and bred with sulfur.
 */
public class HollowSnail extends Animal {
    private static final Ingredient FOOD = Ingredient.of(VenusModule.SULFUR.get());
    /** Ticks between two sulfur lumps while grazing on a hollow deposit. */
    public static final int GRAZE_TICKS = 2400;
    private int graze;

    public HollowSnail(EntityType<? extends HollowSnail> type, Level level) {
        super(type, level);
        this.graze = 600 + (int) (Math.random() * GRAZE_TICKS);
    }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.ARMOR, 8.0);
    }

    public static boolean checkSpawn(EntityType<? extends Animal> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(MercuryModule.HOLLOW_DEPOSIT.get()) || below.is(MercuryModule.MERCURY_REGOLITH.get()) || reason != MobSpawnType.NATURAL)
                && level.getBlockState(pos).isAir();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, FOOD, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
    }

    /** True while it sits on a hollow deposit, rasping at it. */
    public boolean onHollow() {
        return level().getBlockState(blockPosition().below()).is(MercuryModule.HOLLOW_DEPOSIT.get());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && !isBaby() && isAlive() && onHollow() && --graze <= 0) {
            graze = GRAZE_TICKS / 2 + random.nextInt(GRAZE_TICKS);
            spawnAtLocation(new ItemStack(VenusModule.SULFUR.get()));
        }
    }

    /** For tests: make the next graze happen now. */
    public void grazeSoon() {
        graze = 1;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return FOOD.test(stack);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return MercuryEntities.HOLLOW_SNAIL.get().create(level);
    }
}
