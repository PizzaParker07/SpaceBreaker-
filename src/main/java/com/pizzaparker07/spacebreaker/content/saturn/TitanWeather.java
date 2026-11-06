package com.pizzaparker07.spacebreaker.content.saturn;

/**
 * Methane showers on Titan, driven by the world clock so every player sees the same weather: rain comes in spells a few
 * minutes long, now and then, with a soft start and end.
 */
public final class TitanWeather {
    /** Length of one weather window in ticks (5 minutes). */
    public static final long WINDOW = 6000;

    private TitanWeather() {
    }

    /** Rain strength 0..1 at a game time. */
    public static float rain(long gameTime) {
        long w = Math.floorDiv(gameTime, WINDOW);
        long h = w * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        if (Math.floorMod(h, 5) != 0) {
            return 0;
        }
        double t = (double) Math.floorMod(gameTime, WINDOW) / WINDOW;
        double edge = Math.min(t, 1 - t) / 0.15;
        return (float) Math.max(0, Math.min(1, edge));
    }
}
