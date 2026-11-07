package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Io, Europa and Ganymede sit in the Laplace resonance: λ₁ − 3λ₂ + 2λ₃ = 180°, always. Their J2000 phases (from
 * Meeus's mean longitudes) and periods in the data must keep it, so the moons line up in the sky as the real ones.
 */
class GalileanMoonsTest {
    private static final BodySystem SYS = SolarSystemData.system();

    /** Ecliptic longitude of a moon around its planet, degrees. */
    static double longitude(String id, double days) {
        AstroBody m = SYS.require(id);
        Vec3 r = SYS.relativePosition(m, days * DAY);
        return Math.toDegrees(Math.atan2(r.y(), r.x()));
    }

    static double wrap(double deg) {
        double d = deg % 360;
        return d < 0 ? d + 360 : d;
    }

    /** Mean longitude in the planet's equator frame, degrees (the resonance holds for mean longitudes). */
    static double meanLongitude(String id, double days) {
        return Math.toDegrees(SYS.require(id).orbit().meanLongitude(days * DAY));
    }

    @Test
    void laplaceResonance() {
        for (double days : new double[]{0, 1234.5, 9131.25, 36525}) {
            double l1 = meanLongitude("spacebreaker:io", days), l2 = meanLongitude("spacebreaker:europa", days),
                    l3 = meanLongitude("spacebreaker:ganymede", days);
            assertEquals(180, wrap(l1 - 3 * l2 + 2 * l3), 0.5, "Laplace resonance at day " + days);
        }
        // the resonance also locks the periods close to 1:2:4
        assertEquals(2.0, SYS.require("spacebreaker:europa").orbit().period() / SYS.require("spacebreaker:io").orbit().period(), 0.01);
        assertEquals(2.0, SYS.require("spacebreaker:ganymede").orbit().period() / SYS.require("spacebreaker:europa").orbit().period(), 0.02);
    }

    @Test
    void meeusLongitudesAtJ2000() {
        // Meeus, Astronomical Algorithms ch. 44, mean longitudes at J2000
        assertEquals(17.46, wrap(longitude("spacebreaker:io", 0)), 1.5);
        assertEquals(212.07, wrap(longitude("spacebreaker:europa", 0)), 1.5);
        assertEquals(219.37, wrap(longitude("spacebreaker:ganymede", 0)), 1.5);
        assertEquals(78.46, wrap(longitude("spacebreaker:callisto", 0)), 1.5);
    }
}
