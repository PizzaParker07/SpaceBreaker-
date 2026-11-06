package com.pizzaparker07.spacebreaker.content.saturn.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Geyser Drifter: a small, soft, bell-shaped floater of Enceladus' south pole. In a hundredth of a g it rides the
 * plumes up from the tiger stripes, feeding on the frozen ocean spray, and sinks slowly back down between eruptions.
 * It trails four feeding threads and glows a faint blue. Harmless.
 */
public class GeyserDrifter extends PathfinderMob {
    public GeyserDrifter(EntityType<? extends GeyserDrifter> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.12)
                .add(Attributes.MOVEMENT_SPEED, 0.08);
    }

    public static boolean checkSpawn(EntityType<? extends PathfinderMob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos).isAir();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Drift());
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** Pulse phase for the bell animation. */
    public float pulse(float partialTick) {
        return Mth.sin((tickCount + partialTick) * 0.15F);
    }

    /** Wanders slowly a few blocks above the ground, sinking and rising with each pulse of its bell. */
    final class Drift extends Goal {
        private Vec3 target;

        Drift() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return target == null || distanceToSqr(target) < 2 || random.nextInt(80) == 0;
        }

        @Override
        public void start() {
            BlockPos ground = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    blockPosition().offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8));
            target = Vec3.atCenterOf(ground).add(0, 3 + random.nextInt(6), 0);
            getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
