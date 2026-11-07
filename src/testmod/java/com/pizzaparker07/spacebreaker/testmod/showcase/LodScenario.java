package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Distant Horizons check (dev only): a short normal view distance and a long wait so DH builds LOD terrain beyond
 * it, on the Moon (space sky over LODs) and Venus (haze over LODs), plus a Moon volcanic dome and a central-peak crater
 * seen from far away. Run with DH in run-mods and -PwithCompat=true.
 */
final class LodScenario implements ShowcaseScenario {
    @Override
    public void build(ServerLevel level) {
    }

    private static ShowcaseDriver.Shot shot(String name, ResourceKey<Level> dim, float yaw, float pitch, double dayFraction) {
        return new ShowcaseDriver.Shot(name, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 1800,
                p -> {
                    ServerLevel level = p.server.getLevel(dim);
                    if (level == null) {
                        return;
                    }
                    level.getChunk(0, 0); // generate it first: an unloaded chunk reports the world's floor as its height
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) + 30;
                    p.teleportTo(level, 0.5, y, 0.5, yaw, pitch);
                    PlanetTime.setDayFraction(level, dayFraction);
                },
                () -> Minecraft.getInstance().options.renderDistance().set(6));
    }

    /**
     * A distant view of a Moon landform, well past the normal view distance so it is drawn by LODs: the camera stands
     * {@code distance} blocks south-west of the target, {@code height} blocks above the target's ground, looking at it.
     */
    private static ShowcaseDriver.Shot moonTarget(String name, java.util.function.Function<PlanetTerrain, double[]> target,
                                                  double distance, double height, double dayFraction) {
        return new ShowcaseDriver.Shot(name, 6000, 0, 0, 0, 0F, 0F, true, CameraType.FIRST_PERSON, 1800,
                p -> {
                    ServerLevel level = p.server.getLevel(MoonModule.MOON);
                    if (level == null) {
                        return;
                    }
                    PlanetTerrain t = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
                    double[] tg = target.apply(t);
                    double cx = tg[0] - distance * 0.7071;
                    double cz = tg[1] + distance * 0.7071;
                    double ty = groundY(level, tg[0], tg[1]);
                    double cy = Math.max(ty, groundY(level, cx, cz)) + height;
                    p.teleportTo(level, cx, cy, cz, MoonScenario.yaw(tg[0] - cx, tg[1] - cz),
                            MoonScenario.pitch(tg[0] - cx, ty - cy, tg[1] - cz) * 0.8F);
                    PlanetTime.setDayFraction(level, dayFraction);
                },
                () -> Minecraft.getInstance().options.renderDistance().set(6));
    }

    private static double groundY(ServerLevel level, double x, double z) {
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        level.getChunk(bx >> 4, bz >> 4); // generate it first: an unloaded chunk reports the world's floor
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
    }

    /** The volcanic dome nearest the origin (its summit). */
    private static double[] dome(PlanetTerrain t) {
        double[] d = MoonScenario.find(t, (x, z) -> Math.hypot(x, z) > 400 && t.column(x, z).volcanoS < 0.35, 8000);
        if (d == null) {
            return new double[]{0, 0};
        }
        double bx = d[0];
        double bz = d[1];
        for (int i = -48; i <= 48; i += 4) {
            for (int j = -48; j <= 48; j += 4) {
                if (t.column(d[0] + i, d[1] + j).volcanoS < t.column(bx, bz).volcanoS) {
                    bx = d[0] + i;
                    bz = d[1] + j;
                }
            }
        }
        return new double[]{bx, bz};
    }

    /** The largest complex crater (one with a central peak) near the origin. */
    private static double[] peakCrater(PlanetTerrain t) {
        return t.cratersNear(-2000, -2000, 2000, 2000).stream()
                .filter(c -> c.peak() > 0)
                .max(java.util.Comparator.comparingDouble(Crater::radius))
                .map(c -> new double[]{c.x(), c.z()}).orElse(new double[]{0, 0});
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        return List.of(shot("01_lod_moon", MoonModule.MOON, 30F, 12F, 0.15), shot("02_lod_venus", VenusModule.VENUS, 30F, 12F, 0.25),
                // low morning sun so the low relief shows; both targets lie far beyond the 6-chunk view distance
                moonTarget("03_lod_moon_dome", LodScenario::dome, 420, 45, 0.03),
                moonTarget("04_lod_moon_central_peak", LodScenario::peakCrater, 520, 70, 0.06));
    }
}
