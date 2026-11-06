package com.pizzaparker07.spacebreaker.core.astro;

/** Physical and astronomical constants. SI units unless the name says otherwise. */
public final class AstroConstants {
    /** Newtonian constant of gravitation, m³ kg⁻¹ s⁻². */
    public static final double G = 6.67430e-11;
    /** Astronomical unit, m. */
    public static final double AU = 1.495978707e11;
    /** Light year, m. */
    public static final double LIGHT_YEAR = 9.4607304725808e15;
    /** Parsec, m. */
    public static final double PARSEC = 3.0856775814913673e16;
    /** Julian day, s. */
    public static final double DAY = 86400.0;
    /** Julian year, s. */
    public static final double YEAR = 365.25 * DAY;
    /** Standard gravity, m/s². */
    public static final double G0 = 9.80665;

    /** Solar mass, kg. */
    public static final double SOLAR_MASS = 1.98847e30;
    /** Nominal solar radius, m (IAU 2015). */
    public static final double SOLAR_RADIUS = 6.957e8;
    /** Nominal solar luminosity, W (IAU 2015). */
    public static final double SOLAR_LUMINOSITY = 3.828e26;
    /** Nominal solar effective temperature, K. */
    public static final double SOLAR_TEMPERATURE = 5772.0;
    /** Main-sequence lifetime of the Sun, years. */
    public static final double SOLAR_LIFETIME_YEARS = 1.0e10;
    /** Solar gravitational parameter, m³/s². */
    public static final double MU_SUN = 1.32712440018e20;

    /** Earth mass, kg. */
    public static final double EARTH_MASS = 5.9722e24;
    /** Earth mean radius, m. */
    public static final double EARTH_RADIUS = 6.371e6;
    /** Earth gravitational parameter, m³/s². */
    public static final double MU_EARTH = 3.986004418e14;
    /** Jupiter mass, kg. */
    public static final double JUPITER_MASS = 1.89813e27;

    /** Stefan–Boltzmann constant, W m⁻² K⁻⁴. */
    public static final double SIGMA = 5.670374419e-8;
    /** Boltzmann constant, J/K. */
    public static final double K_B = 1.380649e-23;
    /** Atomic mass unit, kg. */
    public static final double AMU = 1.66053906660e-27;
    /** Solar irradiance at 1 AU, W/m². */
    public static final double SOLAR_CONSTANT = 1361.0;

    private AstroConstants() {
    }
}
