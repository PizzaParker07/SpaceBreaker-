package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance probe (dev only): for several worlds, generates fresh terrain on the server and times it, then times
 * how long the client needs until every section in view is built and samples the frame rate. Results go to the log
 * as "[perf] ...". Run with -Pscenario=perf (add -PwithCompat=true for Sodium/Iris and
 * -Dspacebreaker.compat.profile=true through JAVA_TOOL_OPTIONS for GPU timings of the compat passes).
 */
final class PerfScenario implements ShowcaseScenario {
    private static final String[] WORLDS = {"moon", "mars", "titan", "europa"};
    private static final int RADIUS = 4; // 9 x 9 chunks

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
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
                        Probe.start(world);
                    }));
        }
        return s;
    }

    /** Client side: time to a fully built view, then the average frame rate. */
    @EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
    static final class Probe {
        private static String world;
        private static int ticks;
        private static boolean built;
        private static long fpsSum;
        private static int fpsSamples;

        private Probe() {
        }

        static void start(String name) {
            world = name;
            ticks = 0;
            built = false;
            fpsSum = 0;
            fpsSamples = 0;
        }

        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            if (world == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            ticks++;
            if (!built && ticks > 20 && mc.levelRenderer.hasRenderedAllSections()) {
                built = true;
                ShowcaseDriver.log(String.format("[perf] %s client: view built after %d ticks (%.1f s)", world, ticks, ticks / 20.0));
            }
            if (built && ticks % 20 == 0) {
                fpsSum += mc.getFps();
                fpsSamples++;
                if (fpsSamples == 10) {
                    ShowcaseDriver.log(String.format("[perf] %s client: %.1f fps average over 10 s", world, fpsSum / 10.0));
                    world = null;
                }
            }
        }
    }
}
