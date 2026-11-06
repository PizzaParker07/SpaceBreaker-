package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GravityAssistTest {
    private static final String EARTH = "spacebreaker:earth", MERCURY = "spacebreaker:mercury", VENUS = "spacebreaker:venus",
            MARS = "spacebreaker:mars", JUPITER = "spacebreaker:jupiter";

    private static BodySystem sys() {
        return SolarSystemData.system();
    }

    @Test
    void mercuryViaVenusIsMuchCheaper() {
        TransferPlanner.Plan direct = TransferPlanner.plan(sys(), EARTH, MERCURY, false);
        GravityAssist.Tour tour = GravityAssist.tour(sys(), EARTH, MERCURY, false).orElseThrow();
        TransferPlanner.Plan p = tour.plan();
        assertEquals(VENUS, tour.flybyBody());
        assertEquals(13.1e3, direct.total(), 400, "direct Hohmann to low Mercury orbit");
        assertEquals(8.3e3, p.total(), 700, "MESSENGER-class budget to a low orbit");
        assertTrue(direct.total() - p.total() > 4000, "saves more than 4 km/s");
        assertEquals(3.5e3, p.deltaV("departure"), 150, "same injection as for Venus");
        assertTrue(p.deltaV("capture") < 2.0e3, "slow arrival after the Mercury flybys");
        assertTrue(tour.targetFlybys() >= 2 && tour.targetFlybys() <= 6);
        double years = p.transferTime() / AstroConstants.YEAR;
        assertTrue(years > 2.5 && years < 5.5, "years longer than the 105-day Hohmann: " + years);
        assertEquals(584, p.synodicPeriod() / AstroConstants.DAY, 1, "windows of the Earth→Venus leg");
        double sum = 0;
        for (TransferPlanner.Leg l : p.legs()) {
            assertTrue(l.deltaV() >= 0);
            sum += l.deltaV();
        }
        assertEquals(p.total(), sum, 1e-9);
    }

    @Test
    void returnTripMirrorsTheTour() {
        TransferPlanner.Plan out = GravityAssist.tour(sys(), EARTH, MERCURY, false).orElseThrow().plan();
        TransferPlanner.Plan back = GravityAssist.tour(sys(), MERCURY, EARTH, false).orElseThrow().plan();
        assertEquals(out.total(), back.total(), 1e-6);
        assertEquals(out.transferTime(), back.transferTime(), 1e-6);
        TransferPlanner.Plan aero = GravityAssist.tour(sys(), MERCURY, EARTH, true).orElseThrow().plan();
        assertTrue(aero.deltaV("capture") < 100, "aerocapture at Earth on the way home");
    }

    @Test
    void planWithAssistsPicksTheCheaperRoute() {
        assertTrue(TransferPlanner.planWithAssists(sys(), EARTH, MERCURY, false).total()
                < TransferPlanner.plan(sys(), EARTH, MERCURY, false).total());
        // no planet in between, or outside the calibrated inner system: the direct plan is unchanged
        for (String to : new String[]{MARS, VENUS, JUPITER, "spacebreaker:moon"}) {
            assertEquals(TransferPlanner.plan(sys(), EARTH, to, false), TransferPlanner.planWithAssists(sys(), EARTH, to, false), to);
        }
        assertTrue(GravityAssist.tour(sys(), EARTH, JUPITER, false).isEmpty());
        assertTrue(GravityAssist.tour(sys(), MARS, MERCURY, false).isPresent(), "Mars → Mercury via Venus (or Earth)");
    }
}
