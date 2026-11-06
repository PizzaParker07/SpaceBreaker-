package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Real stars near the Sun, placed at their measured positions with catalogue properties and their known
 * planets. Anything not yet measured (more planets, moons) is filled in by the generator, deterministically
 * per world seed. The Sun itself ("sol") is listed for the star map; its system is the data-pack registry.
 */
public final class NearbyStars {

    /**
     * @param key      stable key, also the id path ({@code spacebreaker:<key>})
     * @param nameEn   English name
     * @param nameTr   Turkish name
     * @param lDeg     galactic longitude
     * @param bDeg     galactic latitude
     * @param distancePc distance from the Sun
     * @param mass     M☉
     * @param luminosity L☉
     * @param radius   R☉
     * @param temperature K
     * @param ageYears age estimate
     */
    public record Entry(String key, String nameEn, String nameTr, double lDeg, double bDeg, double distancePc,
                        double mass, double luminosity, double radius, double temperature, double ageYears) {
        /** Galactocentric position, pc. */
        public Vec3 position() {
            double l = Math.toRadians(lDeg), b = Math.toRadians(bDeg);
            return GalaxyModel.SUN.add(new Vec3(Math.cos(b) * Math.cos(l), Math.cos(b) * Math.sin(l), Math.sin(b)).scale(distancePc));
        }

        public StellarModel.Star star() {
            return StellarModel.catalogue(mass, luminosity, radius, temperature);
        }

        public double distanceLy() {
            return distancePc * AstroConstants.PARSEC / AstroConstants.LIGHT_YEAR;
        }
    }

    public static final List<Entry> ALL = List.of(
            new Entry("sol", "Sun", "Güneş", 0, 0, 0, 1.0, 1.0, 1.0, 5772, 4.6e9),
            new Entry("alpha_centauri", "Alpha Centauri", "Alfa Centauri", 315.73, -0.68, 1.3383, 1.0788, 1.519, 1.2175, 5790, 5.3e9),
            new Entry("proxima_centauri", "Proxima Centauri", "Proxima Centauri", 313.94, -1.93, 1.3012, 0.1221, 0.00155, 0.1542, 3042, 4.85e9),
            new Entry("barnards_star", "Barnard's Star", "Barnard Yıldızı", 31.01, 14.06, 1.8282, 0.162, 0.0034, 0.187, 3195, 1.0e10),
            new Entry("sirius", "Sirius", "Sirius", 227.23, -8.89, 2.6371, 2.063, 25.4, 1.711, 9940, 2.4e8),
            new Entry("epsilon_eridani", "Epsilon Eridani", "Epsilon Eridani", 195.84, -48.05, 3.2198, 0.82, 0.34, 0.735, 5084, 6e8),
            new Entry("tau_ceti", "Tau Ceti", "Tau Ceti", 173.10, -73.44, 3.6502, 0.783, 0.52, 0.793, 5344, 5.8e9),
            new Entry("trappist_1", "TRAPPIST-1", "TRAPPIST-1", 69.71, -56.64, 12.467, 0.0898, 0.000553, 0.1192, 2566, 7.6e9)
    );

    public static Optional<Entry> entry(String key) {
        return ALL.stream().filter(e -> e.key.equals(key)).findFirst();
    }

    public static String rootId(String key) {
        return "spacebreaker:" + key;
    }

    /** Builds the system of a real star; empty for "sol" (use the celestial_body registry). */
    public static Optional<StarSystem> system(StarSeed seed) {
        Entry e = entry(seed.fixedKey()).orElseThrow();
        if (e.key.equals("sol")) {
            return Optional.empty();
        }
        String root = switch (e.key) {
            case "alpha_centauri", "sirius" -> rootId(e.key + "_a");
            default -> rootId(e.key);
        };
        String nameEn = e.key.equals("alpha_centauri") || e.key.equals("sirius") ? e.nameEn + " A" : e.nameEn;
        String nameTr = e.key.equals("alpha_centauri") || e.key.equals("sirius") ? e.nameTr + " A" : e.nameTr;
        SystemBuilder b = new SystemBuilder(seed, root, StarKind.MAIN_SEQUENCE, e.star(), nameEn, nameTr);
        Rng rng = new Rng(Rng.hash(seed.id(), 0x4E454152L));
        switch (e.key) {
            case "alpha_centauri" -> {
                b.companion(rootId("alpha_centauri_b"), "Alpha Centauri B", "Alfa Centauri B",
                        StellarModel.catalogue(0.9092, 0.5002, 0.8591, 5260), 23.4, 0.5179, 79.91 * 365.25);
                // binary periastron 11.3 AU: S-type planets are stable inside ~2.5 AU of A
                StarSystemGenerator.addPlanets(b, rng, 2.5, nameEn, nameTr);
            }
            case "proxima_centauri" ->
                    b.planet(rootId("proxima_centauri_b"), "Proxima Centauri b", "Proxima Centauri b", 1.07, 1.07, 0.04857, 0.02, 0, rng);
            case "barnards_star" ->
                    b.planet(rootId("barnards_star_b"), "Barnard's Star b", "Barnard Yıldızı b", 0.37, 0.75, 0.0229, 0.03, 0, rng);
            case "sirius" -> b.companion(rootId("sirius_b"), "Sirius B", "Sirius B",
                    StellarModel.catalogue(1.018, 0.056, 0.0084, 25000), 19.8, 0.5923, 50.13 * 365.25);
            case "epsilon_eridani" -> {
                b.planet(rootId("epsilon_eridani_b"), "Epsilon Eridani b", "Epsilon Eridani b", 0.66 * 317.83, 1.2 * 11.21, 3.53, 0.07, 0, rng);
                StarSystemGenerator.addPlanets(b, new Rng(rng.nextLong()), 2.0, nameEn, nameTr, 0, 1);
            }
            case "tau_ceti" -> StarSystemGenerator.addPlanets(b, rng, 60, nameEn, nameTr);
            case "trappist_1" -> {
                // masses, radii and semi-major axes from Agol et al. (2021)
                double[][] p = {{1.374, 1.116, 0.01154}, {1.308, 1.097, 0.01580}, {0.388, 0.788, 0.02227},
                        {0.692, 0.920, 0.02925}, {1.039, 1.045, 0.03849}, {1.321, 1.129, 0.04683}, {0.326, 0.755, 0.06189}};
                for (int i = 0; i < p.length; i++) {
                    char c = (char) ('b' + i);
                    b.planet(rootId("trappist_1_" + c), "TRAPPIST-1 " + c, "TRAPPIST-1 " + c, p[i][0], p[i][1], p[i][2], 0.005, 0.1, rng);
                }
            }
            default -> {
            }
        }
        return Optional.of(b.build());
    }

    private NearbyStars() {
    }
}
