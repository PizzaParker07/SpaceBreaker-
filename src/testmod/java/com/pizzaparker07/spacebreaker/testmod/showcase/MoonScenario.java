package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Moon: landscape vistas at different sun angles, Earth in the sky, craters, the mare, the poles,
 * creatures and a dungeon. Camera spots are found from the terrain function itself, so the scenario keeps
 * working when the terrain profile changes.
 */
final class MoonScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        // monsters (ticks, drones) are removed on peaceful; mob spawning stays off in the showcase world
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        MoonShowcaseBuilder.build(level, terrain);
        MoonShowcaseBuilder.buildDungeons(level, terrain);
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(MoonModule.MOON);
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    private static Consumer<ServerPlayer> at(double dayFraction) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
    }

    private static Consumer<ServerPlayer> at(double dayFraction, Consumer<ServerPlayer> more) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            more.accept(p);
        };
    }

    /** Longer view distance for landscape shots. */
    static void farView() {
        Minecraft.getInstance().options.renderDistance().set(12);
    }

    static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    static float pitch(double dx, double dy, double dz) {
        return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private double ground(double x, double z) {
        return Math.floor(terrain().height(x, z)) + 1;
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;

        // 1-3: the same vista from a hill at sunrise, noon and sunset
        double vx = 0.5;
        double vz = 0.5;
        double gy = ground(vx, vz);
        Vector3d earth = SkyMath.parentDirection(t.latitude(vz), t.longitude(vx), 0.25);
        float earthYaw = yaw(earth.x, earth.z);
        s.add(new ShowcaseDriver.Shot("01_vista_sunrise", 0, vx, gy + 14, vz, -90F, 8F, true, fp, 220, at(0.975), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("02_vista_noon", 0, vx, gy + 14, vz, earthYaw, 8F, true, fp, 120, at(0.25), null));
        s.add(new ShowcaseDriver.Shot("03_earth_quarter", 0, vx, gy + 3, vz, earthYaw, pitch(earth.x, earth.y, earth.z) * 0.85F,
                true, fp, 60, at(0.0), null));
        s.add(new ShowcaseDriver.Shot("04_vista_sunset", 0, vx, gy + 14, vz, 90F, 8F, true, fp, 120, at(0.505), null));

        // 5: largest crater near the origin, seen from its rim
        Crater big = t.cratersNear(-1200, -1200, 1200, 1200).stream()
                .filter(c -> Math.abs(c.x()) < 1200 && Math.abs(c.z()) < 1200)
                .max(Comparator.comparingDouble(Crater::radius)).orElseThrow();
        double rimX = big.x() - big.radius() * 1.05;
        double rimZ = big.z() + big.radius() * 0.3;
        double rimY = ground(rimX, rimZ);
        s.add(new ShowcaseDriver.Shot("05_great_crater", 0, rimX, rimY + 10, rimZ, yaw(big.x() - rimX, big.z() - rimZ),
                14F, true, fp, 260, at(0.1), null));
        s.add(new ShowcaseDriver.Shot("06_crater_aerial", 0, big.x() - big.radius() * 1.6, rimY + big.radius() * 0.9, big.z(),
                -90F, 30F, true, fp, 260, at(0.33), null));

        // 7: a mare plain with a fresh rayed crater if one is near
        double[] mare = find(t, (x, z) -> t.column(x, z).lowland > 0.9, 3000);
        if (mare != null) {
            double my = ground(mare[0], mare[1]);
            s.add(new ShowcaseDriver.Shot("07_mare", 0, mare[0], my + 6, mare[1], 30F, 10F, true, fp, 220, at(0.12), null));
        }
        Crater fresh = t.cratersNear(-4000, -4000, 4000, 4000).stream().filter(Crater::fresh).filter(c -> c.radius() > 12)
                .filter(c -> t.column(c.x() + c.radius() * 2, c.z()).lowland > 0.6)
                .min(Comparator.comparingDouble(c -> c.x() * c.x() + c.z() * c.z())).orElse(null);
        if (fresh != null) {
            s.add(new ShowcaseDriver.Shot("08_rayed_crater", 0, fresh.x() + fresh.radius() * 2.5, ground(fresh.x(), fresh.z()) + fresh.radius() * 2.2,
                    fresh.z() + fresh.radius() * 2.5, yaw(-1, -1), 38F, true, fp, 260, at(0.25), null));
        }

        // 9-10: the south pole region: low sun, icy shadowed crater floors
        double poleZ = t.profile().poleDistance() * 0.94;
        Crater polar = t.cratersNear(-800, poleZ - 600, 800, poleZ + 300).stream()
                .filter(c -> c.radius() > 12 && Math.abs(t.latitude(c.z())) > 82)
                .min(Comparator.comparingDouble(c -> Math.abs(c.x()) + Math.abs(c.z() - poleZ))).orElse(null);
        if (polar != null) {
            double px = polar.x() + polar.radius() * 1.3;
            double pz = polar.z() - polar.radius() * 0.5;
            s.add(new ShowcaseDriver.Shot("09_polar_crater", 0, px, ground(px, pz) + polar.radius() * 0.6 + 4, pz,
                    yaw(polar.x() - px, polar.z() - pz), 26F, true, fp, 300, at(0.25), null));
        }
        // 11: lunar night under a full Earth
        s.add(new ShowcaseDriver.Shot("10_night_full_earth", 0, vx, gy + 3, vz, earthYaw, pitch(earth.x, earth.y, earth.z) * 0.7F,
                true, fp, 80, at(0.75), null));
        // 12+: close-ups of the showcase stage (blocks, creatures, dungeon) built by MoonShowcaseBuilder
        s.addAll(MoonShowcaseBuilder.shots(this::ground));
        s.addAll(MoonShowcaseBuilder.dungeonShots(t));
        return s;
    }

    interface ColumnTest {
        boolean test(double x, double z);
    }

    /** Spiral search for the nearest column passing the test. */
    static double[] find(PlanetTerrain t, ColumnTest test, int maxDist) {
        for (int r = 0; r <= maxDist; r += 48) {
            int steps = Math.max(1, r / 48 * 6);
            for (int i = 0; i < steps; i++) {
                double a = i * Math.PI * 2 / steps;
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                if (test.test(x, z)) {
                    return new double[]{x, z};
                }
            }
        }
        return null;
    }
}
