package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * Eclipses seen from a moon: its planet passing in front of the sun (from the Moon, every total lunar eclipse on
 * Earth is a total solar eclipse by Earth). Pure geometry from the astro ephemeris: the share of the sun's disc
 * covered by the planet's disc, and a search for the next one.
 */
public final class Eclipses {
    /** Sun radius in metres (the root star of the system may differ; used when the star has no radius). */
    private static final double SUN_RADIUS = 6.957e8;

    private Eclipses() {
    }

    /** Share (0..1) of the sun's disc hidden by {@code parent}, seen from {@code observer}, at time t. */
    public static double coverage(BodySystem sys, String observer, String parent, double t) {
        AstroBody o = sys.require(observer);
        AstroBody p = sys.require(parent);
        AstroBody star = sys.root(o);
        Vec3 obs = sys.position(o, t);
        Vec3 toSun = sys.position(star, t).sub(obs);
        Vec3 toPlanet = sys.position(p, t).sub(obs);
        double ds = toSun.length();
        double dp = toPlanet.length();
        if (dp >= ds) {
            return 0; // the planet is beyond the sun
        }
        double sunR = Math.asin(Math.min(1, (star.radius() > 0 ? star.radius() : SUN_RADIUS) / ds));
        double planetR = Math.asin(Math.min(1, p.radius() / dp));
        double cos = toSun.dot(toPlanet) / (ds * dp);
        double sep = Math.acos(Math.max(-1, Math.min(1, cos)));
        return overlap(sunR, planetR, sep) / (Math.PI * sunR * sunR);
    }

    /** Area of overlap of two discs with radii r1, r2 whose centres are d apart (small-angle, flat). */
    static double overlap(double r1, double r2, double d) {
        if (d >= r1 + r2) {
            return 0;
        }
        if (d <= Math.abs(r1 - r2)) {
            double r = Math.min(r1, r2);
            return Math.PI * r * r;
        }
        double a = r1 * r1 * Math.acos((d * d + r1 * r1 - r2 * r2) / (2 * d * r1));
        double b = r2 * r2 * Math.acos((d * d + r2 * r2 - r1 * r1) / (2 * d * r2));
        double c = 0.5 * Math.sqrt((-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2));
        return a + b - c;
    }

    /**
     * The start of the next eclipse after t (simulation seconds), searching up to {@code horizon} seconds ahead in
     * {@code step} steps and refining to about a minute; NaN if none.
     */
    public static double next(BodySystem sys, String observer, String parent, double t, double horizon, double step) {
        boolean inside = coverage(sys, observer, parent, t) > 0;
        double prev = t;
        for (double x = t + step; x <= t + horizon; x += step) {
            boolean now = coverage(sys, observer, parent, x) > 0;
            if (now && !inside) {
                double lo = prev;
                double hi = x;
                while (hi - lo > 60) {
                    double mid = (lo + hi) / 2;
                    if (coverage(sys, observer, parent, mid) > 0) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                return hi;
            }
            inside = now;
            prev = x;
        }
        return Double.NaN;
    }
}
