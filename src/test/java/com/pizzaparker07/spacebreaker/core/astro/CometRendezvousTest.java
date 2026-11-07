package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometRendezvous;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometRendezvous.Point;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometRendezvous.Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CometRendezvousTest {
    private static final BodySystem SYS = SolarSystemData.system();

    @Test
    void tableForAllComets() throws IOException {
        StringBuilder sb = new StringBuilder("| Comet | Arrive near | Arrival (AU) | Departure from LEO | Velocity match | Total | Flight |\n|---|---|---:|---:|---:|---:|---:|\n");
        for (Comet c : Comet.CATALOGUE) {
            for (Point p : Point.values()) {
                Result r = CometRendezvous.estimate(SYS, c, p, 0).orElseThrow(() -> new AssertionError(c.id() + " " + p));
                assertTrue(r.totalDv() > 0 && Double.isFinite(r.totalDv()));
                assertEquals(r.arrivalTime() - r.departureTime(), r.flightSeconds(), 1);
                sb.append(String.format(Locale.ROOT, "| %s | %s | %.2f | %.1f km/s | %.1f km/s | %.1f km/s | %.2f y |\n", c.nameEn(),
                        p.name().toLowerCase(Locale.ROOT), r.arrivalAu(), r.departureDv() / 1000, r.arrivalDv() / 1000,
                        r.totalDv() / 1000, r.flightYears()));
            }
        }
        // Halley is retrograde: matching it near perihelion costs about 50 km/s, out of reach for chemical rockets
        assertTrue(CometRendezvous.estimate(SYS, Comet.HALLEY, Point.PERIHELION, 0).orElseThrow().totalDv() > 40e3);
        // slow far out: catching 67P near aphelion is cheaper than near perihelion
        assertTrue(CometRendezvous.estimate(SYS, Comet.CHURYUMOV_GERASIMENKO, Point.APHELION, 0).orElseThrow().totalDv()
                < CometRendezvous.estimate(SYS, Comet.CHURYUMOV_GERASIMENKO, Point.PERIHELION, 0).orElseThrow().totalDv());
        // Deep Impact reached Tempel 1 directly (flyby, no match); a full rendezvous is still well under 15 km/s
        assertTrue(CometRendezvous.best(SYS, Comet.TEMPEL_1, 0).orElseThrow().totalDv() < 15e3);
        // Hale–Bopp: no affordable rendezvous at either end
        assertTrue(CometRendezvous.best(SYS, Comet.HALE_BOPP, 0).orElseThrow().totalDv() > 25e3);
        Files.createDirectories(Path.of("build/astro"));
        Files.writeString(Path.of("build/astro/comet_rendezvous.md"), sb.toString());
    }

    @Test
    void precomputedTable() {
        long start = System.nanoTime();
        CometRendezvous.Table table = CometRendezvous.Table.compute(SYS, 0, 2);
        double seconds = (System.nanoTime() - start) / 1e9;
        assertTrue(seconds < 20, "table took " + seconds + " s");
        for (Comet c : Comet.CATALOGUE) {
            var list = table.of(c.id());
            assertEquals(4, list.size(), c.id());
            for (int i = 1; i < list.size(); i++) {
                assertTrue(list.get(i).arrivalTime() > list.get(i - 1).arrivalTime(), "arrival order");
            }
            assertTrue(list.stream().allMatch(r -> r.cometId().equals(c.id())));
            // two passages: the second perihelion is one period after the first
            var peri = list.stream().filter(r -> r.point() == Point.PERIHELION).toList();
            assertEquals(c.periodSeconds(), peri.get(1).arrivalTime() - peri.get(0).arrivalTime(), 121 * AstroConstants.DAY);
        }
        assertTrue(table.of("nope").isEmpty());
        double chemical = 15e3, years = 20;
        assertTrue(table.reachable("churyumov_gerasimenko", 0, chemical, years));
        assertTrue(table.reachable("tempel_1", 0, chemical, years));
        assertTrue(table.reachable("encke", 0, chemical, years));
        assertFalse(table.reachable("halley", 0, 30e3, years), "Halley: too fast near perihelion, too far near aphelion");
        assertFalse(table.reachable("hale_bopp", 0, 30e3, years));
        assertTrue(table.reachable("halley", 0, 60e3, years), "a late-game drive reaches Halley near perihelion");
        var cheap = table.cheapest("churyumov_gerasimenko", 0, chemical, years).orElseThrow();
        var soon = table.soonest("churyumov_gerasimenko", 0, 1e9, 1e9).orElseThrow();
        assertTrue(cheap.totalDv() <= soon.totalDv() || cheap.equals(soon));
        assertTrue(soon.arrivalTime() <= cheap.arrivalTime());
        // options whose departure has passed drop out
        double later = table.of("encke").get(0).departureTime() + 1;
        assertEquals(table.of("encke").size() - 1, table.upcoming("encke", later).size());
    }
}
