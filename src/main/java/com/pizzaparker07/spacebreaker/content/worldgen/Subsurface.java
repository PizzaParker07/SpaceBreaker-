package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Underground on an airless world the day-night swing dies out within a metre of rock: lava tubes and dug-out
 * bases sit at a steady temperature near the average of day and night (about -25 °C on the Moon, instead of
 * +120 °C by day and -173 °C at night). Together with the rock overhead blocking radiation, this makes caves and
 * lava tubes natural shelters. Applies only to vacuum worlds with a planet generator, {@link #DEPTH} or more
 * blocks under the surface, out of the sky.
 */
public final class Subsurface {
    /** Blocks under the surface where the temperature is steady. */
    public static final int DEPTH = 4;

    private Subsurface() {
    }

    static void register() {
        Environment.addTemperatureModifier(Subsurface::offsetC);
    }

    /** True if this position is sheltered underground on an airless planet. */
    public static boolean sheltered(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !(server.getChunkSource().getGenerator() instanceof PlanetChunkGenerator)
                || !Environment.isVacuum(level) || !level.isLoaded(pos) || level.canSeeSky(pos)) {
            return false;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - pos.getY() >= DEPTH;
    }

    static double offsetC(Level level, BlockPos pos) {
        if (!sheltered(level, pos) || ColdTraps.inColdTrap(level, pos)) {
            return 0;
        }
        return Environment.body(level).map(b -> offsetC(b.climate().dayTempC(), b.climate().nightTempC(), Environment.sunElevation(level)))
                .orElse(0.0);
    }

    /** Offset that turns the surface temperature for this sun height into the steady underground one. */
    public static double offsetC(double dayC, double nightC, float sunElevation) {
        double sun = Math.max(0.0, Math.sin(sunElevation));
        double surface = nightC + (dayC - nightC) * Math.sqrt(sun);
        return steadyC(dayC, nightC) - surface;
    }

    /** The steady underground temperature: the mean of day and night. */
    public static double steadyC(double dayC, double nightC) {
        return (dayC + nightC) / 2;
    }
}
