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

    /** Strength (0..1) of the bright ray system at this point; 0 for old craters. */
    public double ray(double px, double pz, double s, double rayLength) {
        if (!fresh || s < 0.95 || s > rayLength) {
            return 0;
        }
        if (s < 1.7) {
            return 1; // continuous bright ejecta halo
        }
        double a = Mth.atan2(pz - z, px - x);
        double p1 = ((seed >>> 32) & 0xFFFF) / 65535.0 * Mth.TWO_PI;
        double p2 = ((seed >>> 48) & 0xFFFF) / 65535.0 * Mth.TWO_PI;
        double v = Math.sin(11 * a + p1) + 0.75 * Math.sin(17 * a + p2) + 0.55 * Math.sin(29 * a + p1 + p2);
        double need = 1.05 + 0.75 * (s - 1.7) / (rayLength - 1.7);
        return v > need ? Math.min(1, (v - need) * 3) : 0;
    }

    static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
