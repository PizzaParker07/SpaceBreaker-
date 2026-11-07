package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class EclipsesTest {
    @Test
    void issLikeOrbitSpendsAThirdInShadow() {
        double f = Eclipses.eclipseFraction(EARTH_RADIUS, EARTH_RADIUS + 400e3, 0);
        assertEquals(0.39, f, 0.01);
        double minutes = f * Kepler.period(EARTH_RADIUS + 400e3, MU_EARTH) / 60;
        assertEquals(36, minutes, 1.5, "≈ 36 minutes of night per ISS orbit");
        // high β: no eclipse season
        assertEquals(0, Eclipses.eclipseFraction(EARTH_RADIUS, EARTH_RADIUS + 400e3, Math.toRadians(75)));
        // geostationary orbit at equinox: ≈ 70 minutes, ≈ 4.8 % of the day
        double geo = Eclipses.eclipseFraction(EARTH_RADIUS, 42164e3, 0);
        assertEquals(70, geo * 86164 / 60, 3);
        // and none away from equinox (β > 8.7°)
        assertEquals(0, Eclipses.eclipseFraction(EARTH_RADIUS, 42164e3, Math.toRadians(10)));
        assertTrue(Eclipses.eclipseFraction(EARTH_RADIUS, EARTH_RADIUS + 400e3, Math.toRadians(30))
                < Eclipses.eclipseFraction(EARTH_RADIUS, EARTH_RADIUS + 400e3, 0));
    }

    @Test
    void shadowGeometry() {
        Vec3 sun = new Vec3(AU, 0, 0);
        double r = EARTH_RADIUS + 400e3;
        assertEquals(1, Eclipses.sunlitFraction(new Vec3(r, 0, 0), sun, EARTH_RADIUS, SOLAR_RADIUS), 1e-12, "day side");
        assertEquals(0, Eclipses.sunlitFraction(new Vec3(-r, 0, 0), sun, EARTH_RADIUS, SOLAR_RADIUS), 1e-12, "deep in the umbra");
        assertEquals(1, Eclipses.sunlitFraction(new Vec3(0, r, 0), sun, EARTH_RADIUS, SOLAR_RADIUS), 1e-12, "terminator above the limb");
        assertTrue(Eclipses.inCylindricalShadow(new Vec3(-r, 0, 0), new Vec3(1, 0, 0), EARTH_RADIUS));
        assertFalse(Eclipses.inCylindricalShadow(new Vec3(-r, 2 * EARTH_RADIUS, 0), new Vec3(1, 0, 0), EARTH_RADIUS));
        // penumbra: crossing the shadow edge, the light fades smoothly from 1 to 0
        double prev = 1;
        boolean partial = false;
        for (double y = 1.2 * EARTH_RADIUS; y > 0.8 * EARTH_RADIUS; y -= 2e3) {
            double f = Eclipses.sunlitFraction(new Vec3(-Math.sqrt(Math.max(0, r * r - y * y)), y, 0), sun, EARTH_RADIUS, SOLAR_RADIUS);
            assertTrue(f <= prev + 1e-9, "monotonic");
            partial |= f > 0.01 && f < 0.99;
            prev = f;
        }
        assertTrue(partial, "a penumbra exists");
        assertEquals(0, Eclipses.sunlitFraction(new Vec3(0, 0, 0), sun, EARTH_RADIUS, SOLAR_RADIUS), "inside the planet");
        // overlap of two equal discs at distance r: lens area
        double lens = Eclipses.discOverlap(1, 1, 1);
        assertEquals(2 * Math.PI / 3 - Math.sqrt(3) / 2, lens, 1e-9);
    }

    @Test
    void stationAverageMatchesTheEclipseFraction() {
        BodySystem s = SolarSystemData.system();
        double alt = 400e3;
        Eclipses.Sunlight first = Eclipses.station(s, "spacebreaker:earth", alt, 0);
        assertEquals(1, first.sunlit(), 1e-9, "starts on the day side");
        assertEquals(1361, first.irradiance(), 50);
        double period = first.orbitPeriod();
        int dark = 0, n = 2000;
        for (int i = 0; i < n; i++) {
            if (Eclipses.station(s, "spacebreaker:earth", alt, i * period / n).sunlit() < 0.5) {
                dark++;
            }
        }
        assertEquals(first.eclipseFraction(), dark / (double) n, 0.02);
        assertEquals(0.39, first.eclipseFraction(), 0.02, "β ≈ 0 in the system plane");
        // the Moon's low orbit: long nights; Mars further from the Sun: weaker light
        Eclipses.Sunlight moon = Eclipses.station(s, "spacebreaker:moon", 100e3, 0);
        assertEquals(0.39, moon.eclipseFraction(), 0.02, "≈ 47 of 118 minutes in low lunar orbit");
        double mars = Eclipses.station(s, "spacebreaker:mars", 300e3, 0).irradiance();
        double d = s.distanceToStar(s.require("spacebreaker:mars"), 0) / AU;
        assertEquals(1361 / (d * d), mars, 15, "follows the real distance (near perihelion at J2000)");
        assertTrue(mars > 490 && mars < 720, "between aphelion and perihelion sunlight");
        // an inclined orbit at β ≈ 0 still crosses the shadow; a polar orbit with the star in its plane does too
        assertTrue(Eclipses.station(s, "spacebreaker:earth", alt, Math.toRadians(51.6), 0).eclipseFraction() > 0.3);
    }
}
