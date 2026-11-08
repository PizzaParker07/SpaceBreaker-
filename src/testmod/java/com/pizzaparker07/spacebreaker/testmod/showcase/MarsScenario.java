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
        int y0 = (int) Math.floor(t.height(STAGE_X, STAGE_Z));
        // creature line-up on the pad, frozen for the photo
        spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.RUST_BEETLE.get(), STAGE_X - 9, y0 + 1, STAGE_Z + 6, 25F);
        spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.STONEGRAZER.get(), STAGE_X - 4, y0 + 1, STAGE_Z + 6, -65F);
        var lurker = spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.CANYON_LURKER.get(), STAGE_X + 2, y0 + 1, STAGE_Z + 6, 40F);
        if (lurker instanceof com.pizzaparker07.spacebreaker.content.mars.entity.CanyonLurker l) {
            l.setHidden(false);
        }
        spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.CANYON_LURKER.get(), STAGE_X + 6, y0 + 1, STAGE_Z + 6, 0F);
        spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.POLAR_WORM.get(), STAGE_X + 9, y0 + 1, STAGE_Z + 6, 35F);
        spawn(level, com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.DUST_DEVIL.get(), STAGE_X + 12, y0 + 1, STAGE_Z + 6, 0F);
        // dungeons near the vista point
        int sy = (int) Math.floor(t.height(STATION_X + 20, STATION_Z + 20)) + 1;
        place(level, new com.pizzaparker07.spacebreaker.content.mars.dungeon.ResearchStationPiece(4242L, STATION_X, sy, STATION_Z, net.minecraft.core.Direction.NORTH));
        int top = (int) Math.floor(t.height(CAVE_X, CAVE_Z));
        cave = new com.pizzaparker07.spacebreaker.content.mars.dungeon.RiverbedCavePiece(5150L, CAVE_X, top, CAVE_Z);
        place(level, cave);
        place(level, tube(t));
        place(level, lander(t));
    }

    static final int LANDER_X = 60;
    static final int LANDER_Z = -60;

    static com.pizzaparker07.spacebreaker.content.mars.dungeon.LanderWreckPiece lander(PlanetTerrain t) {
        int y = (int) Math.floor(t.height(LANDER_X + 7, LANDER_Z + 7)) + 1;
        return new com.pizzaparker07.spacebreaker.content.mars.dungeon.LanderWreckPiece(1976L, LANDER_X, y, LANDER_Z);
    }

    static final int TUBE_X = -40;
    static final int TUBE_Z = -130;

    /** A lava tube under the plains near the vista point (same seed every run). */
    static com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubePiece tube(PlanetTerrain t) {
        return new com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubePiece(7171L,
                com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubeStructure.walk(7171L, TUBE_X, TUBE_Z,
                        (x, z) -> (int) Math.floor(t.height(x, z)) + 1));
    }

    static final int STATION_X = -90;
    static final int STATION_Z = 40;
    static final int CAVE_X = 120;
    static final int CAVE_Z = -80;
    private com.pizzaparker07.spacebreaker.content.mars.dungeon.RiverbedCavePiece cave;

    static void placePiece(ServerLevel level, net.minecraft.world.level.levelgen.structure.StructurePiece piece) {
        place(level, piece);
    }

    private static void place(ServerLevel level, net.minecraft.world.level.levelgen.structure.StructurePiece piece) {
        var bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                var cb = new net.minecraft.world.level.levelgen.structure.BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15,
                        level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(cx * 31L + cz), cb, new net.minecraft.world.level.ChunkPos(cx, cz), bb.getCenter());
            }
        }
    }

    private static net.minecraft.world.entity.Mob spawn(ServerLevel level, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type,
                                                        double x, double y, double z, float yaw) {
        var mob = type.create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(x + 0.5, y, z + 0.5, yaw, 0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setSilent(true);
        level.addFreshEntity(mob);
        return mob;
    }

    List<ShowcaseDriver.Shot> extraShots(PlanetTerrain t) {
        int y0 = (int) Math.floor(t.height(STAGE_X, STAGE_Z));
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        s.add(new ShowcaseDriver.Shot("13_creatures", 0, STAGE_X + 1.5, y0 + 3.2, STAGE_Z + 12.5, 180F, 14F, true, CameraType.FIRST_PERSON, 120,
                at(0.18), null));
        String[] names = {"beetle", "stonegrazer", "lurker", "lurker_hidden", "worm", "dust_devil"};
        int[] xs = {-9, -4, 2, 6, 9, 12};
        for (int i = 0; i < names.length; i++) {
            double d = i == 1 ? 4.5 : i == 5 ? 5.5 : 2.6;
            s.add(new ShowcaseDriver.Shot("14_" + names[i], 0, STAGE_X + xs[i] + 0.5 + (i % 2 == 0 ? -0.4 : 0.4) * d, y0 + 1 + (i == 5 ? 2.2 : 1.4), STAGE_Z + 6.5 + d,
                    180F + (i % 2 == 0 ? 8F : -8F), i == 5 ? 5F : 22F, true, CameraType.FIRST_PERSON, 60, at(0.18), null));
        }
        {
            var body = lander(t).body();
            s.add(new ShowcaseDriver.Shot("21_lost_lander", 0, body.getX() + 0.5 - 9, body.getY() + 5, body.getZ() + 0.5 + 11, -140F, 18F, true,
                    CameraType.FIRST_PERSON, 160, at(0.2), null));
        }
        {
            var tube = tube(t);
            int sky = 3;
            var a = tube.node(sky);
            var b = tube.node(sky + 5);
            float along = (float) Math.toDegrees(Math.atan2(-(a.getX() - b.getX()), a.getZ() - b.getZ()));
            s.add(new ShowcaseDriver.Shot("18_lava_tube_skylight", 0, a.getX() + 0.5 - 5, ground(a.getX(), a.getZ()) + 9, a.getZ() + 0.5 - 5,
                    -45F, 58F, true, CameraType.FIRST_PERSON, 160, at(0.22), null));
            s.add(new ShowcaseDriver.Shot("19_lava_tube_inside", 0, b.getX() + 0.5, tube.floor(sky + 5) + 1.6, b.getZ() + 0.5, along, 4F, true,
                    CameraType.FIRST_PERSON, 120, p -> {
                        at(0.22).accept(p);
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
                    }, null));
        }
        int sy = (int) Math.floor(t.height(STATION_X + 20, STATION_Z + 20)) + 1;
        s.add(new ShowcaseDriver.Shot("15_research_station", 0, STATION_X - 18, sy + 18, STATION_Z + 62, -138F, 16F, true,
                CameraType.FIRST_PERSON, 200, p -> {
                    p.removeAllEffects();
                    at(0.15).accept(p);
                }, null));
        s.add(new ShowcaseDriver.Shot("15b_station_east", 0, STATION_X + 60, sy + 14, STATION_Z - 12, 51F, 13F, true,
                CameraType.FIRST_PERSON, 100, at(0.15), null));
        s.add(new ShowcaseDriver.Shot("15c_station_entrance", 0, STATION_X + 25.5, sy + 3.5, STATION_Z - 12.5, 14F, 6F, true,
                CameraType.FIRST_PERSON, 100, at(0.15), null));
        s.add(new ShowcaseDriver.Shot("16_station_inside", 0, STATION_X + 20.5, sy + 1.6, STATION_Z + 15.5, 0F, 6F, true,
                CameraType.FIRST_PERSON, 100, at(0.2).andThen(MarsScenario::clearDrones), null));
        s.add(new ShowcaseDriver.Shot("16b_station_lab", 0, STATION_X + 28.5, sy + 1.9, STATION_Z + 21.3, -100F, -4F, true,
                CameraType.FIRST_PERSON, 100, at(0.2).andThen(MarsScenario::clearDrones), null));
        s.add(new ShowcaseDriver.Shot("16d_supply_locker", 0, STATION_X + 33.5, sy + 1.9, STATION_Z + 19.2, 180F, 28F, true,
                CameraType.FIRST_PERSON, 100, at(0.2).andThen(MarsScenario::clearDrones), null));
        s.add(new ShowcaseDriver.Shot("16c_station_garage", 0, STATION_X + 11.5, sy + 1.6, STATION_Z + 20.5, 90F, 4F, true,
                CameraType.FIRST_PERSON, 100, at(0.2).andThen(MarsScenario::clearDrones), null));
        {
            var c = new com.pizzaparker07.spacebreaker.content.mars.dungeon.RiverbedCavePiece(5150L, CAVE_X,
                    (int) Math.floor(t.height(CAVE_X, CAVE_Z)), CAVE_Z).chamber();
            s.add(new ShowcaseDriver.Shot("17_riverbed_chamber", 0, c.getX() + 0.5, c.getY() + 1.6, c.getZ() - 4.5, 0F, 18F, true,
                    CameraType.FIRST_PERSON, 120, p -> {
                        at(0.2).accept(p);
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
                    }, null));
        }
        return s;
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
        for (int x = STAGE_X - 13; x <= STAGE_X + 19; x++) {
            for (int z = STAGE_Z - 6; z <= STAGE_Z + 16; z++) {
                for (int y = y0 - 3; y <= y0 + 20; y++) {
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
        s.add(new ShowcaseDriver.Shot("03_great_rift", 0, 200, ground(200, rz) + 55, rz, -90F, 18F, true, fp, 260, at(0.15), null));
        s.add(new ShowcaseDriver.Shot("04_rift_floor", 0, 520, ground(520, t.riftCentre(520)) + 3, t.riftCentre(520), -90F, -4F, true, fp, 220,
                at(0.3), null));

        // the giant shield volcano from its foot
        double[] v = t.volcanoesNear(-2700, -500, -2500, -300).stream().max((a, b) -> Double.compare(a[2], b[2])).orElse(null);
        if (v != null) {
            double fx = v[0] + v[2] * 1.08;
            double fz = v[1];
            s.add(new ShowcaseDriver.Shot("05_volcano_escarpment", 0, fx, ground(fx, fz) + 6, fz, 90F, -6F, true, fp, 300,
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

    /** Keeps the station's drones and beetles out of the interior shots (they wander into the lens). */
    private static void clearDrones(net.minecraft.server.level.ServerPlayer p) {
        p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(48))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }
}
