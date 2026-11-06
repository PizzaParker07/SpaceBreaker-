package com.pizzaparker07.spacebreaker.content.jupiter.entity;

import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Sulfur Crab: a squat, heat-loving crab of Io that grazes the warm crusts around lava lakes and vents, its shell
 * glazed yellow and orange with sulfur. Territorial: it ignores you at a distance but snaps at anyone who walks right up
 * to it. Unharmed by heat and lava.
 */
public class SulfurCrab extends Monster {
    /** Attacks players that come this close. */
    public static final double TERRITORY = 4.5;

    public SulfurCrab(EntityType<? extends SulfurCrab> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        setPathfindingMalus(PathType.LAVA, 8F);
        setPathfindingMalus(PathType.DANGER_FIRE, 0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(JupiterModule.JOVIAN_GROUND) || reason != MobSpawnType.NATURAL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p.distanceToSqr(this) < TERRITORY * TERRITORY));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
