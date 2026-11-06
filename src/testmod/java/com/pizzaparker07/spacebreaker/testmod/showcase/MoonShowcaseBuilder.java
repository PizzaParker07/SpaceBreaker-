package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
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
        for (int x = hx - 2; x <= hx + 4; x++) {
            BlockPos p = new BlockPos(x, y0, STAGE_Z + 6);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof LunarRegolithBlock) {
                level.setBlock(p, LunarRegolithBlock.printFor(st, p, Direction.EAST), Block.UPDATE_CLIENTS);
            }
        }
    }

    static List<ShowcaseDriver.Shot> shots(BiFunction<Double, Double, Double> ground) {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        double y = Math.floor(ground.apply((double) STAGE_X, (double) STAGE_Z) - 1) + 1;
        s.add(new ShowcaseDriver.Shot("20_geology_display", 0, STAGE_X + 0.5, y + 2.6, STAGE_Z + 2.5, 180F, 14F, true, fp, 80,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.12), null));
        s.add(new ShowcaseDriver.Shot("21_lunar_hut", 0, STAGE_X + 0.5, y + 4.5, STAGE_Z - 5.5, -30F, 16F, true, fp, 80,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.08), null));
        s.add(new ShowcaseDriver.Shot("22_bootprints", 0, STAGE_X + 4.5, y + 4.0, STAGE_Z + 16.5, 160F, 40F, true, fp, 60,
                p -> PlanetTime.setDayFraction(p.serverLevel(), 0.1), null));
        return s;
    }
}
