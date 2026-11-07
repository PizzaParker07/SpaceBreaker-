package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocky planet level of detail: Earth over the Moon drawn larger and larger (a dev-only size override standing in
 * for flying closer), so the voxel ball steps from 16 to 32, 64 and 128 blocks across. Frame cost is logged
 * (-Dspacebreaker.blockyTiming, set here) as "Blocky bodies: ...".
 */
final class BlockyLodScenario implements ShowcaseScenario {
    private static final double[] SCALES = {8, 16, 26, 40};

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        PlanetTerrain t = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        level.getChunk(0, 0);
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) + 3;
        Vector3d earth = SkyMath.parentDirection(t.latitude(0.5), t.longitude(0.5), 0.5);
        float yaw = MoonScenario.yaw(earth.x, earth.z);
        float pitch = MoonScenario.pitch(earth.x, earth.y, earth.z);
        List<ShowcaseDriver.Shot> shots = new ArrayList<>();
        for (int i = 0; i < SCALES.length; i++) {
            String scale = Double.toString(SCALES[i]);
            shots.add(new ShowcaseDriver.Shot(String.format("%02d_lod_scale_%d", i + 1, (int) SCALES[i]), 0, 0.5, y, 0.5, yaw, pitch,
                    true, CameraType.FIRST_PERSON, 100, p -> PlanetTime.setDayFraction(p.serverLevel(), 0.6), () -> {
                        System.setProperty("spacebreaker.blockyTiming", "true");
                        System.setProperty("spacebreaker.bodyScale", scale);
                    }));
        }
        return shots;
    }
}
