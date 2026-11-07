package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

class BodyLookTest {
    private static final BodySystem SYS = SolarSystemData.system();

    @SuppressWarnings("unchecked")
    static BodyLook look(String name, Vec3 observer, double days) {
        Map<String, Object> rot = (Map<String, Object>) SolarSystemData.json(SolarSystemData.DIR.resolve(name + ".json")).get("rotation");
        Vec3 pole = rot.get("pole_ra_deg") == null ? null
                : Frames.eclipticFromEquatorial((Double) rot.get("pole_ra_deg"), (Double) rot.get("pole_dec_deg"));
        double tilt = rot.get("axial_tilt_deg") == null ? 0 : (Double) rot.get("axial_tilt_deg");
        return BodyLook.of(SYS, SYS.require("spacebreaker:" + name), pole, tilt, observer, days * DAY);
    }

    static Vec3 earth(double days) {
        return SYS.position(SYS.require("spacebreaker:earth"), days * DAY);
    }

    @Test
    void tiltsAndSpinDirections() {
        Vec3 far = new Vec3(0, 0, 1e15);
        String[] names = {"mercury", "venus", "earth", "mars", "jupiter", "saturn", "uranus", "neptune"};
        double[] tilt = {0.03, 177.4, 23.44, 25.19, 3.13, 26.73, 97.77, 28.32};
        for (int i = 0; i < names.length; i++) {
            BodyLook l = look(names[i], far, 0);
            assertEquals(tilt[i], l.axialTiltDeg(), 3, names[i] + " tilt");
            assertEquals(tilt[i] > 90, l.retrograde(), names[i] + " spin direction");
            assertEquals(1, l.spinAxis().length(), 1e-12);
        }
        // sidereal rotation from the solar day in the data
        assertEquals(23.934, look("earth", far, 0).siderealRotationSeconds() / 3600, 0.01);
        assertEquals(243.0, look("venus", far, 0).siderealRotationSeconds() / DAY, 1.0, "Venus turns once in 243 days, backwards");
        assertEquals(58.65, look("mercury", far, 0).siderealRotationSeconds() / DAY, 0.2);
        assertEquals(24.62, look("mars", far, 0).siderealRotationSeconds() / 3600, 0.02);
    }

    @Test
    void earthTurnsOncePerSiderealDay() {
        Vec3 far = new Vec3(0, 0, 1e15);
        BodyLook a = look("earth", far, 100), b = look("earth", far, 100 + 0.99727);
        assertEquals(0, a.primeMeridian().distance(b.primeMeridian()), 1e-3);
        // body frame round trip
        Vec3 v = new Vec3(0.3, -0.5, 0.8).normalize();
        assertEquals(0, a.toEcliptic(a.toBodyFixed(v)).distance(v), 1e-12);
        assertEquals(1, a.toBodyFixed(a.spinAxis()).z(), 1e-12);
    }

    @Test
    void moonPhasesAndLockedFace() {
        // full moon at the 2022-11-08 eclipse, new moon at the 2022-10-25 partial solar eclipse
        BodyLook full = look("moon", earth(8346.96), 8346.96), dark = look("moon", earth(8333.0), 8333.0);
        assertTrue(full.illuminatedFraction() > 0.99, "full " + full.illuminatedFraction());
        assertTrue(dark.illuminatedFraction() < 0.01, "new " + dark.illuminatedFraction());
        // tidally locked: the prime meridian always faces Earth (within the 6.7° tilt of the spin axis)
        for (double d : new double[]{0, 7, 14, 21, 3000}) {
            BodyLook l = look("moon", earth(d), d);
            Vec3 toEarth = earth(d).sub(SYS.position(SYS.require("spacebreaker:moon"), d * DAY)).normalize();
            assertTrue(l.primeMeridian().dot(toEarth) > Math.cos(Math.toRadians(8)), "near side faces Earth at day " + d);
        }
    }

    @Test
    void outerPlanetsAreAlwaysNearlyFull() {
        for (double d = 0; d < 4000; d += 37) {
            BodyLook j = look("jupiter", earth(d), d);
            assertTrue(Math.toDegrees(j.phaseAngleRad()) < 12.5, "Jupiter phase angle at day " + d);
        }
        // Uranus's spin axis is the data's pole, and the Sun lies near its equator only around 2007
        BodyLook u2007 = look("uranus", earth(2898), 2898), u2028 = look("uranus", earth(10350), 10350);
        assertTrue(Math.abs(u2007.sunInBodyFrame().z()) < 0.1, "equinox 2007");
        assertTrue(Math.abs(u2028.sunInBodyFrame().z()) > 0.9, "solstice 2028: Sun over a pole");
    }
}
