package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.PumpjackBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.PartBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Processing outputs, energy flow through cables, fluid transfer through pipes and the oil chain. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryMachineTests {
    private static MachineBlockEntity place(GameTestHelper h, MachineEntry<?, ?> m, BlockPos pos, Direction facing) {
        h.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, facing));
        return h.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void crusherTurnsOreIntoTwoDust(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.CRUSHER, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(20_000);
        be.items().setStackInSlot(0, new ItemStack(EarthModule.TITANIUM.oreItem.get()));
        h.succeedWhen(() -> {
            ItemStack out = be.items().getStackInSlot(1);
            h.assertTrue(out.is(EarthModule.TITANIUM.dust.get()) && out.getCount() == 2, "expected 2 titanium dust, got " + out);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void arcFurnaceMakesSteel(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.ARC_FURNACE, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(60_000);
        be.items().setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        be.items().setStackInSlot(2, new ItemStack(EarthModule.COAL_DUST.get()));
        h.succeedWhen(() -> h.assertTrue(be.items().getStackInSlot(3).is(EarthModule.STEEL.ingotItem().asItem()), "no steel yet"));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void arcFurnaceSmeltsLikeAFurnace(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.ARC_FURNACE, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(60_000);
        be.items().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        h.succeedWhen(() -> h.assertTrue(be.items().getStackInSlot(3).is(Items.IRON_INGOT), "raw iron not smelted"));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void pressNeedsTheRightDie(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.PRESS, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(20_000);
        be.items().setStackInSlot(0, new ItemStack(EarthModule.ALUMINUM_INGOT.get(), 2));
        h.runAfterDelay(100, () -> {
            h.assertTrue(be.items().getStackInSlot(2).isEmpty(), "pressed without a die");
            be.items().setStackInSlot(1, new ItemStack(EarthModule.WIRE_DIE.get()));
        });
        h.runAfterDelay(240, () -> {
            ItemStack out = be.items().getStackInSlot(2);
            h.assertTrue(out.is(EarthModule.ALUMINUM.wire.get()) && out.getCount() >= 2, "expected aluminum wire, got " + out);
            h.assertTrue(be.items().getStackInSlot(1).is(EarthModule.WIRE_DIE.get()), "die must not be consumed");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void generatorPowersMachineThroughCable(GameTestHelper h) {
        MachineBlockEntity gen = place(h, MachinesModule.COMBUSTION_GENERATOR, new BlockPos(1, 1, 2), Direction.NORTH);
        gen.items().setStackInSlot(0, new ItemStack(Items.COAL, 8));
        for (int x = 2; x <= 4; x++) {
            h.setBlock(new BlockPos(x, 1, 2), MachinesModule.COPPER_CABLE.get());
        }
        MachineBlockEntity crusher = place(h, MachinesModule.CRUSHER, new BlockPos(5, 1, 2), Direction.NORTH);
        h.succeedWhen(() -> h.assertTrue(crusher.energy().getEnergyStored() > 2_000,
                "crusher received " + crusher.energy().getEnergyStored() + " FE"));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cablesConnectOnlyToEnergyBlocks(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), MachinesModule.COPPER_CABLE.get());
        h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        place(h, MachinesModule.BATTERY_BANK, new BlockPos(1, 1, 2), Direction.NORTH);
        BlockState cable = h.getBlockState(new BlockPos(2, 1, 2));
        h.assertTrue(cable.getValue(ConduitBlock.SIDES.get(Direction.WEST)) == ConduitSide.CONNECTED, "cable must connect to the battery");
        h.assertTrue(cable.getValue(ConduitBlock.SIDES.get(Direction.EAST)) == ConduitSide.NONE, "cable must not connect to stone");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void batteryOutputsOnlyFromItsFront(GameTestHelper h) {
        MachineBlockEntity bat = place(h, MachinesModule.BATTERY_BANK, new BlockPos(2, 1, 2), Direction.NORTH);
        bat.energy().generate(100_000);
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(new BlockPos(2, 1, 2));
        var front = level.getCapability(Capabilities.EnergyStorage.BLOCK, abs, Direction.NORTH);
        var side = level.getCapability(Capabilities.EnergyStorage.BLOCK, abs, Direction.EAST);
        h.assertTrue(front != null && front.canExtract() && !front.canReceive(), "front must only extract");
        h.assertTrue(side != null && side.canReceive() && !side.canExtract(), "sides must only receive");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pipeMovesFluidBetweenTanks(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 2), MachinesModule.FLUID_TANK.get());
        for (int x = 2; x <= 4; x++) {
            h.setBlock(new BlockPos(x, 1, 2), MachinesModule.FLUID_PIPE.get());
        }
        h.setBlock(new BlockPos(5, 1, 2), MachinesModule.FLUID_TANK.get());
        TankBlockEntity from = h.getBlockEntity(new BlockPos(1, 1, 2));
        TankBlockEntity to = h.getBlockEntity(new BlockPos(5, 1, 2));
        from.tank().fill(FluidsModule.RP1.stack(4_000), IFluidHandler.FluidAction.EXECUTE);
        // turn the pipe end at the source tank into an extracting end
        BlockPos pipe = new BlockPos(2, 1, 2);
        BlockState ps = h.getBlockState(pipe);
        h.setBlock(pipe, ps.setValue(ConduitBlock.SIDES.get(Direction.WEST), ConduitSide.EXTRACT));
        h.succeedWhen(() -> {
            h.assertTrue(to.tank().getFluidAmount() == 4_000, "moved " + to.tank().getFluidAmount() + " mB");
            h.assertTrue(from.tank().isEmpty(), "source should be empty");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gasPipeRefusesLiquids(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), MachinesModule.GAS_PIPE.get());
        h.setBlock(new BlockPos(3, 1, 2), MachinesModule.PRESSURIZED_TANK.get());
        var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(2, 1, 2)), Direction.WEST);
        h.assertTrue(handler != null, "gas pipe exposes a fluid handler");
        h.assertTrue(handler.fill(FluidsModule.RP1.stack(100), IFluidHandler.FluidAction.EXECUTE) == 0, "gas pipe took a liquid");
        h.assertTrue(handler.fill(FluidsModule.OXYGEN.stack(100), IFluidHandler.FluidAction.EXECUTE) == 100, "gas pipe refused oxygen");
        TankBlockEntity tank = h.getBlockEntity(new BlockPos(3, 1, 2));
        h.assertTrue(tank.tank().getFluidAmount() == 100, "oxygen did not arrive");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void electrolyzerSplitsWater(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.ELECTROLYZER, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(40_000);
        be.tank(0).fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        h.succeedWhen(() -> {
            h.assertTrue(be.tank(1).getFluid().is(FluidsModule.TAG_HYDROGEN) && be.tank(1).getFluidAmount() >= 2_000, "hydrogen " + be.tank(1).getFluidAmount());
            h.assertTrue(be.tank(2).getFluid().is(FluidsModule.TAG_OXYGEN) && be.tank(2).getFluidAmount() >= 1_000, "oxygen " + be.tank(2).getFluidAmount());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void liquefierMakesLox(GameTestHelper h) {
        MachineBlockEntity be = place(h, MachinesModule.LIQUEFIER, new BlockPos(2, 1, 2), Direction.NORTH);
        be.energy().generate(60_000);
        be.tank(0).fill(FluidsModule.OXYGEN.stack(2_000), IFluidHandler.FluidAction.EXECUTE);
        h.succeedWhen(() -> h.assertTrue(be.tank(1).getFluid().is(FluidsModule.TAG_LOX) && be.tank(1).getFluidAmount() == 1_000,
                "LOX " + be.tank(1).getFluidAmount()));
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void pumpjackPumpsCrudeIntoRefinery(GameTestHelper h) {
        // reservoir under the wellhead (pumpjack at (2,3,3) facing north -> wellhead at (2,3,2))
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                h.setBlock(new BlockPos(2 + dx, 1, 2 + dz), FluidsModule.CRUDE_OIL.block.get());
                h.setBlock(new BlockPos(2 + dx, 0, 2 + dz), Blocks.STONE);
                h.setBlock(new BlockPos(2 + dx, 2, 2 + dz), Blocks.STONE);
            }
        }
        BlockPos pj = new BlockPos(2, 3, 3);
        BlockState state = MachinesModule.PUMPJACK.block().get().defaultBlockState();
        h.setBlock(pj, state);
        MachinesModule.PUMPJACK.block().get().build(h.getLevel(), h.absolutePos(pj), h.getBlockState(pj));
        h.assertTrue(h.getBlockState(new BlockPos(2, 3, 2)).getBlock() instanceof PartBlock, "wellhead part missing");
        PumpjackBlockEntity be = h.getBlockEntity(pj);
        be.energy().generate(30_000);
        h.succeedWhen(() -> h.assertTrue(be.tank(0).getFluidAmount() >= 500, "pumped " + be.tank(0).getFluidAmount()));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void breakingAPartBreaksTheMultiblock(GameTestHelper h) {
        BlockPos rf = new BlockPos(2, 1, 2);
        BlockState state = MachinesModule.REFINERY.block().get().defaultBlockState();
        h.setBlock(rf, state);
        MachinesModule.REFINERY.block().get().build(h.getLevel(), h.absolutePos(rf), h.getBlockState(rf));
        h.assertTrue(h.getBlockState(new BlockPos(2, 4, 2)).getBlock() instanceof PartBlock, "tower top missing");
        h.destroyBlock(rf);
        h.assertTrue(h.getBlockState(new BlockPos(2, 3, 2)).isAir(), "parts must go with the controller");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fabricatingRecipeTypeIsOpenToOtherAreas(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.FABRICATING.type().get());
        h.assertTrue(recipes.size() >= 5, "fabricating recipes missing: " + recipes.size());
        h.assertTrue(!h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.CRUSHING.type().get()).isEmpty(), "crushing recipes missing");
        h.succeed();
    }

    private IndustryMachineTests() {
    }
}
