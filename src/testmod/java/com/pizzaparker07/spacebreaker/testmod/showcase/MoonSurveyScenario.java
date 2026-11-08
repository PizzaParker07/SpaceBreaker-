package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonData;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * A survey of the Moon's terrain for a critical review (dev only, -Pscenario=moon_survey): for each kind of ground
 * (mare, highlands, the great crater, the pyroclastic plains, the pole) one frame from the surface and one from 30
 * blocks up, in low morning sun, plus Earth over the near side. Spots are found from the biome layout, so any seed works.
 */
final class MoonSurveyScenario extends OuterShowcase {
    private static final double DAY = 0.12;

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        region(level, "mare", MoonData.MARE);
        region(level, "highlands", MoonData.HIGHLANDS);
        region(level, "crater", MoonData.CRATER);
        region(level, "volcanic", MoonData.VOLCANIC);
        region(level, "polar", MoonData.POLAR);
        int[] mare = find(level, MoonData.MARE);
        if (mare != null) {
            parent(level, t, "survey_earth", mare[0], mare[1], DAY, 8);
        }
    }

    private void region(ServerLevel level, String name, ResourceKey<Biome> biome) {
        int[] p = find(level, biome);
        if (p == null) {
            ShowcaseDriver.log("[moon survey] no " + name);
            return;
        }
        // look toward the middle of the biome patch (the crater's centre, the mare's open floor)
        int[] c = centroid(level, biome, p[0], p[1]);
        double dx = c[0] - p[0];
        double dz = c[1] - p[1];
        double l = Math.max(1, Math.hypot(dx, dz));
        double tx = p[0] + dx / l * 40;
        double tz = p[1] + dz / l * 40;
        look(level, "survey_" + name + "_a_ground", p[0], p[1], 1.7, tx, tz, at(DAY), 200, MoonScenario::farView);
        double bx = p[0] - dx / l * 20;
        double bz = p[1] - dz / l * 20;
        look(level, "survey_" + name + "_b_high", bx, bz, 30, tx + dx / l * 30, tz + dz / l * 30, at(DAY), 200, MoonScenario::farView);
    }

    private static boolean is(ServerLevel level, ResourceKey<Biome> biome, int x, int z) {
        return level.getUncachedNoiseBiome(x >> 2, 64 >> 2, z >> 2).is(biome);
    }

    /** A spot well inside the biome: the first ring point (spiral out from the origin) whose neighbours share it. */
    private static int[] find(ServerLevel level, ResourceKey<Biome> biome) {
        for (int r = 0; r <= 12000; r += 96) {
            int steps = Math.max(1, (int) (2 * Math.PI * r / 96));
            for (int i = 0; i < steps; i++) {
                double a = 2 * Math.PI * i / steps;
                int x = (int) (r * Math.cos(a));
                int z = (int) (r * Math.sin(a));
                if (is(level, biome, x, z) && is(level, biome, x + 24, z) && is(level, biome, x - 24, z) && is(level, biome, x, z + 24)
                        && is(level, biome, x, z - 24)) {
                    return new int[]{x, z};
                }
            }
        }
        return null;
    }

    private static int[] centroid(ServerLevel level, ResourceKey<Biome> biome, int x0, int z0) {
        long sx = 0;
        long sz = 0;
        int n = 0;
        for (int x = x0 - 240; x <= x0 + 240; x += 16) {
            for (int z = z0 - 240; z <= z0 + 240; z += 16) {
                if (is(level, biome, x, z)) {
                    sx += x;
                    sz += z;
                    n++;
                }
            }
        }
        return n == 0 ? new int[]{x0 + 1, z0} : new int[]{(int) (sx / n), (int) (sz / n)};
    }
}
