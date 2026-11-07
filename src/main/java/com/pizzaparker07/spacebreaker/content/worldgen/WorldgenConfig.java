package com.pizzaparker07.spacebreaker.content.worldgen;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings of the planet engine (file: spacebreaker-worlds-server.toml, synced to clients). */
public final class WorldgenConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DAY_LENGTH_EXPONENT;
    public static final ModConfigSpec.IntValue MIN_DAY_MINUTES;
    public static final ModConfigSpec.IntValue MAX_DAY_MINUTES;
    public static final ModConfigSpec.BooleanValue METEOR_SHOWERS;
    public static final ModConfigSpec.IntValue METEOR_INTERVAL_MINUTES;
    public static final ModConfigSpec.IntValue METEOR_DURATION_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("planet_time");
        DAY_LENGTH_EXPONENT = b.comment(
                        "How a body's real solar day maps to a game day: game day = 20 min × (real day / 24 h) ^ exponent.",
                        "1.0 = real proportions (the Moon's day lasts ~9.8 hours), 0.5 = default (~108 min), 0 = every world like Earth.")
                .defineInRange("dayLengthExponent", 0.5, 0.0, 1.0);
        MIN_DAY_MINUTES = b.comment("Shortest allowed game day on any body, in real minutes.")
                .defineInRange("minDayMinutes", 4, 1, 1440);
        MAX_DAY_MINUTES = b.comment("Longest allowed game day on any body, in real minutes.")
                .defineInRange("maxDayMinutes", 600, 1, 100000);
        b.pop();
        b.push("micrometeorites");
        METEOR_SHOWERS = b.comment("Micrometeorite showers on airless worlds (warning, sky streaks, small hits and suit wear in the open).")
                .define("showers", true);
        METEOR_INTERVAL_MINUTES = b.comment("Average real minutes between showers on one world (each gap is 0.5-1.5x this).")
                .defineInRange("intervalMinutes", 60, 2, 100000);
        METEOR_DURATION_SECONDS = b.comment("Length of a shower in seconds.")
                .defineInRange("durationSeconds", 40, 5, 600);
        b.pop();
        SPEC = b.build();
    }

    public static double exponent() {
        return SPEC.isLoaded() ? DAY_LENGTH_EXPONENT.get() : 0.5;
    }

    public static int minMinutes() {
        return SPEC.isLoaded() ? MIN_DAY_MINUTES.get() : 4;
    }

    public static int maxMinutes() {
        return SPEC.isLoaded() ? MAX_DAY_MINUTES.get() : 600;
    }

    private WorldgenConfig() {
    }
}
