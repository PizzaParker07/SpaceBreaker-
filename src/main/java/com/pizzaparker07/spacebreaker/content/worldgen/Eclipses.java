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

    /** One eclipse: geometric start and end (simulation seconds) and the largest covered share. */
    public record Event(double start, double end, double peak) {
        public double middle() {
            return (start + end) / 2;
        }
    }

    /** Shortest time an eclipse is shown to players, in real seconds, and its fade in / out. */
    public static final double MIN_SHOWN_SECONDS = 60;
    public static final double FADE_SECONDS = 10;

    /**
     * The eclipse that is happening at t or comes next (ending after t), searching {@code horizon} seconds ahead, or
     * null. {@code before} widens the search backwards so an eclipse whose presentation window already began is kept.
     */
    public static Event event(BodySystem sys, String observer, String parent, double t, double before, double horizon, double step) {
        double from = t - before;
        double start = coverage(sys, observer, parent, from) > 0 ? from : next(sys, observer, parent, from, horizon + before, step);
        if (Double.isNaN(start)) {
            return null;
        }
        double end = start;
        double peak = 0;
        for (double x = start; x <= start + 2 * 86400; x += 120) {
            double c = coverage(sys, observer, parent, x);
            if (c <= 0) {
                break;
            }
            peak = Math.max(peak, c);
            end = x;
        }
        return new Event(start, end + 60, peak);
    }

    /**
     * How strongly an eclipse shows at simulation time t (0..1 times its peak): real eclipses can pass in seconds at the
     * default time scale, so the effect is stretched to at least {@link #MIN_SHOWN_SECONDS} real seconds around the
     * eclipse's middle, fading in and out over {@link #FADE_SECONDS}; longer eclipses keep their real length.
     *
     * @param simPerReal simulated seconds per real second
     */
    public static double presentation(Event e, double t, double simPerReal) {
        if (e == null || e.peak() <= 0) {
            return 0;
        }
        double realLength = (e.end() - e.start()) / simPerReal;
        double shown = Math.max(MIN_SHOWN_SECONDS, realLength);
        double half = shown / 2 * simPerReal;
        double from = e.middle() - half;
        double to = e.middle() + half;
        if (t <= from || t >= to) {
            return 0;
        }
        double fade = FADE_SECONDS * simPerReal;
        double k = Math.min(1, Math.min(t - from, to - t) / fade);
        return e.peak() * k * k * (3 - 2 * k);
    }

    /** Start of an eclipse's presentation window (simulation seconds). */
    public static double shownFrom(Event e, double simPerReal) {
        double shown = Math.max(MIN_SHOWN_SECONDS, (e.end() - e.start()) / simPerReal);
        return e.middle() - shown / 2 * simPerReal;
    }

    /** End of an eclipse's presentation window (simulation seconds). */
    public static double shownTo(Event e, double simPerReal) {
        double shown = Math.max(MIN_SHOWN_SECONDS, (e.end() - e.start()) / simPerReal);
        return e.middle() + shown / 2 * simPerReal;
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
