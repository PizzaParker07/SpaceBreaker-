package com.pizzaparker07.spacebreaker.core.astro;

import java.util.Optional;

/**
 * Lambert's problem: the conic that joins two positions in a given flight time (single revolution).
 * Universal-variable formulation with Stumpff functions, solved by bisection on z for robustness.
 */
public final class Lambert {
    /** Velocities at the two ends of the transfer. */
    public record Solution(Vec3 v1, Vec3 v2) {
    }

    private Lambert() {
    }

    /**
     * @param prograde true for the transfer that moves counter-clockwise seen from +z (like the planets)
     */
    public static Optional<Solution> solve(Vec3 r1v, Vec3 r2v, double dt, double mu, boolean prograde) {
        double r1 = r1v.length(), r2 = r2v.length();
        double cosT = Math.max(-1, Math.min(1, r1v.dot(r2v) / (r1 * r2)));
        double dTheta = Math.acos(cosT);
        boolean upper = r1v.cross(r2v).z() < 0;
        if (prograde == upper) {
            dTheta = Kepler.TWO_PI - dTheta;
        }
        if (Math.abs(Math.sin(dTheta)) < 1e-6 || dt <= 0) {
            return Optional.empty(); // the transfer plane is undefined at 0° and 180°
        }
        double a = Math.sin(dTheta) * Math.sqrt(r1 * r2 / (1 - Math.cos(dTheta)));
        double target = Math.sqrt(mu) * dt;
        double lo = -4 * Math.PI * Math.PI * 25, hi = 4 * Math.PI * Math.PI * 0.999999;
        // y(z) must be positive: move the lower bound to where y crosses zero
        if (y(lo, r1, r2, a) < 0) {
            if (y(hi, r1, r2, a) < 0) return Optional.empty();
            double bad = lo, good = hi;
            for (int i = 0; i < 200; i++) {
                double mid = 0.5 * (bad + good);
                if (y(mid, r1, r2, a) < 0) bad = mid;
                else good = mid;
            }
            lo = good;
        }
        if (y(lo, r1, r2, a) < 0 || f(lo, r1, r2, a) > target || f(hi, r1, r2, a) < target) {
            return Optional.empty();
        }
        for (int i = 0; i < 200; i++) {
            double mid = 0.5 * (lo + hi);
            if (y(mid, r1, r2, a) < 0 || f(mid, r1, r2, a) < target) lo = mid;
            else hi = mid;
        }
        double z = 0.5 * (lo + hi), y = y(z, r1, r2, a);
        double fl = 1 - y / r1, g = a * Math.sqrt(y / mu), gd = 1 - y / r2;
        Vec3 v1 = r2v.sub(r1v.scale(fl)).scale(1 / g);
        Vec3 v2 = r2v.scale(gd).sub(r1v).scale(1 / g);
        return Optional.of(new Solution(v1, v2));
    }

    private static double f(double z, double r1, double r2, double a) {
        double y = y(z, r1, r2, a);
        return Math.pow(y / c(z), 1.5) * s(z) + a * Math.sqrt(y);
    }

    private static double y(double z, double r1, double r2, double a) {
        return r1 + r2 + a * (z * s(z) - 1) / Math.sqrt(c(z));
    }

    static double c(double z) {
        if (z > 1e-8) return (1 - Math.cos(Math.sqrt(z))) / z;
        if (z < -1e-8) return (Math.cosh(Math.sqrt(-z)) - 1) / -z;
        return 0.5 - z / 24;
    }

    static double s(double z) {
        if (z > 1e-8) {
            double q = Math.sqrt(z);
            return (q - Math.sin(q)) / (q * q * q);
        }
        if (z < -1e-8) {
            double q = Math.sqrt(-z);
            return (Math.sinh(q) - q) / (q * q * q);
        }
        return 1.0 / 6 - z / 120;
    }
}
