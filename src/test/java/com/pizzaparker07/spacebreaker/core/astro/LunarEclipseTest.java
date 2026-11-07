package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real total lunar eclipses from the body data: the Moon passes through Earth's umbra within hours of the
 * observed time. This needs the Moon's secular node regression and apsidal precession; with fixed elements
 * the eclipse seasons drift away from J2000.
 */
class LunarEclipseTest {
    private static final BodySystem SYS = SolarSystemData.system();
    private static final AstroBody EARTH = SYS.require("spacebreaker:earth"), MOON = SYS.require("spacebreaker:moon");
    /** Umbra edge plus the Moon's own radius, seen from Earth (≈ 0.72° + 0.26°). */
    private static final double UMBRAL_CONTACT_DEG = 0.98;

    /** Angle between the Moon and the anti-Sun point seen from Earth, degrees. */
    static double separation(double days) {
        double t = days * DAY;
        Vec3 earth = SYS.position(EARTH, t);
        Vec3 moon = SYS.position(MOON, t).sub(earth);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, moon.normalize().dot(earth.normalize())))));
    }

    /** Time of the smallest separation within ±window days of a guess, and that separation. */
    static double[] closest(double guessDays, double windowDays) {
        double best = Double.MAX_VALUE, at = guessDays;
        for (double d = guessDays - windowDays; d <= guessDays + windowDays; d += 1.0 / 1440) {
            double s = separation(d);
            if (s < best) {
                best = s;
                at = d;
            }
        }
        return new double[]{at, best};
    }

    @Test
    void moonOrbitPrecesses() {
        OrbitalElements o = MOON.orbit();
        assertTrue(o.precessing());
        assertEquals(-19.34, Math.toDegrees(o.nodeRate()) * AstroConstants.YEAR, 0.01, "node regresses in 18.6 years");
        double apsis = Math.toDegrees(o.nodeRate() + o.periapsisArgRate()) * AstroConstants.YEAR;
        assertEquals(360 / 8.85, apsis, 0.1, "line of apsides advances in 8.85 years");
        // the mean longitude still advances at the sidereal rate; the anomaly at the anomalistic rate
        double sidereal = 27.321662 * DAY, anomalistic = 27.554550 * DAY;
        assertEquals(Kepler.TWO_PI / sidereal, o.meanMotion(), 1e-12);
        assertEquals(Kepler.TWO_PI / anomalistic, o.meanMotion() - o.nodeRate() - o.periapsisArgRate(), 1e-11);
        // a fixed orbit is unchanged by the new code
        OrbitalElements fixed = new OrbitalElements(o.mu(), o.semiMajorAxis(), o.eccentricity(), o.inclination(),
                o.longitudeAscendingNode(), o.argumentOfPeriapsis(), o.meanAnomalyAtEpoch());
        assertFalse(fixed.precessing());
        assertEquals(fixed.longitudeAscendingNode(), fixed.longitudeAscendingNode(1e9));
    }

    @Test
    void totalLunarEclipses() {
        double[][] eclipses = {
                {19.6972},   // 2000-01-21 04:44 UTC
                {4006.8451}, // 2010-12-21 08:17
                {5748.6167}, // 2015-09-28 02:48
                {8170.6743}, // 2022-05-16 04:11
                {8346.9576}, // 2022-11-08 10:59
                {9203.7910}, // 2025-03-14 06:59
                {9381.2583}, // 2025-09-07 18:12
        };
        for (double[] e : eclipses) {
            double[] c = closest(e[0], 1.0);
            assertTrue(c[1] < UMBRAL_CONTACT_DEG, "eclipse near day " + e[0] + ": closest " + c[1] + "°");
            assertEquals(e[0], c[0], 4.0 / 24, "eclipse time near day " + e[0]);
        }
    }

    @Test
    void fullMoonsAwayFromTheNodesAreNotEclipsed() {
        // 2022-08-12 01:36 UTC: a full moon between the two 2022 eclipse seasons
        double[] c = closest(8258.5667, 1.0);
        assertTrue(c[1] > 2.0, "no eclipse in August 2022: " + c[1] + "°");
    }
}
