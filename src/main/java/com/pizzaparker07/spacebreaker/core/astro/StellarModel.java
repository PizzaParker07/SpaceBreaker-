package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Main-sequence star properties from mass (solar units), using standard textbook fits.
 * Accuracy is a few tens of percent — plenty for generated stars; real stars use their catalogue values.
 */
public final class StellarModel {

    /** Basic star description. Mass, luminosity and radius in solar units, temperature in K, lifetime in years. */
    public record Star(double mass, double luminosity, double radius, double temperature, double lifetimeYears,
                       SpectralClass spectralClass, int colorRgb) {
    }

    public enum SpectralClass {
        O(30000), B(10000), A(7500), F(6000), G(5200), K(3700), M(2400), L(1300), T(550), Y(0);

        /** Lowest effective temperature of the class, K. */
        public final double minTemperature;

        SpectralClass(double minTemperature) {
            this.minTemperature = minTemperature;
        }

        public static SpectralClass of(double temperature) {
            for (SpectralClass c : values()) {
                if (temperature >= c.minTemperature) {
                    return c;
                }
            }
            return Y;
        }
    }

    /** Mass–luminosity relation (piecewise power law), L☉. */
    public static double luminosity(double mass) {
        if (mass < 0.43) {
            return 0.23 * Math.pow(mass, 2.3);
        }
        if (mass < 2) {
            return Math.pow(mass, 4);
        }
        if (mass < 55) {
            return 1.4 * Math.pow(mass, 3.5);
        }
        return 32000 * mass;
    }

    /** Mass–radius relation, R☉. */
    public static double radius(double mass) {
        return mass < 1 ? Math.pow(mass, 0.8) : Math.pow(mass, 0.57);
    }

    /** Effective temperature from luminosity and radius (Stefan–Boltzmann), K. */
    public static double temperature(double luminosity, double radius) {
        return AstroConstants.SOLAR_TEMPERATURE * Math.pow(luminosity / (radius * radius), 0.25);
    }

    /** Main-sequence lifetime, years (10 Gyr · M / L). */
    public static double lifetimeYears(double mass) {
        return AstroConstants.SOLAR_LIFETIME_YEARS * mass / luminosity(mass);
    }

    public static Star mainSequence(double mass) {
        double l = luminosity(mass);
        double r = radius(mass);
        double t = temperature(l, r);
        return new Star(mass, l, r, t, lifetimeYears(mass), SpectralClass.of(t), blackbodyColor(t));
    }

    /** A star from catalogue values (solar units, K); the lifetime is estimated from mass and luminosity. */
    public static Star catalogue(double mass, double luminosity, double radius, double temperature) {
        return new Star(mass, luminosity, radius, temperature,
                AstroConstants.SOLAR_LIFETIME_YEARS * mass / luminosity, SpectralClass.of(temperature),
                blackbodyColor(temperature));
    }

    /**
     * Apparent colour of a black body as 0xRRGGBB, from the usual fit to the CIE colour matching
     * (valid 1000–40000 K).
     */
    public static int blackbodyColor(double kelvin) {
        double t = Math.max(1000, Math.min(40000, kelvin)) / 100.0;
        double r, g, b;
        if (t <= 66) {
            r = 255;
            g = 99.4708025861 * Math.log(t) - 161.1195681661;
            b = t <= 19 ? 0 : 138.5177312231 * Math.log(t - 10) - 305.0447927307;
        } else {
            r = 329.698727446 * Math.pow(t - 60, -0.1332047592);
            g = 288.1221695283 * Math.pow(t - 60, -0.0755148492);
            b = 255;
        }
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(double v) {
        return (int) Math.round(Math.max(0, Math.min(255, v)));
    }

    /** Stellar mass from the habitable-zone friendly inverse of {@link #luminosity} (bisection), M☉. */
    public static double massForLuminosity(double luminosity) {
        double lo = 0.01, hi = 200;
        for (int i = 0; i < 100; i++) {
            double mid = Math.sqrt(lo * hi);
            if (luminosity(mid) < luminosity) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return Math.sqrt(lo * hi);
    }

    private StellarModel() {
    }
}
