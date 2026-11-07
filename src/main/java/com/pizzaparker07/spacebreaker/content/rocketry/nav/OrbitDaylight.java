package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Eclipses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day and night in orbit. A station circles its body once every {@link #ORBIT_TICKS} game ticks; for part of each
 * orbit it passes through the body's shadow (umbra and penumbra from the astro library, {@link Eclipses}). Worked
 * out from the game time alone, so server (solar panels) and every client (sky, light) agree.
 */
public final class OrbitDaylight {
    /** One orbit, and so one day and one night, in ticks (10 minutes). */
    public static final float ORBIT_TICKS = 12000F;

    /** Server and client have their own registries (both live in single player). */
    private static final Map<RegistryAccess, Optional<BodySystem>> SYSTEMS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Map<String, Double> PERIODS = new ConcurrentHashMap<>();

    private OrbitDaylight() {
    }

    /** Position along the orbit, 0..1: 0 straight under the sun, 0.5 behind the body. */
    public static double phase(double gameTime) {
        double f = gameTime / ORBIT_TICKS;
        return f - Math.floor(f);
    }

    /** Height of the stations' orbit above a body of this radius, m: low orbit, at least 200 km. */
    public static double altitude(double radius) {
        return Math.max(200_000, radius * 0.063);
    }

    /**
     * Visible share of the sun's disc (1 full sun, 0 total shadow) at this point of the orbit.
     *
     * @param sys  the body system, or null when the library has no data (then a plain geometric shadow)
     */
    public static double sunlit(@org.jetbrains.annotations.Nullable BodySystem sys, ResourceLocation body, double gameTime) {
        Last last = LAST;
        if (last != null && last.sys == sys && last.body.equals(body) && last.time == gameTime) {
            return last.value; // the light map asks 256 times a frame
        }
        // a real penumbra lasts seconds; spread dusk and dawn over about ten seconds so the light fades, not blinks
        double sum = 0;
        for (int i = -SMOOTH_SAMPLES; i <= SMOOTH_SAMPLES; i++) {
            sum += rawSunlit(sys, body, gameTime + i * SMOOTH_STEP);
        }
        double v = sum / (2 * SMOOTH_SAMPLES + 1);
        LAST = new Last(sys, body, gameTime, v);
        return v;
    }

    private static final int SMOOTH_SAMPLES = 4;
    private static final double SMOOTH_STEP = 25;

    private record Last(BodySystem sys, ResourceLocation body, double time, double value) {
    }

    private static volatile Last LAST;

    private static double rawSunlit(@org.jetbrains.annotations.Nullable BodySystem sys, ResourceLocation body, double gameTime) {
        double f = phase(gameTime);
        String id = body.toString();
        if (sys != null && sys.get(id).isPresent()) {
            try {
                var b = sys.require(id);
                double alt = altitude(b.radius());
                double period = PERIODS.computeIfAbsent(id, k -> Eclipses.station(sys, k, alt, 0).orbitPeriod());
                return Eclipses.station(sys, id, alt, f * period).sunlit();
            } catch (RuntimeException ignored) {
                // fall through to the simple shadow
            }
        }
        // a low orbit spends about 38 % in shadow, centred on the far side
        double fromNoon = Math.abs(f - 0.5);
        return fromNoon < 0.19 ? 0 : fromNoon < 0.21 ? (fromNoon - 0.19) / 0.02 : 1;
    }

    /** The orbited body of a place in an orbit dimension, if it is one. */
    public static Optional<ResourceLocation> orbitedBody(Level level, double x) {
        ResourceLocation shared = Location.Orbits.SHARED.equals(level.dimension()) ? Locations.bodyForSharedX(x).orElse(null) : null;
        return Locations.at(level.dimension(), shared).filter(Location::orbit).map(Location::body);
    }

    /**
     * Sunlight at a block: 1 outside orbit (surface days are the dimension's own), otherwise the orbit's share of
     * sun. For machines such as solar arrays.
     */
    public static double sunlit(Level level, BlockPos pos) {
        return orbitedBody(level, pos.getX()).map(body -> sunlit(system(level.registryAccess()), body, level.getGameTime())).orElse(1.0);
    }

    /** The body system of these registries (cached). */
    @org.jetbrains.annotations.Nullable
    public static BodySystem system(RegistryAccess access) {
        return SYSTEMS.computeIfAbsent(access, a -> {
            try {
                return BodyAdapter.system(a);
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }).orElse(null);
    }
}
