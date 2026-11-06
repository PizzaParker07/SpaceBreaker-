package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

class EphemerisTest {
    private static final String EARTH = "spacebreaker:earth", MARS = "spacebreaker:mars", MOON = "spacebreaker:moon";

    private static final class Clock {
        double t;
    }

    @Test
    void positionsFollowTheClock() {
        Clock c = new Clock();
        Ephemeris e = new Ephemeris(SolarSystemData.system(), () -> c.t);
        Vec3 a = e.position(EARTH);
        assertSame(a, e.position(EARTH), "cached while time stands still");
        c.t = 91 * DAY;
        Vec3 b = e.position(EARTH);
        assertNotEquals(a, b);
        assertEquals(Math.PI / 2, Math.acos(a.normalize().dot(b.normalize())), 0.05, "a quarter orbit in ~91 days");
        assertEquals(1.0, e.starDistanceAu(EARTH), 0.02);
        assertEquals(1.52, e.starDistanceAu(MARS), 0.15);
        assertEquals(384400e3, e.distance(EARTH, MOON), 30000e3);
    }

    @Test
    void marsWindowsRepeatEverySynodicPeriod() {
        Clock c = new Clock();
        Ephemeris e = new Ephemeris(SolarSystemData.system(), () -> c.t);
        Ephemeris.Window w = e.window(EARTH, MARS);
        assertEquals(44.3, Math.toDegrees(w.plan().phaseAngle()), 0.5);
        double first = w.opensAt();
        assertTrue(w.open() || first > c.t);
        // half a window width before the centre: open
        c.t = first - 0.5 * w.halfWidth();
        assertTrue(e.window(EARTH, MARS).open());
        assertEquals(0, e.window(EARTH, MARS).waitFrom(c.t));
        assertEquals(Math.toDegrees(w.plan().phaseAngle()),
                Math.toDegrees(e.window(EARTH, MARS).currentPhase()), 2.0);
        // after the window: the next one, about 780 days later
        c.t = first + 2 * w.halfWidth();
        Ephemeris.Window next = e.window(EARTH, MARS);
        assertFalse(next.open());
        assertEquals(780, (next.opensAt() - first) / DAY, 60);
        assertTrue(next.waitFrom(c.t) > 0);
        // jumping back in time recomputes too
        c.t = first - 400 * DAY;
        assertEquals(first, e.window(EARTH, MARS).opensAt(), DAY);
    }

    @Test
    void moonIsAlwaysReachable() {
        Clock c = new Clock();
        Ephemeris e = new Ephemeris(SolarSystemData.system(), () -> c.t);
        for (double t = 0; t < 40 * DAY; t += 3.3 * DAY) {
            c.t = t;
            Ephemeris.Window w = e.window(EARTH, MOON);
            assertTrue(w.open());
            assertEquals(0, w.waitFrom(t));
            assertTrue(e.window(MOON, EARTH).open());
        }
    }
}
