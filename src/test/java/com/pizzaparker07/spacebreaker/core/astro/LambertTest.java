package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LambertTest {
    @Test
    void textbookExample() {
        // Curtis, Orbital Mechanics for Engineering Students, example 5.2 (km, s)
        Vec3 r1 = new Vec3(5000e3, 10000e3, 2100e3), r2 = new Vec3(-14600e3, 2500e3, 7000e3);
        Lambert.Solution s = Lambert.solve(r1, r2, 3600, 398600e9, true).orElseThrow();
        assertEquals(-5.9925e3, s.v1().x(), 2);
        assertEquals(1.9254e3, s.v1().y(), 2);
        assertEquals(3.2456e3, s.v1().z(), 2);
        assertEquals(-3.3125e3, s.v2().x(), 2);
        assertEquals(-4.1966e3, s.v2().y(), 2);
        assertEquals(-0.38529e3, s.v2().z(), 2);
    }

    @Test
    void reproducesAKeplerArc() {
        double mu = AstroConstants.MU_SUN;
        OrbitalElements el = new OrbitalElements(mu, 1.7 * AstroConstants.AU, 0.35, Math.toRadians(20), 1.0, 2.0, 0.3);
        for (double frac : new double[]{0.1, 0.3, 0.6, 0.85}) {
            double dt = frac * el.period();
            StateVector a = Kepler.stateFromElements(el, 0), b = Kepler.stateFromElements(el, dt);
            Lambert.Solution s = Lambert.solve(a.position(), b.position(), dt, mu, true).orElseThrow();
            assertEquals(0, s.v1().sub(a.velocity()).length(), 1.0, "v1 at fraction " + frac);
            assertEquals(0, s.v2().sub(b.velocity()).length(), 1.0, "v2 at fraction " + frac);
        }
        // a retrograde orbit is found with prograde = false
        OrbitalElements retro = new OrbitalElements(mu, AstroConstants.AU, 0.1, Math.toRadians(160), 0.5, 1.0, 0);
        double dt = 0.25 * retro.period();
        StateVector a = Kepler.stateFromElements(retro, 0), b = Kepler.stateFromElements(retro, dt);
        Lambert.Solution s = Lambert.solve(a.position(), b.position(), dt, mu, false).orElseThrow();
        assertEquals(0, s.v1().sub(a.velocity()).length(), 1.0);
    }

    @Test
    void degenerateCases() {
        Vec3 r = new Vec3(AstroConstants.AU, 0, 0);
        assertTrue(Lambert.solve(r, r.scale(-1.5), 1e7, AstroConstants.MU_SUN, true).isEmpty(), "180° transfer plane undefined");
        assertTrue(Lambert.solve(r, new Vec3(0, AstroConstants.AU, 0), -1, AstroConstants.MU_SUN, true).isEmpty());
    }
}
