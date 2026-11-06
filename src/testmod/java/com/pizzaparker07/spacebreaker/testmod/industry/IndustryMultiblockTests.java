package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.MultiblockMachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.PartBlock;
import com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;

/** Pumpjack and refinery are defined by data/spacebreaker/spacebreaker/multiblock/*.json (one source for build, check and hologram). */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryMultiblockTests {
    private static void check(GameTestHelper h, MultiblockMachineBlock block, Direction facing) {
        var def = block.definition(h.getLevel()).orElseThrow(() -> new AssertionError("no multiblock JSON for " + block));
        BlockPos pos = new BlockPos(3, 1, 3);
        BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, facing);
        h.setBlock(pos, state);
        BlockPos abs = h.absolutePos(pos);
        // the JSON layout matches the built-in fallback layout
        var json = new HashSet<>(block.partPositions(h.getLevel(), abs, facing));
        var builtin = new HashSet<>(block.partPositions(abs, facing));
        h.assertTrue(json.equals(builtin), "JSON layout differs from the built-in one for " + facing + ": " + json + " vs " + builtin);
        h.assertFalse(Multiblocks.validate(h.getLevel(), def, abs, facing).complete(), "an unbuilt structure must not validate");
        block.build(h.getLevel(), abs, h.getBlockState(pos));
        var v = Multiblocks.validate(h.getLevel(), def, abs, facing);
        h.assertTrue(v.complete(), "built " + block + " facing " + facing + " fails its JSON: " + v.problems());
        h.assertTrue(block.isComplete(h.getLevel(), abs, h.getBlockState(pos)), "machine does not see itself complete");
        // knock out one part: the machine notices
        BlockPos part = block.partPositions(h.getLevel(), abs, facing).get(0);
        h.getLevel().setBlock(part, Blocks.STONE.defaultBlockState(), 3);
        h.assertFalse(block.isComplete(h.getLevel(), abs, h.getBlockState(pos)), "a missing part must be noticed");
        h.assertTrue(Multiblocks.controlledBy(h.getLevel().registryAccess(), h.getBlockState(pos)).stream()
                .anyMatch(e -> e.getKey().getPath().equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath())),
                "hologram lookup by controller");
    }

    @GameTest(template = "empty")
    public static void pumpjackFollowsItsJsonEveryFacing(GameTestHelper h) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            check(h, MachinesModule.PUMPJACK.block().get(), d);
            for (int x = 0; x < 7; x++) {
                for (int y = 1; y < 5; y++) {
                    for (int z = 0; z < 7; z++) {
                        h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void refineryFollowsItsJson(GameTestHelper h) {
        check(h, MachinesModule.REFINERY.block().get(), Direction.EAST);
        h.succeed();
    }

    private IndustryMultiblockTests() {
    }
}
