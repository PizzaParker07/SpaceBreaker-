package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.util.Mth;

/**
 * One impact crater. The profile follows real crater morphology:
 * simple craters are parabolic bowls with a raised rim; complex craters (radius above the profile's
 * {@code complex_radius}) are relatively shallower with a flat floor, terraced walls and a central peak.
 * Outside the rim the ejecta blanket thins with the inverse cube of distance.
 *
 * @param x      centre x
 * @param z      centre z
 * @param radius rim radius in blocks
 * @param depth  floor depth below the pre-impact surface
 * @param rim    rim height above the pre-impact surface
 * @param peak   central peak height above the floor (0 for simple craters)
 * @param floor  flat floor radius as a fraction of the radius (0 for simple bowls)
 * @param fresh  young crater: bright rays and halo
 * @param seed   per-crater random bits (rim irregularity, ray pattern)
 */
public record Crater(double x, double z, double radius, double depth, double rim, double peak, double floor, boolean fresh, long seed) {
    /** Outer edge of the ejecta blanket in radii. */
    public static final double EJECTA_END = 3.0;

    public boolean complex() {
        return floor > 0;
    }

    /** Distance from the centre in radii, including a gentle rim irregularity so craters are not perfect circles. */
    public double normalizedDistance(double px, double pz) {
        double dx = px - x;
        double dz = pz - z;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1e-6) {
            return 0;
        }
        double a = Mth.atan2(dz, dx);
        double p1 = (seed & 0xFFFF) / 65535.0 * Mth.TWO_PI;
        double p2 = ((seed >>> 16) & 0xFFFF) / 65535.0 * Mth.TWO_PI;
        double wobble = 0.045 * Math.sin(3 * a + p1) + 0.03 * Math.sin(5 * a + p2) + 0.015 * Math.sin(9 * a + p1 * 2);
        return d / (radius * (1 + wobble));
    }

    /** Height change at normalized distance s (radii) relative to the pre-impact surface. */
    public double profile(double s) {
        if (s >= EJECTA_END) {
            return 0;
        }
        if (s >= 1) {
            double fade = 1 - smooth((s - EJECTA_END * 0.75) / (EJECTA_END * 0.25));
            return rim / (s * s * s) * fade;
        }
        double h;
        if (floor <= 0) {
            // simple bowl: parabola from -depth at the centre to +rim at the edge
            h = -depth + (depth + rim) * s * s;
        } else if (s < floor) {
            h = -depth;
        } else {
            // terraced wall: two smooth steps between floor and rim
            double t = (s - floor) / (1 - floor);
            double step = 0.55 * smooth(t * 2) + 0.45 * smooth(t * 2 - 1);
            double curve = t * t * 0.35 + step * 0.65;
            h = -depth + (depth + rim) * curve;
        }
        if (peak > 0) {
            double ps = s / 0.2;
            if (ps < 1) {
                double k = 1 - ps * ps;
                h += peak * k * Math.sqrt(k);
            }
        }
        return h;
    }

    /** How much of the old relief survives here (impact melt and fallback smooth crater floors). */
    public double reliefKept(double s) {
        if (s >= 1.15) {
            return 1;
        }
        if (s >= 1) {
            return 0.75 + 0.25 * (s - 1) / 0.15;
        }
        double s2 = s * s;
        return 0.12 + 0.63 * s2 * s2;
    }

    /**
     * Strength (0..1) of the bright ray system at this point; 0 for old craters. A ragged continuous halo
     * near the rim, then a dozen or so streaks of thin ejecta: each with its own direction, length and width,
     * slightly curved, some splitting into branches, tapering and breaking up into patches toward the tip.
     * The generator turns this into a speckle probability, so rays thin out and fade with distance.
     */
    public double ray(double px, double pz, double s, double rayLength) {
        if (!fresh || s < 0.95 || s > rayLength) {
            return 0;
        }
        double a = Mth.atan2(pz - z, px - x);
        // ragged halo of fresh, bright ejecta
        double halo = 1.45 + 0.25 * valueNoise(a * 2.2, seed) + 0.15 * valueNoise(a * 7.1, seed + 1);
        if (s < halo) {
            return 1;
        }
        double best = 0;
        int rays = 9 + (int) ((seed >>> 8) & 7);
        for (int i = 0; i < rays; i++) {
            long h = seed * 31 + i * 0x9E3779B97F4A7C15L;
            double theta = unit(h, 0) * Mth.TWO_PI;
            double len = halo + (rayLength - halo) * (0.35 + 0.65 * unit(h, 1));
            if (s > len) {
                continue;
            }
            double t = (s - halo) / (len - halo);                      // 0 at the halo, 1 at the tip
            double curve = (unit(h, 2) - 0.5) * 0.25;
            double width = (0.08 + 0.1 * unit(h, 3)) * (1 - 0.7 * t);  // angular half-width, tapering
            double strength = (1 - t) * (0.75 + 0.25 * unit(h, 4));
            best = Math.max(best, streak(a, theta + curve * t, width, strength, s, h));
            // a branch splitting off part way out
            double split = 0.3 + 0.4 * unit(h, 5);
            if (unit(h, 6) < 0.55 && t > split) {
                double side = unit(h, 7) < 0.5 ? -1 : 1;
                double bt = (t - split) / (1 - split);
                best = Math.max(best, streak(a, theta + curve * t + side * 0.18 * bt, width * 0.7, strength * 0.8, s, h + 17));
            }
        }
        return best;
    }

    /** One streak: a soft-edged angular band, broken into patches along its length. */
    private static double streak(double a, double theta, double width, double strength, double s, long h) {
        double d = Math.abs(Mth.wrapDegrees(Math.toDegrees(a - theta))) * Mth.DEG_TO_RAD;
        if (d >= width) {
            return 0;
        }
        double across = Math.sqrt(1 - d / width);
        double patches = 0.5 + 0.5 * valueNoise(s * 2.6, h);
        double gaps = patches < 0.25 ? 0 : Math.min(1, (patches - 0.25) / 0.45);
        return strength * across * gaps;
    }

    private static double unit(long h, int i) {
        long v = h + i * 0x632BE59BD9B4E019L;
        v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
        v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
        v ^= v >>> 31;
        return (v >>> 11) * 0x1.0p-53;
    }

    /** Smooth 1-D value noise in [-1, 1] (wraps every 64 units). */
    private static double valueNoise(double t, long h) {
        int i = Mth.floor(t);
        double f = t - i;
        double u = f * f * (3 - 2 * f);
        double v0 = unit(h, i & 63) * 2 - 1;
        double v1 = unit(h, (i + 1) & 63) * 2 - 1;
        return v0 + (v1 - v0) * u;
    }

    static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
