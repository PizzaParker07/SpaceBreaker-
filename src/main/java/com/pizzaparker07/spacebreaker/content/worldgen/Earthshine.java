package com.pizzaparker07.spacebreaker.content.worldgen;

import org.joml.Vector3d;

/**
 * Earthshine on a tidally locked moon: sunlight reflected by the parent planet onto the night side. It follows the
 * parent's phase as drawn in the sky (the same sun and planet directions the sky renderer uses), is strongest under a
 * full planet high overhead and is absent where the planet is below the horizon (the far side).
 * <p>
 * This is a client lighting effect only: far too faint to run solar panels, so the server never uses it.
 */
public final class Earthshine {
    /** Extra sky light (0..1 lightmap scale) under a full planet at the zenith. */
    public static final double MAX = 0.17;
    /** Faint blue of light that comes off oceans and clouds. */
    public static final float BLUE = 0.25F;

    /**
     * Earthshine strength (0..{@link #MAX}) for an observer at the given latitude and longitude.
     *
     * @param timeOfDay   vanilla time of day (0 = noon) of the body's clock
     * @param dayFraction day fraction of the body's clock (drives libration)
     */
    public static double brightness(double latitudeDeg, double longitudeDeg, double timeOfDay, double dayFraction) {
        Vector3d e = SkyMath.parentDirection(latitudeDeg, longitudeDeg, dayFraction);
        if (e.y <= 0) {
            return 0;
        }
        double lit = SkyMath.illuminatedFraction(SkyMath.sunDirection(timeOfDay, latitudeDeg), e);
        // a planet's brightness falls faster than its lit area (phase law): a crescent gives almost nothing
        return MAX * Math.pow(lit, 1.6) * Math.min(1, e.y * 3);
    }

    private Earthshine() {
    }
}
