package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitNetwork;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * How much a big base's cable and duct networks cost per tick: 40 conduits with an endpoint on three sides each
 * (120 endpoints), a hundred offers of energy per tick (a hundred generators) and a hundred items pushed into a duct.
 * Not required (timings depend on the machine); the numbers are logged with a [perf] prefix. Measured 2026-11:
 * energy 2.6 -> 0.5 ms/tick, items 2.4 -> 0.7 ms/tick after per-tick endpoint resolution and round-robin delivery.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConduitPerfTests {
    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final int LENGTH = 40;
    /** Let the JIT compile the hot path first: a running server has done this long before a base gets big. */
    private static final int WARMUP = 20_000;
    private static final int RUNS = 20_000;

    private static void place(GameTestHelper h, Block block, BlockPos pos) {
        BlockPos abs = h.absolutePos(pos);
        h.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(block.defaultBlockState(), h.getLevel(), abs), 3);
    }

    private static void line(GameTestHelper h, Block conduit, Block endpoint) {
        for (int x = 0; x < LENGTH; x++) {
            h.setBlock(new BlockPos(x, 1, 1), endpoint);
            h.setBlock(new BlockPos(x, 1, 3), endpoint);
            h.setBlock(new BlockPos(x, 2, 2), endpoint);
        }
        for (int x = 0; x < LENGTH; x++) {
            place(h, conduit, new BlockPos(x, 1, 2));
        }
    }

    @GameTest(template = "empty", batch = "industry_perf", timeoutTicks = 200, required = false)
    public static void bigEnergyNetworkIsCheap(GameTestHelper h) {
        line(h, MachinesModule.HV_CABLE.get(), MachinesModule.CRUSHER.block().get());
        long[] spent = new long[1];
        int[] measured = new int[1];
        // a hundred generators offering power every tick for 120 ticks; the first 40 only warm up
        for (int t = 0; t < 120; t++) {
            int tick = t;
            h.runAfterDelay(2 + t, () -> {
                IEnergyStorage in = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(new BlockPos(0, 1, 2)), Direction.WEST);
                long t0 = System.nanoTime();
                for (int i = 0; i < 100; i++) {
                    in.receiveEnergy(40, false);
                }
                if (tick >= 40) {
                    spent[0] += System.nanoTime() - t0;
                    measured[0]++;
                }
            });
        }
        h.runAfterDelay(125, () -> {
            ConduitBlockEntity first = h.getBlockEntity(new BlockPos(0, 1, 2));
            ConduitNetwork net = first.network();
            h.assertTrue(net != null && net.endpoints().size() >= 3 * LENGTH, "endpoints " + (net == null ? 0 : net.endpoints().size()));
            double perTickMs = spent[0] / 1e6 / measured[0];
            LOG.info("[perf] energy network, {} endpoints: {} ms per tick for 100 offers", net.endpoints().size(), String.format("%.3f", perTickMs));
            h.assertTrue(perTickMs < 2.0, "energy network too slow: " + perTickMs + " ms/tick");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "industry_perf_items", timeoutTicks = 100, required = false)
    public static void bigItemNetworkIsCheap(GameTestHelper h) {
        line(h, MachinesModule.ITEM_DUCT.get(), Blocks.CHEST);
        BlockPos firstPos = h.absolutePos(new BlockPos(0, 1, 2));
        h.setBlock(new BlockPos(0, 1, 2).west(), Blocks.CHEST);
        h.getLevel().setBlock(firstPos, Block.updateFromNeighbourShapes(MachinesModule.ITEM_DUCT.get().defaultBlockState(), h.getLevel(), firstPos), 3);
        h.getLevel().setBlock(firstPos, h.getLevel().getBlockState(firstPos).setValue(ConduitBlock.SIDES.get(Direction.WEST), ConduitSide.EXTRACT), 3);
        ChestBlockEntity src = (ChestBlockEntity) h.getBlockEntity(new BlockPos(0, 1, 2).west());
        h.runAfterDelay(2, () -> {
            ConduitBlockEntity first = h.getBlockEntity(new BlockPos(0, 1, 2));
            ConduitNetwork net = first.network();
            h.assertTrue(net != null && net.endpoints().size() >= 3 * LENGTH, "endpoints " + (net == null ? 0 : net.endpoints().size()));
            var in = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, firstPos.east(), Direction.WEST);
            for (int i = 0; i < WARMUP; i++) {
                net.distributeItem(new ItemStack(Items.COBBLESTONE), true, null);
            }
            // a hundred hoppers' worth of single items pushed in (one tick of a busy sorting floor), twenty times
            long t0 = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                in.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
            }
            double perTickMs = (System.nanoTime() - t0) / 1e6 / 20;
            LOG.info("[perf] item network, {} endpoints: {} ms per tick for 100 inserts", net.endpoints().size(), String.format("%.3f", perTickMs));
            src.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
            h.assertTrue(perTickMs < 2.0, "item network too slow: " + perTickMs + " ms/tick");
            h.succeed();
        });
    }

    private ConduitPerfTests() {
    }
}
