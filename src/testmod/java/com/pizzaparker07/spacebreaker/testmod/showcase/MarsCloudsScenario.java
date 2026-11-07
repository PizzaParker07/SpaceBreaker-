package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsSeasons;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Martian water-ice clouds: the aphelion cloud belt over the equatorial plains, and morning fog on the great canyon's floor. */
final class MarsCloudsScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MarsModule.MARS;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        com.pizzaparker07.spacebreaker.content.mars.MarsWeather.force(level, 0F, 0);
    }

    private static Consumer<ServerPlayer> when(double ls, double dayFraction) {
        return p -> {
            MarsSeasons.setLs(p.serverLevel(), ls);
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
        };
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        double gy = Math.floor(t.height(0.5, 0.5)) + 3;
        // the aphelion belt: streaks of ice cloud over the equator in the late-morning sky
        s.add(new ShowcaseDriver.Shot("27_aphelion_clouds", 0, 0.5, gy, 0.5, 30F, -32F, true, fp, 160, when(95, 0.16), null));
        // the same sky half a year later: clear
        s.add(new ShowcaseDriver.Shot("28_clear_southern_summer", 0, 0.5, gy, 0.5, 30F, -32F, true, fp, 120, when(270, 0.16), null));
        // the great canyon's floor just after sunrise, the fog lying in it
        double rz = t.riftCentre(520);
        double fy = Math.floor(t.height(520, rz)) + 3;
        s.add(new ShowcaseDriver.Shot("29_canyon_morning_fog", 0, 520, fy, rz, -90F, -2F, true, fp, 220, when(30, 0.015), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("30_canyon_midmorning", 0, 520, fy, rz, -90F, -2F, true, fp, 220, when(30, 0.2), null));
        return s;
    }
}
