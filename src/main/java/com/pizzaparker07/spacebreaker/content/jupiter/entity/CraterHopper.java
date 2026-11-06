package com.pizzaparker07.spacebreaker.content.jupiter.entity;

import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Crater Hopper: a shy, long-legged hopper of Callisto's frosty crater rims. It travels in great bounds - in Callisto's
 * weak gravity one push of its back legs carries it ten blocks - and licks the bright frost off the knobs on the rims.
 * Harmless; it bounds away from anyone who comes close.
 */
public class CraterHopper extends PathfinderMob {
    private int hopCooldown;

    public CraterHopper(EntityType<? extends CraterHopper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.JUMP_STRENGTH, 0.5);
    }

    public static boolean checkSpawn(EntityType<? extends PathfinderMob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos).isAir() && (level.getBlockState(pos.below()).is(JupiterModule.JOVIAN_GROUND) || reason != MobSpawnType.NATURAL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8F, 1.2, 1.5));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Moves in bounds: whenever it walks on the ground, it pushes off into a long, low leap. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isNoAi()) {
            return;
        }
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (onGround() && hopCooldown == 0 && !getNavigation().isDone()) {
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            setDeltaMovement(look.x * 0.45, 0.42, look.z * 0.45);
            hasImpulse = true;
            hopCooldown = 12;
        }
    }

    /** True while airborne in a leap (client animation). */
    public boolean leaping() {
        return !onGround();
    }
}
