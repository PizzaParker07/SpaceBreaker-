package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationHatchBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Station stage 1 (QA): a core with a docking module on every side mouth, all one sealed room; a breach in a tunnel
 * vents the whole station and mending it seals it again; a module that does not fit is refused without leaving any
 * blocks behind. Writes {@code run-gametest/qa/station.md}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationAuditTests {
    /**
     * Own batch, named so that it runs after the rocketry station batches: GameTest places batches one after another on
     * one grid, and an extra batch before them shifts their tests next to stations other tests leave standing.
     */
    static final String QA_BATCH = "qa_space_station";

    @GameTest(template = "empty", timeoutTicks = 600, batch = QA_BATCH)
    public static void fourDockingModulesShareTheCoreAir(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos centre = away(h, 0);
        Direction forward = Direction.SOUTH;
        h.assertTrue(StationModule.buildCore(level, centre, forward, "qa"), "the core unfolds");
        List<String> out = new ArrayList<>();
        List<BlockPos> tunnelWalls = new ArrayList<>();
        int attached = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos mouth = centre.relative(side, 3).above(2);
            var s = level.getBlockState(mouth);
            boolean isMouth = s.getBlock() instanceof StationHatchBlock && s.getValue(StationHatchBlock.CENTER);
            boolean ok = isMouth && StationModule.DOCKING.attach(level, mouth, "qa");
            out.add("- docking module on the " + side.getName() + " mouth: " + (ok ? "attached" : isMouth ? "**refused**" : "no mouth there"));
            if (ok) {
                attached++;
                // a hull block in the tunnel's side wall, half way along
                tunnelWalls.add(mouth.relative(side, 4).relative(side.getClockWise(), 2));
            }
        }
        h.assertTrue(attached == 4, "a docking module on each of the four side mouths: " + out);
        BlockPos dist = centre.relative(forward.getOpposite(), 2).relative(forward.getClockWise(), -2).above();
        BlockPos breach = tunnelWalls.get(0);
        int[] phase = {0};
        int[] sealedVolume = {0};
        String[] last = {""};
        h.onEachTick(() -> {
            if (!(level.getBlockEntity(dist) instanceof OxygenDistributorBlockEntity be)) {
                return;
            }
            var room = be.room();
            last[0] = (room.sealed() ? "sealed " : "not sealed ") + room.volume();
            switch (phase[0]) {
                case 0 -> {
                    if (room.sealed() && room.volume() > 300) {
                        sealedVolume[0] = room.volume();
                        out.add("- core + four tunnels sealed as one room: " + room.volume() + " blocks of air");
                        level.setBlock(breach, Blocks.AIR.defaultBlockState(), 3); // a hole in the far tunnel's wall
                        phase[0] = 1;
                    }
                }
                case 1 -> {
                    if (!room.sealed()) {
                        out.add("- a hole in one tunnel's wall vents the whole station (core included)");
                        level.setBlock(breach, RocketryModule.STATION_HULL.get().defaultBlockState(), 3);
                        phase[0] = 2;
                    }
                }
                case 2 -> {
                    if (room.sealed() && room.volume() == sealedVolume[0]) {
                        out.add("- mended with station hull: sealed again, same volume");
                        phase[0] = 3;
                    }
                }
                default -> {
                }
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(phase[0] == 3, "phase " + phase[0] + ", room " + last[0] + ": " + out);
            ProgressionReport.write("station.md", "## Station stage 1 (`qa/StationAuditTests`)\n\n" + String.join("\n", out) + "\n");
            clear(level, centre);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = QA_BATCH)
    public static void aModuleThatDoesNotFitLeavesNothingBehind(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos centre = away(h, 1);
        Direction forward = Direction.SOUTH;
        h.assertTrue(StationModule.buildCore(level, centre, forward, "qa"), "the core unfolds");
        BlockPos mouth = centre.relative(forward, 3).above(2);
        // a stone pillar four blocks out, where the docking tunnel would run
        BlockPos rock = mouth.relative(forward, 4);
        level.setBlock(rock, Blocks.STONE.defaultBlockState(), 3);
        BlockPos probe = mouth.relative(forward, 2).relative(forward.getClockWise(), 2);
        var before = level.getBlockState(probe);
        h.assertFalse(StationModule.DOCKING.attach(level, mouth, "qa"), "a module is refused where rock is in the way");
        h.assertTrue(level.getBlockState(mouth).getBlock() instanceof StationHatchBlock, "the mouth stays closed");
        h.assertTrue(level.getBlockState(probe) == before, "no hull left half-built");
        level.setBlock(rock, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(StationModule.DOCKING.attach(level, mouth, "qa"), "with the rock gone it attaches");
        clear(level, centre);
        h.succeed();
    }

    /**
     * Station stage 2 airlock (QA): core + airlock module on a side mouth. Opens the doors the way a player would (a
     * click on each) in the order of a careless crew — inner, then outer while the inner is still open — and records
     * when the station stays sealed. Written to {@code run-gametest/qa/station_airlock.md}; only "both shut: sealed"
     * and "outer open, inner shut: sealed" are asserted, the careless case is measured.
     */
    @GameTest(template = "empty", timeoutTicks = 600, batch = QA_BATCH)
    public static void stationAirlockCycle(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos centre = away(h, 2);
        Direction forward = Direction.SOUTH;
        h.assertTrue(StationModule.buildCore(level, centre, forward, "qa"), "the core unfolds");
        BlockPos east = centre.relative(Direction.WEST, 3).above(2);
        h.assertTrue(StationModule.AIRLOCK.attach(level, east, "qa"), "the airlock attaches at a side");
        BlockPos inner = east.relative(Direction.WEST, 1).below(), outer = east.relative(Direction.WEST, 5).below();
        h.assertTrue(level.getBlockState(inner).getBlock() instanceof com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock
                && level.getBlockState(outer).getBlock() instanceof com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock, "two airlock doors");
        var player = h.makeMockServerPlayerInLevel();
        BlockPos dist = centre.relative(forward.getOpposite(), 2).relative(forward.getClockWise(), -2).above();
        List<String> out = new ArrayList<>();
        // steps: 0 both shut, 1 inner open, 2 inner + outer open, 3 inner shut (outer open), 4 all shut
        int[] step = {0};
        int[] settle = {0};
        String[] names = {"both doors shut", "inner door open", "inner and outer open", "outer open, inner shut", "both shut again"};
        h.onEachTick(() -> {
            if (step[0] > 4 || !(level.getBlockEntity(dist) instanceof OxygenDistributorBlockEntity be)) {
                return;
            }
            // give the room scan a moment after each change (the first scan of a new station takes longer)
            var room = be.room();
            if (++settle[0] < 40 || (step[0] == 0 && !room.sealed() && settle[0] < 300)) {
                return;
            }
            out.add(String.format("- %s: %s (%d blocks)%s", names[step[0]], room.sealed() ? "sealed" : "**open to space**", room.volume(),
                    step[0] == 2 && !room.sealed() ? " — the doors do not interlock: one click on each empties the station" : ""));
            if (step[0] == 0 || step[0] == 3 || step[0] == 4) {
                h.assertTrue(room.sealed(), names[step[0]] + " should keep the station sealed: " + out);
            }
            BlockPos click = switch (step[0]) {
                case 0 -> inner;
                case 1 -> outer;
                case 2 -> inner;
                case 3 -> outer;
                default -> null;
            };
            if (click != null) {
                level.getBlockState(click).useWithoutItem(level, player,
                        new net.minecraft.world.phys.BlockHitResult(click.getCenter(), Direction.EAST, click, false));
            }
            step[0]++;
            settle[0] = 0;
        });
        h.succeedWhen(() -> {
            h.assertTrue(step[0] > 4, "airlock steps done: " + out);
            ProgressionReport.write("station_airlock.md", "## Station airlock (`qa/StationAuditTests.stationAirlockCycle`)\n\n" + String.join("\n", out) + "\n");
            clear(level, centre);
        });
    }

    /**
     * Stations are big (a docking tunnel reaches 11 blocks out) and spill over neighbouring tests, so they are built far
     * east of the test grid, each in its own spot.
     */
    static BlockPos away(GameTestHelper h, int slot) {
        BlockPos base = h.absolutePos(BlockPos.ZERO);
        BlockPos c = new BlockPos(base.getX() + 4000 + slot * 64, 100, base.getZ());
        // keep the chunks loaded: the room scan treats an unloaded neighbour as open to space
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                h.getLevel().setChunkForced((c.getX() >> 4) + dx, (c.getZ() >> 4) + dz, true);
            }
        }
        return c;
    }

    /** Stations are built above the test's template, outside what the framework clears: remove them again. */
    static void clear(net.minecraft.server.level.ServerLevel level, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced((centre.getX() >> 4) + dx, (centre.getZ() >> 4) + dz, false);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -4, -16), centre.offset(16, 10, 16))) {
            if (!level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    private StationAuditTests() {
    }
}
