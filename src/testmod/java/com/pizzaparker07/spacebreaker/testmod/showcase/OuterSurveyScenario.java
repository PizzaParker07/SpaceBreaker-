package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A survey of every outer world for a side-by-side review (dev only, -Pscenario=outer_survey): one frame on the surface
 * and one from 30 blocks up, both in daylight and turned toward the parent planet where the world has one (from a spot
 * where it stands about 25 degrees above the horizon). Set SURVEY_WORLDS=a,b to shoot only some.
 */
final class OuterSurveyScenario implements ShowcaseScenario {
    static final String[] WORLDS = {"io", "europa", "ganymede", "callisto", "titan", "enceladus", "miranda", "ariel", "umbriel", "titania",
            "oberon", "triton", "proteus", "nereid", "pluto", "charon", "nix", "hydra", "ceres", "vesta", "bennu", "ryugu", "eros", "itokawa",
            "churyumov_gerasimenko", "encke", "halley", "tempel_1"};
    private static final double DAY = 0.12;

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        String only = System.getenv("SURVEY_WORLDS");
        for (String world : only != null && !only.isBlank() ? only.split(",") : WORLDS) {
            s.add(shot(world, false));
            s.add(shot(world, true));
        }
        return s;
    }

    private static ShowcaseDriver.Shot shot(String world, boolean high) {
        return new ShowcaseDriver.Shot("survey_" + world + (high ? "_b_high" : "_a_ground"), 6000, 0, 0, 0, 0F, 0F, true,
                CameraType.FIRST_PERSON, high ? 220 : 260, p -> place(p, world, high), MoonScenario::farView);
    }

    private static void place(ServerPlayer p, String world, boolean high) {
        ServerLevel level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(world)));
        if (level == null || !(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            ShowcaseDriver.log("[survey] no world " + world);
            return;
        }
        PlanetTerrain t = gen.terrain();
        // a spot on the equator where the parent stands about 25 degrees up (any spot works for worlds without one)
        int bestX = 400;
        double best = Double.MAX_VALUE;
        Vector3d dir = null;
        for (int x = -6000; x <= 6000; x += 40) {
            Vector3d e = SkyMath.parentDirection(t.latitude(0), t.longitude(x), DAY);
            double d = Math.abs(Math.toDegrees(Math.asin(e.y)) - 25);
            if (d < best) {
                best = d;
                bestX = x;
                dir = e;
            }
        }
        float yaw = dir == null ? 30F : MoonScenario.yaw(dir.x, dir.z);
        // near that spot, the place with the clearest view toward the planet (least ground rising in front of the camera)
        double fx = -Math.sin(Math.toRadians(yaw));
        double fz = Math.cos(Math.toRadians(yaw));
        int x = bestX;
        int z = 0;
        double clearest = Double.MAX_VALUE;
        for (int dx = -320; dx <= 320; dx += 64) {
            for (int dz = -320; dz <= 320; dz += 64) {
                int cx = bestX + dx;
                int cz = dz;
                double g = height(level, cx, cz);
                double rise = 0;
                for (int k = 4; k <= 40; k += 4) {
                    rise = Math.max(rise, (height(level, (int) (cx + fx * k), (int) (cz + fz * k)) - g) / k);
                }
                if (rise < clearest) {
                    clearest = rise;
                    x = cx;
                    z = cz;
                }
            }
        }
        double ground = height(level, x, z);
        float elev = dir == null ? 0F : (float) Math.toDegrees(Math.asin(dir.y));
        // on the ground: tipped up just enough to hold the planet and the horizon; from up high: looking down over the land
        float pitch = high ? 28F : Math.max(-20F, -elev + 14F);
        double y = ground + (high ? 30 : 2.2);
        p.teleportTo(level, x + 0.5, y - p.getEyeHeight(), z + 0.5, yaw, pitch);
        PlanetTime.setDayFraction(level, DAY);
        ShowcaseDriver.log(String.format("[survey] %s %s at %d %d, parent elevation %.0f", world, high ? "high" : "ground", x, z, elev));
    }

    private static double height(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }
}
