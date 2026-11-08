package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryData;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import com.pizzaparker07.spacebreaker.content.venus.VenusData;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * A survey of one inner world's terrain for a critical review (dev only, -Pscenario=inner_survey_mars and so on): for
 * each kind of ground one frame from the surface and one from 30 blocks up, in morning sun, plus the sky that world is
 * known for (Phobos and Deimos over Mars, Mars over its moons, the sun from Venus's cloud layer, Mercury's terminator).
 * Spots are found from the biome layout, so any seed works.
 */
final class InnerSurveyScenario extends OuterShowcase {
    private static final double DAY = 0.15;
    private final String world;
    private final ResourceKey<Level> dimension;
    private final List<Region> regions;

    record Region(String name, ResourceKey<Biome> biome) {
    }

    private InnerSurveyScenario(String world, ResourceKey<Level> dimension, List<Region> regions) {
        this.world = world;
        this.dimension = dimension;
        this.regions = regions;
    }

    static InnerSurveyScenario mars() {
        return new InnerSurveyScenario("mars", MarsModule.MARS, List.of(new Region("plains", MarsData.PLAINS), new Region("highlands", MarsData.HIGHLANDS),
                new Region("canyon", MarsData.CANYON), new Region("volcano", MarsData.VOLCANO), new Region("dunes", MarsData.DUNES),
                new Region("valley", MarsData.VALLEY), new Region("crater", MarsData.CRATER), new Region("polar", MarsData.POLAR),
                new Region("ice_cap", MarsData.ICE_CAP)));
    }

    static InnerSurveyScenario venus() {
        return new InnerSurveyScenario("venus", VenusModule.VENUS, List.of(new Region("plains", VenusData.PLAINS), new Region("tesserae", VenusData.HIGHLANDS),
                new Region("volcano", VenusData.VOLCANO), new Region("crater", VenusData.CRATER)));
    }

    static InnerSurveyScenario mercury() {
        return new InnerSurveyScenario("mercury", MercuryModule.MERCURY, List.of(new Region("plains", MercuryData.PLAINS),
                new Region("highlands", MercuryData.HIGHLANDS), new Region("crater", MercuryData.CRATER), new Region("polar", MercuryData.POLAR)));
    }

    static InnerSurveyScenario phobos() {
        return new InnerSurveyScenario("phobos", MarsModule.PHOBOS, List.of(new Region("surface", MarsData.PHOBOS_SURFACE),
                new Region("stickney", MarsData.PHOBOS_CRATER)));
    }

    static InnerSurveyScenario deimos() {
        return new InnerSurveyScenario("deimos", MarsModule.DEIMOS, List.of(new Region("surface", MarsData.DEIMOS_SURFACE)));
    }

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        int[] first = null;
        for (Region r : regions) {
            int[] p = region(level, r);
            first = first == null ? p : first;
        }
        if (first == null) {
            return;
        }
        String w = "survey_" + world + "_sky";
        switch (world) {
            case "mars" -> {
                // Phobos and Deimos at night, and the small sun by day
                shot(w + "_moons_night", first[0] + 0.5, ground(level, first[0], first[1]) + 2, first[1] + 0.5, -90F, -35F, 160, at(0.7), null);
                shot(w + "_sun_day", first[0] + 0.5, ground(level, first[0], first[1]) + 2, first[1] + 0.5, -90F, -30F, 160, at(0.12), null);
            }
            case "venus" -> {
                // inside the cloud deck, and on top of it looking toward the low sun
                shot(w + "_in_clouds", first[0] + 0.5, VenusClimate.CLOUD_BASE + 20, first[1] + 0.5, -90F, 0F, 140, at(0.2), null);
                shot(w + "_above_clouds_sun", first[0] + 0.5, VenusClimate.CLOUD_TOP + 4, first[1] + 0.5, -90F, -6F, 160, at(0.1), null);
            }
            case "mercury" -> {
                // dawn at the terminator: long shadows, the sun just over the edge
                look(level, w + "_terminator_dawn", first[0], first[1], 3, first[0] - 60, first[1], at(0.015), 200, MoonScenario::farView);
                shot(w + "_noon_sun", first[0] + 0.5, ground(level, first[0], first[1]) + 2, first[1] + 0.5, -90F, -40F, 140, at(0.2), null);
            }
            default -> {
                // Mars over the near side, half lit and full
                parent(level, t, w + "_mars_day", first[0], first[1], DAY, 8);
                parent(level, t, w + "_mars_night", first[0], first[1], 0.75, 8);
            }
        }
    }

    private int[] region(ServerLevel level, Region r) {
        int[] p = find(level, r.biome());
        if (p == null) {
            ShowcaseDriver.log("[inner survey] " + world + ": no " + r.name());
            return null;
        }
        int[] c = centroid(level, r.biome(), p[0], p[1]);
        double dx = c[0] - p[0];
        double dz = c[1] - p[1];
        double l = Math.hypot(dx, dz);
        if (l < 1) {
            dx = 1;
            dz = 0.4;
            l = Math.hypot(dx, dz);
        }
        double tx = p[0] + dx / l * 40;
        double tz = p[1] + dz / l * 40;
        String name = "survey_" + world + "_" + r.name();
        look(level, name + "_a_ground", p[0], p[1], 1.7, tx, tz, at(DAY), 200, MoonScenario::farView);
        double bx = p[0] - dx / l * 20;
        double bz = p[1] - dz / l * 20;
        look(level, name + "_b_high", bx, bz, 30, tx + dx / l * 30, tz + dz / l * 30, at(DAY), 200, MoonScenario::farView);
        ShowcaseDriver.log(String.format("[inner survey] %s %s at %d %d", world, r.name(), p[0], p[1]));
        return p;
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
