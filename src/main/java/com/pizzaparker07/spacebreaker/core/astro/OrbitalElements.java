package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Classical Keplerian elements around a central body with gravitational parameter {@code mu}.
 * Angles in radians, distances in metres. The reference plane is the parent's ecliptic/equatorial
 * frame used by the data (ecliptic J2000 for planets).
 *
 * @param mu                    gravitational parameter of the central body, m³/s²
 * @param semiMajorAxis         a, m (negative for hyperbolic orbits)
 * @param eccentricity          e
 * @param inclination           i, rad
 * @param longitudeAscendingNode Ω, rad
 * @param argumentOfPeriapsis   ω, rad
 * @param meanAnomalyAtEpoch    M₀ at t = 0 (J2000 for real bodies), rad
 */
public record OrbitalElements(double mu, double semiMajorAxis, double eccentricity, double inclination,
                              double longitudeAscendingNode, double argumentOfPeriapsis, double meanAnomalyAtEpoch) {

    public static OrbitalElements circular(double mu, double radius) {
        return new OrbitalElements(mu, radius, 0, 0, 0, 0, 0);
    }

    /** Mean motion n, rad/s. */
    public double meanMotion() {
        return Math.sqrt(mu / Math.abs(semiMajorAxis * semiMajorAxis * semiMajorAxis));
    }

    /** Orbital period, s (infinite for open orbits). */
    public double period() {
        return eccentricity >= 1 ? Double.POSITIVE_INFINITY : Kepler.period(semiMajorAxis, mu);
    }

    public double periapsis() {
        return semiMajorAxis * (1 - eccentricity);
    }

    public double apoapsis() {
        return eccentricity >= 1 ? Double.POSITIVE_INFINITY : semiMajorAxis * (1 + eccentricity);
    }

    /** Mean anomaly at time t (s since epoch), wrapped to [0, 2π) for closed orbits. */
    public double meanAnomaly(double t) {
        double m = meanAnomalyAtEpoch + meanMotion() * t;
        return eccentricity < 1 ? Kepler.wrap(m) : m;
    }

    /** Position and velocity at time t (s since epoch). */
    public StateVector stateAt(double t) {
        return Kepler.stateFromElements(this, t);
    }

    /** Longitude of periapsis ϖ = Ω + ω. */
    public double longitudeOfPeriapsis() {
        return longitudeAscendingNode + argumentOfPeriapsis;
    }

    /** Mean longitude at time t, wrapped. */
    public double meanLongitude(double t) {
        return Kepler.wrap(longitudeOfPeriapsis() + meanAnomaly(t));
    }
}
