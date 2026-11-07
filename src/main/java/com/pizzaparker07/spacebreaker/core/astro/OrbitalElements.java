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
 * @param nodeRate              secular dΩ/dt, rad/s (0 = fixed orbit; the Moon's node regresses once in 18.6 years)
 * @param periapsisArgRate      secular dω/dt, rad/s. The mean longitude keeps advancing at the mean motion,
 *                              so the mean anomaly advances at n − dΩ/dt − dω/dt (the anomalistic rate)
 */
public record OrbitalElements(double mu, double semiMajorAxis, double eccentricity, double inclination,
                              double longitudeAscendingNode, double argumentOfPeriapsis, double meanAnomalyAtEpoch,
                              double nodeRate, double periapsisArgRate) {

    /** Fixed (non-precessing) orbit. */
    public OrbitalElements(double mu, double semiMajorAxis, double eccentricity, double inclination,
                           double longitudeAscendingNode, double argumentOfPeriapsis, double meanAnomalyAtEpoch) {
        this(mu, semiMajorAxis, eccentricity, inclination, longitudeAscendingNode, argumentOfPeriapsis, meanAnomalyAtEpoch, 0, 0);
    }

    /** The same orbit with secular precession rates given in degrees per Julian year. */
    public OrbitalElements withSecularRates(double nodeDegPerYear, double periapsisArgDegPerYear) {
        double k = Math.toRadians(1) / AstroConstants.YEAR;
        return new OrbitalElements(mu, semiMajorAxis, eccentricity, inclination, longitudeAscendingNode, argumentOfPeriapsis,
                meanAnomalyAtEpoch, nodeDegPerYear * k, periapsisArgDegPerYear * k);
    }

    public boolean precessing() {
        return nodeRate != 0 || periapsisArgRate != 0;
    }

    /** Ω at time t, rad. */
    public double longitudeAscendingNode(double t) {
        return longitudeAscendingNode + nodeRate * t;
    }

    /** ω at time t, rad. */
    public double argumentOfPeriapsis(double t) {
        return argumentOfPeriapsis + periapsisArgRate * t;
    }

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
        double m = meanAnomalyAtEpoch + (meanMotion() - nodeRate - periapsisArgRate) * t;
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
        return Kepler.wrap(longitudeAscendingNode(t) + argumentOfPeriapsis(t) + meanAnomaly(t));
    }
}
