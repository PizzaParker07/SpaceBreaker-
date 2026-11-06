package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.TerrainHints.ColorFamily;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints.Liquid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The physics-based hints must reproduce the look of the real Solar System. */
class TerrainHintsTest {
    private static TerrainHints h(String name) {
        return TerrainHints.of(SolarSystemData.system(), "spacebreaker:" + name);
    }

    @Test
    void tidalHeating() {
        BodySystem s = SolarSystemData.system();
        AstroBody io = s.require("spacebreaker:io"), jupiter = s.require("spacebreaker:jupiter");
        assertEquals(2.0, TerrainHints.tidalHeatingFlux(io, jupiter), 0.8, "Io ~ 2 W/m² (≈ 1e14 W)");
        double europa = TerrainHints.tidalHeatingFlux(s.require("spacebreaker:europa"), jupiter);
        assertTrue(europa < 0.5 && europa > 0.01, "Europa " + europa);
        assertTrue(TerrainHints.tidalHeatingFlux(s.require("spacebreaker:moon"), s.require("spacebreaker:earth")) < 0.01);
        assertEquals(0, TerrainHints.tidalHeatingFlux(s.require("spacebreaker:mars"), s.require("spacebreaker:sun")));
    }

    @Test
    void boilingPoints() {
        assertEquals(373.15, TerrainHints.boilingPoint(373.15, 2.26e6, 0.018, 101.325), 1e-9);
        assertEquals(273.5, TerrainHints.boilingPoint(373.15, 2.26e6, 0.018, 0.62), 15, "water barely liquid at Mars pressure");
        assertTrue(TerrainHints.boilingPoint(111.7, 5.1e5, 0.016, 146.7) > 94, "methane stays liquid on Titan");
    }

    @Test
    void solarSystemLooksRight() {
        assertEquals(ColorFamily.GREY_REGOLITH, h("moon").colorFamily());
        assertEquals(ColorFamily.GREY_REGOLITH, h("mercury").colorFamily());
        assertEquals(ColorFamily.RUST, h("mars").colorFamily());
        assertEquals(ColorFamily.DARK_BASALT, h("venus").colorFamily());
        assertEquals(ColorFamily.OCEAN, h("earth").colorFamily());
        assertEquals(Liquid.WATER, h("earth").liquid());
        assertEquals(ColorFamily.SULFUR, h("io").colorFamily());
        assertEquals(Liquid.LAVA, h("io").liquid());
        assertEquals(1.0, h("io").volcanism(), 1e-9);
        assertEquals(ColorFamily.PALE_ICE, h("europa").colorFamily());
        assertEquals(ColorFamily.PALE_ICE, h("enceladus").colorFamily());
        assertEquals(ColorFamily.DIRTY_ICE, h("callisto").colorFamily());
        assertEquals(ColorFamily.ORGANIC_HAZE, h("titan").colorFamily());
        assertEquals(Liquid.METHANE, h("titan").liquid());
        assertEquals(ColorFamily.GAS_BANDS, h("jupiter").colorFamily());
        assertEquals(Liquid.NONE, h("mars").liquid());
    }

    @Test
    void cratersFollowAirAndActivity() {
        assertTrue(h("moon").craterDensity() > 5e-3);
        assertTrue(h("callisto").craterDensity() > h("europa").craterDensity(), "Europa's young ice");
        assertTrue(h("io").craterDensity() < 1e-6, "Io has no craters");
        assertTrue(h("venus").craterDensity() <= 2e-6);
        assertTrue(h("mars").craterDensity() < h("moon").craterDensity());
        assertTrue(h("earth").craterDensity() < h("mars").craterDensity());
    }

    @Test
    void iceAndDunes() {
        assertTrue(h("europa").glaciation() > 0.9);
        assertTrue(h("mars").glaciation() > 0.02 && h("mars").glaciation() < 0.2, "polar caps");
        assertTrue(h("moon").glaciation() > 0 && h("moon").glaciation() < 0.02, "polar cold traps");
        assertEquals(0, h("io").glaciation());
        assertTrue(h("mars").dunes() && h("titan").dunes() && h("earth").dunes());
        assertFalse(h("moon").dunes());
    }

    @Test
    void hintsAreDeterministicAndInRange() {
        for (AstroBody b : SolarSystemData.system().all()) {
            TerrainHints a = TerrainHints.of(SolarSystemData.system(), b.id());
            assertEquals(a, TerrainHints.of(SolarSystemData.system(), b.id()));
            for (double v : new double[]{a.liquidCoverage(), a.glaciation(), a.volcanism(), a.roughness()}) {
                assertTrue(v >= 0 && v <= 1, b.id() + " " + a);
            }
            assertTrue(a.craterDensity() >= 0 && a.baseColor() >= 0 && a.baseColor() <= 0xFFFFFF);
        }
    }
}
