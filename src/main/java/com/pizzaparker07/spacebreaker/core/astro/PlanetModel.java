package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Planetary physics estimates: habitable zone, equilibrium and surface temperature, atmosphere retention,
 * tidal locking, surface gravity, escape velocity and rough composition classes. SI units unless noted.
 */
public final class PlanetModel {

    /** Rough bulk composition from mass and density. */
    public enum Composition {
        IRON_RICH, ROCKY, OCEAN, ICY, ICE_GIANT, GAS_GIANT;

        public String serializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // ------------------------------------------------------------------ habitable zone & temperature

    /** Inner edge of the conservative habitable zone (runaway greenhouse), AU. L in L☉. */
    public static double habitableZoneInner(double luminosity) {
        return Math.sqrt(luminosity / 1.1);
    }

    /** Outer edge of the conservative habitable zone (maximum greenhouse), AU. */
    public static double habitableZoneOuter(double luminosity) {
        return Math.sqrt(luminosity / 0.53);
    }

    public static boolean inHabitableZone(double luminosity, double distanceAu) {
        return distanceAu >= habitableZoneInner(luminosity) && distanceAu <= habitableZoneOuter(luminosity);
    }

    /** Stellar flux at a distance, W/m². L in watts, d in metres. */
    public static double flux(double luminosityW, double distanceM) {
        return luminosityW / (4 * Math.PI * distanceM * distanceM);
    }

    /** Flux in W/m² from L in L☉ and distance in AU. */
    public static double fluxSolar(double luminosity, double distanceAu) {
        return AstroConstants.SOLAR_CONSTANT * luminosity / (distanceAu * distanceAu);
    }

    /**
     * Equilibrium temperature of a fast rotator (heat spread over the whole sphere), K.
     * T = [F (1 − A) / (4σ)]^¼. Earth (A = 0.306) ≈ 254 K.
     */
    public static double equilibriumTemperature(double fluxWm2, double albedo) {
        return Math.pow(fluxWm2 * (1 - albedo) / (4 * AstroConstants.SIGMA), 0.25);
    }

    /** Equilibrium temperature of the day side of a tidally locked or slow rotator (factor 2 instead of 4), K. */
    public static double daySideTemperature(double fluxWm2, double albedo) {
        return Math.pow(fluxWm2 * (1 - albedo) / (2 * AstroConstants.SIGMA), 0.25);
    }

    /** Equilibrium temperature from L☉, AU and albedo, K. */
    public static double equilibriumTemperature(double luminosity, double distanceAu, double albedo) {
        return equilibriumTemperature(fluxSolar(luminosity, distanceAu), albedo);
    }

    /**
     * Surface temperature with a grey greenhouse of infrared optical depth τ: T_s = T_eq (1 + ¾τ)^¼.
     * Earth τ ≈ 0.83 gives ≈ 288 K.
     */
    public static double surfaceTemperature(double equilibriumK, double opticalDepth) {
        return equilibriumK * Math.pow(1 + 0.75 * opticalDepth, 0.25);
    }

    /**
     * Rough infrared optical depth from surface pressure (kPa) and CO₂/H₂O-like greenhouse fraction (0..1).
     * Calibrated so Earth (101 kPa, ≈ 1 % effective) ≈ 0.83 and Venus (9200 kPa, 96 % CO₂) is very large.
     */
    public static double opticalDepth(double pressureKpa, double greenhouseFraction) {
        if (pressureKpa <= 0) {
            return 0;
        }
        double column = pressureKpa / 101.325;
        return 0.83 * Math.pow(column, 1.0) * Math.pow(Math.max(greenhouseFraction, 1e-6) / 0.01, 0.3);
    }

    // ------------------------------------------------------------------ gravity & atmospheres

    public static double surfaceGravity(double massKg, double radiusM) {
        return AstroConstants.G * massKg / (radiusM * radiusM);
    }

    public static double escapeVelocity(double massKg, double radiusM) {
        return Math.sqrt(2 * AstroConstants.G * massKg / radiusM);
    }

    /** Root-mean-square thermal speed of a gas (molar mass in g/mol) at temperature T, m/s. */
    public static double thermalVelocity(double temperatureK, double molarMass) {
        return Math.sqrt(3 * AstroConstants.K_B * temperatureK / (molarMass * AstroConstants.AMU));
    }

    /**
     * Jeans-style retention rule of thumb: a gas is kept over billions of years when the escape velocity is
     * at least six times its thermal speed at the exosphere temperature.
     */
    public static boolean retainsGas(double escapeVelocity, double exosphereK, double molarMass) {
        return escapeVelocity >= 6 * thermalVelocity(exosphereK, molarMass);
    }

    /** Lightest molar mass (g/mol) retained by the 6× rule. */
    public static double lightestRetainedMolarMass(double escapeVelocity, double exosphereK) {
        double vMax = escapeVelocity / 6;
        return 3 * AstroConstants.K_B * exosphereK / (vMax * vMax * AstroConstants.AMU);
    }

    /**
     * Exosphere temperature estimate: the upper atmosphere is heated by stellar UV above T_eq. Calibrated to
     * Earth (≈ 1000 K) with the heating scaled by √(flux): T_exo = T_eq + 750 K · √(F / 1361 W/m²)
     * (Titan ≈ 160 K, close to the measured value).
     */
    public static double exosphereTemperature(double equilibriumK, double fluxWm2) {
        return equilibriumK + 750 * Math.sqrt(Math.max(0, fluxWm2) / AstroConstants.SOLAR_CONSTANT);
    }

    // ------------------------------------------------------------------ tides

    /**
     * Tidal locking time (Gladman et al. 1996), years.
     * t ≈ ω a⁶ I Q / (3 G M_p² k₂ R⁵), I = 0.4 m R², with Q = 100 and k₂ = 0.3 (rocky) by default.
     *
     * @param initialSpinPeriod initial rotation period, s (12 h is customary)
     * @param semiMajorAxis     m
     * @param mass              of the satellite/planet that gets locked, kg
     * @param radius            of that body, m
     * @param primaryMass       mass of the body it orbits, kg
     */
    public static double tidalLockTimeYears(double initialSpinPeriod, double semiMajorAxis, double mass, double radius,
                                            double primaryMass, double q, double k2) {
        double omega = Kepler.TWO_PI / initialSpinPeriod;
        double inertia = 0.4 * mass * radius * radius;
        double t = omega * Math.pow(semiMajorAxis, 6) * inertia * q
                / (3 * AstroConstants.G * primaryMass * primaryMass * k2 * Math.pow(radius, 5));
        return t / AstroConstants.YEAR;
    }

    public static double tidalLockTimeYears(double semiMajorAxis, double mass, double radius, double primaryMass) {
        return tidalLockTimeYears(12 * 3600, semiMajorAxis, mass, radius, primaryMass, 100, 0.3);
    }

    /** True when the body would be locked within the system age. */
    public static boolean tidallyLocked(double semiMajorAxis, double mass, double radius, double primaryMass,
                                        double systemAgeYears) {
        return tidalLockTimeYears(semiMajorAxis, mass, radius, primaryMass) < systemAgeYears;
    }

    // ------------------------------------------------------------------ composition & mass-radius

    public static double density(double massKg, double radiusM) {
        return massKg / (4.0 / 3.0 * Math.PI * radiusM * radiusM * radiusM);
    }

    /** Composition class from mass (Earth masses) and mean density (kg/m³). */
    public static Composition composition(double massEarths, double density) {
        if (massEarths > 50) {
            return Composition.GAS_GIANT;
        }
        if (massEarths > 8) {
            return density > 1100 && massEarths < 30 ? Composition.ICE_GIANT : Composition.GAS_GIANT;
        }
        if (density > 6500 && massEarths < 2 || density > 5200 && massEarths < 0.1) {
            return Composition.IRON_RICH;
        }
        if (density > 3200) {
            return Composition.ROCKY;
        }
        if (density > 2200) {
            return Composition.OCEAN;
        }
        return Composition.ICY;
    }

    /**
     * Radius estimate (Earth radii) for a generated planet of a given mass (Earth masses) and composition,
     * from simple mass–radius power laws (Chen &amp; Kipping style).
     */
    public static double radiusFor(double massEarths, Composition c) {
        return switch (c) {
            case IRON_RICH -> 0.8 * Math.pow(massEarths, 0.28);
            case ROCKY -> Math.pow(massEarths, 0.28);
            case OCEAN -> 1.15 * Math.pow(massEarths, 0.28);
            case ICY -> 1.3 * Math.pow(massEarths, 0.3);
            case ICE_GIANT -> 0.7 * Math.pow(massEarths, 0.59);
            case GAS_GIANT -> massEarths < 130 ? 0.7 * Math.pow(massEarths, 0.59) : 11.2 * Math.pow(massEarths / 318, -0.04);
        };
    }

    private PlanetModel() {
    }
}
