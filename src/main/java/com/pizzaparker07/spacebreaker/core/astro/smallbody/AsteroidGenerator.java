package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.Rng;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic small-body populations: the same world seed, region and index always give the same asteroid.
 * <ul>
 *   <li>sizes: cumulative power law N(&gt; D) ∝ D^−2.5 (collisional equilibrium) between the region's limits;</li>
 *   <li>types by distance: S-rich inner belt, C-dominated outer belt, M and V minorities, D beyond Jupiter;</li>
 *   <li>orbits: no main-belt asteroid inside a Kirkwood gap (3:1, 5:2, 7:3, 2:1 resonances with Jupiter);
 *       Trojans librate around Jupiter's L4/L5 points, 60° ahead of or behind it;</li>
 *   <li>spin: bodies larger than ≈ 200 m are rubble piles that cannot spin faster than ≈ 2.2 h;
 *       small monoliths may spin in minutes;</li>
 *   <li>shape: triaxial ellipsoids, smaller bodies more elongated.</li>
 * </ul>
 */
public final class AsteroidGenerator {
    public static final double SIZE_EXPONENT = 2.5;
    public static final double SPIN_BARRIER_HOURS = 2.2;
    public static final double RUBBLE_PILE_KM = 0.2;
    /** Jupiter's J2000 mean longitude and period (for the Trojan clouds). */
    public static final double JUPITER_MEAN_LONGITUDE_DEG = 34.39644;
    public static final double JUPITER_A_AU = 5.20288700;

    /** Kirkwood gaps: centre (AU) and half-width (AU). */
    static final double[][] KIRKWOOD = {{2.502, 0.03}, {2.825, 0.02}, {2.958, 0.015}, {3.279, 0.03}};

    public static boolean inKirkwoodGap(double aAu) {
        for (double[] g : KIRKWOOD) {
            if (Math.abs(aAu - g[0]) < g[1]) {
                return true;
            }
        }
        return false;
    }

    /** Diameter (km) drawn from the cumulative power law between dMin and dMax. */
    public static double sampleDiameter(Rng rng, double dMin, double dMax) {
        double q = SIZE_EXPONENT;
        double lo = Math.pow(dMax, -q), hi = Math.pow(dMin, -q);
        return Math.pow(lo + (hi - lo) * rng.nextDouble(), -1 / q);
    }

    /** Type fractions {C, S, M, V, D} at a distance (after DeMeo &amp; Carry 2014, simplified). */
    public static double[] typeMix(double aAu) {
        if (aAu > 4.5) {
            return new double[]{0.15, 0, 0, 0, 0.85};
        }
        double f = Math.max(0, Math.min(1, (aAu - 2.1) / 1.2)); // 0 inner .. 1 outer belt
        double c = 0.25 + 0.5 * f;
        double s = 0.55 - 0.45 * f;
        double m = 0.07;
        double v = 0.08 * (1 - f);
        double d = 1 - c - s - m - v;
        return new double[]{c, s, m, v, Math.max(0, d)};
    }

    public static Asteroid generate(long worldSeed, Region region, int index) {
        long id = Rng.hash(worldSeed, region.key().hashCode(), index);
        Rng rng = new Rng(id);
        // orbit
        double a;
        double lan = rng.range(0, 2 * Math.PI), argp = rng.range(0, 2 * Math.PI), m0;
        double e = Math.min(region.eMax(), rng.rayleigh(region.eSigma()));
        double inc = Math.toRadians(Math.min(40, rng.rayleigh(region.iSigmaDeg())));
        if (region.trojans()) {
            a = JUPITER_A_AU * (1 + rng.gaussian() * 0.01);
            double side = rng.chance(0.5) ? 60 : -60;
            double libration = rng.gaussian() * 12;
            double meanLongitude = Math.toRadians(JUPITER_MEAN_LONGITUDE_DEG + side + libration);
            m0 = Kepler.wrap(meanLongitude - lan - argp);
        } else {
            do {
                a = rng.range(region.aMinAu(), region.aMaxAu());
            } while (inKirkwoodGap(a));
            m0 = rng.range(0, 2 * Math.PI);
        }
        double aM = a * AstroConstants.AU;
        OrbitalElements orbit = new OrbitalElements(AstroConstants.MU_SUN, aM, e, inc, lan, argp, m0);

        // size, type, structure
        double d = sampleDiameter(rng, region.minDiameterKm(), region.maxDiameterKm());
        SpectralType type = pickType(rng, region.trojans() || a > 30 ? 5.2 : a);
        boolean rubble = d > RUBBLE_PILE_KM ? rng.chance(0.9) : rng.chance(0.2);
        if (type == SpectralType.M && rng.chance(0.6)) {
            rubble = false; // many metal bodies are solid cores
        }
        double porosity = rubble ? rng.range(0.3, 0.5) : rng.range(0.03, 0.15);
        double density = type.grainDensity * (1 - porosity);
        double r = d * 500;
        double mass = density * 4.0 / 3.0 * Math.PI * r * r * r;
        double albedo = rng.range(type.albedoMin, type.albedoMax);
        double elong = d < 1 ? 0.55 : 0.75;
        double ba = rng.range(elong, 0.97);
        double cb = rng.range(elong, 0.97);
        Asteroid.Shape shape = new Asteroid.Shape(ba, ba * cb);
        double rot;
        if (rubble) {
            rot = Math.max(SPIN_BARRIER_HOURS + rng.range(0, 0.3), rng.logRange(3, 30));
        } else {
            rot = d < 0.15 ? rng.logRange(0.05, 5) : rng.logRange(2, 20);
        }
        double veins = clamp(type.metalVeins + rng.gaussian() * 0.08);
        double pgm = type.pgmPpm * rng.logRange(0.5, 2);
        String name = StarNames.properName(id);
        String designation = "SBA " + Long.toString(id >>> 28 & 0xFFFFFFFL, 36).toUpperCase(Locale.ROOT);
        String bodyId = "spacebreaker:asteroid/" + region.key() + "/" + Long.toHexString(id);
        return new Asteroid(bodyId, name, name, designation, region, type, d, shape, rubble, porosity, density, mass,
                albedo, rot, orbit, Map.copyOf(type.composition), veins, pgm);
    }

    /** The first {@code count} asteroids of a region. */
    public static List<Asteroid> population(long worldSeed, Region region, int count) {
        List<Asteroid> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(generate(worldSeed, region, i));
        }
        return out;
    }

    private static SpectralType pickType(Rng rng, double aAu) {
        double[] mix = typeMix(aAu);
        double u = rng.nextDouble();
        SpectralType[] types = {SpectralType.C, SpectralType.S, SpectralType.M, SpectralType.V, SpectralType.D};
        for (int i = 0; i < mix.length; i++) {
            u -= mix[i];
            if (u <= 0) {
                return types[i];
            }
        }
        return SpectralType.C;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private AsteroidGenerator() {
    }
}
