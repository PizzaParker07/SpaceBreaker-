package com.pizzaparker07.spacebreaker.core.astro;

import java.util.Map;

/**
 * Pure-Java view of a celestial body in SI units, used by every calculation in this package.
 * Build one from a data-pack body with {@link BodyAdapter}, or directly (procedural generation, tests).
 *
 * @param id                 resource id, e.g. {@code spacebreaker:mars}
 * @param parent             id of the body it orbits, or {@code null} for a star / system root
 * @param kind               kind of body (same names as {@code BodyType})
 * @param mass               kg
 * @param radius             mean radius, m
 * @param orbit              orbit around the parent, distances in m and angles in rad ({@code mu} = parent's μ); may be {@code null}
 * @param rotationPeriod     solar day length, s
 * @param tidallyLocked      true when one side always faces the parent
 * @param surfacePressureKpa surface (or 1-bar reference) pressure
 * @param composition        atmospheric volume fractions by formula ("N2", "CO2", …)
 * @param dayTempK           typical noon surface temperature, K
 * @param nightTempK         typical pre-dawn surface temperature, K
 * @param albedo             Bond albedo
 * @param landable           true when the body has a surface dimension
 * @param geometricAlbedo    geometric (visual) albedo, negative when unknown (then the Bond albedo is used)
 */
public record AstroBody(String id, String parent, Kind kind, double mass, double radius, OrbitalElements orbit,
                        double rotationPeriod, boolean tidallyLocked, double surfacePressureKpa,
                        Map<String, Double> composition, double dayTempK, double nightTempK, double albedo,
                        boolean landable, double geometricAlbedo) {

    /** Constructor without the geometric albedo. */
    public AstroBody(String id, String parent, Kind kind, double mass, double radius, OrbitalElements orbit,
                     double rotationPeriod, boolean tidallyLocked, double surfacePressureKpa,
                     Map<String, Double> composition, double dayTempK, double nightTempK, double albedo, boolean landable) {
        this(id, parent, kind, mass, radius, orbit, rotationPeriod, tidallyLocked, surfacePressureKpa, composition,
                dayTempK, nightTempK, albedo, landable, -1);
    }

    /** Albedo for brightness in the sky: the geometric albedo when known, otherwise the Bond albedo. */
    public double visualAlbedo() {
        return geometricAlbedo >= 0 ? geometricAlbedo : albedo;
    }

    /** The same body on another orbit. */
    public AstroBody withOrbit(OrbitalElements o) {
        return new AstroBody(id, parent, kind, mass, radius, o, rotationPeriod, tidallyLocked, surfacePressureKpa,
                composition, dayTempK, nightTempK, albedo, landable, geometricAlbedo);
    }

    public enum Kind {
        STAR, TERRESTRIAL, GAS_GIANT, ICE_GIANT, DWARF, MOON, ASTEROID, STATION, COMET;

        public static Kind byName(String name) {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        }

        public String serializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean isGiant() {
            return this == GAS_GIANT || this == ICE_GIANT;
        }
    }

    /** Molar masses (g/mol) of common atmospheric gases. */
    public static final Map<String, Double> MOLAR_MASS = Map.ofEntries(
            Map.entry("H2", 2.016), Map.entry("He", 4.003), Map.entry("CH4", 16.04), Map.entry("NH3", 17.03),
            Map.entry("H2O", 18.02), Map.entry("Ne", 20.18), Map.entry("N2", 28.01), Map.entry("CO", 28.01),
            Map.entry("O2", 32.00), Map.entry("Ar", 39.95), Map.entry("CO2", 44.01), Map.entry("SO2", 64.07),
            Map.entry("Na", 22.99), Map.entry("O", 16.00), Map.entry("H", 1.008), Map.entry("K", 39.10),
            Map.entry("C2H6", 30.07), Map.entry("O3", 48.00), Map.entry("Kr", 83.80), Map.entry("Xe", 131.3));

    /**
     * Builds a body from the data-file units (kg, km, days, hours, degrees, °C). The orbit's μ is derived from the
     * semi-major axis and period, so positions always match the stated period.
     */
    public static AstroBody fromDataUnits(String id, String parent, Kind kind, double massKg, double radiusKm,
                                          double semiMajorAxisKm, double eccentricity, double inclinationDeg,
                                          double lanDeg, double argPeriDeg, double meanAnomalyDeg, double periodDays,
                                          double dayLengthHours, boolean tidallyLocked, double pressureKpa,
                                          Map<String, Double> composition, double dayTempC, double nightTempC,
                                          double albedo, boolean landable) {
        return fromDataUnits(id, parent, kind, massKg, radiusKm, semiMajorAxisKm, eccentricity, inclinationDeg, lanDeg,
                argPeriDeg, meanAnomalyDeg, periodDays, dayLengthHours, tidallyLocked, pressureKpa, composition, dayTempC,
                nightTempC, albedo, landable, -1);
    }

    /** As above, with the geometric albedo (negative when unknown). */
    public static AstroBody fromDataUnits(String id, String parent, Kind kind, double massKg, double radiusKm,
                                          double semiMajorAxisKm, double eccentricity, double inclinationDeg,
                                          double lanDeg, double argPeriDeg, double meanAnomalyDeg, double periodDays,
                                          double dayLengthHours, boolean tidallyLocked, double pressureKpa,
                                          Map<String, Double> composition, double dayTempC, double nightTempC,
                                          double albedo, boolean landable, double geometricAlbedo) {
        OrbitalElements orbit = null;
        if (semiMajorAxisKm > 0 && periodDays > 0) {
            double a = semiMajorAxisKm * 1000.0;
            double n = Kepler.TWO_PI / (periodDays * AstroConstants.DAY);
            orbit = new OrbitalElements(n * n * a * a * a, a, eccentricity, Math.toRadians(inclinationDeg),
                    Math.toRadians(lanDeg), Math.toRadians(argPeriDeg), Math.toRadians(meanAnomalyDeg));
        }
        return new AstroBody(id, parent, kind, massKg, radiusKm * 1000.0, orbit, dayLengthHours * 3600.0,
                tidallyLocked, pressureKpa, Map.copyOf(composition), dayTempC + 273.15, nightTempC + 273.15, albedo,
                landable, geometricAlbedo);
    }

    /** Gravitational parameter μ = GM, m³/s². */
    public double mu() {
        return AstroConstants.G * mass;
    }

    public double surfaceGravity() {
        return mu() / (radius * radius);
    }

    public double escapeVelocity() {
        return Kepler.escapeSpeed(mu(), radius);
    }

    /** Mean density, kg/m³. */
    public double density() {
        return mass / (4.0 / 3.0 * Math.PI * radius * radius * radius);
    }

    public boolean hasAtmosphere() {
        return surfacePressureKpa >= 0.1;
    }

    /** Mean surface temperature estimate, K. */
    public double meanTempK() {
        return (dayTempK + nightTempK) / 2;
    }

    /** Mean molecular mass of the atmosphere, g/mol (29 when unknown). */
    public double meanMolarMass() {
        double sum = 0, frac = 0;
        for (Map.Entry<String, Double> e : composition.entrySet()) {
            Double m = MOLAR_MASS.get(e.getKey());
            if (m != null) {
                sum += m * e.getValue();
                frac += e.getValue();
            }
        }
        return frac > 0 ? sum / frac : 29.0;
    }

    /** Atmospheric scale height H = kT / (m g), m. */
    public double scaleHeight() {
        double m = meanMolarMass() * AstroConstants.AMU;
        return AstroConstants.K_B * meanTempK() / (m * surfaceGravity());
    }

    /** Altitude above which drag is negligible for orbits, m (0 for airless bodies). */
    public double atmosphereTop() {
        return hasAtmosphere() ? 10 * scaleHeight() : 0;
    }

    /**
     * Radius of the default low parking orbit, m: above the atmosphere plus 2 % of the radius, or 5 % of the
     * radius for airless bodies (Earth ≈ 212 km, Moon ≈ 87 km, Mars ≈ 180 km).
     */
    public double lowOrbitRadius() {
        return radius + (hasAtmosphere() ? atmosphereTop() + 0.02 * radius : 0.05 * radius);
    }

    /** Sidereal rotation speed at the equator, m/s (ignores the small solar/sidereal difference). */
    public double equatorialSpeed() {
        if (tidallyLocked && orbit != null && Double.isFinite(orbit.period())) {
            return Kepler.TWO_PI * radius / orbit.period();
        }
        return rotationPeriod > 0 ? Kepler.TWO_PI * radius / rotationPeriod : 0;
    }

    public boolean isStar() {
        return kind == Kind.STAR;
    }
}
