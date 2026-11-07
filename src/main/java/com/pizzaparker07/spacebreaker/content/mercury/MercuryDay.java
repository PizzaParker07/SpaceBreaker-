package com.pizzaparker07.spacebreaker.content.mercury;

import net.minecraft.util.Mth;

/**
 * The Mercurian day. Mercury turns three times on its axis for every two trips around the sun (a 3:2 spin-orbit
 * resonance), so one solar day lasts two Mercurian years, about 176 Earth days. Its orbit is very eccentric
 * (e = 0.206): near perihelion it swings round the sun faster than it spins, and for a few Earth days the sun stops in
 * the sky and moves backwards. Where perihelion falls at sunrise (the "warm pole" longitudes) the sun peeks over the
 * horizon, sinks back and rises a second time. This world sits at such a longitude. (The game's day is longer than
 * its night, so the second perihelion comes a little before sunset: there the sun only stalls low in the west.)
 * <p>
 * The game keeps its compressed day length; only the sun's motion within the day is bent: the evenly running (mean)
 * day fraction is mapped to the true one through Kepler's equation.
 */
public final class MercuryDay {
    /** Orbital eccentricity of Mercury. */
    public static final double E = 0.20563593;
    /** Orbits per solar day. */
    public static final double ORBITS_PER_DAY = 2.0;

    /**
     * Mean day fraction at which the sun crosses the horizon in the morning. The day clock follows vanilla's
     * convention (fraction 0 has the sun already a little up), so the true crossing comes slightly before 0.
     */
    public static final double SUNRISE = horizonFraction();
    /** Perihelion falls exactly at sunrise: this world lies at a "warm pole" longitude. */
    public static final double PERIHELION = SUNRISE;

    private MercuryDay() {
    }

    /** Solves vanilla's sky-angle curve for the morning horizon crossing (time of day 0.75). */
    private static double horizonFraction() {
        double lo = 0.5;
        double hi = 1.0;
        for (int i = 0; i < 60; i++) {
            double d0 = (lo + hi) / 2;
            double tod = (d0 * 2.0 + 0.5 - Math.cos(d0 * Math.PI) / 2.0) / 3.0;
            if (tod < 0.75) {
                lo = d0;
            } else {
                hi = d0;
            }
        }
        return (lo + hi) / 2 + 0.25 - 1.0;
    }

    /** Mean anomaly (radians) at a mean day fraction; two perihelia per day, the first at sunrise. */
    public static double meanAnomaly(double meanFraction) {
        return ORBITS_PER_DAY * 2 * Math.PI * (meanFraction - PERIHELION);
    }

    /** True anomaly (radians, -pi..pi) for a mean anomaly, by Kepler's equation. */
    public static double trueAnomaly(double meanAnomaly) {
        double m = Math.toRadians(Mth.wrapDegrees(Math.toDegrees(meanAnomaly)));
        double ecc = m;
        for (int i = 0; i < 8; i++) {
            ecc -= (ecc - E * Math.sin(ecc) - m) / (1 - E * Math.cos(ecc));
        }
        return 2 * Math.atan2(Math.sqrt(1 + E) * Math.sin(ecc / 2), Math.sqrt(1 - E) * Math.cos(ecc / 2));
    }

    /**
     * Where the sun really is: the mean day fraction minus the equation of centre. The sun's hour angle is the spin
     * angle minus the orbital longitude; any lead of the true orbital longitude over the mean holds the sun back.
     */
    public static double trueFraction(double meanFraction) {
        double m = meanAnomaly(meanFraction);
        double lead = Math.toRadians(Mth.wrapDegrees(Math.toDegrees(trueAnomaly(m) - m)));
        return meanFraction - lead / (2 * Math.PI);
    }

    /** Distance to the sun relative to the mean (semi-major axis): 0.79 at perihelion, 1.21 at aphelion. */
    public static double distance(double meanFraction) {
        double nu = trueAnomaly(meanAnomaly(meanFraction));
        return (1 - E * E) / (1 + E * Math.cos(nu));
    }
}
