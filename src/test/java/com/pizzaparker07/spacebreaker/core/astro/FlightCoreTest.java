package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.flight.FlightState;
import com.pizzaparker07.spacebreaker.core.astro.flight.Gravity;
import com.pizzaparker07.spacebreaker.core.astro.flight.OrbitPredictor;
import com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.DAY;
import static org.junit.jupiter.api.Assertions.*;

class FlightCoreTest {
    private static final BodySystem SYS = SolarSystemData.system();
    private static final AstroBody EARTH = SYS.require("spacebreaker:earth"), MOON = SYS.require("spacebreaker:moon"),
            SUN = SYS.require("spacebreaker:sun");

    @Test
    void sceneScale() {
        SceneScale s = SceneScale.EARTH_MOON;
        assertEquals(128, s.blocks(EARTH.radius()), 1e-9, "Earth's surface");
        double moon = s.blocks(384_400e3);
        assertTrue(moon > 2000 && moon < 2500, "the Moon is ≈ 2 200 blocks out: " + moon);
        double leo = s.blocks(EARTH.radius() + 200e3) - 128;
        assertTrue(leo > 10 && leo < 25, "low orbit is a dozen-plus blocks up: " + leo);
        double prev = -1;
        for (double d = 1e5; d < 1e9; d *= 1.3) {
            double b = s.blocks(d);
            assertTrue(b > prev);
            prev = b;
            assertEquals(d, s.metres(b), d * 1e-12, "round trip");
        }
        Vec3 v = new Vec3(3e8, -1e8, 2e7);
        assertEquals(0, s.toMetres(s.toBlocks(v)).distance(v), 1e-3);
        assertEquals(0, s.toBlocks(v).normalize().distance(v.normalize()), 1e-12, "directions kept");
        // the Moon is drawn at least 24 blocks wide even though its local scale would make it ≈ 2
        assertEquals(24, s.displayRadius(MOON.radius(), 384_400e3, 24), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new SceneScale(0, 1, 1));
    }

    @Test
    void gravityInTheEarthFrame() {
        double r = EARTH.radius() + 400e3;
        Vec3 a = Gravity.acceleration(SYS, EARTH, List.of(EARTH, MOON, SUN), new Vec3(r, 0, 0), 0);
        assertEquals(EARTH.mu() / (r * r), a.length(), 1e-5 * a.length(), "Earth dominates; tides are tiny");
        assertTrue(a.x() < 0, "pulls down");
        // near the Moon the Moon wins
        Vec3 moonRel = SYS.relativePosition(MOON, 0);
        Vec3 nearMoon = moonRel.add(moonRel.normalize().scale(-(MOON.radius() + 100e3)));
        Vec3 g = Gravity.acceleration(SYS, EARTH, List.of(EARTH, MOON), nearMoon, 0);
        assertTrue(g.dot(moonRel.normalize()) > 1.0, "towards the Moon at ≈ 1.4 m/s²: " + g);
    }

    @Test
    void leapfrogKeepsAnOrbit() {
        double r = EARTH.radius() + 400e3, v = Math.sqrt(EARTH.mu() / r);
        BiFunction<Vec3, Double, Vec3> g = (p, t) -> Gravity.pull(EARTH.mu(), p, Vec3.ZERO);
        FlightState s = new FlightState(new Vec3(r, 0, 0), new Vec3(0, v, 0), 10_000);
        double period = Kepler.period(r, EARTH.mu());
        int steps = (int) Math.round(100 * period / 10);
        double minR = r, maxR = r;
        for (int i = 0; i < steps; i++) {
            s = s.step(g, Vec3.ZERO, 0, i * 10, 10);
            double rr = s.position().length();
            minR = Math.min(minR, rr);
            maxR = Math.max(maxR, rr);
        }
        assertTrue(maxR - minR < 2000, "100 orbits at 10 s steps stay within 2 km: " + (maxR - minR));
        assertEquals(10_000, s.mass());
    }

    @Test
    void thrustFollowsTheRocketEquation() {
        double isp = 350, thrust = 20_000, m0 = 5_000, flow = thrust / (isp * AstroConstants.G0);
        FlightState s = new FlightState(Vec3.ZERO, Vec3.ZERO, m0);
        double burn = 100;
        s = s.advance((p, t) -> Vec3.ZERO, new Vec3(thrust, 0, 0), flow, 0, burn, 1000);
        double expected = RocketMath.deltaV(isp, m0, m0 - flow * burn);
        assertEquals(expected, s.velocity().x(), expected * 1e-6);
        assertEquals(m0 - flow * burn, s.mass(), 1e-6);
        // flight assist
        Vec3 slowed = FlightState.assist(new Vec3(10, 0, 0), Vec3.ZERO, 3);
        assertEquals(7, slowed.x(), 1e-12);
        assertEquals(Vec3.ZERO, FlightState.assist(new Vec3(1, 1, 0), Vec3.ZERO, 5));
    }

    @Test
    void predictorFindsTheMoon() {
        double r = EARTH.radius() + 200e3, vc = Math.sqrt(EARTH.mu() / r);
        // circular low orbit: one conic, no events
        StateVector leo = new StateVector(new Vec3(r, 0, 0), new Vec3(0, vc, 0));
        List<OrbitPredictor.Segment> one = OrbitPredictor.predict(SYS, EARTH, leo, 0, 2 * 3600, 60, 4);
        assertEquals(1, one.size());
        assertEquals(OrbitPredictor.End.NONE, one.get(0).endEvent());
        assertEquals(r, one.get(0).periapsis(), 1);
        // falling straight down: impact
        var fall = OrbitPredictor.predict(SYS, EARTH, new StateVector(new Vec3(r + 800e3, 0, 0), new Vec3(0, 300, 0)), 0, 3600, 10, 2);
        assertEquals(OrbitPredictor.End.IMPACT, fall.get(0).endEvent());
        // a translunar injection (≈ 3.1 km/s prograde from low orbit in the Moon's plane) reaches the Moon's sphere of influence
        StateVector moon = MOON.orbit().stateAt(0);
        Vec3 n = moon.position().cross(moon.velocity()).normalize();
        boolean reached = false;
        for (int k = 0; k < 72 && !reached; k++) {
            double ang = Math.toRadians(5 * k);
            Vec3 u = moon.position().normalize(), w = n.cross(u);
            Vec3 pos = u.scale(Math.cos(ang)).add(w.scale(Math.sin(ang))).scale(r);
            Vec3 vel = n.cross(pos).normalize().scale(vc + 3130);
            var path = OrbitPredictor.predict(SYS, EARTH, new StateVector(pos, vel), 0, 7 * DAY, 600, 3);
            if (path.get(0).endEvent() == OrbitPredictor.End.ENTER_SOI && path.get(0).next() == MOON) {
                double days = path.get(0).end() / DAY;
                assertTrue(days > 1.5 && days < 6, "coast to the Moon " + days + " d");
                assertEquals(MOON, path.get(1).body(), "next conic is around the Moon");
                reached = true;
            }
        }
        assertTrue(reached, "some departure point reaches the Moon");
        assertEquals(Double.POSITIVE_INFINITY, OrbitPredictor.soi(SYS, SUN));
        assertEquals(66_100e3, OrbitPredictor.soi(SYS, MOON), 2_000e3);
    }

    @Test
    void readout() {
        var r = new com.pizzaparker07.spacebreaker.core.astro.flight.FlightReadout("spacebreaker:moon", -12, 5e5, true, 1500, 3000,
                true, null, Double.NaN, Double.NaN);
        assertTrue(r.path().isEmpty());
        assertFalse(r.hasNode());
        assertEquals(0.5, r.dvFraction(), 1e-12);
        var n = new com.pizzaparker07.spacebreaker.core.astro.flight.FlightReadout("x", 0, 0, false, 5000, 3000, false, List.of(), 120, 60);
        assertTrue(n.hasNode());
        assertEquals(1, n.dvFraction());
    }

    @Test
    void maneuverNodes() {
        double r = EARTH.radius() + 200e3, vc = Math.sqrt(EARTH.mu() / r);
        StateVector leo = new StateVector(new Vec3(r, 0, 0), new Vec3(0, vc, 0));
        var node = new com.pizzaparker07.spacebreaker.core.astro.flight.ManeuverNode(0, 100, 0, 0);
        OrbitalElements after = Kepler.elementsFromState(node.apply(leo), EARTH.mu());
        double v = vc + 100, a = 1 / (2 / r - v * v / EARTH.mu());
        assertEquals(2 * a - r, after.apoapsis(), 10, "prograde raises the far side (vis-viva)");
        assertEquals(r, after.periapsis(), 10);
        // normal burns tilt the plane, radial burns keep the energy-to-first-order
        var plane = new com.pizzaparker07.spacebreaker.core.astro.flight.ManeuverNode(0, 0, 200, 0);
        assertEquals(Math.atan2(200, vc), Kepler.elementsFromState(plane.apply(leo), EARTH.mu()).inclination(), 1e-6);
        assertEquals(Math.sqrt(3 * 3 + 4 * 4), new com.pizzaparker07.spacebreaker.core.astro.flight.ManeuverNode(0, 3, 0, 4).totalDv(), 1e-12);
        double burn = node.burnSeconds(5000, 20_000, 350);
        assertEquals(RocketMath.burnTimeForDeltaV(100, 5000, 20_000, 350), burn, 1e-9);
        assertEquals(-burn / 2, node.startTime(5000, 20_000, 350), 1e-9, "the burn is centred on the node");
    }

    @Test
    void entryGates() {
        var gate = com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.class;
        assertEquals(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.Zone.SPACE,
                com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.zone(EARTH, 2_000e3));
        assertEquals(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.Zone.ORBIT_SHELL,
                com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.zone(EARTH, 200e3));
        assertEquals(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.Zone.ENTRY,
                com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.zone(EARTH, 60e3));
        assertEquals(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.Zone.SURFACE,
                com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.zone(EARTH, 0));
        assertTrue(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.entryAltitude(EARTH) > 50e3, "Earth's entry interface is in the upper air");
        assertTrue(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.entryAltitude(MOON) < 60e3, "the airless Moon hands over on a low pass");
        double r = EARTH.radius() + 50e3;
        assertTrue(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.descendsThroughEntry(EARTH,
                new StateVector(new Vec3(r, 0, 0), new Vec3(-100, 7000, 0))));
        assertFalse(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.descendsThroughEntry(EARTH,
                new StateVector(new Vec3(r, 0, 0), new Vec3(100, 7000, 0))), "climbing out does not hand over");
        assertNotNull(gate);
    }

    @Test
    void surfacePoints() {
        BodyLook look = BodyLook.of(SYS, EARTH, null, 23.44, new Vec3(0, 0, 1e15), 1234.5 * DAY);
        var p = new com.pizzaparker07.spacebreaker.core.astro.flight.SurfacePoint(41.0, 29.0, 1500);
        Vec3 e = p.toEcliptic(look, EARTH.radius());
        var back = com.pizzaparker07.spacebreaker.core.astro.flight.SurfacePoint.of(e, look, EARTH.radius());
        assertEquals(41.0, back.latitudeDeg(), 1e-9);
        assertEquals(29.0, back.longitudeDeg(), 1e-9);
        assertEquals(1500, back.altitude(), 1e-6);
        // the pole is at latitude 90
        var pole = com.pizzaparker07.spacebreaker.core.astro.flight.SurfacePoint.of(look.spinAxis().scale(EARTH.radius() + 10), look, EARTH.radius());
        assertEquals(90, pole.latitudeDeg(), 1e-6);
        long[] xz = p.mapXZ(100);
        assertEquals(2900, xz[0]);
        assertEquals(-4100, xz[1]);
    }

    @Test
    void stationRendezvous() {
        var orbit = com.pizzaparker07.spacebreaker.core.astro.flight.StationRendezvous.orbit(EARTH, 400e3, 0, 1.0);
        StateVector st = orbit.stateAt(5000);
        assertEquals(EARTH.radius() + 400e3, st.position().length(), 1e-3);
        assertEquals(92.6, orbit.period() / 60, 0.5, "a 400 km orbit takes ≈ 92.6 minutes");
        // a craft 2 km behind moving 10 m/s faster: returns; 200 m/s faster or 100 km away: does not
        Vec3 along = st.velocity().normalize();
        var near = new StateVector(st.position().sub(along.scale(2000)), st.velocity().add(along.scale(10)));
        assertTrue(com.pizzaparker07.spacebreaker.core.astro.flight.StationRendezvous.returnsToPocket(near, orbit, 5000));
        var fast = new StateVector(near.position(), st.velocity().add(along.scale(200)));
        assertFalse(com.pizzaparker07.spacebreaker.core.astro.flight.StationRendezvous.returnsToPocket(fast, orbit, 5000));
        var far = new StateVector(st.position().sub(along.scale(100_000)), st.velocity());
        assertFalse(com.pizzaparker07.spacebreaker.core.astro.flight.StationRendezvous.returnsToPocket(far, orbit, 5000));
    }

    @Test
    void spaceFactor() {
        for (AstroBody b : new AstroBody[]{EARTH, MOON, SYS.require("spacebreaker:mars")}) {
            double top = b.lowOrbitRadius() - b.radius();
            assertEquals(0, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(b, 0), 1e-12, b.id());
            assertEquals(1, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(b, top), 1e-12, b.id());
            double prev = -1;
            for (double h = 0; h <= top * 1.2; h += top / 50) {
                double k = com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(b, h);
                assertTrue(k >= prev - 1e-12 && k >= 0 && k <= 1);
                prev = k;
            }
            double h = com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.altitudeForSpaceFactor(b, 0.5);
            assertEquals(0.5, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(b, h), 1e-6);
        }
        var G = com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.class;
        assertNotNull(G);
        assertEquals(1, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactorForBlocks(EARTH, 300, 300), 1e-12);
        assertEquals(0, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactorForBlocks(EARTH, -5, 300), 1e-12);
        assertEquals(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(EARTH, (EARTH.lowOrbitRadius() - EARTH.radius()) / 2),
                com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactorForBlocks(EARTH, 150, 300), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.blocksToAltitude(EARTH, 1, 0));
        // Earth's sky starts to darken around 20 km; the Moon's view changes from the ground up
        assertEquals(0, com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(EARTH, 15e3), 1e-12);
        assertTrue(com.pizzaparker07.spacebreaker.core.astro.flight.EntryGate.spaceFactor(MOON, 5e3) > 0);
    }
}
