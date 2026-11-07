package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Shadows in orbit: when a satellite (or an orbit station's solar array) is in a planet's shadow, how much of
 * the star is hidden (umbra and penumbra), and what share of an orbit is spent in darkness.
 * Pure Java; positions in metres.
 */
public final class Eclipses {

    /**
     * Sunlight at a station at time t.
     *
     * @param sunlit          visible fraction of the star's disc, 0 (umbra) .. 1 (full sun)
     * @param irradiance      starlight on a panel facing the star, W/m² (already multiplied by {@code sunlit})
     * @param orbitPeriod     period of the station's orbit, s
     * @param eclipseFraction share of each orbit spent in shadow (orbit average)
     * @param phase           position along the orbit, rad
     */
    public record Sunlight(double sunlit, double irradiance, double orbitPeriod, double eclipseFraction, double phase) {
        public boolean inShadow() {
            return sunlit < 0.5;
        }
    }

    /** Cylindrical shadow test: is a point (relative to the planet) behind it as seen from the star? */
    public static boolean inCylindricalShadow(Vec3 pos, Vec3 toStarUnit, double planetRadius) {
        double along = pos.dot(toStarUnit);
        if (along >= 0) {
            return false;
        }
        Vec3 perp = pos.sub(toStarUnit.scale(along));
        return perp.length() < planetRadius;
    }

    /**
     * Visible fraction of the star's disc from a point near a planet, with umbra and penumbra (overlap of the two
     * discs as seen from the point).
     *
     * @param pos        point relative to the planet's centre, m
     * @param starPos    star relative to the planet's centre, m
     * @param planetRadius m
     * @param starRadius m
     */
    public static double sunlitFraction(Vec3 pos, Vec3 starPos, double planetRadius, double starRadius) {
        Vec3 toStar = starPos.sub(pos);
        Vec3 toPlanet = pos.scale(-1);
        double ds = toStar.length(), dp = toPlanet.length();
        if (dp <= planetRadius) {
            return 0; // inside the planet
        }
        double rs = Math.asin(Math.min(1, starRadius / ds));     // angular radius of the star
        double rp = Math.asin(Math.min(1, planetRadius / dp));   // angular radius of the planet
        double sep = Math.acos(Math.max(-1, Math.min(1, toStar.dot(toPlanet) / (ds * dp))));
        if (sep >= rs + rp) {
            return 1;
        }
        if (sep <= rp - rs) {
            return 0; // the planet covers the whole star: umbra
        }
        double overlap = discOverlap(rs, rp, sep);
        return Math.max(0, 1 - overlap / (Math.PI * rs * rs));
    }

    /** Area of overlap of two circles of radii a, b with centre distance d (small-angle, flat). */
    static double discOverlap(double a, double b, double d) {
        if (d >= a + b) {
            return 0;
        }
        if (d <= Math.abs(a - b)) {
            double r = Math.min(a, b);
            return Math.PI * r * r;
        }
        double alpha = Math.acos((d * d + a * a - b * b) / (2 * d * a));
        double beta = Math.acos((d * d + b * b - a * a) / (2 * d * b));
        return a * a * (alpha - Math.sin(2 * alpha) / 2) + b * b * (beta - Math.sin(2 * beta) / 2);
    }

    /**
     * Share of a circular orbit spent in the planet's shadow (cylindrical shadow).
     *
     * @param planetRadius m
     * @param orbitRadius  m
     * @param beta         angle between the orbit plane and the direction to the star, rad
     */
    public static double eclipseFraction(double planetRadius, double orbitRadius, double beta) {
        double betaStar = Math.asin(Math.min(1, planetRadius / orbitRadius));
        if (Math.abs(beta) >= betaStar) {
            return 0;
        }
        double h = orbitRadius - planetRadius;
        double x = Math.sqrt(h * h + 2 * planetRadius * h) / (orbitRadius * Math.cos(beta));
        return Math.acos(Math.min(1, x)) / Math.PI;
    }

    /**
     * Sunlight for a station on a circular orbit around a body at simulated time t. The orbit lies in the system's
     * reference plane (β ≈ 0, the worst case with the longest eclipses) and starts on the sunlit side at J2000.
     *
     * @param altitude height above the body's surface, m
     */
    public static Sunlight station(BodySystem sys, String bodyId, double altitude, double t) {
        return station(sys, bodyId, altitude, 0, t);
    }

    /** As above with an orbit inclination (rad) to the system plane, which reduces eclipses when the star is off-plane. */
    public static Sunlight station(BodySystem sys, String bodyId, double altitude, double inclination, double t) {
        AstroBody b = sys.require(bodyId);
        AstroBody star = sys.root(b);
        double r = b.radius() + altitude;
        double period = Kepler.period(r, b.mu());
        Vec3 starRel = sys.position(star, t).sub(sys.position(b, t));
        double phase = Kepler.wrap(2 * Math.PI * t / period);
        // orbit frame: start at the sub-stellar point, rotate prograde in the (possibly inclined) plane
        Vec3 u = new Vec3(starRel.x(), starRel.y(), 0).normalize();
        if (u.length() == 0) {
            u = new Vec3(1, 0, 0);
        }
        Vec3 vPlane = new Vec3(-u.y(), u.x(), 0);
        Vec3 v = vPlane.scale(Math.cos(inclination)).add(new Vec3(0, 0, Math.sin(inclination)));
        Vec3 pos = u.scale(r * Math.cos(phase)).add(v.scale(r * Math.sin(phase)));
        double lit = star.isStar() ? sunlitFraction(pos, starRel, b.radius(), star.radius()) : 1;
        // β: angle between the orbit plane (normal n = u × v) and the star direction
        Vec3 n = u.cross(v).normalize();
        double beta = Math.asin(Math.max(-1, Math.min(1, n.dot(starRel.normalize()))));
        double lum = SkyEphemeris.luminositySolar(star);
        double flux = AstroConstants.SOLAR_CONSTANT * lum * Math.pow(AstroConstants.AU / starRel.length(), 2);
        return new Sunlight(lit, flux * lit, period, eclipseFraction(b.radius(), r, beta), phase);
    }

    private Eclipses() {
    }
}
