package com.pizzaparker07.spacebreaker.testmod.blueprint;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.blueprint.Hologram;
import com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlueprintGameTests {
    private static final String EMPTY = "empty";
    private static final ResourceLocation HABITAT = Spacebreaker.id("pressurised_habitat");
    private static final ResourceLocation LAUNCH = Spacebreaker.id("launch_site");

    private BlueprintGameTests() {
    }

    private static MultiblockDefinition def(GameTestHelper helper, ResourceLocation id) {
        return Multiblocks.get(helper.getLevel().registryAccess(), id).orElseThrow(() -> new AssertionError("multiblock " + id + " not loaded"));
    }

    @GameTest(template = EMPTY)
    public static void exampleBlueprintsLoad(GameTestHelper helper) {
        MultiblockDefinition habitat = def(helper, HABITAT);
        MultiblockDefinition launch = def(helper, LAUNCH);
        helper.assertTrue(habitat.height() == 4 && launch.height() == 6, "layer counts");
        helper.assertTrue(habitat.placements(BlockPos.ZERO, Direction.NORTH).size() == 5 * 5 * 4, "habitat lists every non-blank cell");
        helper.assertTrue(launch.placements(BlockPos.ZERO, Direction.NORTH).size() == 25 + 6 + 2, "launch site: pad 5x5, tower 6, two attachments");
        helper.assertTrue(Multiblocks.all(helper.getLevel().registryAccess()).size() >= 2, "registry lists the blueprints");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void validatorAcceptsAndPinpointsProblems(GameTestHelper helper) {
        MultiblockDefinition habitat = def(helper, HABITAT);
        BlockPos controller = helper.absolutePos(new BlockPos(3, 2, 3));
        Multiblocks.place(helper.getLevel(), habitat, controller, Direction.NORTH);
        Multiblocks.Validation ok = Multiblocks.validate(helper.getLevel(), habitat, controller, Direction.NORTH);
        helper.assertTrue(ok.complete(), "a habitat built from its blueprint validates, problems: " + ok.problems());
        // knock a hole into the west wall and swap a floor block for dirt
        BlockPos hole = helper.absolutePos(new BlockPos(1, 3, 3));
        BlockPos floor = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.getLevel().setBlock(hole, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(floor, Blocks.DIRT.defaultBlockState(), 3);
        Multiblocks.Validation bad = Multiblocks.validate(helper.getLevel(), habitat, controller, Direction.NORTH);
        helper.assertTrue(bad.problems().size() == 2, "exactly two problems, got " + bad.problems());
        var missing = bad.problems().stream().filter(Multiblocks.Problem::missing).findFirst().orElseThrow();
        var wrong = bad.problems().stream().filter(p -> !p.missing()).findFirst().orElseThrow();
        helper.assertTrue(missing.pos().equals(hole), "missing block reported at " + hole + ", got " + missing.pos());
        helper.assertTrue(wrong.pos().equals(floor) && wrong.actual().is(Blocks.DIRT), "wrong block reported at " + floor);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void validatorFollowsRotation(GameTestHelper helper) {
        MultiblockDefinition launch = def(helper, LAUNCH);
        BlockPos controller = helper.absolutePos(new BlockPos(3, 1, 4));
        Multiblocks.place(helper.getLevel(), launch, controller, Direction.NORTH);
        helper.assertTrue(Multiblocks.validate(helper.getLevel(), launch, controller, Direction.NORTH).complete(), "launch site facing north");
        helper.assertFalse(Multiblocks.validate(helper.getLevel(), launch, controller, Direction.EAST).complete(), "not complete when turned");
        helper.assertTrue(Multiblocks.validateAnyFacing(helper.getLevel(), launch, controller).facing() == Direction.NORTH, "facing is detected");
        BlockPos towerTop = helper.absolutePos(new BlockPos(3, 6, 1));
        helper.getLevel().setBlock(towerTop, Blocks.AIR.defaultBlockState(), 3);
        var v = Multiblocks.validate(helper.getLevel(), launch, controller, Direction.NORTH);
        helper.assertTrue(v.problems().size() == 1 && v.problems().get(0).pos().equals(towerTop), "the missing tower top is pinpointed: " + v.problems());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void controllerFindsItsBlueprint(GameTestHelper helper) {
        var state = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Spacebreaker.id("oxygen_distributor")).defaultBlockState();
        var defs = Multiblocks.controlledBy(helper.getLevel().registryAccess(), state);
        helper.assertTrue(defs.stream().anyMatch(e -> e.getKey().equals(HABITAT)), "the distributor controls the habitat blueprint");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void selfAssemblingFlagParses(GameTestHelper helper) {
        String json = "{\"controller\": \"C\", \"self_assembling\": true, \"layers\": [[\"PCP\"]], "
                + "\"key\": {\"P\": {\"block\": \"minecraft:stone\"}, \"C\": {\"block\": \"minecraft:furnace\"}}}";
        var parsed = MultiblockDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json));
        MultiblockDefinition d = parsed.getOrThrow();
        helper.assertTrue(d.selfAssembling(), "self_assembling is read");
        var ps = d.placements(BlockPos.ZERO, Direction.NORTH);
        helper.assertTrue(ps.stream().filter(d::isController).count() == 1, "exactly one controller placement");
        helper.assertFalse(def(helper, HABITAT).selfAssembling(), "defaults to false");
        String bad = "{\"controller\": \"C\", \"layers\": [[\"PP\"]], \"key\": {\"P\": {\"block\": \"minecraft:stone\"}}}";
        helper.assertTrue(MultiblockDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString(bad)).error().isPresent(), "a missing controller is rejected");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void hologramStateCycles(GameTestHelper helper) {
        Hologram h = Hologram.of(HABITAT).withAnchor(BlockPos.ZERO, Direction.NORTH);
        helper.assertTrue(h.rotated(1).facing() == Direction.EAST && h.rotated(3).facing() == Direction.WEST, "rotation steps");
        Hologram l = h;
        for (int i = 0; i < 4; i++) {
            l = l.nextLayer(4);
            helper.assertTrue(l.layer() == i, "layer " + i);
        }
        helper.assertTrue(l.nextLayer(4).layer() == -1, "back to all layers");
        helper.assertTrue(h.cleared().anchor().isEmpty(), "clearing removes the anchor");
        helper.succeed();
    }
}
