package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Calibration for Miranda, Triton, Pluto and Charon. */
class TransNeptunianTerrainTest {
    private static TerrainHints h(String n) {
        return TerrainHints.of(SolarSystemData.system(), "spacebreaker:" + n);
    }

    private static TerrainParams p(String n) {
        return TerrainParams.of(SolarSystemData.body(n), h(n));
    }

    @Test
    void tritonIsYoungAndActive() {
        assertTrue(p("triton").craterDensity() < 0.6, "Triton's surface is 10–100 Myr old: " + p("triton").craterDensity());
        assertTrue(p("triton").craterDensity() < p("pluto").craterDensity());
        assertTrue(h("triton").plumes(), "nitrogen geysers");
        assertTrue(h("triton").volatileIce() > 0, "nitrogen ice cap");
        assertTrue(h("triton").volcanism() >= 0.5, "cryovolcanism from capture heating");
        assertEquals(TerrainHints.ColorFamily.PALE_ICE, h("triton").colorFamily());
    }

    @Test
    void plutoHasNitrogenGlaciersButNoGeysers() {
        assertTrue(p("pluto").volatileGlaciers(), "Sputnik Planitia");
        assertFalse(h("pluto").plumes());
        assertTrue(p("pluto").craterDensity() > 0.7 && p("pluto").craterDensity() < 1.0, "cratered outside the glaciers");
        assertEquals(TerrainHints.ColorFamily.PALE_ICE, h("pluto").colorFamily());
        @SuppressWarnings("unchecked")
        Map<String, Object> atm = (Map<String, Object>) SolarSystemData.json(SolarSystemData.DIR.resolve("pluto.json")).get("atmosphere");
        @SuppressWarnings("unchecked")
        Map<String, Object> haze = (Map<String, Object>) atm.get("haze");
        assertNotNull(haze, "Pluto's blue haze");
        String c = (String) haze.get("colour");
        assertTrue(c.matches("#[0-9a-f]{6}"));
        int rgb = Integer.parseInt(c.substring(1), 16);
        assertTrue((rgb & 0xFF) > (rgb >> 16 & 0xFF), "the haze scatters blue");
        assertTrue((Double) haze.get("visibility_blocks") > 160, "thin air: clearer than Titan");
    }

    @Test
    void charonAndMirandaAreOldAndRugged() {
        assertEquals(TerrainHints.ColorFamily.DIRTY_ICE, h("charon").colorFamily());
        assertEquals(TerrainHints.ColorFamily.DIRTY_ICE, h("miranda").colorFamily());
        assertTrue(p("charon").craterDensity() > 0.9 && p("miranda").craterDensity() > 0.9);
        assertFalse(h("charon").plumes() || h("miranda").plumes());
        assertFalse(p("charon").volatileGlaciers(), "no atmosphere to freeze out");
        assertTrue(p("miranda").reliefAmplitude() > p("moon").reliefAmplitude(), "Verona Rupes and the coronae");
        assertTrue(p("miranda").complexRadius() >= 55, "tiny gravity: wide simple craters");
    }

    @Test
    void earlierBodiesUnchanged() {
        assertFalse(h("europa").volatileIce() > 0);
        assertTrue(h("enceladus").plumes(), "Enceladus plumes");
        assertFalse(h("callisto").plumes());
        assertFalse(h("mars").plumes() || h("moon").plumes());
    }
}
