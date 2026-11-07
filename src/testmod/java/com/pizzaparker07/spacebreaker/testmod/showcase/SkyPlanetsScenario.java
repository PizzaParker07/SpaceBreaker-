package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetPoints;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Planets in the sky: Venus and Jupiter at dawn/dusk over Earth, the blue Earth point over Mars.
 * Times and camera directions come from the same ephemeris maths the sky renderer uses.
 */
final class SkyPlanetsScenario implements ShowcaseScenario {
    private final boolean mars;

    SkyPlanetsScenario(boolean mars) {
        this.mars = mars;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return mars ? MarsModule.MARS : Level.OVERWORLD;
    }

    @Override
    public void build(ServerLevel level) {
    }

    /** Vanilla's time-of-day curve for a day fraction (0 = sunrise). */
    private static float tod(double fraction) {
        double d0 = Mth.frac(fraction - 0.25);
        double d1 = 0.5 - Math.cos(d0 * Math.PI) / 2.0;
        return (float) (d0 * 2.0 + d1) / 3.0F;
    }

    private static Vector3f world(Vector3f local, double fraction) {
        return new Vector3f(local).rotateX(tod(fraction) * Mth.TWO_PI).rotateY(-Mth.HALF_PI);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        ServerLevel level = server.getLevel(dimension());
        var sys = BodyAdapter.system(level.registryAccess()).orElseThrow();
        String observer = mars ? "spacebreaker:mars" : "spacebreaker:earth";
        List<PlanetPoints.Dot> dots = PlanetPoints.compute(sys, observer, BodyAdapter.simTime(level));
        List<ShowcaseDriver.Shot> shots = new ArrayList<>();
        double y = level.getChunkSource().getGenerator().getBaseHeight(0, 0, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                level, level.getChunkSource().randomState()) + 30;
        int n = 0;
        for (String target : mars ? List.of("spacebreaker:earth", "spacebreaker:jupiter") : List.of("spacebreaker:venus", "spacebreaker:jupiter")) {
            PlanetPoints.Dot dot = dots.stream().filter(d -> d.id().equals(target)).findFirst().orElse(null);
            if (dot == null) {
                continue;
            }
            // the darkest moment with the planet still 5-25 degrees up
            double best = -1;
            double bestSun = 2;
            for (int i = 0; i < 400; i++) {
                double f = i / 400.0;
                double alt = Math.toDegrees(Math.asin(world(dot.local(), f).y));
                double sun = world(new Vector3f(0, 1, 0), f).y;
                if (alt > 5 && alt < 25 && sun < bestSun) {
                    bestSun = sun;
                    best = f;
                }
            }
            if (best < 0) {
                continue;
            }
            Vector3f dir = world(dot.local(), best);
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
            float pitch = (float) -Math.toDegrees(Math.asin(dir.y)) + 6F;
            double fraction = best;
            long dayTime = Math.round(fraction * 24000);
            String name = String.format("%02d_%s_%s", ++n, mars ? "mars" : "earth", target.substring(target.indexOf(':') + 1));
            shots.add(new ShowcaseDriver.Shot(name, dayTime, 0.5, y, 0.5, yaw, pitch, true, CameraType.FIRST_PERSON, 120,
                    p -> {
                        if (mars) {
                            PlanetTime.setDayFraction(p.serverLevel(), fraction);
                        }
                    }, null));
        }
        if (!mars) {
            cometShot(level, y, shots);
        }
        return shots;
    }

    /**
     * Halley's 2061 return over Earth: one shot at the date its tails look longest (low in the dusk), one at the date it
     * stands well away from the sun with its tails still long, high in a dark night.
     */
    private static void cometShot(ServerLevel level, double y, List<ShowcaseDriver.Shot> shots) {
        var sys = BodyAdapter.system(level.registryAccess()).orElseThrow();
        var halley = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.HALLEY;
        double peri = halley.nextPerihelion(0);
        double longT = 0;
        double farT = 0;
        com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.Sighting longest = null;
        com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.Sighting farthest = null;
        for (int day = -90; day <= 90; day += 2) {
            double t = peri + day * 86400.0;
            for (var s : com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.compute(sys, "spacebreaker:earth", t)) {
                if (!s.id().equals(halley.id()) || !s.great()) {
                    continue;
                }
                if (s.head().y < 0.85F && (longest == null || s.head().angle(s.dustTip()) > longest.head().angle(longest.dustTip()))) {
                    longest = s;
                    longT = t;
                }
                if (s.head().y < 0.72F && (farthest == null || s.head().angle(s.dustTip()) > farthest.head().angle(farthest.dustTip()))) {
                    farthest = s;
                    farT = t;
                }
            }
        }
        add(shots, "03_earth_halley_2061", longest, longT, y, -0.15, 8, 35);
        add(shots, "04_earth_halley_2061_night", farthest, farT, y, -0.3, 10, 60);
    }

    private static void add(List<ShowcaseDriver.Shot> shots, String name, com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.Sighting s,
                            double t, double y, double maxSun, double minAlt, double maxAlt) {
        if (s == null) {
            return;
        }
        double fraction = -1;
        double darkest = 2;
        for (int i = 0; i < 400; i++) {
            double f = i / 400.0;
            double alt = Math.toDegrees(Math.asin(world(s.head(), f).y));
            double sun = world(new Vector3f(0, 1, 0), f).y;
            if (alt > minAlt && alt < maxAlt && sun < maxSun && sun < darkest) {
                darkest = sun;
                fraction = f;
            }
        }
        if (fraction < 0) {
            return;
        }
        Vector3f dir = world(s.head(), fraction);
        Vector3f tip = world(s.dustTip(), fraction);
        Vector3f mid = new Vector3f(dir).add(tip).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-mid.x, mid.z));
        float pitch = (float) -Math.toDegrees(Math.asin(mid.y));
        long ticks = com.pizzaparker07.spacebreaker.core.astro.GameClock.ticks(t, BodyAdapter.timeScale());
        long dayTime = Math.round(fraction * 24000);
        shots.add(new ShowcaseDriver.Shot(name, dayTime, 0.5, y, 0.5, yaw, pitch, true, CameraType.FIRST_PERSON, 160,
                p -> ((net.minecraft.world.level.storage.ServerLevelData) p.server.getWorldData().overworldData()).setGameTime(ticks), null));
    }
}
