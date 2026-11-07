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

    /** Speed at the entry interface relative to the body's surface (ignoring rotation), m/s. */
    public static double entrySpeed(StateVector rel) {
        return rel.velocity().length();
    }
}
