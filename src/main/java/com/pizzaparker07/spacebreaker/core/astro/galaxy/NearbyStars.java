package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real stars near the Sun at their measured positions (J2000 right ascension, declination and distance,
 * converted to galactic coordinates), with catalogue properties, companions and their <b>confirmed</b> planets.
 * Inside {@link #CATALOGUE_RADIUS_LY} of the Sun the galaxy holds only these real stars. Planets the generator adds
 * around them are marked "not yet discovered" ({@code StarSystem.Details#discovered()} = false). The Sun ("sol") is
 * listed for the star map; its system is the data-pack registry.
 * <p>
 * Values: SIMBAD / RECONS distances, standard spectral-type masses, luminosities and temperatures, and planet
 * parameters from the discovery papers (minimum masses where only m sin i is known). Only famous or planet-hosting
 * stars are listed; many faint red dwarfs within 25 light years are not (yet).
 */
public final class NearbyStars {
    /** Generated stars never appear closer than this to the Sun, light years. */
    public static final double CATALOGUE_RADIUS_LY = 25.0;
    private static final double LY_TO_PC = AstroConstants.LIGHT_YEAR / AstroConstants.PARSEC;

    /** A confirmed planet: letter, mass (Earth masses, minimum where unknown), radius (Earth radii, ≤ 0 = estimate), a (AU), e. */
    public record Planet(char letter, double massEarths, double radiusEarths, double aAu, double e) {
    }

    /** A companion star: mass, luminosity, radius (solar units), temperature (K), separation (AU), e, period (years), kind. */
    public record Companion(String suffix, double mass, double luminosity, double radius, double temperature, double aAu,
                            double e, double periodYears, StarKind kind) {
    }

    /**
     * @param key        stable key, also the id path ({@code spacebreaker:<key>})
     * @param nameEn     English name
     * @param nameTr     Turkish name
     * @param lDeg       galactic longitude
     * @param bDeg       galactic latitude
     * @param distancePc distance from the Sun
     * @param mass       M☉
     * @param luminosity L☉
     * @param radius     R☉
     * @param temperature K
     * @param ageYears   age estimate
     * @param kind       main sequence or white dwarf
     * @param planets    confirmed planets
     * @param companion  bound companion star, if any
     */
    public record Entry(String key, String nameEn, String nameTr, double lDeg, double bDeg, double distancePc,
                        double mass, double luminosity, double radius, double temperature, double ageYears, StarKind kind,
                        List<Planet> planets, Optional<Companion> companion) {
        /** Galactocentric position, pc. */
        public Vec3 position() {
            double l = Math.toRadians(lDeg), b = Math.toRadians(bDeg);
            return GalaxyModel.SUN.add(new Vec3(Math.cos(b) * Math.cos(l), Math.cos(b) * Math.sin(l), Math.sin(b)).scale(distancePc));
        }

        public StellarModel.Star star() {
            return StellarModel.catalogue(mass, luminosity, radius, temperature);
        }

        public double distanceLy() {
            return distancePc / LY_TO_PC;
        }

        /** Number of confirmed planets. */
        public int knownPlanets() {
            return planets.size();
        }
    }

    /** Galactic {l, b} in degrees from J2000 right ascension (hours) and declination (degrees). */
    public static double[] galactic(double raHours, double decDeg) {
        double a = Math.toRadians(raHours * 15), d = Math.toRadians(decDeg);
        double x = Math.cos(d) * Math.cos(a), y = Math.cos(d) * Math.sin(a), z = Math.sin(d);
        double gx = -0.0548755604 * x - 0.8734370902 * y - 0.4838350155 * z;
        double gy = 0.4941094279 * x - 0.4448296300 * y + 0.7469822445 * z;
        double gz = -0.8676661490 * x - 0.1980763734 * y + 0.4559837762 * z;
        double l = Math.toDegrees(Math.atan2(gy, gx));
        return new double[]{l < 0 ? l + 360 : l, Math.toDegrees(Math.asin(gz))};
    }

    private static final List<Entry> BUILT = new ArrayList<>();

    private static void star(String key, String en, String tr, double raH, double decD, double ly, double mass, double lum,
                             double rad, double temp, double age, StarKind kind, Companion companion, Planet... planets) {
        double[] g = galactic(raH, decD);
        BUILT.add(new Entry(key, en, tr, g[0], g[1], ly * LY_TO_PC, mass, lum, rad, temp, age, kind, List.of(planets),
                Optional.ofNullable(companion)));
    }

    private static void star(String key, String en, String tr, double raH, double decD, double ly, double mass, double lum,
                             double rad, double temp, Planet... planets) {
        star(key, en, tr, raH, decD, ly, mass, lum, rad, temp, 5e9, StarKind.MAIN_SEQUENCE, null, planets);
    }

    private static Planet p(char letter, double me, double re, double a, double e) {
        return new Planet(letter, me, re, a, e);
    }

    private static Companion c(String suffix, double mass, double lum, double rad, double temp, double a, double e, double years) {
        return new Companion(suffix, mass, lum, rad, temp, a, e, years, rad < 0.02 ? StarKind.WHITE_DWARF : StarKind.MAIN_SEQUENCE);
    }

    static {
        BUILT.add(new Entry("sol", "Sun", "Güneş", 0, 0, 0, 1.0, 1.0, 1.0, 5772, 4.6e9, StarKind.MAIN_SEQUENCE, List.of(), Optional.empty()));
        star("alpha_centauri", "Alpha Centauri", "Alfa Centauri", 14.660, -60.835, 4.37, 1.0788, 1.519, 1.2175, 5790, 5.3e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.9092, 0.5002, 0.8591, 5260, 23.4, 0.5179, 79.91));
        star("proxima_centauri", "Proxima Centauri", "Proxima Centauri", 14.495, -62.679, 4.246, 0.1221, 0.00155, 0.1542, 3042, 4.85e9,
                StarKind.MAIN_SEQUENCE, null, p('b', 1.07, 1.07, 0.04857, 0.02), p('d', 0.26, -1, 0.02885, 0.04));
        star("barnards_star", "Barnard's Star", "Barnard Yıldızı", 17.963, 4.693, 5.96, 0.162, 0.0034, 0.187, 3195, 1.0e10,
                StarKind.MAIN_SEQUENCE, null, p('d', 0.263, -1, 0.0188, 0.04), p('b', 0.299, -1, 0.0229, 0.03),
                p('c', 0.335, -1, 0.0274, 0.08), p('e', 0.193, -1, 0.0381, 0.04));
        star("wolf_359", "Wolf 359", "Wolf 359", 10.940, 7.014, 7.86, 0.110, 0.00106, 0.144, 2800);
        star("lalande_21185", "Lalande 21185", "Lalande 21185", 11.055, 35.970, 8.31, 0.389, 0.0219, 0.392, 3547,
                p('b', 2.69, -1, 0.0789, 0.05));
        star("sirius", "Sirius", "Sirius", 6.752, -16.716, 8.60, 2.063, 25.4, 1.711, 9940, 2.4e8, StarKind.MAIN_SEQUENCE,
                c("b", 1.018, 0.056, 0.0084, 25000, 19.8, 0.5923, 50.13));
        star("luyten_726_8", "Luyten 726-8", "Luyten 726-8", 1.652, -17.950, 8.73, 0.102, 0.00006, 0.140, 2670, 5e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.100, 0.00004, 0.140, 2650, 5.4, 0.62, 26.3));
        star("ross_154", "Ross 154", "Ross 154", 18.828, -23.836, 9.70, 0.177, 0.0038, 0.240, 3248);
        star("ross_248", "Ross 248", "Ross 248", 23.698, 44.174, 10.3, 0.136, 0.0018, 0.160, 2799);
        star("epsilon_eridani", "Epsilon Eridani", "Epsilon Eridani", 3.549, -9.458, 10.5, 0.82, 0.34, 0.735, 5084, 6e8,
                StarKind.MAIN_SEQUENCE, null, p('b', 0.66 * 317.83, 1.0 * 11.21, 3.53, 0.07));
        star("lacaille_9352", "Lacaille 9352", "Lacaille 9352", 23.097, -35.853, 10.7, 0.479, 0.033, 0.474, 3688,
                p('b', 4.2, -1, 0.068, 0.05), p('c', 7.6, -1, 0.183, 0.05));
        star("ross_128", "Ross 128", "Ross 128", 11.795, 0.800, 11.0, 0.168, 0.00362, 0.197, 3192,
                p('b', 1.40, -1, 0.0496, 0.04));
        star("ez_aquarii", "EZ Aquarii", "EZ Aquarii", 22.645, -15.300, 11.1, 0.11, 0.0011, 0.12, 2900);
        star("cygni_61", "61 Cygni", "61 Cygni", 21.115, 38.749, 11.4, 0.70, 0.153, 0.665, 4526, 6e9, StarKind.MAIN_SEQUENCE,
                c("b", 0.63, 0.085, 0.595, 4077, 84, 0.49, 678));
        star("procyon", "Procyon", "Procyon", 7.655, 5.225, 11.46, 1.499, 6.93, 2.048, 6530, 1.9e9, StarKind.MAIN_SEQUENCE,
                c("b", 0.602, 0.00049, 0.0123, 7740, 15.0, 0.40, 40.8));
        star("struve_2398", "Struve 2398", "Struve 2398", 18.713, 59.630, 11.5, 0.334, 0.0141, 0.351, 3441, 5e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.248, 0.0087, 0.273, 3345, 56, 0.53, 408));
        star("groombridge_34", "Groombridge 34", "Groombridge 34", 0.307, 44.023, 11.6, 0.38, 0.0223, 0.38, 3700, 5e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.15, 0.0014, 0.18, 3300, 147, 0.0, 2600), p('b', 3.0, -1, 0.072, 0.1));
        star("epsilon_indi", "Epsilon Indi", "Epsilon Indi", 22.055, -56.786, 11.87, 0.76, 0.22, 0.711, 4630, 3.5e9,
                StarKind.MAIN_SEQUENCE, null, p('b', 6.0 * 317.83, 1.1 * 11.21, 28.4, 0.40));
        star("dx_cancri", "DX Cancri", "DX Cancri", 8.495, 26.775, 11.8, 0.109, 0.00065, 0.11, 2840);
        star("tau_ceti", "Tau Ceti", "Tau Ceti", 1.734, -15.937, 11.9, 0.783, 0.52, 0.793, 5344, 5.8e9, StarKind.MAIN_SEQUENCE, null);
        star("gj_1061", "GJ 1061", "GJ 1061", 3.598, -44.513, 12.0, 0.125, 0.0016, 0.156, 2953,
                p('b', 1.37, -1, 0.021, 0.3), p('c', 1.74, -1, 0.035, 0.29), p('d', 1.64, -1, 0.054, 0.53));
        star("yz_ceti", "YZ Ceti", "YZ Ceti", 1.208, -16.999, 12.1, 0.130, 0.0022, 0.157, 3151,
                p('b', 0.70, -1, 0.0163, 0.06), p('c', 1.14, -1, 0.0216, 0.0), p('d', 1.09, -1, 0.0285, 0.07));
        star("luytens_star", "Luyten's Star", "Luyten Yıldızı", 7.457, 5.225, 12.35, 0.26, 0.0088, 0.293, 3382,
                p('c', 1.18, -1, 0.036, 0.17), p('b', 2.89, -1, 0.091, 0.10));
        star("teegardens_star", "Teegarden's Star", "Teegarden Yıldızı", 2.888, 16.877, 12.5, 0.097, 0.00073, 0.107, 2904,
                p('b', 1.05, -1, 0.0259, 0.03), p('c', 1.11, -1, 0.0455, 0.04), p('d', 0.82, -1, 0.0791, 0.07));
        star("kapteyns_star", "Kapteyn's Star", "Kapteyn Yıldızı", 5.194, -45.018, 12.8, 0.281, 0.012, 0.291, 3550, 1.1e10,
                StarKind.MAIN_SEQUENCE, null);
        star("lacaille_8760", "Lacaille 8760", "Lacaille 8760", 21.290, -38.867, 12.95, 0.60, 0.072, 0.51, 3800);
        star("kruger_60", "Kruger 60", "Kruger 60", 22.466, 57.696, 13.1, 0.271, 0.010, 0.35, 3180, 5e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.176, 0.0035, 0.24, 3100, 9.5, 0.41, 44.6));
        star("wolf_1061", "Wolf 1061", "Wolf 1061", 16.507, -12.663, 14.0, 0.294, 0.0109, 0.307, 3342,
                p('b', 1.91, -1, 0.0375, 0.15), p('c', 3.41, -1, 0.089, 0.11), p('d', 7.70, -1, 0.470, 0.55));
        star("van_maanens_star", "Van Maanen's Star", "Van Maanen Yıldızı", 0.818, 5.389, 14.1, 0.67, 0.00017, 0.011, 6220,
                3.0e9, StarKind.WHITE_DWARF, null);
        star("gliese_876", "Gliese 876", "Gliese 876", 22.888, -14.263, 15.2, 0.33, 0.0122, 0.376, 3473,
                p('d', 6.8, -1, 0.0208, 0.08), p('c', 0.714 * 317.83, -1, 0.1296, 0.26), p('b', 2.28 * 317.83, -1, 0.2083, 0.03),
                p('e', 14.6, -1, 0.3343, 0.05));
        star("gliese_1002", "Gliese 1002", "Gliese 1002", 0.110, -7.546, 15.8, 0.12, 0.0014, 0.137, 3024,
                p('b', 1.08, -1, 0.0457, 0.0), p('c', 1.36, -1, 0.0738, 0.0));
        star("groombridge_1618", "Groombridge 1618", "Groombridge 1618", 10.190, 49.453, 15.9, 0.67, 0.12, 0.605, 4310);
        star("eridani_40", "40 Eridani", "40 Eridani", 4.254, -7.653, 16.3, 0.78, 0.46, 0.81, 5072, 5.6e9, StarKind.MAIN_SEQUENCE,
                c("b", 0.573, 0.013, 0.014, 16500, 400, 0.41, 7200));
        star("ophiuchi_70", "70 Ophiuchi", "70 Ophiuchi", 18.091, 2.500, 16.6, 0.89, 0.59, 0.91, 5300, 1.0e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.70, 0.15, 0.67, 4400, 23.2, 0.50, 88.4));
        star("altair", "Altair", "Altair", 19.846, 8.868, 16.73, 1.86, 10.6, 1.79, 7700, 1.0e8, StarKind.MAIN_SEQUENCE, null);
        star("sigma_draconis", "Sigma Draconis", "Sigma Draconis", 19.540, 69.661, 18.8, 0.85, 0.43, 0.778, 5297);
        star("eta_cassiopeiae", "Eta Cassiopeiae", "Eta Cassiopeiae", 0.818, 57.815, 19.4, 0.97, 1.23, 1.04, 5973, 5.4e9,
                StarKind.MAIN_SEQUENCE, c("b", 0.57, 0.062, 0.66, 4036, 71, 0.50, 480));
        star("eridani_82", "82 Eridani", "82 Eridani", 3.330, -43.070, 19.7, 0.85, 0.74, 0.92, 5401);
        star("delta_pavonis", "Delta Pavonis", "Delta Pavonis", 20.145, -66.182, 19.9, 0.99, 1.22, 1.22, 5604);
        star("gliese_581", "Gliese 581", "Gliese 581", 15.323, -7.722, 20.5, 0.31, 0.0122, 0.299, 3480,
                p('e', 1.7, -1, 0.0282, 0.0), p('b', 15.8, -1, 0.0406, 0.0), p('c', 5.5, -1, 0.0721, 0.0));
        star("hd_219134", "HD 219134", "HD 219134", 23.218, 57.169, 21.3, 0.81, 0.27, 0.778, 4699,
                p('b', 4.74, 1.60, 0.0388, 0.0), p('c', 4.36, 1.51, 0.0653, 0.06), p('f', 7.3, -1, 0.1463, 0.15),
                p('d', 16.2, -1, 0.2370, 0.14), p('g', 11.0, -1, 0.3753, 0.0), p('h', 108, -1, 3.11, 0.06));
        star("gliese_667", "Gliese 667", "Gliese 667", 17.311, -34.995, 23.6, 0.33, 0.0137, 0.42, 3350,
                p('b', 5.6, -1, 0.0505, 0.13), p('c', 3.8, -1, 0.125, 0.02));
        star("vega", "Vega", "Vega", 18.616, 38.784, 25.0, 2.14, 40.1, 2.36, 9602, 4.5e8, StarKind.MAIN_SEQUENCE, null);
        star("fomalhaut", "Fomalhaut", "Fomalhaut", 22.961, -29.622, 25.1, 1.92, 16.6, 1.84, 8590, 4.4e8, StarKind.MAIN_SEQUENCE, null);
        star("trappist_1", "TRAPPIST-1", "TRAPPIST-1", 23.107, -5.041, 40.66, 0.0898, 0.000553, 0.1192, 2566, 7.6e9,
                StarKind.MAIN_SEQUENCE, null,
                // masses, radii and semi-major axes from Agol et al. (2021)
                p('b', 1.374, 1.116, 0.01154, 0.005), p('c', 1.308, 1.097, 0.01580, 0.005), p('d', 0.388, 0.788, 0.02227, 0.005),
                p('e', 0.692, 0.920, 0.02925, 0.005), p('f', 1.039, 1.045, 0.03849, 0.005), p('g', 1.321, 1.129, 0.04683, 0.005),
                p('h', 0.326, 0.755, 0.06189, 0.005));
    }

    public static final List<Entry> ALL = List.copyOf(BUILT);

    public static Optional<Entry> entry(String key) {
        return ALL.stream().filter(e -> e.key.equals(key)).findFirst();
    }

    public static String rootId(String key) {
        return "spacebreaker:" + key;
    }

    /** Stars with planets the generator may fill in, limited by a companion's stability zone; 0 = none. */
    private static double fillLimitAu(Entry e) {
        if (e.kind != StarKind.MAIN_SEQUENCE) {
            return 0;
        }
        if (e.companion.isPresent()) {
            Companion c = e.companion.get();
            return 0.3 * c.aAu() * (1 - c.e());
        }
        return 60;
    }

    /**
     * Builds the system of a real star; empty for "sol" (use the celestial_body registry). Confirmed planets are
     * marked discovered; planets the generator adds beyond them are not.
     */
    public static Optional<StarSystem> system(StarSeed seed) {
        Entry e = entry(seed.fixedKey()).orElseThrow();
        if (e.key.equals("sol")) {
            return Optional.empty();
        }
        boolean named = e.companion.isPresent();
        String root = named ? rootId(e.key + "_a") : rootId(e.key);
        String nameEn = named ? e.nameEn + " A" : e.nameEn;
        String nameTr = named ? e.nameTr + " A" : e.nameTr;
        StarKind kind = e.kind;
        SystemBuilder b = new SystemBuilder(seed, root, kind, e.star(), nameEn, nameTr);
        b.discovered(true);
        Rng rng = new Rng(Rng.hash(seed.id(), 0x4E454152L));
        if (e.companion.isPresent()) {
            Companion c = e.companion.get();
            StellarModel.Star cs = StellarModel.catalogue(c.mass(), c.luminosity(), c.radius(), c.temperature());
            b.companion(rootId(e.key + "_" + c.suffix()), e.nameEn + " B", e.nameTr + " B", cs, c.aAu(), c.e(), c.periodYears() * 365.25);
            b.binary(new StarSystem.Binary(cs, c.aAu(), c.e(), c.periodYears() * 365.25));
        }
        double outermost = 0;
        for (Planet p : e.planets) {
            String base = named ? e.key + "_a" : e.key;
            b.planet(rootId(base + "_" + p.letter()), nameEn + " " + p.letter(), nameTr + " " + p.letter(), p.massEarths(),
                    p.radiusEarths(), p.aAu(), p.e(), 0.5, rng);
            outermost = Math.max(outermost, p.aAu() * (1 + p.e()));
        }
        b.discovered(false);
        double limit = fillLimitAu(e);
        if (limit > 0 && !e.key.equals("trappist_1")) {
            int nextLetter = e.planets.stream().mapToInt(p -> p.letter() - 'b' + 1).max().orElse(0);
            StarSystemGenerator.addPlanets(b, new Rng(rng.nextLong()), limit, nameEn, nameTr, outermost, nextLetter);
        }
        return Optional.of(b.build());
    }

    private NearbyStars() {
    }
}
