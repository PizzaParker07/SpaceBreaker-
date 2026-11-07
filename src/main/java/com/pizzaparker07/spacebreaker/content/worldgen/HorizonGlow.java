package com.pizzaparker07.spacebreaker.content.worldgen;

/**
 * Lunar horizon glow: just after sunset (and before sunrise) on an airless world, electrostatically lofted dust
 * scatters sunlight into a thin bright band along the horizon above the hidden Sun, as early lunar landers photographed.
 * It is rare: only some sunsets and sunrises show it. Purely visual, decided the same way on every client.
 */
public final class HorizonGlow {
    /** One in this many sunsets or sunrises shows the glow. */
    public static final int ONE_IN = 3;
    /** The glow lasts while the Sun is between 0 and this many degrees below the horizon. */
    public static final double DEPTH_DEG = 12;
    /** Sun depression (degrees) at which the glow is brightest. */
    public static final double PEAK_DEG = 5;

    /** Whether the sunset ({@code sunset}) or sunrise of solar day {@code day} shows the glow, for a world {@code seed}. */
    public static boolean shows(long seed, long day, boolean sunset) {
        long h = seed * 0x9E3779B97F4A7C15L + day * 0xC2B2AE3D27D4EB4FL + (sunset ? 0x165667B19E3779F9L : 0);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return Math.floorMod(h, ONE_IN) == 0;
    }

    /** Brightness (0..1) of the glow for a sun at {@code sunElevationDeg}: none in daylight, gone below {@link #DEPTH_DEG}. */
    public static double strength(double sunElevationDeg) {
        double d = -sunElevationDeg;
        if (d <= 0 || d >= DEPTH_DEG) {
            return 0;
        }
        double t = d < PEAK_DEG ? d / PEAK_DEG : 1 - (d - PEAK_DEG) / (DEPTH_DEG - PEAK_DEG);
        return t * t * (3 - 2 * t);
    }

    /** Glow brightness for a clock at {@code dayTime}, observer latitude and world seed. */
    public static double at(PlanetTime.Clock clock, long dayTime, double latitudeDeg, long seed) {
        double fraction = clock.fraction(dayTime);
        long day = Math.floorDiv(dayTime + clock.offset(), clock.dayTicks());
        boolean sunset = fraction > 0.25 && fraction < 0.75;
        if (!shows(seed, day, sunset)) {
            return 0;
        }
        return strength(SkyMath.sunElevation(clock.timeOfDay(dayTime), latitudeDeg));
    }

    private HorizonGlow() {
    }
}
