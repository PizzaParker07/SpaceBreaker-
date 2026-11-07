package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Long worlds: at the default time scale (1 real second per simulated hour) 1000 hours of play is about
 * 410 simulated years; at the slowest setting (0.01) it is about 41 000 years. Positions must stay finite,
 * periodic and on their orbits; nothing may drift.
 */
class LongRunTest {
    private static final BodySystem SYS = SolarSystemData.system();
    private static final long THOUSAND_HOURS = 1000L * 3600 * 20;

    @Test
    void clockScale() {
        double years = GameClock.simSeconds(THOUSAND_HOURS, 1.0) / AstroConstants.YEAR;
        assertEquals(410.7, years, 0.5);
        assertEquals(41070, GameClock.simSeconds(THOUSAND_HOURS, 0.01) / AstroConstants.YEAR, 50);
        assertEquals(THOUSAND_HOURS, GameClock.ticks(GameClock.simSeconds(THOUSAND_HOURS, 1.0), 1.0));
        // consecutive ticks stay distinct far into a world
        double t1 = GameClock.simSeconds(THOUSAND_HOURS * 100, 0.01), t2 = GameClock.simSeconds(THOUSAND_HOURS * 100 + 1, 0.01);
        assertEquals(GameClock.simSecondsPerTick(0.01), t2 - t1, 1e-3);
    }

    @Test
    void orbitsStayPeriodicAndOnTrack() {
        for (double t : new double[]{GameClock.simSeconds(THOUSAND_HOURS, 1.0), GameClock.simSeconds(THOUSAND_HOURS, 0.01)}) {
            for (AstroBody b : SYS.all()) {
                if (b.orbit() == null) continue;
                OrbitalElements o = b.orbit();
                StateVector s = o.stateAt(t);
                assertTrue(Double.isFinite(s.position().length()) && Double.isFinite(s.velocity().length()), b.id());
                double r = s.position().length();
                double a = o.semiMajorAxis(), e = o.eccentricity();
                assertTrue(r >= a * (1 - e) * (1 - 1e-9) && r <= a * (1 + e) * (1 + 1e-9), b.id() + " stays between periapsis and apoapsis");
                assertEquals(Kepler.visViva(o.mu(), r, a), s.velocity().length(), 1e-6 * s.velocity().length(), b.id() + " vis-viva");
                if (!o.precessing()) {
                    Vec3 later = o.stateAt(t + o.period()).position();
                    assertEquals(0, later.distance(s.position()) / a, 1e-6, b.id() + " repeats after one period");
                }
            }
        }
    }

    @Test
    void keplerSolverAtHighEccentricity() {
        for (Comet c : Comet.CATALOGUE) {
            OrbitalElements o = c.elements();
            for (double f = 0; f < 1; f += 0.001) {
                double t = 1e12 + f * o.period();
                double r = o.stateAt(t).position().length();
                assertTrue(r >= o.periapsis() * (1 - 1e-9) && r <= o.apoapsis() * (1 + 1e-9), c.id() + " at " + f);
            }
        }
    }
}
