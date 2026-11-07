package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.RocketMath;
import com.pizzaparker07.spacebreaker.core.astro.StateVector;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * A planned burn in the craft's local orbital frame at a time: prograde (along the velocity), normal (along the
 * orbit's angular momentum) and radial (outwards, completing a right-handed frame).
 */
public record ManeuverNode(double time, double prograde, double normal, double radial) {

    public double totalDv() {
        return Math.sqrt(prograde * prograde + normal * normal + radial * radial);
    }

    /** The burn as a vector, for a state relative to the body it orbits. */
    public Vec3 deltaV(StateVector s) {
        Vec3 t = s.velocity().normalize();
        Vec3 n = s.position().cross(s.velocity()).normalize();
        Vec3 r = t.cross(n);
        return t.scale(prograde).add(n.scale(normal)).add(r.scale(radial));
    }

    /** The state right after an impulsive burn. */
    public StateVector apply(StateVector s) {
        return new StateVector(s.position(), s.velocity().add(deltaV(s)));
    }

    /** Burn duration for a craft of mass {@code mass} (kg) with {@code thrust} (N) at {@code isp} (s). */
    public double burnSeconds(double mass, double thrust, double isp) {
        return RocketMath.burnTimeForDeltaV(totalDv(), mass, thrust, isp);
    }

    /** When to start burning so the burn is centred on the node. */
    public double startTime(double mass, double thrust, double isp) {
        return time - 0.5 * burnSeconds(mass, thrust, isp);
    }
}
