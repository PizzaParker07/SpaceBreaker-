package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The real-star neighbourhood: positions, confirmed planets, names and the "not yet discovered" flag. */
class NearbyStarsCatalogueTest {
    private static final Galaxy G = new Galaxy(2026);

    private static StarSystem sys(String key) {
        return G.system(G.fixedStar(key).orElseThrow()).orElseThrow();
    }

    @Test
    void equatorialToGalactic() {
        double[] centre = NearbyStars.galactic(17.7611, -29.0078);   // Sgr A*
        assertEquals(0, Math.min(centre[0], 360 - centre[0]), 0.1);
        assertEquals(0, centre[1], 0.1);
        double[] proxima = NearbyStars.galactic(14.495, -62.679);
        assertEquals(313.94, proxima[0], 0.1);
        assertEquals(-1.93, proxima[1], 0.1);
        double[] pole = NearbyStars.galactic(12.857, 27.13);          // north galactic pole
        assertEquals(90, pole[1], 0.1);
    }

    @Test
    void onlyRealStarsInsideTheCatalogueRadius() {
        double pc = NearbyStars.CATALOGUE_RADIUS_LY * AstroConstants.LIGHT_YEAR / AstroConstants.PARSEC;
        List<StarSeed> near = G.starsWithin(GalaxyModel.SUN, pc);
        assertTrue(near.stream().allMatch(StarSeed::fixed), "no made-up stars within 25 ly");
        assertTrue(near.size() >= 40, "the catalogue lists " + near.size());
        long inside = G.fixedStars().stream().filter(s -> s.position().distance(GalaxyModel.SUN) <= pc).count();
        assertEquals(inside, near.size());
        for (NearbyStars.Entry e : NearbyStars.ALL) {
            if (!e.key().equals("sol")) {
                assertEquals(e.distanceLy(), G.fixedStar(e.key()).orElseThrow().distanceFromSun() * AstroConstants.PARSEC / AstroConstants.LIGHT_YEAR, 1e-6);
                assertFalse(e.nameTr().isBlank());
            }
        }
    }

    @Test
    void confirmedPlanetCounts() {
        Map<String, Integer> known = Map.of("proxima_centauri", 2, "barnards_star", 4, "alpha_centauri", 0, "tau_ceti", 0,
                "epsilon_eridani", 1, "gliese_876", 4, "hd_219134", 6, "trappist_1", 7, "teegardens_star", 3, "wolf_359", 0);
        known.forEach((k, n) -> assertEquals(n, NearbyStars.entry(k).orElseThrow().knownPlanets(), k));
        for (var e : known.entrySet()) {
            StarSystem s = sys(e.getKey());
            long discovered = s.planets().stream().filter(p -> s.details(p.id()).discovered()).count();
            assertEquals((long) e.getValue(), discovered, e.getKey() + ": discovered planets");
        }
        // Alpha Centauri: anything the generator adds is marked as not yet discovered
        StarSystem alpha = sys("alpha_centauri");
        assertTrue(alpha.planets().stream().noneMatch(p -> alpha.details(p.id()).discovered()));
        assertTrue(alpha.details(alpha.rootId()).discovered(), "the star itself is known");
    }

    @Test
    void planetPeriodsMatchTheDiscoveryPapers() {
        Object[][] refs = {
                {"proxima_centauri", "spacebreaker:proxima_centauri_b", 11.19}, {"proxima_centauri", "spacebreaker:proxima_centauri_d", 5.12},
                {"barnards_star", "spacebreaker:barnards_star_b", 3.15}, {"barnards_star", "spacebreaker:barnards_star_e", 6.74},
                {"teegardens_star", "spacebreaker:teegardens_star_b", 4.91}, {"ross_128", "spacebreaker:ross_128_b", 9.87},
                {"luytens_star", "spacebreaker:luytens_star_b", 18.65}, {"wolf_1061", "spacebreaker:wolf_1061_c", 17.87},
                {"lalande_21185", "spacebreaker:lalande_21185_b", 12.95}, {"gliese_876", "spacebreaker:gliese_876_b", 61.1},
                {"gliese_581", "spacebreaker:gliese_581_e", 3.15}, {"yz_ceti", "spacebreaker:yz_ceti_b", 2.02},
                {"gj_1061", "spacebreaker:gj_1061_c", 6.69}, {"lacaille_9352", "spacebreaker:lacaille_9352_b", 9.26},
        };
        for (Object[] r : refs) {
            AstroBody p = sys((String) r[0]).bodies().require((String) r[1]);
            double days = p.orbit().period() / AstroConstants.DAY;
            assertEquals((Double) r[2], days, (Double) r[2] * 0.06, r[1] + " period " + days);
        }
    }

    @Test
    void namesKindsAndCompanions() {
        assertEquals("Barnard Yıldızı", NearbyStars.entry("barnards_star").orElseThrow().nameTr());
        assertEquals("Kapteyn Yıldızı", NearbyStars.entry("kapteyns_star").orElseThrow().nameTr());
        assertEquals("Wolf 359", NearbyStars.entry("wolf_359").orElseThrow().nameTr());
        assertEquals(StarKind.WHITE_DWARF, StellarSummary.of(G.fixedStar("van_maanens_star").orElseThrow()).kind());
        StarSystem procyon = sys("procyon");
        assertTrue(procyon.binary().isPresent());
        assertTrue(procyon.bodies().require("spacebreaker:procyon_b").radius() < 0.02 * AstroConstants.SOLAR_RADIUS, "white dwarf companion");
        assertEquals("Procyon A", procyon.nameEn());
        for (AstroBody p : sys("alpha_centauri").planets()) {
            assertTrue(p.orbit().apoapsis() / AstroConstants.AU <= 0.3 * 23.4 * (1 - 0.5179) + 1e-9, "stable around A");
        }
    }

    @Test
    void brightestNeighbours() {
        List<GalaxyMap.Marker> sky = GalaxyMap.local(G, G.sun(), 26, 10);
        assertEquals("Sirius", sky.get(0).nameEn());
        assertTrue(sky.stream().anyMatch(m -> m.nameEn().equals("Vega") && m.apparentMagnitude() < 0.5));
        assertTrue(sky.stream().anyMatch(m -> m.nameEn().equals("Procyon") && Math.abs(m.apparentMagnitude() - 0.34) < 0.4));
        assertTrue(sky.stream().anyMatch(m -> m.nameEn().equals("Altair") && Math.abs(m.apparentMagnitude() - 0.77) < 0.4));
    }
}
