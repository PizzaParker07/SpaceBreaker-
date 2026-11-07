package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.StateVector;

/**
 * A station's fixed circular orbit (altitude, inclination, phase at J2000) and the "return to the pocket next to it"
 * test: inside the orbit shell, close to the station, and slow relative to it.
 */
public final class StationRendezvous {
    /** Relative speed below which a craft returns to the station's orbit pocket, m/s. */
    public static final double RETURN_SPEED = 50;
    /** Distance within which it counts as next to the station, m. */
    public static final double RETURN_DISTANCE = 50_000;

    private StationRendezvous() {
    }

    /** Circular orbit around {@code body}: altitude (m), inclination (rad), phase = mean longitude at J2000 (rad). */
    public static OrbitalElements orbit(AstroBody body, double altitude, double inclination, double phase) {
        return new OrbitalElements(body.mu(), body.radius() + altitude, 0, inclination, 0, 0, phase);
    }

    public static StateVector stationState(OrbitalElements orbit, double t) {
        return orbit.stateAt(t);
    }

    public static double relativeSpeed(StateVector craft, OrbitalElements station, double t) {
        return craft.velocity().sub(station.stateAt(t).velocity()).length();
    }

    public static double distance(StateVector craft, OrbitalElements station, double t) {
        return craft.position().distance(station.stateAt(t).position());
    }

    /** Whether a craft (state relative to the body) should drop into the pocket next to this station. */
    public static boolean returnsToPocket(StateVector craft, OrbitalElements station, double t) {
        return distance(craft, station, t) <= RETURN_DISTANCE && relativeSpeed(craft, station, t) <= RETURN_SPEED;
    }
}
