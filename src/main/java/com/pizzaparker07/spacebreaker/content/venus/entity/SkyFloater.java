package com.pizzaparker07.spacebreaker.content.venus.entity;

import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Base of the cloud-deck creatures: they float in the habitable band of the Venusian clouds, never sink into the
 * furnace below or climb out of the clouds, and drift with smooth steering.
 */
public abstract class SkyFloater extends FlyingMob {
    protected SkyFloater(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
        this.moveControl = new FloatControl(this);
    }

    /** Preferred speed while drifting (blocks per tick). */
    protected abstract double cruise();

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 96 * 96 && !hasCustomName();
    }

    /** Keeps a target height inside the cloud band. */
    protected static double clampHeight(double y) {
        return Mth.clamp(y, VenusClimate.CLOUD_BASE + 4, VenusClimate.CLOUD_TOP + 12);
    }

    /** Steering toward the wanted position, accelerating gently, like the ghast's. */
    protected static class FloatControl extends MoveControl {
        private final SkyFloater mob;

        FloatControl(SkyFloater mob) {
            super(mob);
            this.mob = mob;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            Vec3 d = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double len = d.length();
            if (len < 0.5) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.6));
                return;
            }
            Vec3 want = d.scale(Math.min(1, speedModifier) * mob.cruise() / len);
            mob.setDeltaMovement(mob.getDeltaMovement().add(want.subtract(mob.getDeltaMovement()).scale(0.08)));
            Vec3 m = mob.getDeltaMovement();
            if (m.horizontalDistanceSqr() > 1e-4) {
                float yaw = (float) (Mth.atan2(m.z, m.x) * Mth.RAD_TO_DEG) - 90F;
                mob.setYRot(Mth.approachDegrees(mob.getYRot(), yaw, 6F));
                mob.yBodyRot = mob.getYRot();
            }
        }
    }

    /** Wander to a random nearby point in clear air. */
    protected static class DriftGoal extends Goal {
        private final SkyFloater mob;

        DriftGoal(SkyFloater mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl c = mob.getMoveControl();
            if (!c.hasWanted()) {
                return true;
            }
            double dx = c.getWantedX() - mob.getX();
            double dy = c.getWantedY() - mob.getY();
            double dz = c.getWantedZ() - mob.getZ();
            double d = dx * dx + dy * dy + dz * dz;
            return d < 1 || d > 3600;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            var r = mob.getRandom();
            for (int i = 0; i < 6; i++) {
                double x = mob.getX() + (r.nextFloat() * 2 - 1) * 20;
                double y = clampHeight(mob.getY() + (r.nextFloat() * 2 - 1) * 6);
                double z = mob.getZ() + (r.nextFloat() * 2 - 1) * 20;
                if (mob.level().noCollision(mob, mob.getBoundingBox().move(x - mob.getX(), y - mob.getY(), z - mob.getZ()))) {
                    mob.getMoveControl().setWantedPosition(x, y, z, 1.0);
                    return;
                }
            }
        }
    }

    /** True if a floater of this size fits at a spot of open air. */
    public static boolean openAir(Level level, BlockPos pos, double half) {
        return level.noCollision(new AABB(pos).inflate(half));
    }
}
