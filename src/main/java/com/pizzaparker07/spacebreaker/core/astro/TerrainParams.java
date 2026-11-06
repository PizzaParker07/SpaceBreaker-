package com.pizzaparker07.spacebreaker.core.astro;

import java.util.Optional;

/**
 * {@link TerrainHints} translated into the numbers the planet engine's terrain profiles use (block units),
 * calibrated so the real bodies come out close to their hand-made profiles (Moon: crater density 1.0,
 * Venus ≈ 0.35). Pure data: whoever builds a {@code TerrainProfile} for a generated body copies these
 * values and picks blocks from {@link #palette()} (or the {@link #referenceProfile()} materials).
 *
 * @param baseHeight          mean surface height (y)
 * @param reliefAmplitude     highland hill height, blocks
 * @param reliefScale         horizontal size of hills, blocks
 * @param detailAmplitude     small bumps, blocks
 * @param roughness           block-scale raggedness (0 = smooth)
 * @param lowlandThreshold    noise level above which ground is lowland (lower = more lowlands)
 * @param lowlandDepth        lowland floor below the mean height, blocks
 * @param craterDensity       multiplier on the default crater classes (Moon = 1)
 * @param lowlandCraterDensity multiplier inside lowlands
 * @param depthRatio          simple crater depth / diameter
 * @param rimRatio            rim height / diameter
 * @param complexRadius       radius (blocks) above which craters get flat floors and central peaks
 * @param freshChance         share of young, rayed craters
 * @param polarLatitude       latitude where the polar region starts (95 = none)
 * @param iceLatitude         latitude above which crater floors hold ice (0 = the whole surface is ice)
 * @param iceDepth            ice layer thickness, blocks
 * @param seaLevel            liquid surface height, or −1 when there is no surface liquid
 * @param liquid              the liquid filling the seas
 * @param dunes               add dune fields
 * @param volcanoes           add shield volcanoes
 * @param valleys             add river valleys
 * @param iceCap              add polar ice caps
 * @param palette             colour family for block choice
 */
public record TerrainParams(int baseHeight, double reliefAmplitude, double reliefScale, double detailAmplitude,
                            double roughness, double lowlandThreshold, double lowlandDepth, double craterDensity,
                            double lowlandCraterDensity, double depthRatio, double rimRatio, double complexRadius,
                            double freshChance, double polarLatitude, double iceLatitude, int iceDepth, int seaLevel,
                            TerrainHints.Liquid liquid, boolean dunes, boolean volcanoes, boolean valleys, boolean iceCap,
                            TerrainHints.ColorFamily palette) {
    public static final int BASE_HEIGHT = 80;
    private static final double MOON_G = 1.62;

    /** Parameters for a landable body (gas giants have no terrain: use {@code hints.colorFamily()} only). */
    public static TerrainParams of(AstroBody body, TerrainHints h) {
        double g = Math.max(0.005, body.surfaceGravity());
        double p = body.surfacePressureKpa();
        double lowG = clamp(Math.pow(MOON_G / g, 0.2), 0.7, 1.6);
        double amp = (18 + 18 * h.roughness() + 10 * h.volcanism()) * lowG;
        double lowThreshold = clamp(0.3 - 0.35 * h.volcanism() - 0.5 * h.liquidCoverage(), -0.3, 0.9);
        double lowDepth = 10 + 10 * h.volcanism() + 6 * h.liquidCoverage();
        double density = h.craterDensity() <= 0 ? 0.05
                : clamp(0.35 + 0.176 * Math.log10(h.craterDensity() / 2e-6), 0.05, 1.3);
        boolean thick = p > 50, air = body.hasAtmosphere();
        double depthRatio = thick ? 0.12 : air ? 0.15 : 0.2;
        double rimRatio = thick ? 0.035 : air ? 0.04 : 0.055;
        double complexRadius = clamp(34 * Math.pow(MOON_G / g, 0.3), 18, 60);
        double fresh = (thick ? 0 : air ? 0.05 : 0.15) * (1 - h.volcanism());
        double iceLat;
        double polarLat;
        if (h.glaciation() > 0.9) {
            iceLat = 0;
            polarLat = 0;
        } else if (h.glaciation() > 0) {
            // both caps together cover the glaciated fraction of the sphere: 1 − sin φ = f
            iceLat = Math.toDegrees(Math.asin(1 - h.glaciation()));
            polarLat = Math.max(0, iceLat - 8);
        } else {
            iceLat = 95;
            polarLat = 95;
        }
        int sea = h.liquidCoverage() > 0 && h.liquid() != TerrainHints.Liquid.LAVA
                ? (int) Math.round(BASE_HEIGHT - lowDepth * (1 - h.liquidCoverage())) : -1;
        return new TerrainParams(BASE_HEIGHT, amp, 500 + 200 * (1 - h.roughness()), 1.5 + 1.5 * h.roughness(),
                h.roughness() * 1.2, lowThreshold, lowDepth, density, clamp(1 - 0.7 * h.volcanism(), 0.2, 1),
                depthRatio, rimRatio, complexRadius, fresh, polarLat, iceLat, h.glaciation() > 0.9 ? 8 : 4, sea,
                h.liquid(), h.dunes(), h.volcanism() > 0.25, h.liquid() == TerrainHints.Liquid.WATER
                || h.liquid() == TerrainHints.Liquid.METHANE, air && h.glaciation() > 0.01 && h.glaciation() <= 0.9,
                h.colorFamily());
    }

    /** A hand-made profile whose blocks fit this palette (until generic colour-variant blocks exist). */
    public Optional<String> referenceProfile() {
        return switch (palette) {
            case GREY_REGOLITH -> Optional.of("spacebreaker:moon");
            case RUST -> Optional.of("spacebreaker:mars");
            case DARK_BASALT, SULFUR -> Optional.of("spacebreaker:venus");
            default -> Optional.empty();
        };
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
