package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance probe for the outer worlds (dev only), the same measurement as {@link PerfScenario}: fresh chunk
 * generation on the server and, on the client, time to a built view and the frame rate. The Moon runs first as the
 * baseline. Results go to the log as "[perf] ...". Run with -Pscenario=perf_outer.
 */
final class OuterPerfScenario implements ShowcaseScenario {
    private static final String[] WORLDS = {"moon", "ceres", "io", "europa", "ganymede", "titan", "enceladus", "miranda", "ariel", "titania",
            "umbriel", "oberon", "triton", "pluto", "charon"};
    private static final int RADIUS = 4; // 9 x 9 chunks

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        // first the busiest ground (server only): where the whole-world features of each moon do their work
        for (String[] spot : HOTSPOTS) {
            String world = spot[0];
            s.add(new ShowcaseDriver.Shot("perf_hot_" + world, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 40,
                    p -> {
                        ServerLevel level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(world)));
                        if (level == null || !(level.getChunkSource().getGenerator() instanceof com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator gen)) {
                            return;
                        }
                        double pd = gen.terrain().profile().poleDistance();
                        int x = (int) Double.parseDouble(spot[1]);
                        int z = (int) (-Double.parseDouble(spot[2]) / 90.0 * pd);
                        long t0 = System.nanoTime();
                        int n = 0;
                        for (int cx = -RADIUS; cx <= RADIUS; cx++) {
                            for (int cz = -RADIUS; cz <= RADIUS; cz++) {
                                level.getChunk((x >> 4) + cx, (z >> 4) + cz);
                                n++;
                            }
                        }
                        double ms = (System.nanoTime() - t0) / 1.0e6;
                        ShowcaseDriver.log(String.format("[perf] %s hot spot (%s): %d fresh chunks in %.0f ms (%.1f ms/chunk)", world, spot[3], n, ms, ms / n));
                    }, () -> { }));
        }
        for (int i = 0; i < WORLDS.length; i++) {
            String world = WORLDS[i];
            int x = 6000 + i * 3000;
            int z = 6000;
            s.add(new ShowcaseDriver.Shot("perf_" + world, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 600,
                    p -> {
                        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(world));
                        ServerLevel level = p.server.getLevel(key);
                        if (level == null) {
                            ShowcaseDriver.log("[perf] no world " + world);
                            return;
                        }
                        long t0 = System.nanoTime();
                        int n = 0;
                        for (int cx = -RADIUS; cx <= RADIUS; cx++) {
                            for (int cz = -RADIUS; cz <= RADIUS; cz++) {
                                level.getChunk((x >> 4) + cx, (z >> 4) + cz);
                                n++;
                            }
                        }
                        double ms = (System.nanoTime() - t0) / 1.0e6;
                        ShowcaseDriver.log(String.format("[perf] %s server: %d fresh chunks in %.0f ms (%.1f ms/chunk)", world, n, ms, ms / n));
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 3;
                        p.teleportTo(level, x + 0.5, y, z + 0.5, 30F, 10F);
                    },
                    () -> {
                        Minecraft.getInstance().options.renderDistance().set(12);
                        PerfScenario.Probe.start(world);
                    }));
        }
        return s;
    }

    /** world, block x, latitude, what is there. */
    private static final String[][] HOTSPOTS = {
            {"miranda", "380", "-18", "corona and great cliff"},
            {"ariel", "300", "-6", "rift valley"},
            {"titania", "250", "-4", "great canyon"},
            {"titania", "-900", "-40", "crater basin"},
            {"umbriel", "320", "-4", "Wunda ring"},
            {"oberon", "-420", "-4.4", "great mountain"},
            {"oberon", "520", "8", "chasm"},
            {"triton", "0", "-20", "cantaloupe terrain"},
            {"triton", "0", "-80", "geyser cap"},
            {"pluto", String.valueOf(com.pizzaparker07.spacebreaker.content.pluto.PlutoData.heart().x()),
                    String.valueOf(com.pizzaparker07.spacebreaker.content.pluto.PlutoData.HEART_LATITUDE), "nitrogen heart cells"},
            {"io", "0", "0", "lava lakes"},
            {"vesta", "0", "-80", "Rheasilvia basin"},
    };

}
