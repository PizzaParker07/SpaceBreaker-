package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Local climate on worlds with a terrain profile: poles are colder than the equator (the body's climate numbers
 * are equatorial), and Martian dust storms block sunlight, cool the days and warm the nights.
 */
public final class MarsClimate {
    private static final Map<RegistryAccess, Map<ResourceLocation, Double>> POLE_DISTANCE = new WeakHashMap<>();

    private MarsClimate() {
    }

    static void register() {
        Environment.addTemperatureModifier(MarsClimate::temperatureOffset);
        Environment.addSunlightModifier((level, pos) -> 1 - 0.85 * MarsWeather.stormStrength(level));
        // Martian dust settling on solar cells: about three sols to full cover, six times faster in a storm
        Environment.addDustSource((level, pos) -> MarsWeather.dusty(level)
                ? com.pizzaparker07.spacebreaker.core.env.DustLoad.rate(MarsWeather.stormStrength(level)) : 0);
        com.pizzaparker07.spacebreaker.content.suit.EnvironmentSensorItem.dustRate = Environment::dustRate;
        PanelDust.register();
        // the whole atmosphere breathes with the seasons as CO2 freezes onto and sublimates from the polar caps
        Environment.addPressureModifier((level, pos) -> level.dimension() == MarsModule.MARS && level instanceof net.minecraft.server.level.ServerLevel sl
                ? MarsSeasons.pressureFactor(MarsSeasons.ls(sl)) : 1);
    }

    /** Pole-to-equator distance of the level's terrain profile, or NaN without one. */
    public static double poleDistance(Level level) {
        var body = Environment.body(level);
        if (body.isEmpty()) {
            return Double.NaN;
        }
        Map<ResourceLocation, Double> byBody;
        synchronized (POLE_DISTANCE) {
            byBody = POLE_DISTANCE.computeIfAbsent(level.registryAccess(), access -> {
                Map<ResourceLocation, Double> m = new java.util.HashMap<>();
                access.registry(TerrainProfile.REGISTRY).ifPresent(r -> r.stream().forEach(t -> m.put(t.body(), t.poleDistance())));
                return m;
            });
        }
        var key = level.registryAccess().registry(com.pizzaparker07.spacebreaker.core.body.Bodies.REGISTRY)
                .map(r -> r.getKey(body.get())).orElse(null);
        Double d = key == null ? null : byBody.get(key);
        return d == null ? Double.NaN : d;
    }

    /** Latitude in degrees at a position, or 0 on worlds without a terrain profile. */
    public static double latitude(Level level, BlockPos pos) {
        double pd = poleDistance(level);
        return Double.isNaN(pd) ? 0 : PlanetTerrain.latitude(pos.getZ() + 0.5, pd);
    }

    static double temperatureOffset(Level level, BlockPos pos) {
        if (level.dimension() != MarsModule.MARS) {
            return 0;
        }
        double lat = latitude(level, pos) / 90;
        double offset = -75 * lat * lat; // polar winter: below the CO2 frost point
        float storm = MarsWeather.stormStrength(level);
        if (storm > 0) {
            boolean day = Environment.sunElevation(level) > 0;
            offset += storm * (day ? -14 : 22);
        }
        return offset;
    }
}
