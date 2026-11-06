package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The mapper must land close to the hand-made terrain profiles of the real bodies. */
class TerrainParamsTest {
    private static TerrainParams p(String name) {
        AstroBody b = SolarSystemData.body(name);
        return TerrainParams.of(b, TerrainHints.of(SolarSystemData.system(), b.id()));
    }

    @Test
    void cratersMatchTheHandMadeProfiles() {
        assertEquals(1.0, p("moon").craterDensity(), 0.25);       // moon.json: 1.0
        assertEquals(1.05, p("mercury").craterDensity(), 0.25);   // mercury.json: 1.05
        assertEquals(0.35, p("venus").craterDensity(), 0.2);      // venus.json: 0.35
        assertTrue(p("mars").craterDensity() > p("venus").craterDensity());
        assertEquals(0.2, p("moon").depthRatio(), 1e-9);
        assertEquals(0.12, p("venus").depthRatio(), 1e-9);
        assertEquals(34, p("moon").complexRadius(), 0.5);
        assertTrue(p("phobos").complexRadius() > p("mars").complexRadius(), "low gravity, wider simple craters");
    }

    @Test
    void landformsAndLiquids() {
        TerrainParams mars = p("mars");
        assertTrue(mars.dunes() && mars.iceCap());
        assertTrue(mars.iceLatitude() > 55 && mars.iceLatitude() < 85, "Mars caps at " + mars.iceLatitude());
        assertEquals(-1, mars.seaLevel());
        assertEquals(0, p("europa").iceLatitude(), "an ice world");
        assertTrue(p("io").volcanoes());
        assertTrue(p("earth").seaLevel() > 0 && p("earth").valleys());
        assertEquals(TerrainHints.Liquid.METHANE, p("titan").liquid());
        assertTrue(p("titan").seaLevel() > 0 && p("titan").seaLevel() < TerrainParams.BASE_HEIGHT);
        assertEquals(95, p("venus").iceLatitude(), 1e-9);
        assertTrue(p("moon").iceLatitude() > 80, "only polar cold traps on the Moon");
    }

    @Test
    void referenceProfilesForExistingBlocks() {
        assertEquals("spacebreaker:moon", p("moon").referenceProfile().orElseThrow());
        assertEquals("spacebreaker:mars", p("mars").referenceProfile().orElseThrow());
        assertEquals("spacebreaker:venus", p("venus").referenceProfile().orElseThrow());
        assertTrue(p("europa").referenceProfile().isEmpty(), "needs ice blocks");
    }

    @Test
    void valuesStayInRangeForEveryLandableBody() {
        for (AstroBody b : SolarSystemData.system().all()) {
            if (!b.landable()) {
                continue;
            }
            TerrainParams t = p(b.id().substring("spacebreaker:".length()));
            assertTrue(t.reliefAmplitude() > 0 && t.reliefAmplitude() < 120, b.id());
            assertTrue(t.craterDensity() >= 0.05 && t.craterDensity() <= 1.3, b.id());
            assertTrue(t.iceLatitude() >= 0 && t.iceLatitude() <= 95 && t.polarLatitude() <= t.iceLatitude() + 1e-9, b.id());
            assertTrue(t.seaLevel() == -1 || t.seaLevel() <= TerrainParams.BASE_HEIGHT, b.id());
        }
    }
}
