package com.pizzaparker07.spacebreaker.core.astro;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Generates the Δv map into build/astro and checks that docs/ASTRO.md carries the current version. */
class DeltaVMapTest {
    static final String BEGIN = "<!-- delta-v-map:begin -->", END = "<!-- delta-v-map:end -->";

    @Test
    void mapIsGeneratedAndDocumented() throws Exception {
        String map = DeltaVMap.markdown(SolarSystemData.system(), "spacebreaker:earth");
        Path out = Path.of("build/astro/delta_v_map.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, map, StandardCharsets.UTF_8);
        assertTrue(map.contains("| Mars |"));
        assertTrue(map.contains("| Earth → Moon |"));
        assertFalse(map.contains("NaN") || map.contains("Infinity"), map);
        Path doc = Path.of("docs/ASTRO.md");
        String text = Files.readString(doc, StandardCharsets.UTF_8);
        int a = text.indexOf(BEGIN), b = text.indexOf(END);
        assertTrue(a >= 0 && b > a, "docs/ASTRO.md needs the delta-v map markers");
        String inDoc = text.substring(a + BEGIN.length(), b).strip();
        assertEquals(map.strip(), inDoc, "docs/ASTRO.md is out of date: copy build/astro/delta_v_map.md between the markers");
    }

    @Test
    void durations() {
        assertEquals("5.0 h", DeltaVMap.duration(5 * 3600));
        assertEquals("259 d", DeltaVMap.duration(259 * AstroConstants.DAY));
        assertEquals("3.5 y", DeltaVMap.duration(3.5 * AstroConstants.YEAR));
        assertEquals("—", DeltaVMap.duration(Double.POSITIVE_INFINITY));
    }
}
