package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.galaxy.Galaxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterstellarTest {
    @Test
    void nearestStarsFromTheSun() {
        Galaxy g = new Galaxy(42);
        List<Interstellar.Destination> d = Interstellar.nearby(g, g.sun(), 15);
        assertEquals("Proxima Centauri", d.get(0).nameEn());
        assertEquals(4.24, d.get(0).distanceLy(), 0.02);
        assertEquals("Alfa Centauri", d.get(1).nameTr());
        for (int i = 1; i < d.size(); i++) {
            assertTrue(d.get(i).distanceLy() >= d.get(i - 1).distanceLy());
            assertTrue(d.get(i).distanceLy() <= 15);
        }
        assertTrue(d.stream().anyMatch(x -> x.nameEn().equals("Sirius")));
        assertTrue(d.stream().noneMatch(x -> x.star().id() == g.sun().id()));
        assertEquals(d, Interstellar.nearby(new Galaxy(42), new Galaxy(42).sun(), 15), "deterministic");
        assertTrue(d.size() > 10, "generated neighbours fill the gaps: " + d.size());
    }

    @Test
    void relativisticTrips() {
        Interstellar.Trip t = Interstellar.trip(4.24, 0.1);
        assertEquals(42.4, t.travelYears(), 1e-9);
        assertEquals(42.4 * Math.sqrt(1 - 0.01), t.shipYears(), 1e-9);
        assertEquals(2 * Interstellar.C * 0.10034, t.deltaV(), 1e5);
        assertTrue(Interstellar.trip(4.24, 0.9).shipYears() < Interstellar.trip(4.24, 0.9).travelYears() * 0.5);
        assertThrows(IllegalArgumentException.class, () -> Interstellar.trip(1, 1));
        assertEquals(424, Interstellar.scaledTravelSeconds(4.24, 100), 1e-9);
    }
}
