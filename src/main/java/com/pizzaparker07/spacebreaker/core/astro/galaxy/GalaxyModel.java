package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.Vec3;

/**
 * Smooth stellar number density of a barred spiral galaxy shaped like the Milky Way, in stars per pc³.
 * Galactocentric frame in parsecs: the centre at the origin, the disk in the x–y plane, the Sun on the −x axis
 * slightly above the plane, galactic rotation towards +y at the Sun.
 * Components: thin and thick exponential disks with four logarithmic spiral arms, a triaxial bar/bulge
 * rotated 27° from the Sun–centre line, and a sparse power-law halo. Normalised to 0.1 stars/pc³ at the Sun.
 */
public final class GalaxyModel {
    /** Sun's position (pc): 8.178 kpc from the centre, 20.8 pc above the plane. */
    public static final Vec3 SUN = new Vec3(-8178, 0, 20.8);
    public static final double SOLAR_NEIGHBOURHOOD_DENSITY = 0.1;
    public static final double DISK_RADIUS = 16000;

    private static final double THIN_SCALE_LENGTH = 2600, THIN_SCALE_HEIGHT = 300;
    private static final double THICK_SCALE_LENGTH = 3600, THICK_SCALE_HEIGHT = 900, THICK_FRACTION = 0.12;
    private static final double BAR_ANGLE = Math.toRadians(27);
    private static final double BAR_X = 1700, BAR_Y = 640, BAR_Z = 440, BAR_DENSITY = 25;
    private static final int ARMS = 4;
    private static final double PITCH = Math.toRadians(12.5), ARM_WIDTH = 450, ARM_CONTRAST = 0.8, ARM_START = 3500;
    private static final double HALO_DENSITY = 1.5e-4;

    private static final double NORMALISATION = SOLAR_NEIGHBOURHOOD_DENSITY / raw(SUN.x(), SUN.y(), SUN.z());

    public static double density(Vec3 p) {
        return density(p.x(), p.y(), p.z());
    }

    public static double density(double x, double y, double z) {
        return NORMALISATION * raw(x, y, z);
    }

    private static double raw(double x, double y, double z) {
        double r = Math.hypot(x, y);
        double az = Math.abs(z);
        double truncation = r > DISK_RADIUS ? Math.exp(-(r - DISK_RADIUS) / 500) : 1;
        double thin = Math.exp(-r / THIN_SCALE_LENGTH - az / THIN_SCALE_HEIGHT);
        double thick = THICK_FRACTION * Math.exp(-r / THICK_SCALE_LENGTH - az / THICK_SCALE_HEIGHT) * Math.exp(8178 / THICK_SCALE_LENGTH - 8178 / THIN_SCALE_LENGTH);
        double disk = (thin * (1 + ARM_CONTRAST * armFactor(x, y, r)) + thick) * truncation;
        // the disk has a central hole where the bar dominates
        disk *= 1 - Math.exp(-Math.pow(r / 2500, 2));
        double s = BAR_ANGLE;
        double bx = x * Math.cos(s) + y * Math.sin(s);
        double by = -x * Math.sin(s) + y * Math.cos(s);
        double rs = Math.sqrt(sq(bx / BAR_X) + sq(by / BAR_Y) + sq(z / BAR_Z));
        double bar = BAR_DENSITY * Math.exp(-rs) * Math.exp(-8178 / THIN_SCALE_LENGTH);
        double rr = Math.sqrt(r * r + z * z);
        double halo = HALO_DENSITY * Math.pow(8178 / Math.max(rr, 500), 3.5) * Math.exp(-8178 / THIN_SCALE_LENGTH);
        return disk + bar + halo;
    }

    /** 0..1: closeness to the nearest spiral arm (1 on the arm's ridge). */
    public static double armFactor(double x, double y, double r) {
        if (r < ARM_START * 0.6) {
            return 0;
        }
        double theta = Math.atan2(y, x);
        double best = 0;
        for (int k = 0; k < ARMS; k++) {
            double phase = BAR_ANGLE + k * Math.PI / 2;
            // log spiral: theta = phase + ln(r / start) / tan(pitch); compare azimuths modulo 2π
            double expected = phase + Math.log(r / ARM_START) / Math.tan(PITCH);
            double d = wrap(theta - expected);
            double dist = Math.abs(d) * r * Math.sin(PITCH);
            best = Math.max(best, Math.exp(-sq(dist / ARM_WIDTH)));
        }
        return best;
    }

    /** Expected number of stars in an axis-aligned cube (Simpson-like 3-point sampling per axis). */
    public static double expectedStars(Vec3 min, double size) {
        double sum = 0;
        double[] w = {1, 4, 1};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    sum += w[i] * w[j] * w[k] * density(min.x() + size * i / 2, min.y() + size * j / 2, min.z() + size * k / 2);
                }
            }
        }
        return sum / 216 * size * size * size;
    }

    private static double wrap(double a) {
        a = a % (2 * Math.PI);
        if (a > Math.PI) {
            a -= 2 * Math.PI;
        }
        if (a < -Math.PI) {
            a += 2 * Math.PI;
        }
        return a;
    }

    private static double sq(double v) {
        return v * v;
    }

    private GalaxyModel() {
    }
}
