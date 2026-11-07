package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the sky looks like from a body: every other body of its system with its direction, brightness,
 * size, phase and colour. Pure Java; time is simulated seconds since J2000 ({@code BodyAdapter.simTime(level)}
 * gives it for a level, consistent with the server's {@code orbitalTimeScale}).
 * <p>
 * Directions are unit vectors in the system frame (ecliptic J2000 for the Solar System) and are seen from
 * the observer body's centre; {@link #toHorizontal} turns one into azimuth/altitude for a place on the surface.
 * Brightness: m = V(1, α) + 5 log₁₀(r·Δ / AU²); V(1, α) is the measured phase curve for Mercury, Venus, Earth,
 * Mars and Jupiter (Mallama &amp; Hilton 2018), otherwise H (from size and geometric albedo) plus a Lambert phase law.
 * Stars: m = M + 5 log₁₀(d / 10 pc). Saturn's rings are not included in its brightness.
 */
public final class SkyEphemeris {
    /** H = 5 log₁₀(1329 km / (D √p)): the standard magnitude–size relation. */
    private static final double H_CONSTANT_KM = 1329.0;

    /**
     * One object in the sky.
     *
     * @param id                 body id
     * @param direction          unit vector from the observer to the object (system frame)
     * @param distance           m
     * @param apparentMagnitude  smaller is brighter (Sun from Earth −26.7, Venus ≈ −4, naked-eye limit ≈ 6)
     * @param angularDiameter    rad
     * @param phaseAngle         Sun–object–observer angle, rad (0 = full)
     * @param illuminated        lit fraction of the disc, 0..1
     * @param elongation         angle from the star as seen by the observer, rad
     * @param colorRgb           colour of the disc or point, 0xRRGGBB
     * @param star               true for a star
     */
    public record SkyObject(String id, Vec3 direction, double distance, double apparentMagnitude, double angularDiameter,
                            double phaseAngle, double illuminated, double elongation, int colorRgb, boolean star) {
        /** Angular diameter in degrees. */
        public double angularDiameterDeg() {
            return Math.toDegrees(angularDiameter);
        }
    }

    /** Azimuth (rad, 0 = north, clockwise towards east) and altitude above the horizon (rad). */
    public record Horizontal(double azimuth, double altitude) {
        public boolean aboveHorizon() {
            return altitude > 0;
        }
    }

    /** Everything brighter than {@code faintest} seen from {@code observerId} at time t, brightest first. */
    public static List<SkyObject> visible(BodySystem sys, String observerId, double t, double faintest) {
        AstroBody obs = sys.require(observerId);
        AstroBody star = sys.root(obs);
        Vec3 po = sys.position(obs, t);
        Vec3 ps = sys.position(star, t);
        List<SkyObject> out = new ArrayList<>();
        for (AstroBody b : sys.all()) {
            if (b == obs) {
                continue;
            }
            SkyObject o = object(sys, obs, star, po, ps, b, t);
            if (o.apparentMagnitude() <= faintest) {
                out.add(o);
            }
        }
        out.sort(Comparator.comparingDouble(SkyObject::apparentMagnitude));
        return out;
    }

    /** One body as seen from the observer at time t. */
    public static SkyObject view(BodySystem sys, String observerId, String targetId, double t) {
        AstroBody obs = sys.require(observerId);
        AstroBody star = sys.root(obs);
        return object(sys, obs, star, sys.position(obs, t), sys.position(star, t), sys.require(targetId), t);
    }

    private static SkyObject object(BodySystem sys, AstroBody obs, AstroBody star, Vec3 po, Vec3 ps, AstroBody b, double t) {
        Vec3 pb = sys.position(b, t);
        Vec3 toB = pb.sub(po);
        double delta = toB.length();
        Vec3 dir = toB.normalize();
        double size = 2 * Math.asin(Math.min(1, b.radius() / delta));
        Vec3 toStar = ps.sub(po);
        double elong = toStar.length() > 0 ? angle(dir, toStar.normalize()) : 0;
        if (b.isStar()) {
            double l = luminositySolar(b);
            double m = 4.83 - 2.5 * Math.log10(Math.max(l, 1e-12)) + 5 * Math.log10(delta / AstroConstants.PARSEC / 10);
            int col = StellarModel.blackbodyColor(b.dayTempK());
            return new SkyObject(b.id(), dir, delta, m, size, 0, 1, elong, col, true);
        }
        Vec3 bToStar = ps.sub(pb);
        double r = bToStar.length();
        double alpha = angle(bToStar.normalize(), po.sub(pb).normalize());
        double mag = reducedMagnitude(b, alpha) + 5 * Math.log10(r * delta / (AstroConstants.AU * AstroConstants.AU));
        // scale for stars other than the Sun: brightness ∝ stellar luminosity
        double lStar = luminositySolar(star);
        mag -= 2.5 * Math.log10(lStar);
        double lit = (1 + Math.cos(alpha)) / 2;
        return new SkyObject(b.id(), dir, delta, mag, size, alpha, lit, elong, color(b), false);
    }

    /**
     * Magnitude at 1 AU from the star and the observer at phase angle α ("reduced magnitude").
     * Mercury, Venus, Earth, Mars and Jupiter use their measured phase curves (Mallama &amp; Hilton 2018), which keep
     * Venus bright as a thin crescent; every other body uses H plus a Lambert-sphere phase law.
     */
    public static double reducedMagnitude(AstroBody b, double alpha) {
        double a = Math.toDegrees(alpha);
        switch (b.id()) {
            case "spacebreaker:mercury" -> {
                a = Math.min(a, 170);
                return -0.613 + 6.3280e-2 * a - 1.6336e-3 * a * a + 3.3644e-5 * a * a * a - 3.4265e-7 * Math.pow(a, 4)
                        + 1.6893e-9 * Math.pow(a, 5) - 3.0334e-12 * Math.pow(a, 6);
            }
            case "spacebreaker:venus" -> {
                if (a <= 163.7) {
                    return -4.384 - 1.044e-3 * a + 3.687e-4 * a * a - 2.814e-6 * a * a * a + 8.938e-9 * Math.pow(a, 4);
                }
                a = Math.min(a, 179);
                return 236.05828 - 2.81914 * a + 8.39034e-3 * a * a;
            }
            case "spacebreaker:earth" -> {
                return -3.99 - 1.060e-3 * a + 2.054e-4 * a * a;
            }
            case "spacebreaker:mars" -> {
                return a <= 50 ? -1.601 + 0.02267 * a - 0.0001302 * a * a : -0.367 - 0.02573 * a + 0.0003445 * a * a;
            }
            case "spacebreaker:jupiter" -> {
                return a <= 12 ? -9.395 - 3.7e-4 * a + 6.16e-4 * a * a
                        : -9.428 - 2.5 * Math.log10(1 - 1.507 * (a / 180) - 0.363 * Math.pow(a / 180, 2)
                        - 0.062 * Math.pow(a / 180, 3) + 2.809 * Math.pow(a / 180, 4) - 1.876 * Math.pow(a / 180, 5));
            }
            default -> {
                return absoluteMagnitude(b) - 2.5 * Math.log10(Math.max(lambert(alpha), 1e-9));
            }
        }
    }

    /** Absolute magnitude H of a body from its diameter and geometric albedo. */
    public static double absoluteMagnitude(AstroBody b) {
        double dKm = 2 * b.radius() / 1000;
        return 5 * Math.log10(H_CONSTANT_KM / (dKm * Math.sqrt(Math.max(b.visualAlbedo(), 1e-4))));
    }

    /** Lambert-sphere phase function Φ(α), 1 at full phase. */
    public static double lambert(double alpha) {
        return (Math.sin(alpha) + (Math.PI - alpha) * Math.cos(alpha)) / Math.PI;
    }

    /** Luminosity of a star body in L☉ from its radius and temperature. */
    public static double luminositySolar(AstroBody star) {
        double rr = star.radius() / AstroConstants.SOLAR_RADIUS;
        return rr * rr * Math.pow(star.dayTempK() / AstroConstants.SOLAR_TEMPERATURE, 4);
    }

    /** Colour of a body seen from space. */
    public static int color(AstroBody b) {
        if (b.kind() == AstroBody.Kind.ICE_GIANT) {
            return 0x8CC8E0;
        }
        if (b.surfacePressureKpa() > 1000 && !b.kind().isGiant()) {
            return 0xEDE3C3; // a cloud-covered world (Venus)
        }
        if (b.surfacePressureKpa() > 10 && b.meanTempK() < 150 && b.composition().containsKey("CH4") && !b.kind().isGiant()) {
            return 0xD9A35A; // orange organic haze (Titan)
        }
        TerrainHints h = TerrainHints.of(b, null, 1361, 4.6e9, b.id().hashCode());
        if (h.colorFamily() == TerrainHints.ColorFamily.OCEAN) {
            return 0x8FB2E0; // blue marble
        }
        return lighten(h.colorFamily().base, 0.15);
    }

    /**
     * Converts a sky direction to azimuth/altitude for an observer on the body's surface.
     *
     * @param axialTilt     obliquity of the observer body, rad (tilt of its equator to the system frame, about x)
     * @param rotationAngle how far the body has turned since its prime meridian faced +x, rad (local sidereal angle)
     * @param latitude      observer latitude, rad
     */
    public static Horizontal toHorizontal(Vec3 direction, double axialTilt, double rotationAngle, double latitude) {
        // system frame -> body equatorial frame (rotate about x by the tilt)
        double ce = Math.cos(axialTilt), se = Math.sin(axialTilt);
        double x = direction.x();
        double y = direction.y() * ce + direction.z() * se;
        double z = -direction.y() * se + direction.z() * ce;
        double ra = Math.atan2(y, x);
        double dec = Math.asin(Math.max(-1, Math.min(1, z)));
        double hourAngle = rotationAngle - ra;
        double sinAlt = Math.sin(dec) * Math.sin(latitude) + Math.cos(dec) * Math.cos(latitude) * Math.cos(hourAngle);
        double alt = Math.asin(Math.max(-1, Math.min(1, sinAlt)));
        double az = Math.atan2(-Math.sin(hourAngle) * Math.cos(dec),
                Math.sin(dec) * Math.cos(latitude) - Math.cos(dec) * Math.sin(latitude) * Math.cos(hourAngle));
        return new Horizontal(Kepler.wrap(az), alt);
    }

    static double angle(Vec3 a, Vec3 b) {
        return Math.acos(Math.max(-1, Math.min(1, a.dot(b))));
    }

    private static int lighten(int rgb, double f) {
        int r = (int) Math.min(255, (rgb >> 16 & 255) + (255 - (rgb >> 16 & 255)) * f);
        int g = (int) Math.min(255, (rgb >> 8 & 255) + (255 - (rgb >> 8 & 255)) * f);
        int b = (int) Math.min(255, (rgb & 255) + (255 - (rgb & 255)) * f);
        return r << 16 | g << 8 | b;
    }

    private SkyEphemeris() {
    }
}
