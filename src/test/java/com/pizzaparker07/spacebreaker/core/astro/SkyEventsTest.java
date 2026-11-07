package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.AU;
import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

/** Planet positions in Earth's sky on real dates: conjunctions, oppositions and transits. */
class SkyEventsTest {
    private static final BodySystem SYS = SolarSystemData.system();

    static Vec3 fromEarth(String id, double days) {
        double t = days * DAY;
        Vec3 target = id.equals("spacebreaker:sun") ? Vec3.ZERO : SYS.position(SYS.require(id), t);
        return target.sub(SYS.position(SYS.require("spacebreaker:earth"), t));
    }

    static double separation(String a, String b, double days) {
        Vec3 u = fromEarth(a, days).normalize(), v = fromEarth(b, days).normalize();
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, u.dot(v)))));
    }

    /** {smallest separation, day} within ±window days, hourly. */
    static double[] closest(String a, String b, double day, double window) {
        double best = 1e9, at = day;
        for (double d = day - window; d <= day + window; d += 1.0 / 24) {
            double s = separation(a, b, d);
            if (s < best) {
                best = s;
                at = d;
            }
        }
        return new double[]{best, at};
    }

    @Test
    void greatConjunction2020() {
        // Jupiter and Saturn 0.1° apart on 2020-12-21
        double[] c = closest("spacebreaker:jupiter", "spacebreaker:saturn", 7660.25, 15);
        assertTrue(c[0] < 0.5, "separation " + c[0]);
        assertEquals(7660.25, c[1], 4, "date");
    }

    @Test
    void marsOpposition2003() {
        // closest approach in 60 000 years: 0.3727 AU on 2003-08-27
        double best = 1e9, at = 0;
        for (double d = 1325; d <= 1345; d += 1.0 / 24) {
            double r = fromEarth("spacebreaker:mars", d).length() / AU;
            if (r < best) {
                best = r;
                at = d;
            }
        }
        assertEquals(0.3727, best, 0.005, "distance");
        assertEquals(1334.9, at, 2, "date");
        // opposite the Sun in longitude; 6.6° south of the ecliptic (near perihelion, below Mars's node line)
        Vec3 mars = fromEarth("spacebreaker:mars", 1335.24), sun = fromEarth("spacebreaker:sun", 1335.24);
        double dLon = Math.toDegrees(Math.atan2(mars.y(), mars.x()) - Math.atan2(sun.y(), sun.x()));
        assertEquals(180, ((dLon % 360) + 360) % 360, 2, "opposition in longitude");
        assertEquals(-6.6, Math.toDegrees(Math.asin(mars.normalize().z())), 1.0, "ecliptic latitude");
    }

    @Test
    void transitsAcrossTheSun() {
        // the Sun's disc is 0.27° in radius: a transit passes inside it
        double[][] transits = {{1619.8472, 0}, {4539.5618, 0}, {7254.1389, 1}}; // Venus 2004, Venus 2012, Mercury 2019
        for (double[] tr : transits) {
            String planet = tr[1] == 0 ? "spacebreaker:venus" : "spacebreaker:mercury";
            double[] c = closest(planet, "spacebreaker:sun", tr[0], 2);
            assertTrue(c[0] < 0.27, planet + " transit near day " + tr[0] + ": " + c[0] + "°");
            assertEquals(tr[0], c[1], 0.5, planet + " transit time");
        }
    }
}
