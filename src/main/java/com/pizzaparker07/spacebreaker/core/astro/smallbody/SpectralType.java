package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import java.util.Map;

/**
 * Asteroid spectral classes (Bus–DeMeo, simplified) with typical bulk properties from meteorite analogues.
 *
 * @param grainDensity  density of the solid material (no voids), kg/m³
 * @param albedoMin     geometric albedo range
 * @param albedoMax
 * @param metalVeins    0..1, how rich in metal veins a mining world of this type is
 * @param pgmPpm        typical platinum-group-metal content, parts per million by mass
 * @param composition   mass fractions of the main materials
 */
public enum SpectralType {
    /** Carbonaceous: dark, primitive, hydrated clays, organics (meteorites: carbonaceous chondrites). */
    C(2400, 0.03, 0.09, 0.1, 0.8, Map.of("phyllosilicates", 0.55, "water", 0.10, "organics", 0.06, "magnetite", 0.12,
            "olivine", 0.12, "iron_nickel", 0.02, "sulfides", 0.03)),
    /** Stony: olivine and pyroxene with metal grains (ordinary chondrites). */
    S(3400, 0.15, 0.30, 0.3, 1.5, Map.of("olivine", 0.40, "pyroxene", 0.30, "plagioclase", 0.08, "iron_nickel", 0.15,
            "sulfides", 0.06, "chromite", 0.01)),
    /** Metallic: iron–nickel cores of shattered protoplanets (iron meteorites). */
    M(7000, 0.10, 0.20, 0.9, 25.0, Map.of("iron_nickel", 0.88, "sulfides", 0.06, "silicates", 0.05, "phosphides", 0.01)),
    /** Basaltic crust from Vesta (HED meteorites). */
    V(3300, 0.30, 0.45, 0.05, 0.2, Map.of("pyroxene", 0.55, "plagioclase", 0.40, "iron_nickel", 0.01, "silica", 0.02,
            "chromite", 0.02)),
    /** Dark primitive ice-rich bodies of the outer Solar System (Trojans, Kuiper belt). */
    D(1900, 0.03, 0.10, 0.05, 0.5, Map.of("water_ice", 0.35, "silicates", 0.35, "organics", 0.20, "co2_ice", 0.05,
            "iron_nickel", 0.02, "sulfides", 0.03));

    public final double grainDensity;
    public final double albedoMin;
    public final double albedoMax;
    public final double metalVeins;
    public final double pgmPpm;
    public final Map<String, Double> composition;

    SpectralType(double grainDensity, double albedoMin, double albedoMax, double metalVeins, double pgmPpm,
                 Map<String, Double> composition) {
        this.grainDensity = grainDensity;
        this.albedoMin = albedoMin;
        this.albedoMax = albedoMax;
        this.metalVeins = metalVeins;
        this.pgmPpm = pgmPpm;
        this.composition = composition;
    }

    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
