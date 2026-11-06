package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

class TransferPlannerTest {
    private static final String EARTH = "spacebreaker:earth", MOON = "spacebreaker:moon", MARS = "spacebreaker:mars",
            VENUS = "spacebreaker:venus", PHOBOS = "spacebreaker:phobos", JUPITER = "spacebreaker:jupiter",
            IO = "spacebreaker:io", EUROPA = "spacebreaker:europa", TITAN = "spacebreaker:titan";

    private static BodySystem sys() {
        return SolarSystemData.system();
    }

    @Test
    void lowOrbitAltitudes() {
        assertEquals(210e3, SolarSystemData.body("earth").lowOrbitRadius() - 6371e3, 25e3);
        assertEquals(87e3, SolarSystemData.body("moon").lowOrbitRadius() - 1737.4e3, 1e3);
        assertEquals(180e3, SolarSystemData.body("mars").lowOrbitRadius() - 3389.5e3, 30e3);
        assertEquals(8.4e3, SolarSystemData.body("earth").scaleHeight(), 0.3e3);
        assertEquals(11.1e3, SolarSystemData.body("mars").scaleHeight(), 0.6e3);
        assertEquals(15.9e3, SolarSystemData.body("venus").scaleHeight(), 1.0e3);
    }

    @Test
    void ascentFromEarthMoonAndMars() {
        assertEquals(9.3e3, TransferPlanner.ascent(SolarSystemData.body("earth"), 1.3), 300);
        assertEquals(1.9e3, TransferPlanner.ascent(SolarSystemData.body("moon"), 2.0), 150);
        assertEquals(4.0e3, TransferPlanner.ascent(SolarSystemData.body("mars"), 1.5), 300);
        assertTrue(TransferPlanner.ascent(SolarSystemData.body("phobos")) < 15, "Phobos: you could almost jump off");
        assertEquals(Double.POSITIVE_INFINITY, TransferPlanner.gravityLoss(SolarSystemData.body("earth"), 0.9));
        assertTrue(TransferPlanner.ascent(SolarSystemData.body("earth"), 3) < TransferPlanner.ascent(SolarSystemData.body("earth"), 1.2));
    }

    @Test
    void earthRotationHelps() {
        AstroBody earth = SolarSystemData.body("earth");
        assertEquals(465, earth.equatorialSpeed(), 3);
        assertEquals(4.6, SolarSystemData.body("moon").equatorialSpeed(), 0.1);
    }

    @Test
    void dragAndParachutes() {
        assertEquals(130, TransferPlanner.dragLoss(SolarSystemData.body("earth")), 5);
        assertTrue(TransferPlanner.dragLoss(SolarSystemData.body("mars")) < 30);
        assertEquals(0, TransferPlanner.dragLoss(SolarSystemData.body("moon")));
        assertEquals(8, TransferPlanner.parachuteTerminalSpeed(SolarSystemData.body("earth")), 0.5);
        assertTrue(TransferPlanner.parachuteTerminalSpeed(SolarSystemData.body("mars")) > 30, "Mars air is thin");
        assertTrue(TransferPlanner.parachuteTerminalSpeed(SolarSystemData.body("titan")) < 5, "Titan is great for parachutes");
        assertEquals(1.2, TransferPlanner.surfaceDensity(SolarSystemData.body("earth")), 0.05);
        assertEquals(5.4, TransferPlanner.surfaceDensity(SolarSystemData.body("titan")), 0.3);
    }

    @Test
    void descentUsesTheAtmosphere() {
        AstroBody mars = SolarSystemData.body("mars");
        assertTrue(TransferPlanner.descent(mars, true) < 0.2 * TransferPlanner.descent(mars, false));
        AstroBody moon = SolarSystemData.body("moon");
        assertEquals(TransferPlanner.descent(moon, true), TransferPlanner.descent(moon, false), 1e-9);
        assertTrue(TransferPlanner.descent(SolarSystemData.body("earth"), true) < 150);
    }

    @Test
    void earthToMoon() {
        TransferPlanner.Plan p = TransferPlanner.plan(sys(), EARTH, MOON, false);
        assertEquals(3.13e3, p.deltaV("departure"), 40);     // trans-lunar injection
        assertEquals(0.82e3, p.deltaV("capture"), 60);       // lunar orbit insertion
        assertEquals(5.0, p.transferTime() / DAY, 0.2);
        TransferPlanner.Plan back = TransferPlanner.plan(sys(), MOON, EARTH, false);
        assertEquals(p.total(), back.total(), 1);
        TransferPlanner.Plan aero = TransferPlanner.plan(sys(), MOON, EARTH, true);
        assertTrue(aero.deltaV("capture") < 100, "aerocapture at Earth");
        assertEquals(0, TransferPlanner.timeToWindow(sys(), EARTH, MOON, 0), "always a window to a moon");
    }

    @Test
    void earthToMars() {
        TransferPlanner.Plan p = TransferPlanner.plan(sys(), EARTH, MARS, false);
        assertEquals(3.6e3, p.deltaV("departure"), 60);
        assertEquals(2.1e3, p.deltaV("capture"), 80);
        assertEquals(259, p.transferTime() / DAY, 1.5);
        assertEquals(780, p.synodicPeriod() / DAY, 1);
        assertEquals(44.3, Math.toDegrees(p.phaseAngle()), 0.5);
        TransferPlanner.Plan aero = TransferPlanner.plan(sys(), EARTH, MARS, true);
        assertTrue(aero.deltaV("capture") < 150, "aerobraking at Mars: " + aero.deltaV("capture"));
        assertEquals(p.deltaV("departure"), aero.deltaV("departure"), 1e-9);
    }

    @Test
    void earthToVenusAndJupiter() {
        TransferPlanner.Plan v = TransferPlanner.plan(sys(), EARTH, VENUS, false);
        assertEquals(3.5e3, v.deltaV("departure"), 150);
        assertEquals(146, v.transferTime() / DAY, 2);
        assertEquals(584, v.synodicPeriod() / DAY, 1);
        TransferPlanner.Plan j = TransferPlanner.plan(sys(), EARTH, JUPITER, false);
        assertEquals(6.3e3, j.deltaV("departure"), 150);
        assertEquals(2.73, j.transferTime() / AstroConstants.YEAR, 0.05);
    }

    @Test
    void nestedAndSiblingMoons() {
        TransferPlanner.Plan p = TransferPlanner.plan(sys(), EARTH, PHOBOS, false);
        TransferPlanner.Plan m = TransferPlanner.plan(sys(), EARTH, MARS, false);
        assertEquals(m.transferTime(), p.transferTime(), 1e-6);
        assertTrue(p.deltaV("capture") > 0 && p.deltaV("capture") < 3e3);
        TransferPlanner.Plan ie = TransferPlanner.plan(sys(), IO, EUROPA, false);
        assertTrue(ie.total() > 1e3 && ie.total() < 5e3, "Io -> Europa " + ie.total());
        assertTrue(TransferPlanner.plan(sys(), JUPITER, IO, false).deltaV("departure") > 0);
        TransferPlanner.Plan titanAero = TransferPlanner.plan(sys(), "spacebreaker:saturn", TITAN, true);
        assertTrue(titanAero.deltaV("capture") < TransferPlanner.plan(sys(), "spacebreaker:saturn", TITAN, false).deltaV("capture"));
        assertThrows(IllegalArgumentException.class, () -> TransferPlanner.plan(sys(), "spacebreaker:sun", MOON, false));
        assertEquals(0, TransferPlanner.plan(sys(), MARS, MARS, false).total());
    }

    @Test
    void launchWindowsLineUp() {
        BodySystem s = sys();
        TransferPlanner.Plan p = TransferPlanner.plan(s, EARTH, MARS, false);
        double wait = TransferPlanner.timeToWindow(s, EARTH, MARS, 0);
        assertTrue(wait >= 0 && wait <= p.synodicPeriod() * 1.05, "wait " + wait / DAY);
        double phase = TransferPlanner.currentPhase(s, EARTH, MARS, wait);
        assertEquals(Math.toDegrees(p.phaseAngle()), Math.toDegrees(phase), 0.05);
        // a later start finds the following window, one synodic period on
        double next = TransferPlanner.timeToWindow(s, EARTH, MARS, wait + 10 * DAY) + wait + 10 * DAY;
        assertEquals(p.synodicPeriod() / DAY, (next - wait) / DAY, 60);
    }

    @Test
    void positionsOverTime() {
        BodySystem s = sys();
        AstroBody earth = SolarSystemData.body("earth");
        AstroBody moon = SolarSystemData.body("moon");
        // Earth stays between perihelion and aphelion and returns after one year
        for (double t = 0; t < AstroConstants.YEAR; t += 10 * DAY) {
            double r = s.distanceToStar(earth, t);
            assertTrue(r > 147.0e9 && r < 152.2e9, "Earth distance " + r);
        }
        assertEquals(0, s.position(earth, 0).distance(s.position(earth, 365.256 * DAY)), 1e6);
        // Earth at J2000 (1 January) is near perihelion: ~147.1 million km
        assertEquals(147.1e9, s.distanceToStar(earth, 0), 0.3e9);
        double dm = s.distance(earth, moon, 0);
        assertTrue(dm > 356e6 && dm < 407e6);
        assertEquals(s.distanceToStar(earth, 0), s.distanceToStar(moon, 0), 0.5e9);
        // Mars–Earth distance stays inside its real range
        AstroBody mars = SolarSystemData.body("mars");
        double min = Double.MAX_VALUE, max = 0;
        for (double t = 0; t < 16 * AstroConstants.YEAR; t += 2 * DAY) {
            double d = s.distance(earth, mars, t);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        assertEquals(55.7e9, min, 3e9);
        assertEquals(401e9, max, 5e9);
    }

    @Test
    void gameClock() {
        // default scale: 1 real second per simulated hour
        assertEquals(3600, GameClock.simSeconds(20, 1.0), 1e-9);
        assertEquals(180, GameClock.simSecondsPerTick(1.0), 1e-9);
        double yearReal = GameClock.realSeconds(AstroConstants.YEAR, 1.0);
        assertEquals(2.435, yearReal / 3600, 0.01);
        assertEquals(20, GameClock.ticks(3600, 1.0));
        assertEquals(GameClock.simSeconds(GameClock.ticks(1e6, 2.0), 2.0), 1e6, 200);
    }
}
