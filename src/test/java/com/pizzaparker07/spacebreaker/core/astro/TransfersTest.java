package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static com.pizzaparker07.spacebreaker.core.astro.AstroConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class TransfersTest {
    private static final double MARS_A = 1.524 * AU;

    @Test
    void earthToMarsHohmann() {
        Transfers.Transfer t = Transfers.hohmann(MU_SUN, AU, MARS_A);
        assertEquals(2.94e3, t.dv1(), 15);
        assertEquals(2.65e3, t.dv2(), 15);
        assertEquals(259, t.time() / DAY, 1);
    }

    @Test
    void leoToGeoHohmann() {
        Transfers.Transfer t = Transfers.hohmann(MU_EARTH, 6678e3, 42164e3);
        assertEquals(2.42e3, t.dv1(), 15);
        assertEquals(1.46e3, t.dv2(), 15);
        assertEquals(3.89e3, t.total(), 20);
        assertEquals(5.25, t.time() / 3600, 0.05);
    }

    @Test
    void hohmannIsSymmetric() {
        Transfers.Transfer up = Transfers.hohmann(MU_SUN, AU, MARS_A);
        Transfers.Transfer down = Transfers.hohmann(MU_SUN, MARS_A, AU);
        assertEquals(up.total(), down.total(), 1e-6);
        assertEquals(up.dv1(), down.dv2(), 1e-6);
    }

    @Test
    void biEllipticBeatsHohmannForLargeRatios() {
        double r1 = 7000e3, r2 = 20 * r1;
        Transfers.Transfer h = Transfers.hohmann(MU_EARTH, r1, r2);
        Transfers.Transfer b = Transfers.biElliptic(MU_EARTH, r1, r2, 60 * r1);
        assertTrue(b.total() < h.total(), "bi-elliptic should win above ratio 15.58");
        Transfers.Transfer small = Transfers.biElliptic(MU_EARTH, r1, 5 * r1, 10 * r1);
        assertTrue(small.total() > Transfers.hohmann(MU_EARTH, r1, 5 * r1).total());
        // with rb = r2 it degenerates into a Hohmann transfer
        Transfers.Transfer same = Transfers.biElliptic(MU_EARTH, r1, r2, r2);
        assertEquals(h.total(), same.total(), 1e-6);
    }

    @Test
    void departureFromLeoToMars() {
        double vInf = Transfers.hohmann(MU_SUN, AU, MARS_A).dv1();
        double dv = Transfers.departure(MU_EARTH, EARTH_RADIUS + 200e3, vInf);
        assertEquals(3.6e3, dv, 60); // the classic ~3.6 km/s trans-Mars injection
    }

    @Test
    void escapeFromLeo() {
        assertEquals(3.2e3, Transfers.escapeFromCircular(MU_EARTH, EARTH_RADIUS + 200e3), 30);
        assertEquals(Transfers.departure(MU_EARTH, 7000e3, 0), Transfers.escapeFromCircular(MU_EARTH, 7000e3), 1e-9);
    }

    @Test
    void captureOptions() {
        double mu = 4.2828e13; // Mars
        double r = 3389.5e3 + 300e3;
        double vInf = 2.65e3;
        double circ = Transfers.captureCircular(mu, r, vInf);
        double ell = Transfers.captureElliptic(mu, r, 20 * r, vInf);
        double aero = Transfers.captureAerobrake(mu, r, 3389.5e3 + 110e3);
        assertEquals(2.09e3, circ, 50);
        assertTrue(ell < circ);
        assertTrue(aero < 100 && aero > 0, "aerobraking leaves only a small periapsis raise: " + aero);
        assertEquals(0, Transfers.captureAerobrake(mu, r, r + 1));
    }

    @Test
    void synodicPeriods() {
        assertEquals(780, Transfers.synodicPeriod(365.256, 686.980), 1);
        assertEquals(584, Transfers.synodicPeriod(365.256, 224.701), 1);
        assertEquals(398.9, Transfers.synodicPeriod(365.256, 4332.59), 0.5);
        assertEquals(29.53, Transfers.synodicPeriod(365.256, 27.3217), 0.01); // lunar phases
    }

    @Test
    void marsPhaseAngleIs44Degrees() {
        assertEquals(44.3, Math.toDegrees(Transfers.hohmannPhaseAngle(MU_SUN, AU, MARS_A)), 0.5);
        // Venus: target must trail by ~ -54°
        assertEquals(-54.0, Math.toDegrees(Transfers.hohmannPhaseAngle(MU_SUN, AU, 0.723 * AU)), 1.0);
    }

    @Test
    void timeToPhase() {
        double n1 = 2 * Math.PI / 365.256, n2 = 2 * Math.PI / 686.980; // rad/day
        double req = Math.toRadians(44);
        // already there -> zero wait
        assertEquals(0, Transfers.timeToPhase(req, req, n1, n2), 1e-9);
        // just missed the window -> one synodic period minus a bit
        double wait = Transfers.timeToPhase(req - 1e-6, req, n1, n2);
        assertEquals(Transfers.synodicPeriod(365.256, 686.980), wait, 1);
        // the result really gives the required phase
        double phase0 = Math.toRadians(120);
        double t = Transfers.timeToPhase(phase0, req, n1, n2);
        assertEquals(Kepler.wrap(req), Kepler.wrap(phase0 + (n2 - n1) * t), 1e-9);
        assertTrue(t > 0 && t < 781);
    }
}
