package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemCardTest {
    private static final Galaxy G = new Galaxy(77);

    private static SystemCard card(String key) {
        return SystemCard.of(G, G.fixedStar(key).orElseThrow());
    }

    @Test
    void confirmedVersusUndiscovered() {
        SystemCard alpha = card("alpha_centauri");
        assertEquals(0, alpha.knownPlanets(), "no confirmed planets around Alpha Centauri");
        assertEquals(alpha.planets().size(), alpha.undiscoveredPlanets());
        assertTrue(alpha.binary());
        SystemCard proxima = card("proxima_centauri");
        assertEquals(2, proxima.knownPlanets());
        SystemCard.PlanetCard b = proxima.planets().stream().filter(p -> p.id().equals("spacebreaker:proxima_centauri_b")).findFirst().orElseThrow();
        assertTrue(b.discovered() && b.inHabitableZone() && b.landable());
        assertEquals(11.19, b.periodDays(), 0.2);
        assertEquals(4, card("barnards_star").knownPlanets());
        assertEquals(7, card("trappist_1").knownPlanets());
        assertEquals(0, card("trappist_1").undiscoveredPlanets());
        assertEquals("Barnard Yıldızı", card("barnards_star").nameTr());
    }

    @Test
    void planetsAreOrderedAndPlausible() {
        for (NearbyStars.Entry e : NearbyStars.ALL) {
            SystemCard c = card(e.key());
            assertEquals(c.knownPlanets() + c.undiscoveredPlanets(), c.planets().size(), e.key());
            if (!e.key().equals("sol")) {
                assertEquals(e.knownPlanets(), c.knownPlanets(), e.key());
            }
            double prev = 0;
            for (SystemCard.PlanetCard p : c.planets()) {
                assertTrue(p.semiMajorAxisAu() > prev, e.key() + " ordered by distance");
                prev = p.semiMajorAxisAu();
                assertTrue(p.massEarths() > 0 && p.radiusEarths() > 0 && p.equilibriumTempK() > 0 && p.periodDays() > 0);
                assertEquals(p.kind().isGiant(), !p.landable());
            }
            assertTrue(c.habitableZoneInnerAu() < c.habitableZoneOuterAu());
        }
        assertTrue(card("sol").planets().isEmpty(), "the Sun's planets come from the data registry");
        assertTrue(card("trappist_1").habitableZonePlanets().stream().anyMatch(p -> p.nameEn().equals("TRAPPIST-1 e")));
        assertEquals(AstroBody.Kind.GAS_GIANT, card("epsilon_eridani").planets().stream()
                .filter(p -> p.id().equals("spacebreaker:epsilon_eridani_b")).findFirst().orElseThrow().kind());
    }

    @Test
    void generatedStarsHaveOnlyUndiscoveredPlanets() {
        for (StarSeed s : G.starsWithin(GalaxyModel.SUN.add(new com.pizzaparker07.spacebreaker.core.astro.Vec3(30, 0, 0)), 4)) {
            if (s.fixed()) {
                continue;
            }
            SystemCard c = SystemCard.of(G, s);
            assertEquals(0, c.knownPlanets(), "nobody has found planets around a generated star yet");
            assertTrue(c.designation().startsWith("SB "));
            assertTrue(c.distanceLy() >= NearbyStars.CATALOGUE_RADIUS_LY);
        }
    }
}
