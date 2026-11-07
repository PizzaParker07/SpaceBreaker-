package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * Outgassing of a comet as a function of its distance from the Sun.
 * <ul>
 *   <li>Water production Q(r) = Z₁ · A_active · g(r), with Marsden's water-ice sublimation law
 *       g(r) = α (r/r₀)^−m (1 + (r/r₀)^n)^−k (r₀ = 2.808 AU, m = 2.15, n = 5.093, k = 4.6142, g(1 AU) = 1).
 *       Activity falls off steeply beyond ≈ 3 AU: water ice barely sublimates there.</li>
 *   <li>Z₁ = 4·10²¹ molecules m⁻² s⁻¹: absorbed sunlight at 1 AU over the latent heat of water
 *       (51 kJ/mol), averaged over a rotating nucleus (a quarter of the subsolar value).</li>
 *   <li>Coma: Haser model, n(ρ) = Q / (4π v ρ²) · e^(−ρ/L); the edge is where n drops to
 *       10⁸ m⁻³. Outflow v = 0.85 km/s · r^−½, scale length L = v · 8.3·10⁴ s · r².</li>
 *   <li>Tail: empirical length 2·10⁶ km · √(Q / 10²⁸) / r, pointing away from the Sun.</li>
 * </ul>
 */
public final class CometActivity {
    public static final double Z_1AU = 4e21;
    public static final double WATER_MOLECULE_KG = 18.015 * AstroConstants.AMU;
    public static final double COMA_EDGE_DENSITY = 1e8;
    public static final double WATER_LIFETIME_1AU = 8.3e4;
    private static final double G_ALPHA = 0.111262, G_R0 = 2.808, G_M = 2.15, G_N = 5.093, G_K = 4.6142;

    public enum Level {
        DORMANT("Dormant", "Uykuda"),
        WEAK("Weak", "Zayıf"),
        ACTIVE("Active", "Etkin"),
        GREAT("Great comet", "Büyük kuyruklu yıldız");

        public final String nameEn, nameTr;

        Level(String en, String tr) {
            nameEn = en;
            nameTr = tr;
        }

        public static Level of(double waterPerSecond) {
            if (waterPerSecond < 1e24) return DORMANT;
            if (waterPerSecond < 1e27) return WEAK;
            if (waterPerSecond < 1e29) return ACTIVE;
            return GREAT;
        }
    }

    /**
     * @param distanceAu      heliocentric distance
     * @param waterPerSecond  water molecules per second
     * @param gasKgPerSecond  water mass loss
     * @param dustKgPerSecond dust mass loss
     * @param comaRadiusKm    radius of the gas coma (0 when dormant)
     * @param tailLengthKm    visible tail length (0 when dormant)
     * @param tailDirection   unit vector away from the Sun
     * @param level           activity class
     */
    public record Activity(double distanceAu, double waterPerSecond, double gasKgPerSecond, double dustKgPerSecond,
                           double comaRadiusKm, double tailLengthKm, Vec3 tailDirection, Level level) {
        public boolean active() {
            return level != Level.DORMANT;
        }

        public double totalKgPerSecond() {
            return gasKgPerSecond + dustKgPerSecond;
        }
    }

    private CometActivity() {
    }

    /** Marsden water-ice sublimation law, normalised to 1 at 1 AU. */
    public static double sublimation(double rAu) {
        double x = rAu / G_R0;
        return G_ALPHA * Math.pow(x, -G_M) * Math.pow(1 + Math.pow(x, G_N), -G_K);
    }

    public static double waterProduction(Comet c, double rAu) {
        return Z_1AU * c.activeFraction() * c.surfaceAreaM2() * sublimation(rAu);
    }

    public static double outflowSpeed(double rAu) {
        return 850 / Math.sqrt(rAu);
    }

    public static double scaleLength(double rAu) {
        return outflowSpeed(rAu) * WATER_LIFETIME_1AU * rAu * rAu;
    }

    /** Haser number density at distance ρ (m) from the nucleus, m⁻³. */
    public static double haserDensity(double q, double rAu, double rho) {
        return q / (4 * Math.PI * outflowSpeed(rAu) * rho * rho) * Math.exp(-rho / scaleLength(rAu));
    }

    /** Coma radius, m: where the Haser density falls to {@link #COMA_EDGE_DENSITY}. */
    public static double comaRadius(double q, double rAu, double nucleusRadiusM) {
        if (haserDensity(q, rAu, nucleusRadiusM) <= COMA_EDGE_DENSITY) return 0;
        double lo = nucleusRadiusM, hi = 60 * scaleLength(rAu);
        if (haserDensity(q, rAu, hi) > COMA_EDGE_DENSITY) return hi;
        for (int i = 0; i < 100; i++) {
            double mid = Math.sqrt(lo * hi);
            if (haserDensity(q, rAu, mid) > COMA_EDGE_DENSITY) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    public static Activity at(Comet c, Vec3 heliocentric) {
        double r = heliocentric.length() / AstroConstants.AU;
        double q = waterProduction(c, r);
        double gas = q * WATER_MOLECULE_KG;
        Level level = Level.of(q);
        boolean on = level != Level.DORMANT;
        double coma = on ? comaRadius(q, r, c.radiusKm() * 1000) / 1000 : 0;
        double tail = on ? 2e6 * Math.sqrt(q / 1e28) / r : 0;
        return new Activity(r, q, gas, gas * c.dustToGas(), coma, tail, heliocentric.normalize(), level);
    }

    /** Activity at simulated time t (s from J2000). */
    public static Activity at(Comet c, double t) {
        return at(c, c.position(t));
    }

    /**
     * Heliocentric total magnitude of the coma (as if seen from 1 AU) from the water production, after Jorda et al.
     * (2008): log₁₀ Q = 30.675 − 0.2453 m_H.
     */
    public static double comaHeliocentricMagnitude(double waterPerSecond) {
        return (30.675 - Math.log10(Math.max(waterPerSecond, 1)) ) / 0.2453;
    }

    /** Absolute magnitude H of the bare nucleus, from its size and geometric albedo. */
    public static double nucleusAbsoluteMagnitude(Comet c) {
        return 5 * Math.log10(1329 / (2 * c.radiusKm() * Math.sqrt(c.geometricAlbedo())));
    }

    /**
     * Apparent total magnitude seen from {@code observer} (both positions heliocentric, m): the brighter of the coma,
     * m_H + 5 log Δ, and the bare nucleus, H + 5 log (r Δ) (phase effects ignored). About 6 or less is naked-eye.
     */
    public static double apparentMagnitude(Comet c, Vec3 comet, Vec3 observer) {
        double r = comet.length() / AstroConstants.AU, delta = comet.sub(observer).length() / AstroConstants.AU;
        double coma = comaHeliocentricMagnitude(waterProduction(c, r)) + 5 * Math.log10(delta);
        double nucleus = nucleusAbsoluteMagnitude(c) + 5 * Math.log10(r * delta);
        return Math.min(coma, nucleus);
    }

    public static boolean nakedEye(double magnitude) {
        return magnitude <= 6.0;
    }

    /** Water lost over one orbit, kg (integrated in mean anomaly). */
    public static double waterLossPerOrbit(Comet c) {
        int steps = 20000;
        double a = c.semiMajorAxisAu(), e = c.eccentricity(), sum = 0;
        for (int i = 0; i < steps; i++) {
            double ecc = Kepler.eccentricAnomaly(Kepler.TWO_PI * (i + 0.5) / steps, e);
            sum += waterProduction(c, a * (1 - e * Math.cos(ecc)));
        }
        return sum / steps * c.periodSeconds() * WATER_MOLECULE_KG;
    }

    /** Mean surface lowering per orbit, m (gas and dust over the whole surface). */
    public static double erosionPerOrbit(Comet c) {
        return waterLossPerOrbit(c) * (1 + c.dustToGas()) / (c.density() * c.surfaceAreaM2());
    }

    /** Distance inside which the comet is at least {@link Level#WEAK}, AU. */
    public static double activationDistance(Comet c) {
        double lo = 0.05, hi = 50;
        for (int i = 0; i < 100; i++) {
            double mid = 0.5 * (lo + hi);
            if (waterProduction(c, mid) >= 1e24) lo = mid;
            else hi = mid;
        }
        return lo;
    }
}
