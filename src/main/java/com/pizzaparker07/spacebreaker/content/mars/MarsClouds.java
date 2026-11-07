package com.pizzaparker07.spacebreaker.content.mars;

import net.minecraft.util.Mth;

/**
 * Martian water-ice clouds and fog, as seen from orbit since Viking:
 * <ul>
 *   <li>morning fog in the deep canyons (Valles Marineris, Noctis Labyrinthus): night-chilled air on the canyon floor
 *       holds a thin white ice haze that burns off within a couple of hours of sunrise and thins with height;</li>
 *   <li>the aphelion cloud belt: around Mars' farthest point from the sun (Ls ~70-120) the colder atmosphere grows a
 *       band of thin, bluish-white ice clouds over the equator.</li>
 * </ul>
 * Pure functions; the client draws both (fog in the canyon, a cloud veil in the sky).
 */
public final class MarsClouds {
    /** Day fractions of the fog window (the clock's convention: the sun crosses the horizon at about -0.033). */
    public static final double FOG_FROM = -0.04;
    public static final double FOG_TO = 0.12;
    /** Fog fills the lowest blocks of the canyon and is gone this many blocks above the floor. */
    public static final double FOG_DEPTH = 8;
    public static final double FOG_TOP = 30;
    /** Aphelion season (Ls, degrees) and its half width; equatorial belt half width (latitude). */
    public static final double BELT_LS = 95;
    public static final double BELT_LS_WIDTH = 28;
    public static final double BELT_LAT = 25;

    private MarsClouds() {
    }

    /** Morning canyon fog, 0..1, at a (true) day fraction: builds at dawn, thickest just after sunrise, gone by mid-morning. */
    public static double canyonFog(double dayFraction) {
        double f = dayFraction > 0.5 ? dayFraction - 1 : dayFraction;
        if (f <= FOG_FROM || f >= FOG_TO) {
            return 0;
        }
        double t = (f - FOG_FROM) / (FOG_TO - FOG_FROM);
        // rises quickly before sunrise, lingers, then burns off: a skewed bump peaking a little after sunrise
        return Math.pow(Math.sin(Math.PI * Math.pow(t, 0.7)), 2);
    }

    /** How much of the fog remains at a height above the canyon floor: full in the bottom blocks, thinning upward. */
    public static double fogAtHeight(double aboveFloor) {
        if (aboveFloor <= FOG_DEPTH) {
            return 1;
        }
        return Mth.clamp(1 - (aboveFloor - FOG_DEPTH) / (FOG_TOP - FOG_DEPTH), 0, 1);
    }

    /** Aphelion cloud cover, 0..1, at a season and latitude. */
    public static double cloudBelt(double ls, double latitude) {
        double d = Mth.wrapDegrees(ls - BELT_LS);
        double season = Math.exp(-(d * d) / (BELT_LS_WIDTH * BELT_LS_WIDTH));
        double band = Math.exp(-(latitude * latitude) / (BELT_LAT * BELT_LAT));
        return season < 0.02 ? 0 : season * (0.35 + 0.65 * band);
    }
}
