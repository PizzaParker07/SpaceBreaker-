package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.galaxy.Rng;

/**
 * Terrain parameters for the planet engine, derived from a body's physics so real and generated worlds are
 * treated the same way. Pure data: the chunk generator decides how to use it.
 *
 * @param colorFamily    dominant surface look
 * @param baseColor      main surface colour, 0xRRGGBB
 * @param accentColor    secondary colour (bright dust, ice, lava, dark plains…), 0xRRGGBB
 * @param craterDensity  cumulative craters ≥ 1 km per km² (Moon highlands ≈ 2·10⁻², Venus ≈ 2·10⁻⁶)
 * @param liquid         surface liquid, if any
 * @param liquidCoverage fraction of the surface covered by that liquid, 0..1
 * @param glaciation     fraction of the surface covered by ice, 0..1 (1 = an ice world)
 * @param volcanism      present-day volcanic activity, 0..1 (Io = 1)
 * @param roughness      overall relief, 0..1 (smooth plains → rugged)
 * @param dunes          true when wind can build dunes (enough air, loose material)
 * @param tidalHeating   tidal heat flux at the surface, W/m² (Io ≈ 2)
 * @param volatileIce    fraction covered by frozen nitrogen/methane/CO glaciers (Pluto's Sputnik Planitia, Triton's cap)
 * @param plumes         active geysers or plumes (Enceladus, Europa: tidal; Triton: sunlight on nitrogen ice)
 */
public record TerrainHints(ColorFamily colorFamily, int baseColor, int accentColor, double craterDensity,
                           Liquid liquid, double liquidCoverage, double glaciation, double volcanism, double roughness,
                           boolean dunes, double tidalHeating, double volatileIce, boolean plumes) {
    /** Nitrogen freezes below this temperature, K. */
    public static final double NITROGEN_FREEZE = 63.2;

    public enum ColorFamily {
        GREY_REGOLITH(0x8C8681, 0x5E5B58), RUST(0xC1683C, 0xD9A066), DARK_BASALT(0x4A4440, 0x8A6A3A),
        PALE_ICE(0xE6EBEF, 0xB9A58E), DIRTY_ICE(0x9A8F80, 0xD8DCDF), SULFUR(0xD9C25A, 0x3B2E22),
        ORGANIC_HAZE(0xB07A3A, 0x4E3A2A), OCEAN(0x3A5F8A, 0x6E8B4A), GAS_BANDS(0xC8A27A, 0xE8DCC8);

        public final int base;
        public final int accent;

        ColorFamily(int base, int accent) {
            this.base = base;
            this.accent = accent;
        }

        public String serializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Liquid {
        NONE, WATER, METHANE, NITROGEN, LAVA;

        public String serializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Tidal dissipation k₂/Q used for moons (Io's measured value). */
    public static final double K2_OVER_Q = 0.015;
    /** Io's tidal heat flux, W/m² (the scale for volcanism = 1). */
    public static final double IO_FLUX = 2.0;

    /**
     * Tidal heat flux of a body on an eccentric orbit: Ė = (21/2)(k₂/Q) G M_p² R⁵ n e² / a⁶, spread over the
     * surface, W/m². Zero without a parent or orbit.
     */
    public static double tidalHeatingFlux(AstroBody body, AstroBody parent) {
        if (parent == null || body.orbit() == null || parent.isStar()) {
            return 0;
        }
        OrbitalElements o = body.orbit();
        double r = body.radius();
        double e = o.eccentricity();
        double power = 10.5 * K2_OVER_Q * AstroConstants.G * parent.mass() * parent.mass() * Math.pow(r, 5)
                * o.meanMotion() * e * e / Math.pow(o.semiMajorAxis(), 6);
        return power / (4 * Math.PI * r * r);
    }

    /** Boiling point of a liquid at a pressure from Clausius–Clapeyron, K. */
    static double boilingPoint(double tAt1Atm, double latentJkg, double molarKg, double pressureKpa) {
        double k = 8.314462618 / (latentJkg * molarKg);
        return 1 / (1 / tAt1Atm - k * Math.log(pressureKpa / 101.325));
    }

    /**
     * Hints for a body.
     *
     * @param parent    the body it orbits (for tidal heating), may be null
     * @param fluxWm2   starlight at the body
     * @param ageYears  age of the system
     * @param seed      variation seed (e.g. the star id); real bodies use a hash of their id
     */
    public static TerrainHints of(AstroBody b, AstroBody parent, double fluxWm2, double ageYears, long seed) {
        Rng rng = new Rng(Rng.hash(seed, b.id().hashCode()));
        if (b.kind().isGiant() || b.isStar()) {
            return new TerrainHints(ColorFamily.GAS_BANDS, tint(ColorFamily.GAS_BANDS.base, rng), ColorFamily.GAS_BANDS.accent,
                    0, Liquid.NONE, 0, 0, 0, 0, false, 0, 0, false);
        }
        double me = b.mass() / AstroConstants.EARTH_MASS;
        PlanetModel.Composition comp = PlanetModel.composition(me, b.density());
        boolean icy = comp == PlanetModel.Composition.ICY || comp == PlanetModel.Composition.OCEAN;
        double p = b.surfacePressureKpa();
        double t = b.meanTempK();
        double tidal = tidalHeatingFlux(b, parent);

        // volcanism: radiogenic heat fades with age and is larger in bigger bodies; tides add more
        double internal = Math.min(1, Math.sqrt(me) * Math.exp(-ageYears / 4e9));
        double tidalV = tidal <= 0.01 ? 0 : Math.min(1, Math.log10(tidal / 0.01) / Math.log10(IO_FLUX / 0.01));
        // large retrograde moons were captured, and tides heated them hard while their orbits circularised (Triton)
        double capture = parent != null && !parent.isStar() && b.orbit() != null
                && b.orbit().inclination() > Math.PI / 2 && b.mass() > 1e21 ? 0.5 : 0;
        double volcanism = Math.max(internal, Math.max(tidalV, capture));
        if (icy && tidalV < 0.2 && capture == 0) {
            volcanism *= 0.3; // ice shells: cryovolcanism at most
        }

        // surface liquids
        Liquid liquid = Liquid.NONE;
        double coverage = 0;
        if (t > 1000 || tidalV >= 0.9) {
            liquid = Liquid.LAVA;
            coverage = t > 1000 ? rng.range(0.3, 0.8) : rng.range(0.005, 0.03);
        } else if (p > 0.6 && t > 273.15 && t < boilingPoint(373.15, 2.26e6, 0.018, p)) {
            liquid = Liquid.WATER;
            coverage = comp == PlanetModel.Composition.OCEAN ? rng.range(0.85, 1.0) : rng.range(0.2, 0.9);
        } else if (p > 10 && t > 90.7 && t < boilingPoint(111.7, 5.1e5, 0.016, p)) {
            liquid = Liquid.METHANE;
            coverage = rng.range(0.01, 0.05);
        } else if (p > 10 && t > 63.2 && t < boilingPoint(77.4, 2.0e5, 0.028, p)) {
            liquid = Liquid.NITROGEN;
            coverage = rng.range(0.01, 0.1);
        }

        // ice cover
        double glaciation;
        if (icy && t < 273.15) {
            // bright ice worlds show their ice; dark ones (Callisto, Ceres, Titan) are covered by dust or sediment
            glaciation = (b.albedo() > 0.3 ? 1 : 0.15) * (1 - coverage);
        } else if (liquid == Liquid.LAVA) {
            glaciation = 0;
        } else if (b.hasAtmosphere()) {
            glaciation = 0.1 * clamp((300 - t) / 80);
        } else {
            glaciation = b.nightTempK() < 110 ? 0.005 : 0; // cold traps in polar craters
        }

        // craters: an old airless surface keeps them all; air, liquids and lava erase them
        double atm = 1 / (1 + Math.pow(p / 2.0, 1.2));
        double crater = 1e-2 * atm * Math.pow(1 - volcanism, 2) * (1 - coverage);
        if (icy) {
            crater *= Math.exp(-12 * Math.max(tidalV, capture)); // tidally heated ice shells resurface fast (Europa ≈ 60 Myr)
        }
        if (liquid != Liquid.NONE && liquid != Liquid.LAVA) {
            crater *= 0.05; // rain and rivers wear craters down (Earth, Titan)
        }
        if (p > 1000) {
            crater = Math.min(crater, 2e-6);
        }

        ColorFamily family;
        if (liquid == Liquid.LAVA && tidalV >= 0.9 && t < 1000) { // above ~1000 K sulfur boils off: molten rock looks dark
            family = ColorFamily.SULFUR;
        } else if (t > 600 && p > 1000 || liquid == Liquid.LAVA) {
            family = ColorFamily.DARK_BASALT;
        } else if (liquid == Liquid.WATER) {
            family = ColorFamily.OCEAN;
        } else if (liquid == Liquid.METHANE || p > 10 && t < 150 && b.composition().containsKey("CH4")) {
            family = ColorFamily.ORGANIC_HAZE;
        } else if (b.albedo() < 0.1) {
            family = ColorFamily.GREY_REGOLITH;
        } else if (icy && t < 273.15) {
            family = b.albedo() > 0.5 ? ColorFamily.PALE_ICE : ColorFamily.DIRTY_ICE;
        } else if (p > 0.1 && p < 20 && t > 150 && t < 320) {
            family = ColorFamily.RUST;
        } else if (!b.hasAtmosphere()) {
            family = ColorFamily.GREY_REGOLITH;
        } else {
            family = ColorFamily.DARK_BASALT;
        }

        // frozen volatiles: a thin nitrogen atmosphere over a surface cold enough to freeze it
        double volatileIce = b.composition().getOrDefault("N2", 0.0) > 0.5 && p > 0 && b.nightTempK() < NITROGEN_FREEZE
                ? 0.2 : 0;
        if (volatileIce > 0) {
            crater *= 0.5; // glaciers fill and erase craters where they flow
        }
        boolean plumes = icy && (tidalV > 0.15 || volatileIce > 0 && fluxWm2 > 1.0);
        double g = b.surfaceGravity();
        double roughness = clamp(0.3 + crater * 20 + volcanism * 0.3 - coverage * 0.3 + (g < 2 ? 0.1 : 0));
        boolean dunes = p > 0.3 && coverage < 0.95 && liquid != Liquid.LAVA;
        return new TerrainHints(family, tint(family.base, rng), family.accent, crater, liquid, coverage, glaciation,
                volcanism, roughness, dunes, tidal, volatileIce, plumes);
    }

    /** Hints for a body of the Solar System: flux from its distance to the Sun, age 4.6 Gyr. */
    public static TerrainHints of(BodySystem sys, String id) {
        AstroBody b = sys.require(id);
        AstroBody star = sys.root(b);
        double au = 1;
        for (AstroBody x : sys.ancestry(b)) {
            if (x.parent() != null && x.parent().equals(star.id()) && x.orbit() != null) {
                au = x.orbit().semiMajorAxis() / AstroConstants.AU;
            }
        }
        double flux = AstroConstants.SOLAR_CONSTANT / (au * au);
        return of(b, sys.parent(b).orElse(null), flux, 4.6e9, Rng.hash(id.hashCode()));
    }

    /** A slight deterministic variation (±8 %) of a colour. */
    private static int tint(int rgb, Rng rng) {
        double f = rng.range(0.92, 1.08);
        int r = (int) Math.min(255, (rgb >> 16 & 0xFF) * f);
        int g = (int) Math.min(255, (rgb >> 8 & 0xFF) * f);
        int bl = (int) Math.min(255, (rgb & 0xFF) * f);
        return r << 16 | g << 8 | bl;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
