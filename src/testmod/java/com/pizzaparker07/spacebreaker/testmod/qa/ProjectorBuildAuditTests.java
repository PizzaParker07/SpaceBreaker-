package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintBuilder;
import com.pizzaparker07.spacebreaker.content.blueprint.Hologram;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QA (beta.1 feedback, "the hologram projector does not place automatically"): the projector's Build action, checked
 * beyond the suit team's own tests. Every blueprint builds completely in creative mode and validates; a survival build
 * takes from the inventory exactly what it places (half the materials: half placed, the rest listed as missing, extra
 * items untouched); a build stops when the player walks away and keeps what was placed. Writes
 * {@code run-gametest/qa/projector_build.md}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProjectorBuildAuditTests {
    private static final String BATCH = "qa_projector_build";
    private static final ResourceLocation LAUNCH = Spacebreaker.id("launch_site");

    /** Each blueprint built by a creative player far from the test grid, then validated block by block. */
    @GameTest(template = "empty", timeoutTicks = 3000, batch = BATCH)
    public static void everyBlueprintBuildsCompletely(GameTestHelper h) {
        var level = h.getLevel();
        var all = Multiblocks.all(level.registryAccess());
        h.assertFalse(all.isEmpty(), "there are blueprints");
        record Run(ResourceLocation id, MultiblockDefinition def, BlockPos controller, ServerPlayer player, BlockPos centre) {
        }
        List<Run> runs = new ArrayList<>();
        int slot = 0;
        for (var e : all) {
            BlockPos centre = StationAuditTests.away(h, 10 + slot++);
            BlockPos controller = centre.above(4);
            floorUnder(level, e.getValue(), controller);
            ServerPlayer p = player(h, GameType.CREATIVE, controller.offset(0, 0, 12));
            boolean started = BlueprintBuilder.start(p, Hologram.of(e.getKey()).withAnchor(controller, Direction.NORTH));
            h.assertTrue(started, "the build of " + e.getKey() + " starts");
            runs.add(new Run(e.getKey(), e.getValue(), controller, p, centre));
        }
        h.succeedWhen(() -> {
            List<String> out = new ArrayList<>();
            for (Run r : runs) {
                var res = BlueprintBuilder.lastResult(r.player());
                h.assertTrue(res != null, r.id() + " still building");
                var v = Multiblocks.validate(level, r.def(), r.controller(), Direction.NORTH);
                out.add(String.format("- %s: placed %d, missing %s, blocked %d, complete %s; validation %d/%d%s", r.id().getPath(),
                        res.placed(), res.missing(), res.blocked(), res.complete(), v.built(), v.total(),
                        v.complete() ? "" : " **problems: " + v.problems().stream().limit(3).map(pr -> pr.pos() + " wants " + pr.expected()
                                + " has " + pr.actual()).toList() + "**"));
                h.assertTrue(res.complete() && !res.stopped() && res.missing().isEmpty(), r.id() + " built completely in creative: " + res);
                h.assertTrue(v.complete(), r.id() + " validates after the build: " + v.problems());
                h.assertTrue(r.player().getInventory().isEmpty(), r.id() + ": creative build gives nothing back");
            }
            ProgressionReport.write("projector_build.md", "## Projector build (`qa/ProjectorBuildAuditTests`)\n\nEvery blueprint, creative:\n\n"
                    + String.join("\n", out) + "\n");
            for (Run r : runs) {
                level.getServer().getPlayerList().remove(r.player());
                StationAuditTests.clear(level, r.centre());
            }
        });
    }

    /** Half of every material and some extras: half is placed, the rest is listed, nothing else is touched. */
    @GameTest(template = "empty", timeoutTicks = 600, batch = BATCH)
    public static void survivalBuildTakesExactlyWhatItPlaces(GameTestHelper h) {
        var level = h.getLevel();
        var def = Multiblocks.get(level.registryAccess(), LAUNCH).orElseThrow();
        Map<Item, Integer> need = materials(def);
        Map<Item, Integer> give = new LinkedHashMap<>();
        need.forEach((item, n) -> give.put(item, n / 2));
        BlockPos centre = StationAuditTests.away(h, 40);
        BlockPos controller = centre.above(4);
        floorUnder(level, def, controller);
        ServerPlayer p = player(h, GameType.SURVIVAL, controller.offset(0, 0, 8));
        give.forEach((item, n) -> {
            if (n > 0) {
                p.getInventory().add(new ItemStack(item, n));
            }
        });
        p.getInventory().add(new ItemStack(Items.DIRT, 64));     // not part of the blueprint
        p.getInventory().add(new ItemStack(Items.IRON_INGOT, 7));
        h.assertTrue(BlueprintBuilder.start(p, Hologram.of(LAUNCH).withAnchor(controller, Direction.NORTH)), "the build starts");
        h.succeedWhen(() -> {
            var res = BlueprintBuilder.lastResult(p);
            h.assertTrue(res != null, "still building");
            int given = give.values().stream().mapToInt(Integer::intValue).sum();
            h.assertTrue(res.placed() == given, "placed as many blocks as were carried: " + res.placed() + " of " + given);
            for (var e : need.entrySet()) {
                int left = p.getInventory().countItem(e.getKey());
                h.assertTrue(left == 0, "every carried " + e.getKey() + " used, " + left + " left");
                int short_ = e.getValue() - give.get(e.getKey());
                h.assertTrue(res.missing().getOrDefault(e.getKey(), 0) == short_, "missing " + e.getKey() + ": " + short_ + ", listed "
                        + res.missing().getOrDefault(e.getKey(), 0));
            }
            h.assertTrue(p.getInventory().countItem(Items.DIRT) == 64 && p.getInventory().countItem(Items.IRON_INGOT) == 7, "extras untouched");
            h.assertFalse(res.complete(), "not complete with half the materials");
            ProgressionReport.write("projector_build_survival.md", "## Projector build, half the materials\n\n- placed " + res.placed()
                    + " of " + need.values().stream().mapToInt(Integer::intValue).sum() + ", missing " + res.missing() + "\n");
            level.getServer().getPlayerList().remove(p);
            StationAuditTests.clear(level, centre);
        });
    }

    /** The job stops when the player walks away; what was placed stays and was paid for. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = BATCH)
    public static void buildStopsWhenThePlayerWalksAway(GameTestHelper h) {
        var level = h.getLevel();
        var def = Multiblocks.get(level.registryAccess(), LAUNCH).orElseThrow();
        Map<Item, Integer> need = materials(def);
        BlockPos centre = StationAuditTests.away(h, 41);
        BlockPos controller = centre.above(4);
        floorUnder(level, def, controller);
        ServerPlayer p = player(h, GameType.SURVIVAL, controller.offset(0, 0, 8));
        need.forEach((item, n) -> p.getInventory().add(new ItemStack(item, n)));
        int carried = need.values().stream().mapToInt(Integer::intValue).sum();
        h.assertTrue(BlueprintBuilder.start(p, Hologram.of(LAUNCH).withAnchor(controller, Direction.NORTH)), "the build starts");
        h.runAfterDelay(20, () -> p.teleportTo(controller.getX() + 80.5, controller.getY(), controller.getZ() + 0.5));
        h.succeedWhen(() -> {
            var res = BlueprintBuilder.lastResult(p);
            h.assertTrue(res != null, "still building");
            h.assertTrue(res.stopped() && !res.complete(), "stopped, not complete: " + res);
            h.assertTrue(res.placed() > 0 && res.placed() < carried, "some blocks placed before leaving: " + res.placed());
            int left = need.keySet().stream().mapToInt(i -> p.getInventory().countItem(i)).sum();
            h.assertTrue(carried - left == res.placed(), "paid exactly for what was placed: used " + (carried - left) + ", placed " + res.placed());
            h.assertFalse(BlueprintBuilder.building(p), "no job left running");
            level.getServer().getPlayerList().remove(p);
            StationAuditTests.clear(level, centre);
        });
    }

    private static Map<Item, Integer> materials(MultiblockDefinition def) {
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (var pl : def.placements(BlockPos.ZERO, Direction.NORTH)) {
            if (!pl.spec().isAir()) {
                totals.merge(pl.spec().item(), 1, Integer::sum);
            }
        }
        return totals;
    }

    /** Stone under the blueprint's lowest layer, so blocks that need support have it. */
    private static void floorUnder(net.minecraft.server.level.ServerLevel level, MultiblockDefinition def, BlockPos controller) {
        var placements = def.placements(controller, Direction.NORTH);
        int minY = placements.stream().mapToInt(pl -> pl.pos().getY()).min().orElse(controller.getY());
        for (var pl : placements) {
            level.setBlock(new BlockPos(pl.pos().getX(), minY - 1, pl.pos().getZ()), Blocks.STONE.defaultBlockState(), 2);
        }
    }

    private static ServerPlayer player(GameTestHelper h, GameType mode, BlockPos at) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(mode);
        p.getInventory().clearContent();
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private ProjectorBuildAuditTests() {
    }
}
