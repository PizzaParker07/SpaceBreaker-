package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Radiation from a burning nuclear thermal engine. Only while it burns: a cold reactor with fresh fuel is safe
 * to stand next to. The dose falls with the square of the distance from the reactor, and a shadow shield on
 * top of the reactor protects everything straight above it (the rest of the rocket and its crew).
 * Registered as an {@code Environment} radiation source, so the suit's dose system picks it up.
 */
public final class NuclearExhaust {
    /** Dose rate one metre from a burning reactor, mSv/day. */
    public static final double DOSE_AT_ONE_METRE = 500_000;
    /** Beyond this the reactor adds nothing worth counting, blocks. */
    public static final double REACH = 48;
    /** What the shadow shield lets through to points above the reactor. */
    public static final double SHADOW_SHIELD = 0.002;
    /** Half-angle of the cone the shadow shield covers, measured from straight up (cos of 35 degrees). */
    private static final double SHIELD_COS = Math.cos(Math.toRadians(35));

    private NuclearExhaust() {
    }

    /** Whether this rocket's working stage is a nuclear thermal stage that is burning right now. */
    public static boolean reactorHot(RocketEntity rocket) {
        if (!rocket.burning()) {
            return false;
        }
        RocketLayout layout = rocket.layout();
        int stage = rocket.activeStage();
        return stage < layout.stages().size() && layout.stages().get(stage).propellant() == Propellant.NUCLEAR;
    }

    /** Where the reactor sits: at the foot of what is left of the rocket. */
    public static Vec3 reactor(RocketEntity rocket) {
        return rocket.position().add(0, 0.5, 0);
    }

    /** Dose rate at a point from one rocket, mSv/day. */
    public static double dose(RocketEntity rocket, Vec3 at) {
        if (!reactorHot(rocket)) {
            return 0;
        }
        Vec3 d = at.subtract(reactor(rocket));
        double r2 = Math.max(1.0, d.lengthSqr());
        double dose = DOSE_AT_ONE_METRE / r2;
        if (d.y > 0 && d.y / Math.sqrt(r2) >= SHIELD_COS) {
            dose *= SHADOW_SHIELD;
        }
        return dose;
    }

    /** {@code Environment.RadiationSource}: the dose from every burning nuclear engine nearby. */
    public static double doseMsvDay(Level level, BlockPos pos) {
        Vec3 at = Vec3.atCenterOf(pos);
        double total = 0;
        for (RocketEntity rocket : level.getEntitiesOfClass(RocketEntity.class, new AABB(pos).inflate(REACH))) {
            total += dose(rocket, at);
        }
        return total;
    }
}
