package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** One spot on an inner world at dawn, noon and night, looking toward the sunrise: a check of sky, fog and cloud colours. */
final class InnerDayCycleScenario implements ShowcaseScenario {
    private final ResourceKey<Level> dimension;
    private final String name;
    /** Fixed camera height, or NaN for a few blocks above the ground. */
    private final double y;

    InnerDayCycleScenario(ResourceKey<Level> dimension, String name, double y) {
        this.dimension = dimension;
        this.name = name;
        this.y = y;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public void build(ServerLevel level) {
        if (level.dimension() == com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARS) {
            com.pizzaparker07.spacebreaker.content.mars.MarsWeather.force(level, 0F, 0);
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension);
        double cy = Double.isNaN(y) ? Math.floor(((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain().height(0.5, 0.5)) + 6 : y;
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        double[] at = {-0.02, 0.25, 0.75};
        String[] when = {"dawn", "noon", "night"};
        for (int i = 0; i < 3; i++) {
            double f = at[i];
            s.add(new ShowcaseDriver.Shot("daycycle_" + name + "_" + when[i], 0, 0.5, cy, 0.5, -90F, -8F, true, CameraType.FIRST_PERSON, 100,
                    p -> PlanetTime.setDayFraction(p.serverLevel(), f), null));
        }
        return s;
    }
}
