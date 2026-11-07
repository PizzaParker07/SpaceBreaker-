package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsClimate;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsSeasons;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The northern plains near the winter cap edge: frost thick toward the pole, thinning to patches toward the edge; bare in summer. */
final class MarsFrostScenario implements ShowcaseScenario {
    private static final double LATITUDE = 57;
    private static final int R = 112;
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

    private int z0() {
        return (int) Math.round(-LATITUDE / 90 * terrain().profile().poleDistance());
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
    }

    /** Sets the season and lets the frost catch up at once over the whole view. */
    private Consumer<ServerPlayer> season(double ls, double dayFraction) {
        return p -> {
            ServerLevel level = p.serverLevel();
            PlanetTime.setDayFraction(level, dayFraction);
            MarsSeasons.setLs(level, ls);
            int z0 = z0();
            for (int x = -R; x <= R; x++) {
                for (int z = z0 - R; z <= z0 + R; z++) {
                    double lat = MarsClimate.latitude(level, new BlockPos(x, 0, z));
                    MarsSeasons.updateColumn(level, x, z, MarsSeasons.wantsFrost(x, z, lat, ls, false));
                }
            }
        };
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        int z0 = z0();
        double y = Math.floor(terrain().height(0, z0 + 30)) + 9;
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new ShowcaseDriver.Shot("26_summer_bare", 0, 0.5, y, z0 + 30.5, 180F, 14F, true, fp, 260, season(95, 0.22), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("25_winter_frost", 0, 0.5, y, z0 + 30.5, 180F, 14F, true, fp, 200, season(275, 0.22), MoonScenario::farView));
        return s;
    }
}
