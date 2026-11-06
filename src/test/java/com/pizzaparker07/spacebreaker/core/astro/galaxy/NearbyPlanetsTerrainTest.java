package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;
import com.pizzaparker07.spacebreaker.core.astro.TerrainParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hints and terrain parameters of the planets around real nearby stars stay physically consistent. */
class NearbyPlanetsTerrainTest {
    private static final long[] SEEDS = {1, 42, 20261106L};

    private static void eachPlanet(java.util.function.BiConsumer<StarSystem, AstroBody> check) {
        for (long seed : SEEDS) {
            Galaxy g = new Galaxy(seed);
            for (StarSeed f : g.fixedStars()) {
                g.system(f).ifPresent(sys -> sys.bodies().all().stream().filter(b -> !b.isStar()).forEach(b -> check.accept(sys, b)));
            }
        }
    }

    @Test
    void liquidsAndIceMatchTheTemperatures() {
        eachPlanet((sys, b) -> {
            TerrainHints h = sys.terrainHints(b.id());
            double t = b.meanTempK();
            switch (h.liquid()) {
                case WATER -> assertTrue(t > 273.15 && b.surfacePressureKpa() > 0.6, b.id() + " water at " + t);
                case METHANE -> assertTrue(t > 90 && t < 200, b.id());
                case LAVA -> assertTrue(t > 1000 || h.volcanism() >= 0.9, b.id() + " lava at " + t);
                default -> { }
            }
            if (h.liquid() == TerrainHints.Liquid.LAVA && t > 1000) {
                assertEquals(TerrainHints.ColorFamily.DARK_BASALT, h.colorFamily(), b.id() + ": molten surfaces look dark");
            }
            if (b.nightTempK() > 300) {
                assertTrue(h.glaciation() < 0.01, b.id() + " too warm for ice");
            }
            if (b.kind().isGiant()) {
                assertEquals(TerrainHints.ColorFamily.GAS_BANDS, h.colorFamily());
            } else {
                TerrainParams p = TerrainParams.of(b, h);
                assertTrue(p.craterDensity() >= 0.05 && p.craterDensity() <= 1.3, b.id());
                assertTrue(p.reliefAmplitude() > 0 && p.reliefAmplitude() < 150, b.id());
                assertTrue(p.iceLatitude() >= 0 && p.iceLatitude() <= 95, b.id());
            }
        });
    }

    @Test
    void trappist1GetsColderOutwards() {
        for (long seed : SEEDS) {
            Galaxy g = new Galaxy(seed);
            StarSystem t = g.system(g.fixedStar("trappist_1").orElseThrow()).orElseThrow();
            double prev = Double.MAX_VALUE;
            for (char c = 'b'; c <= 'h'; c++) {
                AstroBody p = t.bodies().require("spacebreaker:trappist_1_" + c);
                double irr = t.details(p.id()).irradiance();
                assertTrue(irr < prev, "irradiance falls outwards");
                prev = irr;
                assertTrue(p.landable());
                assertNotEquals(TerrainHints.ColorFamily.GAS_BANDS, t.terrainHints(p.id()).colorFamily());
            }
            // b receives ~4 times Earth's sunlight: never icy
            assertTrue(t.terrainHints("spacebreaker:trappist_1_b").glaciation() < 0.05);
        }
    }

    @Test
    void sameSeedSameHints() {
        Galaxy a = new Galaxy(99), b = new Galaxy(99);
        StarSystem pa = a.system(a.fixedStar("proxima_centauri").orElseThrow()).orElseThrow();
        StarSystem pb = b.system(b.fixedStar("proxima_centauri").orElseThrow()).orElseThrow();
        assertEquals(pa.terrainHints("spacebreaker:proxima_centauri_b"), pb.terrainHints("spacebreaker:proxima_centauri_b"));
        StarSystem eps = a.system(a.fixedStar("epsilon_eridani").orElseThrow()).orElseThrow();
        assertEquals(TerrainHints.ColorFamily.GAS_BANDS, eps.terrainHints("spacebreaker:epsilon_eridani_b").colorFamily());
    }
}
