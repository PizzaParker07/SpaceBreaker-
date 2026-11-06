package com.pizzaparker07.spacebreaker.core.astro;

/**
 * Maps game time to simulated orbital time. The server option {@code orbitalTimeScale} says how many real
 * seconds one simulated hour takes (default 1: an Earth year passes in about 2.4 real hours).
 * Simulated time 0 is the J2000 epoch, so real-world planet positions are used at the start of a world.
 */
public final class GameClock {
    public static final double TICKS_PER_SECOND = 20.0;

    /** Simulated seconds since J2000 after {@code gameTicks} ticks. */
    public static double simSeconds(long gameTicks, double realSecondsPerSimHour) {
        return gameTicks / TICKS_PER_SECOND / realSecondsPerSimHour * 3600.0;
    }

    /** Simulated seconds per game tick. */
    public static double simSecondsPerTick(double realSecondsPerSimHour) {
        return 3600.0 / (realSecondsPerSimHour * TICKS_PER_SECOND);
    }

    /** Real seconds a simulated duration takes to pass in game. */
    public static double realSeconds(double simSeconds, double realSecondsPerSimHour) {
        return simSeconds / 3600.0 * realSecondsPerSimHour;
    }

    /** Game ticks a simulated duration takes. */
    public static long ticks(double simSeconds, double realSecondsPerSimHour) {
        return Math.round(realSeconds(simSeconds, realSecondsPerSimHour) * TICKS_PER_SECOND);
    }

    private GameClock() {
    }
}
