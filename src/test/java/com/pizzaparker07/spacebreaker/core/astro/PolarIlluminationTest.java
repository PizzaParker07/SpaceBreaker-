package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolarIlluminationTest {
    @SuppressWarnings("unchecked")
    private static PolarIllumination p(String name) {
        Map<String, Object> rot = (Map<String, Object>) SolarSystemData.json(SolarSystemData.DIR.resolve(name + ".json")).get("rotation");
        double tilt = rot.containsKey("axial_tilt_deg") ? (Double) rot.get("axial_tilt_deg") : 0;
        return PolarIllumination.of(SolarSystemData.system(), "spacebreaker:" + name, tilt);
    }

    @Test
    void geometry() {
        assertEquals(82.23, PolarIllumination.effectiveObliquity(97.77), 1e-9, "Uranus lies on its side");
        assertEquals(60.49, PolarIllumination.effectiveObliquity(119.51), 1e-9);
        assertEquals(23.44, PolarIllumination.maxSunElevation(90, 23.44), 1e-9, "midnight sun at the pole reaches the tilt");
        assertEquals(90, PolarIllumination.maxSunElevation(10, 23.44), 1e-9);
        assertEquals(21.8, PolarIllumination.rimAngle(0.2), 0.05);
        assertTrue(PolarIllumination.permanentlyShadowed(88, 1.54, 0.1, 0.27));
        assertFalse(PolarIllumination.permanentlyShadowed(70, 1.54, 0.1, 0.27));
        assertFalse(PolarIllumination.permanentlyShadowed(89, 25.19, 0.2, 0.18), "Mars' tilt lights every crater floor");
    }

    @Test
    void theMoonAndMercuryKeepIceInPolarCraters() {
        PolarIllumination moon = p("moon");
        assertEquals(80.5, moon.psrLatitudeDeg(), 1.0, "lunar cold traps start near 80°");
        assertEquals(0.0008, moon.psrAreaFraction(), 0.0004, "≈ 31 000 km² ≈ 0.08 % of the surface");
        assertEquals(40, moon.coldTrapTempK(), 15, "LRO measures 25–60 K");
        assertTrue(moon.iceStable());
        assertFalse(moon.polarDayNight());
        PolarIllumination mercury = p("mercury");
        assertEquals(79.4, mercury.psrLatitudeDeg(), 1.0);
        assertTrue(mercury.coldTrapTempK() > 45 && mercury.coldTrapTempK() < 100, "Mercury cold traps ≈ 50–100 K: " + mercury.coldTrapTempK());
        assertTrue(mercury.iceStable(), "MESSENGER saw ice in Mercury's polar craters");
        PolarIllumination ceres = p("ceres");
        assertTrue(ceres.iceStable(), "Dawn found ice in Ceres' shadowed craters");
        assertTrue(ceres.psrLatitudeDeg() > 80);
    }

    @Test
    void tiltedOrAiryWorldsHaveNoColdTraps() {
        for (String n : new String[]{"mars", "earth", "pluto"}) {
            PolarIllumination x = p(n);
            assertEquals(90, x.psrLatitudeDeg(), 1e-9, n);
            assertEquals(0, x.psrAreaFraction(), 1e-12, n);
            assertFalse(x.iceStable(), n);
            assertTrue(x.polarDayNight(), n + " has polar day and night");
        }
        assertFalse(p("venus").iceStable(), "thick hot air");
        assertFalse(p("titan").iceStable(), "air carries heat into every crater");
    }
}
