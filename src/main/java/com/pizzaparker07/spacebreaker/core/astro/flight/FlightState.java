package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.function.BiFunction;

/**
 * A craft's translational state in a scene frame (real metres and m/s) with its mass, and a symplectic integrator
 * step (velocity Verlet / leapfrog), which keeps orbits from drifting over long flights.
 */
public record FlightState(Vec3 position, Vec3 velocity, double mass) {

    /**
     * Advances by dt.
     *
     * @param gravity  acceleration at a position and time (e.g. {@link Gravity#acceleration})
     * @param thrust   thrust force, N, held constant over the step (zero to coast)
     * @param massFlow propellant flow, kg/s, while thrusting
     * @param t        time at the start of the step
     */
    public FlightState step(BiFunction<Vec3, Double, Vec3> gravity, Vec3 thrust, double massFlow, double t, double dt) {
        double m1 = Math.max(mass - massFlow * dt, 1e-9);
        double mMid = 0.5 * (mass + m1);
        Vec3 aThrust = thrust.scale(1 / mMid);
        Vec3 a0 = gravity.apply(position, t).add(aThrust);
        Vec3 vHalf = velocity.add(a0.scale(0.5 * dt));
        Vec3 p1 = position.add(vHalf.scale(dt));
        Vec3 a1 = gravity.apply(p1, t + dt).add(aThrust);
        Vec3 v1 = vHalf.add(a1.scale(0.5 * dt));
        return new FlightState(p1, v1, m1);
    }

    /** Several equal substeps (for a game tick that covers a long simulated time). */
    public FlightState advance(BiFunction<Vec3, Double, Vec3> gravity, Vec3 thrust, double massFlow, double t, double dt, int substeps) {
        FlightState s = this;
        double h = dt / substeps;
        for (int i = 0; i < substeps; i++) {
            s = s.step(gravity, thrust, massFlow, t + i * h, h);
        }
        return s;
    }

    /**
     * Velocity after the optional "flight assist": damps the velocity relative to {@code reference} by at most
     * {@code maxDv} this step; returns the new velocity (the Δv used is the change).
     */
    public static Vec3 assist(Vec3 velocity, Vec3 reference, double maxDv) {
        Vec3 rel = velocity.sub(reference);
        double s = rel.length();
        return s <= maxDv ? reference : velocity.sub(rel.scale(maxDv / s));
    }
}
