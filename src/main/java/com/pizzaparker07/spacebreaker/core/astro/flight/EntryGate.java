package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.StateVector;

/**
 * Where a craft is relative to a body's layers, for switching scenes: free space, the orbit shell (low orbit),
 * the entry interface (top of the air, or a low pass over an airless world) and the surface.
 */
public final class EntryGate {
    public enum Zone {
        SPACE, ORBIT_SHELL, ENTRY, SURFACE
    }

    private EntryGate() {
    }

    /** Altitude below which the orbit shell begins, m (twice the low-orbit altitude). */
    public static double shellAltitude(AstroBody b) {
        return 2 * (b.lowOrbitRadius() - b.radius());
    }

    /** Altitude of the entry interface, m: the top of the atmosphere, or half the low-orbit altitude if airless. */
    public static double entryAltitude(AstroBody b) {
        return b.hasAtmosphere() ? b.atmosphereTop() : 0.5 * (b.lowOrbitRadius() - b.radius());
    }

    public static Zone zone(AstroBody b, double altitude) {
        if (altitude <= 0) return Zone.SURFACE;
        if (altitude <= entryAltitude(b)) return Zone.ENTRY;
        if (altitude <= shellAltitude(b)) return Zone.ORBIT_SHELL;
        return Zone.SPACE;
    }

    /**
     * Whether a craft (state relative to the body) should hand over to the surface world: inside the entry interface
     * and descending.
     */
    public static boolean descendsThroughEntry(AstroBody b, StateVector rel) {
        double alt = rel.position().length() - b.radius();
        return alt <= entryAltitude(b) && rel.position().dot(rel.velocity()) < 0;
    }

    /**
     * "Space factor" k in 0..1 for seamless surface-to-orbit visuals: 0 low in the air (sky as usual), 1 at low-orbit
     * altitude, where the view matches the orbit sky and the dimension switch happens. Smoothstep between a quarter of
     * the entry altitude (the surface on airless worlds) and the low-orbit altitude.
     */
    public static double spaceFactor(AstroBody b, double altitude) {
        double a0 = b.hasAtmosphere() ? 0.25 * entryAltitude(b) : 0;
        double a1 = b.lowOrbitRadius() - b.radius();
        double x = Math.max(0, Math.min(1, (altitude - a0) / (a1 - a0)));
        return x * x * (3 - 2 * x);
    }

    /**
     * {@link #spaceFactor} for worlds measured in blocks: {@code blocksAboveGround} out of {@code topBlocks} (the height
     * at which the climb is treated as reaching low orbit) maps linearly onto 0..low-orbit altitude.
     */
    public static double spaceFactorForBlocks(AstroBody b, double blocksAboveGround, double topBlocks) {
        return spaceFactor(b, blocksToAltitude(b, blocksAboveGround, topBlocks));
    }

    /** The linear blocks-to-altitude mapping used by {@link #spaceFactorForBlocks}, m. */
    public static double blocksToAltitude(AstroBody b, double blocksAboveGround, double topBlocks) {
        if (topBlocks <= 0) throw new IllegalArgumentException("topBlocks must be positive");
        return Math.max(0, blocksAboveGround) / topBlocks * (b.lowOrbitRadius() - b.radius());
    }

    /** Altitude at which {@link #spaceFactor} reaches {@code k} (inverse, by bisection), m. */
    public static double altitudeForSpaceFactor(AstroBody b, double k) {
        double lo = 0, hi = b.lowOrbitRadius() - b.radius();
        for (int i = 0; i < 60; i++) {
            double mid = 0.5 * (lo + hi);
            if (spaceFactor(b, mid) < k) lo = mid;
            else hi = mid;
        }
        return hi;
    }

    /** Speed at the entry interface relative to the body's surface (ignoring rotation), m/s. */
    public static double entrySpeed(StateVector rel) {
        return rel.velocity().length();
    }
}
