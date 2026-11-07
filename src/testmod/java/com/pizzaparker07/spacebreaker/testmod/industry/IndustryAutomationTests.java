package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.RegolithExcavatorBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mid-game automation: redstone control, filtered pipe ends, and a Moon base that makes LOX on its own. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryAutomationTests {
    private static MachineBlockEntity machine(GameTestHelper h, MachineEntry<?, ?> m, BlockPos pos, Direction facing) {
        h.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, facing));
        MachineBlockEntity be = h.getBlockEntity(pos);
        be.energy().generate(be.energy().getMaxEnergyStored());
        return be;
    }

    private static void pipe(GameTestHelper h, Block pipe, BlockPos pos) {
        BlockPos abs = h.absolutePos(pos);
        h.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(pipe.defaultBlockState(), h.getLevel(), abs), 3);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "industry_automation")
    public static void redstoneModesGateAMachine(GameTestHelper h) {
        h.assertTrue(MachineBlockEntity.allows(0, false) && MachineBlockEntity.allows(0, true), "ignore: always runs");
        h.assertTrue(MachineBlockEntity.allows(1, true) && !MachineBlockEntity.allows(1, false), "with signal");
        h.assertTrue(MachineBlockEntity.allows(2, false) && !MachineBlockEntity.allows(2, true), "without signal");
        MachineBlockEntity crusher = machine(h, MachinesModule.CRUSHER, new BlockPos(2, 1, 2), Direction.NORTH);
        crusher.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
        crusher.setRedstoneMode(1);
        h.runAfterDelay(30, () -> {
            h.assertTrue(crusher.progress() == 0 && crusher.data.get(6) == 9, "no signal: the crusher waits (status " + crusher.data.get(6) + ")");
            h.setBlock(new BlockPos(3, 1, 2), Blocks.REDSTONE_BLOCK);
        });
        h.runAfterDelay(60, () -> {
            h.assertTrue(crusher.progress() > 0 || !crusher.items().getStackInSlot(1).isEmpty(), "with a signal it runs");
            h.succeed();
        });
    }

    /** One pulling end, two tanks: the end filtered for lava gets no water, the plain one fills. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "industry_automation")
    public static void filteredPipeEndOnlyPassesItsFluid(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 2), MachinesModule.FLUID_TANK.get());
        TankBlockEntity source = h.getBlockEntity(new BlockPos(0, 1, 2));
        source.tank().fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
        h.setBlock(new BlockPos(2, 2, 2), MachinesModule.FLUID_TANK.get());
        h.setBlock(new BlockPos(3, 1, 2), MachinesModule.FLUID_TANK.get());
        TankBlockEntity filtered = h.getBlockEntity(new BlockPos(2, 2, 2));
        TankBlockEntity plain = h.getBlockEntity(new BlockPos(3, 1, 2));
        Block fluidPipe = MachinesModule.FLUID_PIPE.get();
        pipe(h, fluidPipe, new BlockPos(1, 1, 2));
        pipe(h, fluidPipe, new BlockPos(2, 1, 2));
        BlockPos first = h.absolutePos(new BlockPos(1, 1, 2));
        var st = h.getLevel().getBlockState(first);
        h.getLevel().setBlock(first, st.setValue(ConduitBlock.SIDES.get(Direction.WEST), ConduitSide.EXTRACT), 3);
        ConduitBlockEntity junction = h.getBlockEntity(new BlockPos(2, 1, 2));
        junction.setFilter(Direction.UP, Fluids.LAVA);
        h.succeedWhen(() -> {
            h.assertTrue(plain.tank().getFluidAmount() >= 2_000, "plain end fills: " + plain.tank().getFluidAmount());
            h.assertTrue(filtered.tank().isEmpty(), "the lava-only end lets no water through: " + filtered.tank().getFluidAmount());
        });
    }

    /**
     * Fully automatic LOX: regolith under an excavator, which feeds a regolith heater, whose oxygen flows into a
     * cryogenic liquefier, whose LOX flows into a cryogenic tank. Nobody touches anything; power comes in each tick.
     */
    @GameTest(template = "empty", timeoutTicks = 2400, batch = "industry_automation")
    public static void moonBaseMakesLoxOnItsOwn(GameTestHelper h) {
        Block regolith = BuiltInRegistries.BLOCK.get(Spacebreaker.id("lunar_regolith"));
        h.assertTrue(regolith != Blocks.AIR, "lunar regolith missing");
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 1; y <= 3; y++) {
                    h.setBlock(new BlockPos(x, y, z), regolith);
                }
            }
        }
        MachineBlockEntity excavator = machine(h, MachinesModule.REGOLITH_EXCAVATOR, new BlockPos(2, 4, 2), Direction.NORTH);
        MachineBlockEntity heater = machine(h, MachinesModule.REGOLITH_HEATER, new BlockPos(3, 4, 2), Direction.NORTH);
        MachineBlockEntity liquefier = machine(h, MachinesModule.LIQUEFIER, new BlockPos(4, 4, 2), Direction.NORTH);
        h.setBlock(new BlockPos(5, 4, 2), MachinesModule.CRYOGENIC_TANK.get());
        TankBlockEntity tank = h.getBlockEntity(new BlockPos(5, 4, 2));
        h.succeedWhen(() -> {
            for (MachineBlockEntity m : new MachineBlockEntity[]{excavator, heater, liquefier}) {
                m.energy().generate(2_000);
            }
            h.assertTrue(tank.tank().getFluid().is(FluidsModule.TAG_LOX) && tank.tank().getFluidAmount() >= 500,
                    "LOX in the tank: " + tank.tank().getFluid().getFluid() + " " + tank.tank().getFluidAmount() + " (heater " + heater.tank(0).getFluidAmount()
                            + "/" + heater.tank(1).getFluidAmount() + ", liquefier " + liquefier.tank(0).getFluidAmount() + "/" + liquefier.tank(1).getFluidAmount() + ")");
            h.assertTrue(h.getBlockState(new BlockPos(2, 3, 2)).isAir(), "the excavator dug the ground under it");
        });
    }

    @GameTest(template = "empty", batch = "industry_automation")
    public static void excavatorWorksLayerByLayer(GameTestHelper h) {
        BlockPos m = new BlockPos(0, 100, 0);
        h.assertTrue(RegolithExcavatorBlockEntity.target(m, 0).equals(new BlockPos(-2, 99, -2)), "first block: corner of the top layer");
        h.assertTrue(RegolithExcavatorBlockEntity.target(m, 24).equals(new BlockPos(2, 99, 2)), "end of the first layer");
        h.assertTrue(RegolithExcavatorBlockEntity.target(m, 25).equals(new BlockPos(-2, 98, -2)), "then one layer down");
        h.succeed();
    }

    /**
     * Cargo rockets unload only into chests, barrels and shulker boxes near their port, never into machines. No
     * industry block may carry those tags (a heater or RTG would swallow cargo); a vanilla chest beside them still
     * takes items through the block item capability.
     */
    @GameTest(template = "empty", batch = "industry_automation")
    public static void cargoSkipsIndustryMachines(GameTestHelper h) {
        java.util.List<Block> industry = new java.util.ArrayList<>();
        MachinesModule.MACHINES.forEach(m -> industry.add(m.block().get()));
        MachinesModule.TANKS.forEach(t -> industry.add(t.get()));
        industry.add(MachinesModule.SOLAR_PANEL.get());
        industry.add(MachinesModule.SOLAR_ARRAY.get());
        industry.add(MachinesModule.POWER_MONITOR.get());
        for (Block b : industry) {
            var state = b.defaultBlockState();
            h.assertTrue(!state.is(net.neoforged.neoforge.common.Tags.Blocks.CHESTS) && !state.is(net.neoforged.neoforge.common.Tags.Blocks.BARRELS)
                    && !state.is(net.minecraft.tags.BlockTags.SHULKER_BOXES), BuiltInRegistries.BLOCK.getKey(b) + " must not look like cargo storage");
        }
        h.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
        var chest = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(1, 1, 1)), null);
        h.assertTrue(chest != null && net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(chest,
                new ItemStack(MachinesModule.RTG_FUEL_PELLET.get(), 4), false).isEmpty(), "a chest takes the cargo");
        h.succeed();
    }

    /** The sintering furnace turns Martian (and lunar) regolith into sintered blocks that seal a base. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "industry_automation")
    public static void sinteringFurnaceMakesAirtightBlocksFromAnyRegolith(GameTestHelper h) {
        MachineBlockEntity furnace = machine(h, MachinesModule.SINTERING_FURNACE, new BlockPos(2, 1, 2), Direction.NORTH);
        furnace.items().setStackInSlot(0, new ItemStack(BuiltInRegistries.ITEM.get(Spacebreaker.id("martian_regolith")), 2));
        var sintered = com.pizzaparker07.spacebreaker.content.moon.MoonModule.SINTERED_REGOLITH.get();
        h.setBlock(new BlockPos(0, 1, 0), sintered);
        h.setBlock(new BlockPos(0, 2, 0), com.pizzaparker07.spacebreaker.content.moon.MoonModule.SINTERED_REGOLITH_BRICKS.base().get());
        h.assertTrue(com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(h.getLevel(), h.absolutePos(new BlockPos(0, 1, 0)),
                h.getBlockState(new BlockPos(0, 1, 0))), "sintered regolith seals a room");
        h.assertTrue(com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(h.getLevel(), h.absolutePos(new BlockPos(0, 2, 0)),
                h.getBlockState(new BlockPos(0, 2, 0))), "sintered regolith bricks seal a room");
        h.succeedWhen(() -> {
            furnace.energy().generate(1_000);
            h.assertTrue(furnace.items().getStackInSlot(1).is(sintered.asItem()) && furnace.items().getStackInSlot(1).getCount() == 2,
                    "sintered blocks out: " + furnace.items().getStackInSlot(1));
        });
    }

    private IndustryAutomationTests() {
    }
}
