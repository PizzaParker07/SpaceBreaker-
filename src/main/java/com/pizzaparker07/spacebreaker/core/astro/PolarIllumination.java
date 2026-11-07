package com.pizzaparker07.spacebreaker.core.astro;

/**
 * How light reaches a body's poles: the highest the star ever climbs there, where crater floors stay in
 * permanent shadow (PSRs), how cold those cold traps get and whether water ice survives in them.
 * Feeds the planet engine's polar ice (e.g. {@code TerrainProfile.Poles.iceLatitude}).
 *
 * @param effectiveObliquityDeg  axial tilt folded to 0..90° (Uranus 97.8° → 82.2°)
 * @param maxPoleSunElevationDeg highest elevation of the star seen from the pole during a year
 * @param psrLatitudeDeg         lowest latitude where a typical crater floor never sees the star (90 = nowhere)
 * @param psrAreaFraction        share of the whole surface in permanent shadow
 * @param coldTrapTempK          temperature of those crater floors (lit only by scattered light)
 * @param iceStable              water ice survives for billions of years in them (airless and below 110 K)
 * @param polarDayNight          poles have months-long day and night (obliquity above a few degrees)
 */
public record PolarIllumination(double effectiveObliquityDeg, double maxPoleSunElevationDeg, double psrLatitudeDeg,
                                double psrAreaFraction, double coldTrapTempK, boolean iceStable, boolean polarDayNight) {
    /** Depth-to-diameter ratio of a typical degraded polar crater. */
    public static final double TYPICAL_DEPTH_RATIO = 0.1;
    /** Angular radius of the Sun seen from 1 AU, degrees. */
    public static final double SUN_RADIUS_DEG_AT_1AU = 0.267;
    /** Water ice loses less than ~1 mm per billion years below this, K. */
    public static final double ICE_STABILITY_K = 110;
    /** Share of the polar cap area that is crater floor deep enough to stay dark. */
    static final double FLOOR_SHARE = 0.06;
    /** Share of starlight that reaches a shadowed floor by scattering off crater walls. */
    static final double SCATTER = 1e-4;
    /** Internal (geothermal) heat flow of a small rocky body, W/m² (Moon ≈ 0.016). */
    static final double GEOTHERMAL = 0.02;

    /** Axial tilt folded into 0..90°. */
    public static double effectiveObliquity(double axialTiltDeg) {
        double t = Math.abs(axialTiltDeg) % 180;
        return t > 90 ? 180 - t : t;
    }

    /** Highest elevation the star reaches at a latitude over a year, degrees. */
    public static double maxSunElevation(double latitudeDeg, double axialTiltDeg) {
        return Math.min(90, 90 - Math.abs(latitudeDeg) + effectiveObliquity(axialTiltDeg));
    }

    /** Elevation of a crater rim seen from the centre of its floor, degrees. */
    public static double rimAngle(double depthRatio) {
        return Math.toDegrees(Math.atan(2 * depthRatio));
    }

    /** True when the centre of a crater floor at this latitude never sees any part of the star. */
    public static boolean permanentlyShadowed(double latitudeDeg, double axialTiltDeg, double depthRatio, double sunRadiusDeg) {
        return maxSunElevation(latitudeDeg, axialTiltDeg) + sunRadiusDeg < rimAngle(depthRatio);
    }

    /**
     * Polar light for a body.
     *
     * @param axialTiltDeg body's axial tilt (data file {@code rotation.axial_tilt_deg})
     * @param fluxWm2      starlight at the body
     * @param distanceAu   distance from the star (for the star's angular size)
     */
    public static PolarIllumination of(AstroBody b, double axialTiltDeg, double fluxWm2, double distanceAu) {
        double eps = effectiveObliquity(axialTiltDeg);
        double sunR = SUN_RADIUS_DEG_AT_1AU / Math.max(distanceAu, 1e-3);
        double psrLat = Math.min(90, 90 - rimAngle(TYPICAL_DEPTH_RATIO) + eps + sunR);
        double area = psrLat >= 90 ? 0 : (1 - Math.sin(Math.toRadians(psrLat))) * FLOOR_SHARE;
        double floor = Math.pow((fluxWm2 * SCATTER * (1 - b.albedo()) + GEOTHERMAL) / AstroConstants.SIGMA, 0.25);
        double trap = Math.max(25, floor);
        boolean ice = area > 0 && !b.hasAtmosphere() && !b.kind().isGiant() && trap < ICE_STABILITY_K;
        return new PolarIllumination(eps, eps, psrLat, area, trap, ice, eps > 3);
    }

    /** For a Solar System body: flux and distance from its star distance at J2000. */
    public static PolarIllumination of(BodySystem sys, String id, double axialTiltDeg) {
        AstroBody b = sys.require(id);
        double au = 1;
        AstroBody star = sys.root(b);
        for (AstroBody x : sys.ancestry(b)) {
            if (star.id().equals(x.parent()) && x.orbit() != null) {
                au = x.orbit().semiMajorAxis() / AstroConstants.AU;
            }
        }
        return of(b, axialTiltDeg, AstroConstants.SOLAR_CONSTANT / (au * au), au);
    }
}
