package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mars: vistas at noon and at a blue sunset, the dune sea, the great rift from its rim, the giant shield volcano,
 * an old river valley, the polar ice cap, a dust storm, Phobos at night, and a geology display. Camera spots are
 * found from the terrain function, so the scenario follows profile changes.
 */
final class MarsScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;
    static final int STAGE_X = 40;
    static final int STAGE_Z = 30;

    @Override
    public ResourceKey<Level> dimension() {
        return MarsModule.MARS;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        MarsWeather.force(level, 0F, 0);
        buildStage(level);
        extraBuild(level, terrain);
    }

    /** Hook for later content (creatures, dungeons). */
    void extraBuild(ServerLevel level, PlanetTerrain t) {
    }

    List<ShowcaseDriver.Shot> extraShots(PlanetTerrain t) {
        return List.of();
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(MarsModule.MARS);
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    private void buildStage(ServerLevel level) {
        int y0 = (int) Math.floor(terrain.height(STAGE_X, STAGE_Z));
        for (int x = STAGE_X - 13; x <= STAGE_X + 13; x++) {
            for (int z = STAGE_Z - 6; z <= STAGE_Z + 8; z++) {
                for (int y = y0 - 3; y <= y0 + 12; y++) {
                    BlockState s = y < y0 ? MarsModule.MARTIAN_SANDSTONE.base().get().defaultBlockState()
                            : y == y0 ? MarsModule.MARTIAN_REGOLITH.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
        Block[] natural = {MarsModule.MARTIAN_REGOLITH.get(), MarsModule.HEMATITE_SOIL.get(), MarsModule.PERCHLORATE_SOIL.get(),
                MarsModule.MARTIAN_CLAY.get(), MarsModule.CO2_FROST.get(), MarsModule.MARTIAN_SAND.get(), MarsModule.OLIVINE_SAND.get(),
                MarsModule.MARTIAN_BASALT.base().get(), MarsModule.MARTIAN_SANDSTONE.base().get(), MarsModule.MARTIAN_MUDSTONE.get(),
                MarsModule.MARTIAN_ICE.get(), MarsModule.DUSTY_ICE.get(), MarsModule.DRY_ICE.get()};
        for (int i = 0; i < natural.length; i++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(new BlockPos(STAGE_X - 12 + i * 2, y0 + y, STAGE_Z - 4), natural[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        Block[] built = {MarsModule.POLISHED_MARTIAN_BASALT.base().get(), MarsModule.MARTIAN_BASALT_BRICKS.base().get(),
                MarsModule.GYPSUM_BLOCK.get(), MarsModule.HEMATITE_ORE.get(), MarsModule.GYPSUM_ORE.get(),
                com.pizzaparker07.spacebreaker.content.moon.MoonModule.METEORIC_IRON.get()};
        for (int i = 0; i < built.length; i++) {
            level.setBlock(new BlockPos(STAGE_X - 12 + i * 2, y0 + 1, STAGE_Z), built[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(new BlockPos(STAGE_X, y0 + 1, STAGE_Z), MarsModule.MARTIAN_SANDSTONE.stairs().get().defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(STAGE_X + 2, y0 + 1, STAGE_Z), MarsModule.MARTIAN_BASALT_BRICKS.wall().get().defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(STAGE_X + 4, y0 + 1, STAGE_Z), MarsModule.MARTIAN_SANDSTONE.slab().get().defaultBlockState(), Block.UPDATE_CLIENTS);
        // a trail of boot prints across the pad
        for (int x = STAGE_X - 10; x <= STAGE_X + 10; x++) {
            BlockPos p = new BlockPos(x, y0, STAGE_Z + 4);
            level.setBlock(p, com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock.printFor(level.getBlockState(p), p,
                    net.minecraft.core.Direction.EAST), Block.UPDATE_CLIENTS);
        }
    }

    static Consumer<ServerPlayer> at(double dayFraction) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            MarsWeather.force(p.serverLevel(), 0F, 0);
        };
    }

    static Consumer<ServerPlayer> storm(double dayFraction, float strength) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            MarsWeather.force(p.serverLevel(), strength, 24000);
        };
    }

    private double ground(double x, double z) {
        return Math.floor(terrain().height(x, z)) + 1;
    }

    static float yaw(double dx, double dz) {
        return MoonScenario.yaw(dx, dz);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        double vx = 0.5;
        double vz = 0.5;
        double gy = ground(vx, vz);
        s.add(new ShowcaseDriver.Shot("01_vista_noon", 0, vx, gy + 12, vz, 30F, 6F, true, fp, 240, at(0.2), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("02_blue_sunset", 0, vx, gy + 12, vz, 90F, 4F, true, fp, 140, at(0.49), null));

        // the great rift, from the north rim looking along it
        double rz = t.riftCentre(300);
        double rimZ = rz - 150;
        s.add(new ShowcaseDriver.Shot("03_great_rift", 0, 300, ground(300, rimZ) + 8, rimZ, yaw(220, 150), 22F, true, fp, 260, at(0.15), null));
        s.add(new ShowcaseDriver.Shot("04_rift_floor", 0, 520, ground(520, t.riftCentre(520)) + 3, t.riftCentre(520), -90F, -4F, true, fp, 220,
                at(0.3), null));

        // the giant shield volcano from its foot
        double[] v = t.volcanoesNear(-2700, -500, -2500, -300).stream().max((a, b) -> Double.compare(a[2], b[2])).orElse(null);
        if (v != null) {
            double fx = v[0] + v[2] * 1.45;
            double fz = v[1] + v[2] * 0.25;
            s.add(new ShowcaseDriver.Shot("05_shield_volcano", 0, fx, ground(fx, fz) + 30, fz, yaw(v[0] - fx, v[1] - fz), 3F, true, fp, 300,
                    at(0.12), null));
            double cx = v[0] + v[2] * 0.08;
            s.add(new ShowcaseDriver.Shot("06_caldera", 0, cx, ground(cx, v[1]) + 10, v[1], yaw(v[0] - cx, 0), 25F, true, fp, 260, at(0.22), null));
        }
        // a dune field
        double[] dunes = MoonScenario.find(t, (x, z) -> t.column(x, z).dune > 0.9, 3000);
        if (dunes != null) {
            s.add(new ShowcaseDriver.Shot("07_dune_sea", 0, dunes[0], ground(dunes[0], dunes[1]) + 6, dunes[1], 115F, 12F, true, fp, 240,
                    at(0.42), null));
        }
        // an ancient valley meeting the plains
        double[] delta = MoonScenario.find(t, (x, z) -> t.column(x, z).delta > 0.6, 3000);
        double[] valley = MoonScenario.find(t, (x, z) -> t.column(x, z).valley > 0.8, 3000);
        double[] pick = delta != null ? delta : valley;
        if (pick != null) {
            s.add(new ShowcaseDriver.Shot("08_ancient_valley", 0, pick[0] - 20, ground(pick[0] - 20, pick[1] - 20) + 14, pick[1] - 20,
                    yaw(20, 20), 22F, true, fp, 240, at(0.18), null));
        }
        // the north polar cap
        double pz = -t.profile().poleDistance() * 0.9;
        s.add(new ShowcaseDriver.Shot("09_polar_cap", 0, 0, ground(0, pz) + 10, pz, 180F, 10F, true, fp, 300, at(0.2), null));
        // the stage: blocks in the sun, then the same place in a dust storm
        int y0 = (int) Math.floor(t.height(STAGE_X, STAGE_Z));
        s.add(new ShowcaseDriver.Shot("10_geology", 0, STAGE_X - 1, y0 + 4, STAGE_Z + 7, 180F, 28F, true, fp, 120, at(0.2), null));
        s.add(new ShowcaseDriver.Shot("11_dust_storm", 0, vx, gy + 3, vz, 60F, 2F, true, fp, 200, storm(0.2, 1F), null));
        // night: Phobos and Deimos
        s.add(new ShowcaseDriver.Shot("12_night_moons", 0, vx, gy + 3, vz, -90F, -35F, true, fp, 160, at(0.7), null));
        s.addAll(extraShots(t));
        return s;
    }
}
