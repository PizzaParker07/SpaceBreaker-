package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The seeded, thread-safe terrain function of one planet: height, lowland share, craters, rays, latitude
 * and climate zone for any column. Pure maths (no world access), so it is cheap to call from worldgen threads,
 * structure placement, the sky renderer and tests. Results only depend on the profile and the seed.
 */
public final class PlanetTerrain {
    /** Climate / geology zone of a column, mapped to biomes by {@link PlanetBiomeSource}. */
    public enum Zone { HIGHLANDS, LOWLANDS, CRATER, VOLCANIC, POLAR, DUNES, CANYON, VOLCANO, VALLEY, ICE_CAP }

    /** Craters this large or larger count as a crater zone (biome) inside their rim. */
    public static final double CRATER_ZONE_RADIUS = 22;

    private final TerrainProfile profile;
    private final long seed;
    private final SimplexNoise lowNoise;
    private final SimplexNoise lowNoise2;
    private final SimplexNoise highNoise;
    private final SimplexNoise detailNoise;
    private final SimplexNoise ridgeNoise;
    private final SimplexNoise volcanicNoise;
    private final SimplexNoise brecciaNoise;
    private final ConcurrentHashMap<Long, List<Crater>> craterCells = new ConcurrentHashMap<>();
    private final LandformSampler landforms;

    public PlanetTerrain(TerrainProfile profile, long seed) {
        this.profile = profile;
        this.seed = seed;
        this.lowNoise = noise(1);
        this.lowNoise2 = noise(2);
        this.highNoise = noise(3);
        this.detailNoise = noise(4);
        this.ridgeNoise = noise(5);
        this.volcanicNoise = noise(6);
        this.brecciaNoise = noise(7);
        this.landforms = new LandformSampler(profile, seed);
    }

    private SimplexNoise noise(int salt) {
        return new SimplexNoise(new XoroshiroRandomSource(mix(seed, 0x5EEDL * salt + salt)));
    }

    public TerrainProfile profile() {
        return profile;
    }

    public long seed() {
        return seed;
    }

    // ------------------------------------------------------------------ latitude / longitude

    /**
     * Latitude in degrees for a z coordinate: 0 at z = 0, +90 at the north pole (z = -poleDistance),
     * -90 at the south pole. Past a pole the latitude falls again (you walk over the pole to the far side).
     */
    public double latitude(double z) {
        return latitude(z, profile.poleDistance());
    }

    public static double latitude(double z, double poleDistance) {
        double t = -z / poleDistance;           // 1 = north pole
        double m = Mth.positiveModulo(t + 1, 4.0); // triangle wave with period 4
        double tri = m < 2 ? m - 1 : 3 - m;
        return 90 * tri;
    }

    /** Longitude in degrees (-180..180) for an x coordinate, same scale as latitude. */
    public double longitude(double x) {
        return Mth.wrapDegrees(90 * x / profile.poleDistance());
    }

    // ------------------------------------------------------------------ columns

    /** Everything the generator needs to know about one column. */
    public static final class Column {
        public double height;
        public double lowland;
        public double latitude;
        /** Normalized distance to the centre of the innermost crater zone crater (radii), or 99. */
        public double craterS = 99;
        public double craterRadius;
        /** Normalized distance to any crater with radius >= 5 (for walls and shadowed floors), or 99. */
        public double anyCraterS = 99;
        public double anyCraterRadius;
        public boolean anyCraterComplex;
        public double ray;
        public boolean volcanic;
        public double breccia;
        // ---- optional landforms (see Landforms); neutral values when a world has none
        /** Dune field strength 0..1, local dune height and whether this field is made of the second sand. */
        public double dune;
        public double duneHeight;
        public boolean altDune;
        /** River valley floor 0..1 and sediment fan (delta) 0..1. */
        public double valley;
        public double delta;
        /** Normalized distance to the nearest shield volcano centre (radii), or 99. */
        public double volcanoS = 99;
        public double volcanoRadius;
        /** Normalized distance from the rift centre line (1 = rim), or 99. */
        public double rift = 99;
        /** Ice cap thickness on top of the ground, trough depth, and whether the cap wears frost. */
        public double cap;
        public double trough;
        public boolean frost;
        /** Index of the soil patch covering this column, or -1. */
        public int patch = -1;
        /** Tessera share 0..1. */
        public double tessera;

        public Zone zone(TerrainProfile p) {
            if (cap >= 1) {
                return Zone.ICE_CAP;
            }
            if (Math.abs(latitude) >= p.poles().polarLatitude()) {
                return Zone.POLAR;
            }
            if (rift < 1) {
                return Zone.CANYON;
            }
            if (craterS < 0.9) {
                return Zone.CRATER;
            }
            if (volcanoS < 0.92) {
                return Zone.VOLCANO;
            }
            if (dune > 0.5) {
                return Zone.DUNES;
            }
            if (valley > 0.45) {
                return Zone.VALLEY;
            }
            if (volcanic) {
                return Zone.VOLCANIC;
            }
            return lowland > 0.5 ? Zone.LOWLANDS : Zone.HIGHLANDS;
        }

        /** True when the crater floor is permanently shadowed (near a pole, deep inside a crater). */
        public boolean shadowed(TerrainProfile p) {
            double lat = Math.abs(latitude);
            double ice = p.poles().iceLatitude();
            if (lat < ice || anyCraterS >= 1 || anyCraterRadius < 5) {
                return false;
            }
            double t = Mth.clamp((lat - ice) / Math.max(1, 90 - ice), 0, 1);
            return anyCraterS < 0.45 + 0.42 * t;
        }
    }

    public Column column(double x, double z) {
        Column c = new Column();
        sample(x, z, cratersNear(x, z, x, z), c);
        return c;
    }

    public double height(double x, double z) {
        return column(x, z).height;
    }

    /** Fills {@code out} for column (x, z) using a pre-collected crater list. */
    public void sample(double x, double z, List<Crater> craters, Column out) {
        TerrainProfile.Relief relief = profile.relief();
        TerrainProfile.Lowlands low = profile.lowlands();
        out.latitude = latitude(z);
        out.lowland = lowland(x, z, out.latitude);

        // highland relief: rolling hills plus some ridged massifs
        double hs = 1.0 / relief.scale();
        double hills = fbm(highNoise, x * hs, z * hs, 5);
        double ridged = 1 - Math.abs(fbm(ridgeNoise, x * hs * 0.6 + 31.7, z * hs * 0.6 - 12.3, 3));
        double high = relief.amplitude() * (0.7 * hills + 0.3 * (ridged * ridged * 2 - 1));
        // lowland floors: flat lava plains with low wrinkle ridges
        double ridge = 1 - Math.abs(ridgeNoise.getValue(x / 260.0 + 400, z / 260.0 - 400));
        double lowFloor = -low.depth() + low.ridgeHeight() * Math.pow(ridge, 6) * 2;
        double ds = 1.0 / relief.detailScale();
        double detail = relief.detailAmplitude() * fbm(detailNoise, x * ds, z * ds, 2);
        if (relief.roughness() > 0) {
            // ragged block-scale unevenness: breaks long smooth slopes into irregular steps instead of clean contour lines
            detail += relief.roughness() * (0.65 * detailNoise.getValue(x / 4.3 + 91.1, z / 4.3 - 17.7) + 0.35 * detailNoise.getValue(x / 1.9 - 5.5, z / 1.9 + 44.4));
        }

        double built = landforms.active() ? landforms.build(x, z, out) : 0;
        double craterSum = 0;
        double kept = 1;
        double ray = 0;
        for (Crater cr : craters) {
            double dx = x - cr.x();
            double dz = z - cr.z();
            double reach = cr.radius() * Crater.EJECTA_END;
            double reachRay = cr.fresh() ? cr.radius() * profile.craters().rayLength() : reach;
            double d2 = dx * dx + dz * dz;
            if (d2 > reachRay * reachRay) {
                continue;
            }
            double s = cr.normalizedDistance(x, z);
            if (cr.fresh()) {
                ray = Math.max(ray, cr.ray(x, z, s, profile.craters().rayLength()));
            }
            if (d2 > reach * reach) {
                continue;
            }
            craterSum += cr.profile(s);
            kept = Math.min(kept, cr.reliefKept(s));
            if (cr.radius() >= CRATER_ZONE_RADIUS && s < out.craterS) {
                out.craterS = s;
                out.craterRadius = cr.radius();
            }
            if (cr.radius() >= 5 && s < out.anyCraterS) {
                out.anyCraterS = s;
                out.anyCraterRadius = cr.radius();
                out.anyCraterComplex = cr.complex();
            }
        }
        double m = out.lowland;
        // very large swells shared by highlands and basins (old basin rims, buried topography)
        double swell = relief.amplitude() * 0.45 * highNoise.getValue(x / (relief.scale() * 4.3) + 77.7, z / (relief.scale() * 4.3) - 19.1);
        double terrain = Mth.lerp(m, high * kept, lowFloor) + swell * (1 - 0.6 * m) + detail * kept * (1 - 0.5 * m);
        out.height = profile.baseHeight() + terrain + built + craterSum;
        out.ray = ray;
        if (landforms.active()) {
            out.height = landforms.finish(x, z, out.height, out);
        }

        out.volcanic = low.volcanicPatch() < 1 && m > 0.3 && m < 0.97 && out.anyCraterS > 1.3
                && fbm(volcanicNoise, x / 150.0, z / 150.0, 2) > low.volcanicPatch() - 0.35 * (1 - Math.abs(m - 0.65) * 2);
        out.breccia = brecciaNoise.getValue(x / 48.0, z / 48.0);
    }

    /** Lowland (mare) share 0..1 at a position; none near the poles. */
    public double lowland(double x, double z, double latitude) {
        TerrainProfile.Lowlands low = profile.lowlands();
        double ls = 1.0 / low.scale();
        double n = 0.75 * fbm(lowNoise, x * ls, z * ls, 3) + 0.25 * lowNoise2.getValue(x * ls * 3.1, z * ls * 3.1);
        if (low.dichotomy().isPresent()) {
            TerrainProfile.Dichotomy d = low.dichotomy().get();
            double line = d.latitude() + d.wave() * lowNoise2.getValue(x / (low.scale() * 1.7) + 33.3, 7.7);
            n += d.strength() * (2 * Crater.smooth((latitude - line) / d.width() + 0.5) - 1);
        }
        double m = Crater.smooth((n - low.threshold() + low.edge()) / (2 * low.edge()));
        double polar = profile.poles().polarLatitude();
        double fade = 1 - Crater.smooth((Math.abs(latitude) - (polar - 10)) / 10);
        return m * fade;
    }

    private static double fbm(SimplexNoise n, double x, double z, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += n.getValue(x * f + i * 17.17, z * f - i * 9.31) * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2.03;
        }
        return sum / norm;
    }

    // ------------------------------------------------------------------ landforms

    /** Shield volcanoes reaching a rectangle: {x, z, radius, height} each (empty without volcanoes). */
    public List<double[]> volcanoesNear(double x0, double z0, double x1, double z1) {
        return landforms.volcanoesNear(x0, z0, x1, z1).stream().map(v -> new double[]{v.x(), v.z(), v.radius(), v.height()}).toList();
    }

    /** Centre line z of the great rift at x (NaN without a rift). */
    public double riftCentre(double x) {
        return profile.landforms().rift().isPresent() ? landforms.riftCentre(x) : Double.NaN;
    }

    /** Layered rock at a position, or null when the profile has no strata. */
    public net.minecraft.world.level.block.state.BlockState stratum(int x, int y, int z) {
        return landforms.stratum(x, y, z);
    }

    // ------------------------------------------------------------------ craters

    /** All craters whose ejecta (or rays) can reach the rectangle [x0..x1] × [z0..z1]. */
    public List<Crater> cratersNear(double x0, double z0, double x1, double z1) {
        List<Crater> out = new ArrayList<>();
        List<TerrainProfile.CraterClass> classes = profile.craters().classes();
        double rayLen = Math.max(Crater.EJECTA_END, profile.craters().rayLength());
        for (int ci = 0; ci < classes.size(); ci++) {
            TerrainProfile.CraterClass cls = classes.get(ci);
            double reach = cls.maxRadius() * rayLen;
            int cell = cls.cell();
            int gx0 = Mth.floor((x0 - reach) / cell);
            int gx1 = Mth.floor((x1 + reach) / cell);
            int gz0 = Mth.floor((z0 - reach) / cell);
            int gz1 = Mth.floor((z1 + reach) / cell);
            for (int gx = gx0; gx <= gx1; gx++) {
                for (int gz = gz0; gz <= gz1; gz++) {
                    for (Crater c : cell(ci, gx, gz)) {
                        double r = c.radius() * (c.fresh() ? rayLen : Crater.EJECTA_END);
                        if (c.x() + r >= x0 && c.x() - r <= x1 && c.z() + r >= z0 && c.z() - r <= z1) {
                            out.add(c);
                        }
                    }
                }
            }
        }
        // big craters first, so later (smaller) ones are "younger" and sit on top
        out.sort((a, b) -> Double.compare(b.radius(), a.radius()));
        return out;
    }

    private List<Crater> cell(int classIndex, int gx, int gz) {
        long key = ((long) classIndex << 58) ^ ((long) (gx & 0x1FFFFFFF) << 29) ^ (gz & 0x1FFFFFFF);
        List<Crater> cached = craterCells.get(key);
        if (cached != null) {
            return cached;
        }
        List<Crater> made = makeCell(classIndex, gx, gz);
        if (craterCells.size() > 20000) {
            craterCells.clear();
        }
        craterCells.put(key, made);
        return made;
    }

    private List<Crater> makeCell(int classIndex, int gx, int gz) {
        TerrainProfile.Craters cfg = profile.craters();
        TerrainProfile.CraterClass cls = cfg.classes().get(classIndex);
        List<Crater> list = new ArrayList<>(cls.perCell());
        for (int slot = 0; slot < cls.perCell(); slot++) {
            long h = mix(seed, mix(classIndex * 7919L + slot, mix(gx, gz)));
            double cx = (gx + unit(h, 0)) * cls.cell();
            double cz = (gz + unit(h, 1)) * cls.cell();
            double lat = latitude(cz);
            double chance = cls.chance() * cfg.density();
            chance *= Mth.lerp(lowland(cx, cz, lat), 1.0, cfg.lowlandDensity());
            if (Math.abs(lat) >= profile.poles().polarLatitude()) {
                chance *= profile.poles().polarCraters();
            }
            if (unit(h, 2) >= chance) {
                continue;
            }
            // power-law sizes: many small, few big
            double u = unit(h, 3);
            double radius = cls.minRadius() * Math.pow(cls.maxRadius() / cls.minRadius(), u * u);
            boolean fresh = radius >= 8 && unit(h, 4) < cfg.freshChance();
            list.add(shape(cx, cz, radius, fresh, mix(h, 99)));
        }
        return List.copyOf(list);
    }

    /** Builds a crater with realistic proportions for its size. */
    public Crater shape(double x, double z, double radius, boolean fresh, long bits) {
        TerrainProfile.Craters cfg = profile.craters();
        double rc = cfg.complexRadius();
        double diameter = 2 * radius;
        double depth;
        double floor = 0;
        double peak = 0;
        if (radius <= rc) {
            depth = cfg.depthRatio() * diameter;
        } else {
            // complex craters get relatively shallower (depth ~ D^0.3) with a flat floor and a central peak
            depth = cfg.depthRatio() * 2 * rc * Math.pow(radius / rc, 0.3);
            floor = Mth.clamp(0.3 + 0.12 * (radius / rc - 1), 0.3, 0.55);
            peak = depth * cfg.centralPeak();
        }
        double rim = cfg.rimRatio() * diameter * (radius > rc ? Math.pow(rc / radius, 0.35) : 1);
        // blocks are coarse: small craters get a little vertical exaggeration so they still read as craters
        depth *= 1 + 1.2 * Math.exp(-radius / 8);
        rim = rim * (1 + 2 * Math.exp(-radius / 10)) + 0.6;
        if (fresh) {
            depth *= 1.1;
            rim *= 1.15;
        }
        return new Crater(x, z, radius, depth, rim, peak, floor, fresh, bits);
    }

    // ------------------------------------------------------------------ hashing

    static long mix(long a, long b) {
        long z = a * 0x9E3779B97F4A7C15L + b;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A uniform double in [0, 1) from hash h and stream index i. */
    static double unit(long h, int i) {
        long v = mix(h, i + 1);
        return (v >>> 11) * 0x1.0p-53;
    }
}
