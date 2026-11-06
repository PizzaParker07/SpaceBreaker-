package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.TerrainHints.ColorFamily;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints.Liquid;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Calibration of hints and terrain parameters for the asteroids and the moons of Jupiter and Saturn. */
class OuterTerrainTest {
    private static TerrainHints h(String n) {
        return TerrainHints.of(SolarSystemData.system(), "spacebreaker:" + n);
    }

    private static TerrainParams p(String n) {
        return TerrainParams.of(SolarSystemData.body(n), h(n));
    }

    @Test
    void colourFamilies() {
        assertEquals(ColorFamily.GREY_REGOLITH, h("ceres").colorFamily());
        assertEquals(ColorFamily.GREY_REGOLITH, h("vesta").colorFamily());
        assertEquals(ColorFamily.SULFUR, h("io").colorFamily());
        assertEquals(ColorFamily.PALE_ICE, h("europa").colorFamily());
        assertEquals(ColorFamily.DIRTY_ICE, h("ganymede").colorFamily());
        assertEquals(ColorFamily.DIRTY_ICE, h("callisto").colorFamily());
        assertEquals(ColorFamily.ORGANIC_HAZE, h("titan").colorFamily());
        assertEquals(ColorFamily.PALE_ICE, h("enceladus").colorFamily());
    }

    @Test
    void craterRanking() {
        // Callisto is the most cratered; Europa, Titan and Io are young surfaces
        assertTrue(p("callisto").craterDensity() >= p("ganymede").craterDensity());
        assertTrue(p("ganymede").craterDensity() > p("enceladus").craterDensity());
        assertTrue(p("enceladus").craterDensity() > p("europa").craterDensity());
        assertEquals(0.35, p("europa").craterDensity(), 0.15);
        assertEquals(0.4, p("titan").craterDensity(), 0.15);
        assertEquals(0.05, p("io").craterDensity(), 1e-9, "no impact craters on Io");
        assertTrue(p("ceres").craterDensity() > 0.9 && p("vesta").craterDensity() > 0.9);
        assertTrue(p("vesta").complexRadius() > p("moon").complexRadius(), "low gravity: wider simple craters");
    }

    @Test
    void reliefAndIce() {
        assertTrue(p("europa").reliefAmplitude() < p("moon").reliefAmplitude(), "Europa is flat");
        assertTrue(p("vesta").reliefAmplitude() > p("moon").reliefAmplitude(), "Vesta has huge relief");
        assertEquals(0, p("europa").iceLatitude(), 1e-9);
        assertEquals(0, p("enceladus").iceLatitude(), 1e-9);
        assertTrue(h("callisto").glaciation() < 0.3, "Callisto's dark dust hides its ice");
        assertTrue(h("ceres").glaciation() < 0.3, "Ceres is dark clay over ice");
        assertFalse(p("titan").iceCap(), "Titan has lakes at the poles, not ice caps");
        assertTrue(p("io").volcanoes());
        assertEquals(Liquid.LAVA, h("io").liquid());
        assertEquals(-1, p("io").seaLevel(), "lava lakes are features, not seas");
    }

    @Test
    void titanMethaneSeasAndHaze() {
        TerrainParams t = p("titan");
        assertEquals(Liquid.METHANE, t.liquid());
        assertTrue(h("titan").liquidCoverage() >= 0.01 && h("titan").liquidCoverage() <= 0.05, "lakes cover ~2 %");
        assertTrue(t.dunes(), "equatorial organic dunes");
        assertTrue(t.valleys(), "methane river channels");
        @SuppressWarnings("unchecked")
        Map<String, Object> atm = (Map<String, Object>) SolarSystemData.json(SolarSystemData.DIR.resolve("titan.json")).get("atmosphere");
        @SuppressWarnings("unchecked")
        Map<String, Object> haze = (Map<String, Object>) atm.get("haze");
        assertNotNull(haze, "Titan needs haze data");
        assertTrue(((String) haze.get("colour")).matches("#[0-9a-f]{6}"));
        assertTrue(((String) haze.get("high_colour")).matches("#[0-9a-f]{6}"));
        assertTrue((Double) haze.get("visibility_blocks") > 64, "clearer than Venus at the surface");
    }

    @Test
    void tidalHeatOrder() {
        BodySystem s = SolarSystemData.system();
        double io = TerrainHints.tidalHeatingFlux(s.require("spacebreaker:io"), s.require("spacebreaker:jupiter"));
        double eu = TerrainHints.tidalHeatingFlux(s.require("spacebreaker:europa"), s.require("spacebreaker:jupiter"));
        double ga = TerrainHints.tidalHeatingFlux(s.require("spacebreaker:ganymede"), s.require("spacebreaker:jupiter"));
        double ca = TerrainHints.tidalHeatingFlux(s.require("spacebreaker:callisto"), s.require("spacebreaker:jupiter"));
        assertTrue(io > eu && eu > ga && ga > ca);
    }
}
