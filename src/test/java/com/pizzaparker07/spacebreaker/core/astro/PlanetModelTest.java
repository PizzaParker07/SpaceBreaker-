package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanetModelTest {
    @Test
    void earthEquilibriumTemperature() {
        assertEquals(255, PlanetModel.equilibriumTemperature(1.0, 1.0, 0.306), 1.5);
        assertEquals(PlanetModel.equilibriumTemperature(1361, 0.306), PlanetModel.equilibriumTemperature(1, 1, 0.306), 1e-9);
        double fromWatts = PlanetModel.equilibriumTemperature(PlanetModel.flux(SOLAR_LUMINOSITY, AU), 0.306);
        assertEquals(255, fromWatts, 1.5);
    }

    @Test
    void greenhouseWarmsEarthTo288() {
        double teq = PlanetModel.equilibriumTemperature(1, 1, 0.306);
        double tau = PlanetModel.opticalDepth(101.325, 0.01);
        assertEquals(288, PlanetModel.surfaceTemperature(teq, tau), 2);
        assertEquals(teq, PlanetModel.surfaceTemperature(teq, PlanetModel.opticalDepth(0, 0.5)), 1e-12);
        double venus = PlanetModel.surfaceTemperature(PlanetModel.equilibriumTemperature(1, 0.723, 0.76),
                PlanetModel.opticalDepth(9200, 0.965));
        assertTrue(venus > 650, "Venus runaway greenhouse: " + venus);
    }

    @Test
    void marsAndDaySideTemperatures() {
        assertEquals(210, PlanetModel.equilibriumTemperature(1, 1.524, 0.25), 3);
        assertTrue(PlanetModel.daySideTemperature(1361, 0.11) > PlanetModel.equilibriumTemperature(1361, 0.11));
    }

    @Test
    void habitableZone() {
        assertTrue(PlanetModel.inHabitableZone(1, 1));
        assertFalse(PlanetModel.inHabitableZone(1, 0.72), "Venus is too close");
        assertTrue(PlanetModel.inHabitableZone(1, 1.3));
        assertFalse(PlanetModel.inHabitableZone(1, 5.2));
        // a dim red dwarf has a tiny habitable zone (TRAPPIST-1, 0.000553 L☉: e/f/g at 0.029–0.047 AU)
        assertTrue(PlanetModel.inHabitableZone(0.000553, 0.029));
        assertTrue(PlanetModel.inHabitableZone(0.000553, 0.031));
        assertEquals(0.95, PlanetModel.habitableZoneInner(1), 0.01);
        assertEquals(1.37, PlanetModel.habitableZoneOuter(1), 0.01);
    }

    @Test
    void gravityAndEscape() {
        assertEquals(9.82, PlanetModel.surfaceGravity(EARTH_MASS, EARTH_RADIUS), 0.01);
        assertEquals(11186, PlanetModel.escapeVelocity(EARTH_MASS, EARTH_RADIUS), 5);
        assertEquals(2380, PlanetModel.escapeVelocity(7.342e22, 1737.4e3), 5);
    }

    @Test
    void atmosphereRetention() {
        double earthEsc = 11186, earthExo = 1000;
        assertTrue(PlanetModel.retainsGas(earthEsc, earthExo, 28), "Earth keeps nitrogen");
        assertTrue(PlanetModel.retainsGas(earthEsc, earthExo, 32), "and oxygen");
        assertFalse(PlanetModel.retainsGas(earthEsc, earthExo, 2), "but loses hydrogen");
        assertFalse(PlanetModel.retainsGas(earthEsc, earthExo, 4), "and helium");
        assertFalse(PlanetModel.retainsGas(2380, 400, 28), "the Moon cannot keep air");
        assertTrue(PlanetModel.retainsGas(59500, 1000, 2), "Jupiter keeps hydrogen");
        assertEquals(1840, PlanetModel.thermalVelocity(273, 2.016), 10);
        double lightest = PlanetModel.lightestRetainedMolarMass(earthEsc, earthExo);
        assertTrue(lightest > 4 && lightest < 28, "Earth's cut-off between He and N2: " + lightest);
        assertEquals(1000, PlanetModel.exosphereTemperature(255, 1361), 10);
        assertEquals(160, PlanetModel.exosphereTemperature(82, 15), 15);
    }

    @Test
    void tidalLocking() {
        double age = 4.5e9;
        assertTrue(PlanetModel.tidallyLocked(384400e3, 7.342e22, 1737.4e3, EARTH_MASS, age), "the Moon is locked to Earth");
        assertFalse(PlanetModel.tidallyLocked(AU, EARTH_MASS, EARTH_RADIUS, SOLAR_MASS, age), "Earth is not locked to the Sun");
        assertTrue(PlanetModel.tidallyLocked(0.0485 * AU, 1.07 * EARTH_MASS, 1.07 * EARTH_RADIUS, 0.1221 * SOLAR_MASS, 1e9),
                "Proxima b should be locked");
        assertTrue(PlanetModel.tidalLockTimeYears(9376e3, 1.0659e16, 11.08e3, 6.4171e23) < 1e9, "Phobos is locked");
    }

    @Test
    void compositionClasses() {
        assertEquals(PlanetModel.Composition.ROCKY, PlanetModel.composition(1, 5514));
        assertEquals(PlanetModel.Composition.IRON_RICH, PlanetModel.composition(0.055, 5427));
        assertEquals(PlanetModel.Composition.ROCKY, PlanetModel.composition(0.0123, 3344));
        assertEquals(PlanetModel.Composition.OCEAN, PlanetModel.composition(0.008, 3013));
        assertEquals(PlanetModel.Composition.ICY, PlanetModel.composition(0.025, 1942));
        assertEquals(PlanetModel.Composition.ICE_GIANT, PlanetModel.composition(17.1, 1638));
        assertEquals(PlanetModel.Composition.GAS_GIANT, PlanetModel.composition(317.8, 1326));
        assertEquals(PlanetModel.Composition.GAS_GIANT, PlanetModel.composition(95.2, 687));
    }

    @Test
    void massRadiusRelations() {
        assertEquals(1.0, PlanetModel.radiusFor(1, PlanetModel.Composition.ROCKY), 1e-12);
        assertEquals(11.2, PlanetModel.radiusFor(318, PlanetModel.Composition.GAS_GIANT), 0.1);
        assertEquals(3.9, PlanetModel.radiusFor(17, PlanetModel.Composition.ICE_GIANT), 0.5);
        assertTrue(PlanetModel.radiusFor(2, PlanetModel.Composition.ICY) > PlanetModel.radiusFor(2, PlanetModel.Composition.ROCKY));
    }
}
