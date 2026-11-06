package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Impulsive transfer maths: Hohmann, bi-elliptic, patched-conic departure and capture, synodic periods,
 * phase angles and launch windows. All inputs and outputs in SI units (m, m/s, s, rad).
 */
public final class Transfers {

    /** Result of a two- or three-burn transfer between circular coplanar orbits. */
    public record Transfer(double dv1, double dv2, double dv3, double time) {
        public double total() {
            return dv1 + dv2 + dv3;
        }
    }

    /** Hohmann transfer between circular orbits of radius r1 and r2 around mu. Burns are magnitudes. */
    public static Transfer hohmann(double mu, double r1, double r2) {
        double at = (r1 + r2) / 2;
        double dv1 = Math.abs(Kepler.visViva(mu, r1, at) - Kepler.circularSpeed(mu, r1));
        double dv2 = Math.abs(Kepler.circularSpeed(mu, r2) - Kepler.visViva(mu, r2, at));
        return new Transfer(dv1, dv2, 0, Math.PI * Math.sqrt(at * at * at / mu));
    }

    /** Bi-elliptic transfer from r1 to r2 via an intermediate apoapsis rb. */
    public static Transfer biElliptic(double mu, double r1, double r2, double rb) {
        double a1 = (r1 + rb) / 2;
        double a2 = (r2 + rb) / 2;
        double dv1 = Kepler.visViva(mu, r1, a1) - Kepler.circularSpeed(mu, r1);
        double dv2 = Math.abs(Kepler.visViva(mu, rb, a2) - Kepler.visViva(mu, rb, a1));
        double dv3 = Math.abs(Kepler.visViva(mu, r2, a2) - Kepler.circularSpeed(mu, r2));
        double t = Math.PI * (Math.sqrt(a1 * a1 * a1 / mu) + Math.sqrt(a2 * a2 * a2 / mu));
        return new Transfer(Math.abs(dv1), dv2, dv3, t);
    }

    /** Hyperbolic excess speeds (departure, arrival) of a Hohmann transfer between two circular orbits. */
    public static double[] hohmannExcessSpeeds(double mu, double r1, double r2) {
        Transfer t = hohmann(mu, r1, r2);
        return new double[]{t.dv1(), t.dv2()};
    }

    /**
     * Δv to leave a circular parking orbit of radius rPark around a body (mu) with hyperbolic excess speed vInf,
     * burning at periapsis (Oberth effect).
     */
    public static double departure(double mu, double rPark, double vInf) {
        return Math.sqrt(vInf * vInf + 2 * mu / rPark) - Math.sqrt(mu / rPark);
    }

    /** Δv to capture from a hyperbolic approach (vInf) into a circular orbit of radius rOrbit. */
    public static double captureCircular(double mu, double rOrbit, double vInf) {
        return departure(mu, rOrbit, vInf);
    }

    /** Δv to capture into an elliptical orbit with periapsis rp and apoapsis ra (burn at periapsis). */
    public static double captureElliptic(double mu, double rp, double ra, double vInf) {
        double a = (rp + ra) / 2;
        return Math.sqrt(vInf * vInf + 2 * mu / rp) - Kepler.visViva(mu, rp, a);
    }

    /**
     * Capture with aerobraking: the atmosphere removes the approach energy and lowers apoapsis to the
     * target circular orbit; only the periapsis-raise burn at apoapsis remains (from the top of the
     * atmosphere rAtm up to rOrbit).
     */
    public static double captureAerobrake(double mu, double rOrbit, double rAtm) {
        if (rAtm >= rOrbit) {
            return 0;
        }
        double a = (rOrbit + rAtm) / 2;
        return Kepler.circularSpeed(mu, rOrbit) - Kepler.visViva(mu, rOrbit, a);
    }

    /** Δv from a circular orbit of radius r to escape (parabolic). */
    public static double escapeFromCircular(double mu, double r) {
        return (Math.sqrt(2) - 1) * Kepler.circularSpeed(mu, r);
    }

    /** Synodic period of two orbits with periods t1 and t2 (same units in and out). */
    public static double synodicPeriod(double t1, double t2) {
        return 1 / Math.abs(1 / t1 - 1 / t2);
    }

    /**
     * Phase angle (target ahead of origin, rad, in (−π, π]) at departure for a Hohmann transfer from a circular
     * orbit r1 to r2 around mu. Negative means the target must trail the origin (outer-to-inner transfers).
     */
    public static double hohmannPhaseAngle(double mu, double r1, double r2) {
        double tTransfer = hohmann(mu, r1, r2).time();
        double n2 = Math.sqrt(mu / (r2 * r2 * r2));
        return Kepler.wrapSigned(Math.PI - n2 * tTransfer);
    }

    /**
     * Time (s) until the target is at the required phase angle ahead of the origin.
     *
     * @param currentPhase  current angle of the target ahead of the origin, rad
     * @param requiredPhase required angle, rad
     * @param n1            mean motion of the origin, rad/s
     * @param n2            mean motion of the target, rad/s
     */
    public static double timeToPhase(double currentPhase, double requiredPhase, double n1, double n2) {
        double rate = n2 - n1; // rate of change of the phase angle
        if (rate == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double diff = requiredPhase - currentPhase;
        double t = Kepler.wrap(rate > 0 ? diff : -diff) / Math.abs(rate);
        return t;
    }

    /** Burn time Δv/a for a constant acceleration, for estimates only. */
    public static double burnTimeAtAcceleration(double dv, double acceleration) {
        return dv / acceleration;
    }

    private Transfers() {
    }
}
