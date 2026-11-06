package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GalaxyMapTest {
    @Test
    void localSkyAroundTheSun() {
        Galaxy g = new Galaxy(7);
        List<GalaxyMap.Marker> m = GalaxyMap.local(g, g.sun(), 20, 50);
        assertEquals("Sirius", m.get(0).nameEn(), "Sirius is the brightest star in our sky");
        assertEquals(-1.46, m.get(0).apparentMagnitude(), 0.3);
        GalaxyMap.Marker alpha = m.stream().filter(x -> x.nameEn().equals("Alpha Centauri")).findFirst().orElseThrow();
        assertEquals(0.0, alpha.apparentMagnitude(), 0.3);
        assertEquals(4.37, alpha.distanceLy(), 0.02);
        assertEquals(alpha.distanceLy(), alpha.offsetLy().length(), 1e-9);
        for (int i = 1; i < m.size(); i++) {
            assertTrue(m.get(i).apparentMagnitude() >= m.get(i - 1).apparentMagnitude());
        }
        assertTrue(m.size() <= 50);
        assertTrue(m.stream().noneMatch(x -> x.seed().id() == g.sun().id()));
        assertEquals(m, GalaxyMap.local(new Galaxy(7), new Galaxy(7).sun(), 20, 50), "deterministic");
        GalaxyMap.Marker gen = m.stream().filter(x -> !x.seed().fixed()).findFirst().orElseThrow();
        assertTrue(gen.designation().startsWith("SB "));
    }

    @Test
    void magnitudes() {
        assertEquals(4.83, GalaxyMap.apparentMagnitude(1, 10), 1e-9, "the Sun seen from 10 pc");
        assertEquals(GalaxyMap.apparentMagnitude(1, 10) + 5, GalaxyMap.apparentMagnitude(1, 100), 1e-9);
        assertTrue(GalaxyMap.apparentMagnitude(100, 10) < GalaxyMap.apparentMagnitude(1, 10));
    }

    @Test
    void overviewLooksLikeABarredSpiral() {
        GalaxyMap.Overview o = GalaxyMap.overview(96);
        int c = o.cells() / 2;
        double centre = o.brightness()[c][c];
        double sun = o.brightness()[o.sun()[0]][o.sun()[1]];
        assertEquals(1.0, centre, 0.15, "the bulge is the brightest region");
        assertTrue(sun > 0.2 && sun < centre, "the Sun sits in the bright disk " + sun);
        assertTrue(o.brightness()[0][0] < 0.1, "dark corners");
        assertTrue(o.sun()[0] < c && Math.abs(o.sun()[1] - c) <= 1, "the Sun is on the −x side");
        // the arms tint cells blue somewhere on the disk, the bulge stays yellow
        boolean blue = false;
        for (int i = 0; i < o.cells(); i++) {
            for (int j = 0; j < o.cells(); j++) {
                int col = o.color()[i][j];
                assertTrue(o.brightness()[i][j] >= 0 && o.brightness()[i][j] <= 1);
                blue |= (col & 0xFF) > (col >> 16 & 0xFF);
            }
        }
        assertTrue(blue);
        assertEquals(GalaxyMap.OLD_STARS, o.color()[c][c]);
    }
}
