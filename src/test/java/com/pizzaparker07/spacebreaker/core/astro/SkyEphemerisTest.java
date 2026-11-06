package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The sky from Earth on known dates, and from other worlds. */
class SkyEphemerisTest {
    private static final Instant J2000 = Instant.parse("2000-01-01T11:58:55.816Z");
    private static final String EARTH = "spacebreaker:earth";

    private static double t(String isoDate) {
        return Duration.between(J2000, Instant.parse(isoDate + "T00:00:00Z")).toMillis() / 1000.0;
    }

    private static SkyEphemeris.SkyObject fromEarth(String target, String date) {
        return SkyEphemeris.view(SolarSystemData.system(), EARTH, "spacebreaker:" + target, t(date));
    }

    private static double arcsec(double rad) {
        return Math.toDegrees(rad) * 3600;
    }

    @Test
    void theSunAndTheFullMoon() {
        SkyEphemeris.SkyObject sun = fromEarth("sun", "2024-03-20");
        assertEquals(-26.74, sun.apparentMagnitude(), 0.06);
        assertEquals(0.533, sun.angularDiameterDeg(), 0.012);
        assertTrue(sun.star());
        // brightest Moon over one lunation: full moon ≈ −12.7
        double best = 0, bestLit = 0;
        for (double d = 0; d < 30; d += 0.05) {
            SkyEphemeris.SkyObject m = SkyEphemeris.view(SolarSystemData.system(), EARTH, "spacebreaker:moon", t("2024-01-10") + d * 86400);
            if (m.apparentMagnitude() < best) {
                best = m.apparentMagnitude();
                bestLit = m.illuminated();
            }
        }
        assertEquals(-12.7, best, 0.4);
        assertTrue(bestLit > 0.99);
    }

    @Test
    void venusAtGreatestElongation2020() {
        SkyEphemeris.SkyObject v = fromEarth("venus", "2020-03-24");
        assertEquals(46.1, Math.toDegrees(v.elongation()), 2.0);
        assertEquals(-4.4, v.apparentMagnitude(), 0.15);
        assertEquals(0.5, v.illuminated(), 0.08, "half lit at greatest elongation");
        assertEquals(24.7, arcsec(v.angularDiameter()), 2.0);
    }

    @Test
    void venusThroughItsWholeCycle() {
        // superior conjunction (26 Mar 2021): small, full and far, still ≈ −3.9
        SkyEphemeris.SkyObject sup = fromEarth("venus", "2021-03-26");
        assertTrue(Math.toDegrees(sup.elongation()) < 3);
        assertEquals(-3.9, sup.apparentMagnitude(), 0.15);
        // greatest brilliancy (28 Apr 2020): a fat crescent at ≈ −4.7
        assertEquals(-4.7, fromEarth("venus", "2020-04-28").apparentMagnitude(), 0.2);
        // near inferior conjunction, ~12° from the Sun: a thin crescent but still brighter than Jupiter ever gets
        double t0 = t("2020-05-10");
        SkyEphemeris.SkyObject thin = null;
        for (double d = 0; d < 30; d += 0.25) {
            SkyEphemeris.SkyObject v = SkyEphemeris.view(SolarSystemData.system(), EARTH, "spacebreaker:venus", t0 + d * 86400);
            if (Math.abs(Math.toDegrees(v.elongation()) - 12) < 0.5) {
                thin = v;
                break;
            }
        }
        assertNotNull(thin, "Venus passes 12° from the Sun before inferior conjunction");
        assertTrue(thin.illuminated() < 0.1, "a thin crescent");
        assertTrue(thin.apparentMagnitude() < -3.5, "still dazzling: " + thin.apparentMagnitude());
        // over a full synodic cycle Venus never drops below −3.0 except right at inferior conjunction
        for (double d = 0; d < 584; d += 1) {
            SkyEphemeris.SkyObject v = SkyEphemeris.view(SolarSystemData.system(), EARTH, "spacebreaker:venus", t("2020-01-01") + d * 86400);
            if (Math.toDegrees(v.elongation()) > 6) {
                assertTrue(v.apparentMagnitude() < -3.0, "day " + d + ": " + v.apparentMagnitude());
            }
        }
    }

    @Test
    void jupiterAndMarsAtOpposition() {
        SkyEphemeris.SkyObject j = fromEarth("jupiter", "2023-11-03");
        assertTrue(Math.toDegrees(j.elongation()) > 174, "Jupiter opposite the Sun: " + Math.toDegrees(j.elongation()));
        assertEquals(-2.9, j.apparentMagnitude(), 0.3);
        assertEquals(49.5, arcsec(j.angularDiameter()), 2.0);
        SkyEphemeris.SkyObject m = fromEarth("mars", "2020-10-13");
        assertTrue(Math.toDegrees(m.elongation()) > 170, "Mars at opposition: " + Math.toDegrees(m.elongation()));
        assertEquals(-2.6, m.apparentMagnitude(), 0.5);
        assertEquals(22.3, arcsec(m.angularDiameter()), 2.0);
        SkyEphemeris.SkyObject s = fromEarth("saturn", "2023-08-27");
        assertTrue(Math.toDegrees(s.elongation()) > 172, "Saturn at opposition");
        assertEquals(0.4, s.apparentMagnitude(), 0.5, "the globe alone (rings add up to ~0.8 mag)");
    }

    @Test
    void nightSkyFromEarthAndMars() {
        List<SkyEphemeris.SkyObject> earthSky = SkyEphemeris.visible(SolarSystemData.system(), EARTH, t("2023-11-03"), 6);
        assertEquals("spacebreaker:sun", earthSky.get(0).id());
        assertEquals("spacebreaker:moon", earthSky.get(1).id());
        assertTrue(earthSky.stream().anyMatch(o -> o.id().equals("spacebreaker:jupiter")));
        assertTrue(earthSky.stream().noneMatch(o -> o.id().equals("spacebreaker:pluto")), "Pluto is far too faint");
        for (int i = 1; i < earthSky.size(); i++) {
            assertTrue(earthSky.get(i).apparentMagnitude() >= earthSky.get(i - 1).apparentMagnitude());
        }
        // from Mars, Earth is a blue "star", brightest around inferior conjunction ± a few weeks (≈ −2.5)
        double best = 99;
        for (double d = 0; d < 800; d += 2) {
            best = Math.min(best, SkyEphemeris.view(SolarSystemData.system(), "spacebreaker:mars", EARTH, t("2020-01-01") + d * 86400).apparentMagnitude());
        }
        assertEquals(-2.5, best, 0.6);
        assertEquals(0x8FB2E0, SkyEphemeris.color(SolarSystemData.body("earth")));
    }

    @Test
    void fromIoJupiterFillsTheSky() {
        List<SkyEphemeris.SkyObject> sky = SkyEphemeris.visible(SolarSystemData.system(), "spacebreaker:io", t("2025-06-01"), 6);
        SkyEphemeris.SkyObject jupiter = sky.stream().filter(o -> o.id().equals("spacebreaker:jupiter")).findFirst().orElseThrow();
        assertEquals(19.1, jupiter.angularDiameterDeg(), 0.3);
        for (String moon : new String[]{"europa", "ganymede", "callisto"}) {
            SkyEphemeris.SkyObject o = sky.stream().filter(x -> x.id().equals("spacebreaker:" + moon)).findFirst().orElseThrow();
            assertTrue(o.apparentMagnitude() < 0, moon + " shines brightly from Io: " + o.apparentMagnitude());
            assertTrue(o.angularDiameter() > 0);
        }
    }

    @Test
    void horizontalCoordinates() {
        Vec3 x = new Vec3(1, 0, 0);
        assertEquals(90, Math.toDegrees(SkyEphemeris.toHorizontal(x, 0, 0, 0).altitude()), 1e-6, "overhead at local noon");
        assertEquals(-90, Math.toDegrees(SkyEphemeris.toHorizontal(x, 0, Math.PI, 0).altitude()), 1e-6, "underfoot at midnight");
        SkyEphemeris.Horizontal pole = SkyEphemeris.toHorizontal(new Vec3(0, 0, 1), 0, 1.234, Math.toRadians(45));
        assertEquals(45, Math.toDegrees(pole.altitude()), 1e-6, "the celestial pole sits at the latitude");
        assertEquals(0, Math.toDegrees(Math.min(pole.azimuth(), 2 * Math.PI - pole.azimuth())), 1e-6, "due north");
        // rising in the east: a quarter turn before transit, on the equator
        SkyEphemeris.Horizontal rise = SkyEphemeris.toHorizontal(x, 0, -Math.PI / 2, 0);
        assertEquals(0, Math.toDegrees(rise.altitude()), 1e-6);
        assertEquals(90, Math.toDegrees(rise.azimuth()), 1e-6);
        // Earth's tilt: the summer Sun (ecliptic longitude 90°) reaches +23.4° declination
        SkyEphemeris.Horizontal summer = SkyEphemeris.toHorizontal(new Vec3(0, 1, 0), Math.toRadians(23.44), Math.PI / 2, 0);
        assertEquals(90 - 23.44, Math.toDegrees(summer.altitude()), 1e-6);
    }

    @Test
    void lambertPhaseAndAbsoluteMagnitudes() {
        assertEquals(1, SkyEphemeris.lambert(0), 1e-12);
        assertEquals(1 / Math.PI, SkyEphemeris.lambert(Math.PI / 2), 1e-12);
        assertEquals(0, SkyEphemeris.lambert(Math.PI), 1e-12);
        assertEquals(-4.4, SkyEphemeris.absoluteMagnitude(SolarSystemData.body("venus")), 0.15);
        assertEquals(-9.4, SkyEphemeris.absoluteMagnitude(SolarSystemData.body("jupiter")), 0.15);
        assertEquals(-1.6, SkyEphemeris.absoluteMagnitude(SolarSystemData.body("mars")), 0.15);
        assertEquals(-0.6, SkyEphemeris.absoluteMagnitude(SolarSystemData.body("mercury")), 0.15);
    }
}
