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
    private static final String[] WORLDS = {"moon", "churyumov_gerasimenko", "encke", "halley", "tempel_1", "ceres", "io", "europa",
            "ganymede", "titan", "enceladus", "miranda", "ariel", "titania", "umbriel", "oberon", "triton", "pluto", "charon", "nix", "hydra", "proteus", "nereid", "bennu", "ryugu", "eros", "itokawa"};
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
                        Mspt.start(world);
                    }));
        }
        return s;
    }

    /** world, block x, latitude, what is there. */
    private static final String[][] HOTSPOTS = {
            {"moon", "-3000", "0", "warm-up, not counted"},
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
            {"proteus", String.valueOf(com.pizzaparker07.spacebreaker.content.neptune.Proteus.pharos().x()), String.valueOf(com.pizzaparker07.spacebreaker.content.neptune.Proteus.pharos().latitude()), "Pharos basin"},
            {"ryugu", "0", "0", "equatorial ridge"},
            {"itokawa", String.valueOf(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.musesSea().x()), String.valueOf(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.musesSea().latitude()), "Muses Sea"},
            {"tempel_1", String.valueOf(com.pizzaparker07.spacebreaker.content.belt.Comets.tempelPlain().x()), String.valueOf(com.pizzaparker07.spacebreaker.content.belt.Comets.tempelPlain().latitude()), "smooth plain"},
    };


    /**
     * Server tick time while the player stands in a world (the integrated server): every second for 10 s, the mean and the
     * worst of the server's last 100 tick times and the mean of the world's own tick times, logged as "[perf] ... ticks".
     */
    @net.neoforged.fml.common.EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    static final class Mspt {
        private static String world;
        private static int ticks;
        private static double meanSum;
        private static double worldSum;
        private static double worst;
        private static int samples;

        private Mspt() {
        }

        static void start(String name) {
            world = name;
            ticks = 0;
            meanSum = 0;
            worldSum = 0;
            worst = 0;
            samples = 0;
        }

        @net.neoforged.bus.api.SubscribeEvent
        static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (world == null || server == null) {
                return;
            }
            ticks++;
            // let the view settle first (the first second is the chunk burst measured separately)
            if (ticks <= 60 || ticks % 20 != 0) {
                return;
            }
            long[] all = server.getTickTimesNanos();
            double sum = 0;
            for (long t : all) {
                sum += t;
                worst = Math.max(worst, t / 1.0e6);
            }
            meanSum += sum / all.length / 1.0e6;
            long[] own = server.getTickTime(ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(world)));
            if (own != null && own.length > 0) {
                double o = 0;
                for (long t : own) {
                    o += t;
                }
                worldSum += o / own.length / 1.0e6;
            }
            samples++;
            if (samples == 10) {
                ShowcaseDriver.log(String.format("[perf] %s server ticks: %.2f ms mean, %.2f ms worst, the world itself %.2f ms", world,
                        meanSum / samples, worst, worldSum / samples));
                world = null;
            }
        }
    }
}
