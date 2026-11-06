package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;
import com.pizzaparker07.spacebreaker.core.astro.TerrainParams;

/**
 * Terrain of a small asteroid world.
 *
 * @param hints          physics-based hints (colour, craters; always airless)
 * @param params         terrain-profile numbers
 * @param rockFraction   exposed bedrock and boulders vs fine regolith, 0..1 (small bodies: mostly rock)
 * @param boulderDensity 0..1, boulder fields (rubble piles are covered in them)
 * @param metalVeins     0..1, richness of metal veins (M type ≈ 0.9)
 * @param pgmPpm         platinum-group metals, ppm
 * @param iceFraction    water/CO₂ ice in the ground, 0..1 (outer belt, Trojans, Kuiper belt)
 */
public record AsteroidTerrain(TerrainHints hints, TerrainParams params, double rockFraction, double boulderDensity,
                              double metalVeins, double pgmPpm, double iceFraction) {
}
