package com.pizzaparker07.spacebreaker.core.env;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One place to ask "what is it like here?": gravity, air, pressure, temperature, radiation and sunlight
 * for any level and position. Dimensions without a celestial body (Nether, End, other mods) count as
 * Earth-like so nothing breaks there. Systems that change local conditions (sealed oxygenated rooms,
 * heaters, shielding) plug in through the provider hooks below.
 */
public final class Environment {
    private static final List<OxygenSource> OXYGEN_SOURCES = new CopyOnWriteArrayList<>();
    private static final List<TemperatureModifier> TEMPERATURE_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<SunlightModifier> SUNLIGHT_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<PressureModifier> PRESSURE_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<RadiationSource> RADIATION_SOURCES = new CopyOnWriteArrayList<>();

    /**
     * A local source of radiation (e.g. an unshielded RTG pellet someone carries), in mSv/day at a position.
     * Added on top of the body's sky dose; overhead cover does not shield it.
     */
    @FunctionalInterface
    public interface RadiationSource {
        double doseMsvDay(Level level, BlockPos pos);
    }

    public static void addRadiationSource(RadiationSource source) {
        RADIATION_SOURCES.add(source);
    }

    /** Multiplies the ambient pressure at a position (e.g. thinner air with altitude); 1 = unchanged. */
    @FunctionalInterface
    public interface PressureModifier {
        double factor(Level level, BlockPos pos);
    }

    public static void addPressureModifier(PressureModifier modifier) {
        PRESSURE_MODIFIERS.add(modifier);
    }

    /** Something that can make a position breathable even in vacuum (e.g. an oxygenated, sealed room). */
    @FunctionalInterface
    public interface OxygenSource {
        boolean providesOxygen(Level level, BlockPos pos);
    }

    /** Adds a local temperature offset in °C (e.g. heaters, lava, coolers). */
    @FunctionalInterface
    public interface TemperatureModifier {
        double offsetC(Level level, BlockPos pos);
    }

    /** Multiplies the sunlight reaching a position (e.g. dust storms, cloud decks); 1 = unchanged. */
    @FunctionalInterface
    public interface SunlightModifier {
        double factor(Level level, BlockPos pos);
    }

    public static void addSunlightModifier(SunlightModifier modifier) {
        SUNLIGHT_MODIFIERS.add(modifier);
    }

    public static void addOxygenSource(OxygenSource source) {
        OXYGEN_SOURCES.add(source);
    }

    public static void addTemperatureModifier(TemperatureModifier modifier) {
        TEMPERATURE_MODIFIERS.add(modifier);
    }

    public static Optional<CelestialBody> body(Level level) {
        return Bodies.forLevel(level);
    }

    /** Real surface gravity in m/s². */
    public static double gravity(Level level) {
        return body(level).map(b -> b.physical().surfaceGravity()).orElse(CelestialBody.EARTH_GRAVITY);
    }

    /**
     * Gravity multiplier actually applied to entities, after the server's realism setting.
     * In orbit dimensions this is 0 (free fall).
     */
    public static double gravityFactor(Level level) {
        if (Bodies.isOrbit(level)) {
            return 0.0;
        }
        double real = gravity(level) / CelestialBody.EARTH_GRAVITY;
        return SbConfig.realism().scaleGravity(real);
    }

    public static boolean isVacuum(Level level) {
        return body(level).map(b -> b.atmosphere().isVacuum() || Bodies.isOrbit(level)).orElse(false);
    }

    /** Whether an unprotected player can breathe at this position. */
    public static boolean canBreathe(Level level, BlockPos pos) {
        boolean natural = body(level).map(b -> b.atmosphere().breathable() && !Bodies.isOrbit(level)).orElse(true);
        if (natural) {
            return true;
        }
        for (OxygenSource source : OXYGEN_SOURCES) {
            if (source.providesOxygen(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Ambient pressure in kPa. */
    public static double pressureKpa(Level level, BlockPos pos) {
        if (Bodies.isOrbit(level)) {
            return 0.0;
        }
        double p = body(level).map(b -> b.atmosphere().pressureKpa()).orElse(101.325);
        for (PressureModifier modifier : PRESSURE_MODIFIERS) {
            p *= modifier.factor(level, pos);
        }
        return p;
    }

    /** Sun elevation above the horizon in radians (negative at night). */
    public static float sunElevation(Level level) {
        float angle = Mth.wrapDegrees(level.getSunAngle(1F) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        return Mth.HALF_PI - Math.abs(angle);
    }

    /** Ambient temperature in °C: interpolates between the body's night and day temperatures by sun height. */
    public static double temperatureC(Level level, BlockPos pos) {
        double base = body(level).map(b -> {
            double day = b.climate().dayTempC();
            double night = b.climate().nightTempC();
            double sun = Math.max(0.0, Math.sin(sunElevation(level)));
            double t = Math.sqrt(sun);
            return night + (day - night) * t;
        }).orElse(15.0);
        for (TemperatureModifier modifier : TEMPERATURE_MODIFIERS) {
            base += modifier.offsetC(level, pos);
        }
        return base;
    }

    /**
     * Radiation dose rate in mSv/day at this position. Every solid block overhead (up to 16) halves the dose,
     * so living under regolith or deep underground is a real defence. Local sources (see {@link RadiationSource})
     * are added on top.
     */
    public static double radiation(Level level, BlockPos pos) {
        double local = 0;
        for (RadiationSource source : RADIATION_SOURCES) {
            local += source.doseMsvDay(level, pos);
        }
        return skyRadiation(level, pos) + local;
    }

    /** Only the body's own (cosmic and solar) dose at a position, after overhead cover. */
    public static double skyRadiation(Level level, BlockPos pos) {
        double surface = body(level).map(CelestialBody::radiation).orElse(0.0);
        if (surface <= 0) {
            return 0;
        }
        int cover = 0;
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < 16 && p.getY() < level.getMaxBuildHeight(); i++) {
            p.move(0, 1, 0);
            if (level.getBlockState(p).isSolidRender(level, p)) {
                cover++;
            }
        }
        return surface * Math.pow(0.5, cover);
    }

    /** Sunlight relative to Earth (1.0 = 1361 W/m²). */
    public static double solarFactor(Level level) {
        return body(level).map(b -> b.solarIrradiance() / CelestialBody.EARTH_IRRADIANCE).orElse(1.0);
    }

    /**
     * Sunlight relative to Earth at a position, after local effects such as dust storms. Solar machines should
     * use this rather than {@link #solarFactor(Level)}.
     */
    public static double solarFactor(Level level, BlockPos pos) {
        return solarFactor(level) * sunlightModifier(level, pos);
    }

    /** Only the local modifiers (weather, clouds) at a position: 1 = clear sky. */
    public static double sunlightModifier(Level level, BlockPos pos) {
        double f = 1;
        for (SunlightModifier modifier : SUNLIGHT_MODIFIERS) {
            f *= modifier.factor(level, pos);
        }
        return f;
    }

    private Environment() {
    }
}
