package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Moons orbit in their planet's equator, not in the ecliptic: planets with a spin pole in the data put their moons'
 * elements in that frame, so Uranus's moons (98° tilt) and Saturn's are eclipsed only in the years around equinox.
 */
class MoonFramesTest {
    private static final BodySystem SYS = SolarSystemData.system();

    private static Vec3 orbitNormal(AstroBody b) {
        StateVector s = b.orbit().stateAt(0);
        return s.position().cross(s.velocity()).normalize();
    }

    @Test
    void polesMatchTheAxialTilts() {
        for (String n : new String[]{"mars", "jupiter", "saturn", "uranus", "neptune", "pluto"}) {
            AstroBody planet = SYS.require("spacebreaker:" + n);
            Map<String, Object> rot = cast(SolarSystemData.json(SolarSystemData.DIR.resolve(n + ".json")).get("rotation"));
            Vec3 pole = Frames.eclipticFromEquatorial((Double) rot.get("pole_ra_deg"), (Double) rot.get("pole_dec_deg"));
            double tilt = Math.toDegrees(Math.acos(pole.dot(orbitNormal(planet))));
            assertEquals((Double) rot.get("axial_tilt_deg"), tilt, 3.0, n + " obliquity from its pole");
        }
        // Earth's pole is the ecliptic pole tilted by the obliquity
        assertEquals(Frames.OBLIQUITY_J2000_DEG, Math.toDegrees(Math.acos(Frames.eclipticFromEquatorial(0, 90).z())), 1e-9);
    }

    @Test
    void moonsFollowTheirPlanetsEquator() {
        AstroBody uranus = SYS.require("spacebreaker:uranus");
        Vec3 pole = Frames.eclipticFromEquatorial(77.311, 15.175);
        for (String m : new String[]{"miranda", "ariel", "umbriel", "titania", "oberon"}) {
            AstroBody moon = SYS.require("spacebreaker:" + m);
            assertNotNull(moon.orbit().referencePole(), m);
            double i = Math.toDegrees(Math.acos(orbitNormal(moon).dot(pole)));
            assertEquals(moon.orbit().inclination(), Math.toRadians(i), 1e-6, m + " inclination is to Uranus's equator");
            assertTrue(Math.toDegrees(Math.acos(orbitNormal(moon).z())) > 90, m + " is retrograde to the ecliptic, like Uranus's spin");
        }
        assertNotNull(uranus.orbit());
        // Triton stays retrograde to Neptune's spin
        Vec3 np = Frames.eclipticFromEquatorial(299.36, 43.46);
        assertTrue(orbitNormal(SYS.require("spacebreaker:triton")).dot(np) < -0.8);
        // Earth has no pole in the data: the Moon stays referred to the ecliptic
        assertNull(SYS.require("spacebreaker:moon").orbit().referencePole());
    }

    /** Whether the moon passes through its planet's cylindrical shadow during [startDay, startDay + days). */
    private static boolean eclipsed(String moonId, double startDay, double days) {
        AstroBody moon = SYS.require(moonId), planet = SYS.parent(moon).orElseThrow();
        for (double d = startDay; d < startDay + days; d += 1.0 / 48) {
            double t = d * DAY;
            Vec3 p = SYS.position(planet, t);
            if (Eclipses.inCylindricalShadow(SYS.position(moon, t).sub(p), p.scale(-1).normalize(), planet.radius())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void uranianMoonsAreEclipsedOnlyNearEquinox() {
        double equinox2007 = 2898.0, solstice2028 = 10350.0; // 2007-12-07 and 2028 (days from J2000)
        for (String m : new String[]{"miranda", "ariel", "titania", "oberon"}) {
            assertTrue(eclipsed("spacebreaker:" + m, equinox2007 - 400, 800), m + " is eclipsed around the 2007 equinox");
            assertFalse(eclipsed("spacebreaker:" + m, solstice2028, 30), m + " is in sunlight at the 2028 solstice");
        }
    }

    @Test
    void titanIsEclipsedOnlyNearSaturnsEquinox() {
        assertTrue(eclipsed("spacebreaker:titan", 3509 - 300, 600), "Titan around the 2009 equinox");
        assertFalse(eclipsed("spacebreaker:titan", 6352, 32), "Titan at the 2017 solstice");
        // Phobos's shadow cone is ±21° wide but Mars is tilted 25°: no eclipses near the solstice of
        // January 2000 (Ls ≈ 270°), eclipses every orbit half a Mars season later
        assertFalse(eclipsed("spacebreaker:phobos", 0, 1));
        assertTrue(eclipsed("spacebreaker:phobos", 150, 1));
        assertTrue(eclipsed("spacebreaker:io", 0, 2));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return (Map<String, Object>) o;
    }
}
