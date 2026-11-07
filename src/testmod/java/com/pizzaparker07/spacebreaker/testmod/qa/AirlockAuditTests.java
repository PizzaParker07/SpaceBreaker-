package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks;
import com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Airlock audit with the real room flood fill: a base room behind an inner sliding door, a small chamber, and an outer
 * sliding door to the vacuum. Through a full door cycle (by hand and by redstone) the base room must never be open to the
 * outside. A second build with a two-door-wide airlock, and a cycle cancelled half way, are measured and written to
 * {@code run-gametest/qa/airlock.md}; only the single-door rule is asserted.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirlockAuditTests {
    private static final int CYCLE = SlidingDoorBlock.CYCLE_TICKS;
    private static final List<String> REPORT = new ArrayList<>();

    /** Hull x 0..6, y 1..5, z 0..9; base room z 1..4, partition wall z 5 (inner doors), chamber z 6..8, outer wall z 9. */
    private static void build(GameTestHelper h, int... doorXs) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 0; z <= 9; z++) {
                    boolean shell = x == 0 || x == 6 || y == 1 || y == 5 || z == 0 || z == 9 || z == 5;
                    level.setBlock(h.absolutePos(new BlockPos(x, y, z)),
                            shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        var door = BaseBlocks.SLIDING_DOOR.get().defaultBlockState().setValue(SlidingDoorBlock.FACING, Direction.SOUTH);
        for (int x : doorXs) {
            SlidingDoorBlock.place(level, h.absolutePos(new BlockPos(x, 2, 5)), door);
            SlidingDoorBlock.place(level, h.absolutePos(new BlockPos(x, 2, 9)), door);
        }
    }

    /** A fresh, complete scan of the base room. */
    private static SealedRoom.State room(GameTestHelper h) {
        SealedRoom r = new SealedRoom(h.absolutePos(new BlockPos(3, 3, 2)), 400);
        while (!r.tick(h.getLevel())) {
            // runs to the end within a few budgets
        }
        return r.state();
    }

    private static boolean open(GameTestHelper h, int x, int z) {
        return h.getLevel().getBlockState(h.absolutePos(new BlockPos(x, 2, z))).getValue(SlidingDoorBlock.OPEN);
    }

    private static void click(GameTestHelper h, ServerPlayer p, int x, int z) {
        BlockPos pos = h.absolutePos(new BlockPos(x, 2, z));
        h.getLevel().getBlockState(pos).useWithoutItem(h.getLevel(), p, new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false));
    }

    @GameTest(template = "empty", timeoutTicks = 4 * CYCLE + 100)
    public static void airlockKeepsTheBaseSealedThroughACycle(GameTestHelper h) {
        build(h, 3);
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        h.assertTrue(room(h) == SealedRoom.State.SEALED, "both doors shut: the base is sealed");
        click(h, p, 3, 5);
        h.assertTrue(open(h, 3, 5), "inner door opens at once while the outer one is shut");
        h.assertTrue(room(h) == SealedRoom.State.SEALED, "inner door open: base and chamber are one sealed room");
        click(h, p, 3, 9);
        h.assertFalse(open(h, 3, 5), "asking for the outer door shuts the inner one first");
        h.assertTrue(room(h) == SealedRoom.State.SEALED, "during the cycle the base stays sealed");
        // every tick of the cycle and after it, by hand, then the way back in by redstone
        for (int t = 1; t <= CYCLE + 10; t++) {
            int tick = t;
            h.runAtTickTime(t, () -> h.assertTrue(room(h) == SealedRoom.State.SEALED, "base sealed at cycle tick " + tick));
        }
        h.runAtTickTime(CYCLE + 12, () -> {
            h.assertTrue(open(h, 3, 9) && !open(h, 3, 5), "after the cycle the outer door is open, the inner one shut");
            h.getLevel().setBlock(h.absolutePos(new BlockPos(3, 4, 5)), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        });
        for (int t = CYCLE + 13; t <= 2 * CYCLE + 25; t++) {
            int tick = t;
            h.runAtTickTime(t, () -> h.assertTrue(room(h) == SealedRoom.State.SEALED, "base sealed during the redstone cycle, tick " + tick));
        }
        h.runAtTickTime(2 * CYCLE + 30, () -> {
            h.assertTrue(open(h, 3, 5) && !open(h, 3, 9), "redstone on the inner door: outer shut, inner open after the cycle");
            REPORT.add("- single-door airlock, by hand and by redstone: base sealed on every tick of both cycles ✅");
            ProgressionReport.write("airlock.md", "## Airlock audit\n\n" + String.join("\n", REPORT) + "\n");
            h.succeed();
        });
    }

    /** Two doors side by side in each wall: each pair only sees the door in its own line. Measured, not asserted. */
    @GameTest(template = "empty", timeoutTicks = 2 * CYCLE + 60)
    public static void wideAirlockIsMeasured(GameTestHelper h) {
        build(h, 2, 3);
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        click(h, p, 2, 5);
        click(h, p, 3, 5);
        click(h, p, 2, 9); // left outer: shuts left inner, right inner stays open?
        h.runAtTickTime(CYCLE + 10, () -> {
            boolean leaks = room(h) == SealedRoom.State.LEAKING;
            REPORT.add("- two-door-wide airlock (both inner leaves open, then the left outer leaf asked to open): "
                    + (leaks ? "the right inner leaf stays open while the left outer leaf opens → base LEAKS ❌"
                    : "base stays sealed ✅")
                    + " (inner " + open(h, 2, 5) + "/" + open(h, 3, 5) + ", outer " + open(h, 2, 9) + "/" + open(h, 3, 9) + ")");
            ProgressionReport.write("airlock.md", "## Airlock audit\n\n" + String.join("\n", REPORT) + "\n");
            Spacebreaker.LOGGER.info("QA airlock: wide airlock leaks = {}", leaks);
            h.succeed();
        });
    }

    /** Asking the outer door to shut again during its cycle: does it still open at the end? Measured, not asserted. */
    @GameTest(template = "empty", timeoutTicks = 2 * CYCLE + 60)
    public static void cancelledCycleIsMeasured(GameTestHelper h) {
        build(h, 3);
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        click(h, p, 3, 5);
        click(h, p, 3, 9);
        h.runAtTickTime(CYCLE / 2, () -> click(h, p, 3, 9));
        h.runAtTickTime(CYCLE + 10, () -> {
            boolean opened = open(h, 3, 9);
            REPORT.add("- outer door clicked again half way through its cycle: "
                    + (opened ? "it opens anyway at the end of the cycle (the second click is lost) ⚠️" : "it stays shut ✅")
                    + "; base " + room(h));
            ProgressionReport.write("airlock.md", "## Airlock audit\n\n" + String.join("\n", REPORT) + "\n");
            h.succeed();
        });
    }

    private AirlockAuditTests() {
    }
}
