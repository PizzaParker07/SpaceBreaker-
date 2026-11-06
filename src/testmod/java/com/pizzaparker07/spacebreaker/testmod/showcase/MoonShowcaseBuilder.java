package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import com.pizzaparker07.spacebreaker.content.moon.entity.RegolithTick;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.LavaTubePiece;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.OutpostPiece;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** A small geology display and building sample on the Moon, next to the showcase vista point. */
final class MoonShowcaseBuilder {
    static final int STAGE_X = 60;
    static final int STAGE_Z = 40;
    private static int floorY;

    private MoonShowcaseBuilder() {
    }

    static int floor(PlanetTerrain t) {
        return (int) Math.floor(t.height(STAGE_X, STAGE_Z));
    }

    static void build(ServerLevel level, PlanetTerrain terrain) {
        floorY = floor(terrain);
        int y0 = floorY;
        // level a 26 x 20 pad of regolith
        for (int x = STAGE_X - 12; x <= STAGE_X + 13; x++) {
            for (int z = STAGE_Z - 6; z <= STAGE_Z + 13; z++) {
                for (int y = y0 - 3; y <= y0 + 14; y++) {
                    BlockState s = y < y0 ? MoonModule.ANORTHOSITE.base().get().defaultBlockState()
                            : y == y0 ? MoonModule.LUNAR_REGOLITH.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
        // geology display: two-high pillars of every natural block
        Block[] natural = {MoonModule.LUNAR_REGOLITH.get(), MoonModule.MARE_REGOLITH.get(), MoonModule.VOLCANIC_GLASS_SOIL.get(),
                MoonModule.ICY_REGOLITH.get(), MoonModule.LUNAR_ICE.get(), MoonModule.MARE_BASALT.get(), MoonModule.ANORTHOSITE.base().get(),
                MoonModule.IMPACT_BRECCIA.get(), MoonModule.METEORIC_IRON.get(), MoonModule.ILMENITE_ORE.get(), MoonModule.KREEP_ORE.get()};
        for (int i = 0; i < natural.length; i++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(new BlockPos(STAGE_X - 10 + i * 2, y0 + y, STAGE_Z - 4), natural[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // building sample: a small hut of anorthosite bricks and sintered regolith
        int hx = STAGE_X + 2;
        int hz = STAGE_Z + 7;
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 7; z++) {
                boolean edge = x == 0 || x == 8 || z == 0 || z == 6;
                level.setBlock(new BlockPos(hx + x, y0, hz + z), MoonModule.SINTERED_REGOLITH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                if (!edge) {
                    continue;
                }
                for (int y = 1; y <= 3; y++) {
                    boolean corner = (x == 0 || x == 8) && (z == 0 || z == 6);
                    boolean window = y == 2 && !corner && (x == 4 || z == 3);
                    Block b = corner ? MoonModule.POLISHED_ANORTHOSITE.base().get()
                            : window ? Blocks.GLASS : MoonModule.ANORTHOSITE_BRICKS.base().get();
                    level.setBlock(new BlockPos(hx + x, y0 + y, hz + z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int x = -1; x < 10; x++) {
            for (int z = -1; z < 8; z++) {
                boolean rim = x == -1 || x == 9 || z == -1 || z == 7;
                BlockState roof = rim
                        ? MoonModule.SINTERED_REGOLITH_BRICKS.slab().get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        : MoonModule.SINTERED_REGOLITH_BRICKS.base().get().defaultBlockState();
                level.setBlock(new BlockPos(hx + x, y0 + 4, hz + z), roof, Block.UPDATE_CLIENTS);
            }
        }
        // steps and a low wall in front
        for (int x = 0; x < 9; x++) {
            Block wall = MoonModule.ANORTHOSITE_BRICKS.wall().get();
            if (x != 4) {
                level.setBlock(new BlockPos(hx + x, y0 + 1, hz - 2), wall.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.setBlock(new BlockPos(hx + 4, y0 + 1, hz - 1), MoonModule.POLISHED_ANORTHOSITE.stairs().get().defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(hx + 4, y0 + 1, hz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(new BlockPos(hx + 4, y0 + 2, hz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        // boot prints walking up to the door
        for (int z = STAGE_Z + 12; z >= hz - 3; z--) {
            BlockPos p = new BlockPos(hx + 4 - (z > STAGE_Z + 6 ? 6 : 0), y0, z);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof LunarRegolithBlock) {
                level.setBlock(p, LunarRegolithBlock.printFor(st, p, Direction.NORTH), Block.UPDATE_CLIENTS);
            }
        }
        // creatures, posed (no AI) so the camera can see them
        spawn(level, MoonEntities.REGOLITH_TICK.get(), STAGE_X - 8.5, y0 + 1, STAGE_Z + 5.5, 20F, false);
        spawn(level, MoonEntities.REGOLITH_TICK.get(), STAGE_X - 6.5, y0 + 1, STAGE_Z + 3.5, -30F, true);
        spawn(level, MoonEntities.ELDER_REGOLITH_TICK.get(), STAGE_X - 3.0, y0 + 1, STAGE_Z + 4.5, -15F, false);
        spawn(level, MoonEntities.DUST_SWIMMER.get(), STAGE_X - 10.0, y0 + 1, STAGE_Z + 9.0, 60F, false);
        spawn(level, MoonEntities.DUST_SWIMMER.get(), STAGE_X - 5.0, y0 + 1, STAGE_Z + 9.5, -40F, false);
        spawn(level, MoonEntities.GLIMMER_BEETLE.get(), STAGE_X - 7.5, y0 + 1, STAGE_Z + 8.0, 160F, false);
        spawn(level, MoonEntities.GLIMMER_BEETLE.get(), STAGE_X - 6.2, y0 + 1, STAGE_Z + 8.6, 200F, false);
        level.setBlock(new BlockPos(STAGE_X - 9, y0 + 1, STAGE_Z + 7), MoonModule.GLIMMER_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        var drone = spawn(level, MoonEntities.SURVEY_DRONE.get(), STAGE_X - 6.0, y0 + 3.2, STAGE_Z + 6.0, 10F, false);
        drone.setNoGravity(true);
        for (int x = hx - 2; x <= hx + 4; x++) {
            BlockPos p = new BlockPos(x, y0, STAGE_Z + 6);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof LunarRegolithBlock) {
                level.setBlock(p, LunarRegolithBlock.printFor(st, p, Direction.EAST), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static net.minecraft.world.entity.Mob spawn(ServerLevel level, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type,
                                                          double x, double y, double z, float yaw, boolean burrowed) {
        net.minecraft.world.entity.Mob mob = type.create(level);
        mob.moveTo(x, y, z, yaw, 0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        if (mob instanceof RegolithTick tick) {
            tick.setBurrowed(burrowed);
        }
        level.addFreshEntity(mob);
        return mob;
    }

    // ------------------------------------------------------------------ dungeons

    static final long TUBE_SEED = 77123L;

    /** A flat spot for the outpost east of the stage (same search on client and server). */
    static int[] outpostSpot(PlanetTerrain t) {
        for (int r = 70; r < 900; r += 16) {
            for (int a = 0; a < 16; a++) {
                int x = STAGE_X + (int) (Math.cos(a * Math.PI / 8) * r);
                int z = STAGE_Z + (int) (Math.sin(a * Math.PI / 8) * r);
                double min = 1e9, max = -1e9;
                for (int dx = -15; dx <= 15; dx += 5) {
                    for (int dz = -15; dz <= 15; dz += 5) {
                        double h = t.height(x + dx, z + dz);
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                    }
                }
                if (max - min < 4 && t.column(x, z).anyCraterS > 2) {
                    return new int[]{x, (int) Math.round(t.height(x, z)) + 1, z};
                }
            }
        }
        return new int[]{STAGE_X + 80, (int) Math.round(t.height(STAGE_X + 80, STAGE_Z)) + 1, STAGE_Z};
    }

    static int[] tubeStart(PlanetTerrain t) {
        int x = STAGE_X - 90;
        int z = STAGE_Z + 30;
        return new int[]{x, (int) Math.floor(t.height(x, z)), z};
    }

    static List<int[]> tubePath(PlanetTerrain t) {
        int[] st = tubeStart(t);
        return LavaTubePiece.walk(RandomSource.create(TUBE_SEED), st[0], st[2], (x, z) -> (int) Math.floor(t.height(x, z)));
    }

    /** Runs a piece over every chunk it touches, like world generation does. */
    static void placePiece(ServerLevel level, StructurePiece piece) {
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                BoundingBox cb = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, cb, new ChunkPos(cx, cz), bb.getCenter());
            }
        }
    }

    static void buildDungeons(ServerLevel level, PlanetTerrain t) {
        int[] o = outpostSpot(t);
        placePiece(level, new OutpostPiece(4243L, o[0] - 15, o[1], o[2] - 15, Direction.SOUTH));
        int[] st = tubeStart(t);
        placePiece(level, new LavaTubePiece(TUBE_SEED, tubePath(t), st[1]));
    }

    static List<ShowcaseDriver.Shot> dungeonShots(PlanetTerrain t) {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> noon = p -> PlanetTime.setDayFraction(p.serverLevel(), 0.2);
        java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> dark = p -> {
            PlanetTime.setDayFraction(p.serverLevel(), 0.2);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        };
        int[] o = outpostSpot(t);
        double x0 = o[0] - 15;
        double z0 = o[2] - 15;
        double y = o[1];
        s.add(new ShowcaseDriver.Shot("30_outpost_aerial", 0, x0 - 12, y + 18, z0 + 44, MoonScenario.yaw(27, -29), 30F, true, fp, 200, noon, null));
        s.add(new ShowcaseDriver.Shot("31_outpost_airlock", 0, x0 + 15.5, y + 2.2, z0 + 33.5, 180F, 4F, true, fp, 60, noon, null));
        s.add(new ShowcaseDriver.Shot("32_outpost_hub", 0, x0 + 15.5, y + 2.7, z0 + 18.6, 180F, 14F, true, fp, 60, dark, null));
        s.add(new ShowcaseDriver.Shot("33_outpost_lab", 0, x0 + 15.5, y + 2.7, z0 + 6.6, 180F, 20F, true, fp, 60, dark, null));
        s.add(new ShowcaseDriver.Shot("34_drone_bay", 0, x0 + 7.4, y + 2.7, z0 + 15.5, 90F, 12F, true, fp, 60, dark, null));
        s.add(new ShowcaseDriver.Shot("38_crew_quarters", 0, x0 + 7.4, y + 2.7, z0 + 4.5, 90F, 18F, true, fp, 60, dark, null));
        s.add(new ShowcaseDriver.Shot("39_vault_puzzle", 0, x0 + 19.5, y + 2.6, z0 + 5.5, -90F, 8F, true, fp, 60, dark, null));
        s.add(new ShowcaseDriver.Shot("40_greenhouse", 0, x0 + 23.5, y + 3.2, z0 + 15.5, -90F, 20F, true, fp, 60, noon, null));
        List<int[]> path = tubePath(t);
        int[] n0 = path.get(0);
        int[] st = tubeStart(t);
        s.add(new ShowcaseDriver.Shot("35_lava_tube_skylight", 0, n0[0] + 7, st[1] + 7, n0[2] + 7, MoonScenario.yaw(-7, -7), 50F, true, fp, 120, noon, null));
        int[] a = path.get(3);
        int[] b = path.get(6);
        s.add(new ShowcaseDriver.Shot("36_lava_tube", 0, a[0] + 0.5, a[1] + 0.2, a[2] + 0.5, MoonScenario.yaw(b[0] - a[0], b[2] - a[2]), 4F,
                true, fp, 80, dark, null));
        int wi = path.size() * 2 / 3;
        int[] w = path.get(wi);
        int[] wp = path.get(wi - 1);
        s.add(new ShowcaseDriver.Shot("37_probe_wreck", 0, wp[0] + 0.5, w[1] + 1.0, wp[2] + 0.5, MoonScenario.yaw(w[0] - wp[0], w[2] - wp[2]), 18F,
                true, fp, 80, dark, null));
        return s;
    }

    static List<ShowcaseDriver.Shot> shots(BiFunction<Double, Double, Double> ground) {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        double y = Math.floor(ground.apply((double) STAGE_X, (double) STAGE_Z) - 1) + 1;
        s.add(new ShowcaseDriver.Shot("20_geology_display", 0, STAGE_X + 0.5, y + 2.6, STAGE_Z + 2.5, 180F, 14F, true, fp, 80,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.12), null));
        s.add(new ShowcaseDriver.Shot("21_lunar_hut", 0, STAGE_X + 0.5, y + 4.5, STAGE_Z - 5.5, -30F, 16F, true, fp, 80,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.08), null));
        s.add(new ShowcaseDriver.Shot("22_bootprints", 0, STAGE_X + 2.5, y + 5.0, STAGE_Z + 19.5, 170F, 38F, true, fp, 60,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.1), null));
        s.add(new ShowcaseDriver.Shot("23_creatures", 0, STAGE_X - 6.0, y + 2.4, STAGE_Z + 12.5, 180F, 14F, true, fp, 80,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.12), null));
        s.add(new ShowcaseDriver.Shot("24_tick_macro", 0, STAGE_X - 8.0, y + 1.2, STAGE_Z + 7.6, 190F, 18F, true, fp, 40,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.12), null));
        s.add(new ShowcaseDriver.Shot("26_glimmer_night", 0, STAGE_X - 7.0, y + 1.4, STAGE_Z + 10.8, 180F, 22F, true, fp, 60,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.75), null));
        s.add(new ShowcaseDriver.Shot("25_drone_macro", 0, STAGE_X - 6.0, y + 3.0, STAGE_Z + 8.6, 180F, 2F, true, fp, 40,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.12), null));
        return s;
    }
}
