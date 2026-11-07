package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mercury.MercuryDay;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
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
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mercury's double sunrise, seen from one spot looking east over a crater rim that lies exactly on the horizon:
 * the sun's swollen perihelion disc peeks over the rim, sinks back behind it, then rises for good.
 */
final class MercurySunriseScenario implements ShowcaseScenario {
    /** Distance of the rim from the camera and its half width, blocks. */
    private static final int RIM = 48;
    private static final int HALF = 20;
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MercuryModule.MERCURY;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    /** Eye height of the camera; the rim's top edge is at the same height, so it marks the true horizon. */
    private int eye() {
        return (int) Math.floor(terrain().height(0, 0)) + 3;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        int top = eye();
        var crust = MercuryModule.MERCURIAN_CRUST.get().defaultBlockState();
        for (int z = -HALF; z <= HALF; z++) {
            for (int x = RIM; x <= RIM + 3; x++) {
                int ground = (int) Math.floor(terrain.height(x, z)) - 2;
                for (int y = Math.min(ground, top - 6); y < top; y++) {
                    level.setBlock(new BlockPos(x, y, z), crust, 2);
                }
                for (int y = top; y < top + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // a clear line of sight from the camera to the rim
        for (int x = 1; x < RIM; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = top - 1; y < top + 8; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static Consumer<ServerPlayer> at(double meanFraction) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), meanFraction);
    }

    private static void narrow() {
        Minecraft.getInstance().options.fov().set(30);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        double p = MercuryDay.PERIHELION;
        double y = eye();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new ShowcaseDriver.Shot("08_sunrise_first", 0, 0.5, y, 0.5, -90F, -2F, true, fp, 160, at(p - 0.023), MercurySunriseScenario::narrow));
        s.add(new ShowcaseDriver.Shot("09_sun_sinks_back", 0, 0.5, y, 0.5, -90F, -2F, true, fp, 120, at(p + 0.023), MercurySunriseScenario::narrow));
        s.add(new ShowcaseDriver.Shot("10_sunrise_second", 0, 0.5, y, 0.5, -90F, -2F, true, fp, 120, at(p + 0.07), MercurySunriseScenario::narrow));
        return s;
    }
}
