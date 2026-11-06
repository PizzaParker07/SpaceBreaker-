package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The whole Phase 1 industry chain in one world, with no hand-offs by the player:
 * bauxite ore -> crusher -> hopper -> arc furnace -> hopper -> press = aluminum plate;
 * crude-oil pocket -> pumpjack -> refinery = RP-1; water -> electrolyzer -> liquefier = liquid oxygen
 * (hydrogen leaves through the other outlet into a pressurized tank).
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryChainTest {
    private static MachineBlockEntity machine(GameTestHelper h, MachineEntry<?, ?> m, BlockPos pos, Direction facing, int energy) {
        h.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, facing));
        MachineBlockEntity be = h.getBlockEntity(pos);
        be.energy().generate(energy);
        return be;
    }

    @GameTest(template = "empty", timeoutTicks = 1600)
    public static void oreAndOilToPlatesRp1AndLox(GameTestHelper h) {
        // --- materials: crusher above a hopper into the arc furnace, which drains into the press
        MachineBlockEntity crusher = machine(h, MachinesModule.CRUSHER, new BlockPos(1, 3, 1), Direction.NORTH, 20_000);
        crusher.items().setStackInSlot(0, new ItemStack(EarthModule.ALUMINUM.oreItem.get(), 2));
        h.setBlock(new BlockPos(1, 2, 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        machine(h, MachinesModule.ARC_FURNACE, new BlockPos(2, 2, 1), Direction.NORTH, 60_000);
        h.setBlock(new BlockPos(2, 1, 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        MachineBlockEntity press = machine(h, MachinesModule.PRESS, new BlockPos(3, 1, 1), Direction.NORTH, 20_000);
        press.items().setStackInSlot(1, new ItemStack(EarthModule.PLATE_DIE.get()));

        // --- fuel: a crude-oil pocket under a pumpjack whose wellhead touches the refinery
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                h.setBlock(new BlockPos(2 + dx, 0, 6 + dz), Blocks.STONE);
                h.setBlock(new BlockPos(2 + dx, 1, 6 + dz), FluidsModule.CRUDE_OIL.block.get());
                h.setBlock(new BlockPos(2 + dx, 2, 6 + dz), Blocks.STONE);
            }
        }
        BlockPos pj = new BlockPos(2, 3, 7);
        BlockState pjs = MachinesModule.PUMPJACK.block().get().defaultBlockState();
        h.setBlock(pj, pjs);
        MachinesModule.PUMPJACK.block().get().build(h.getLevel(), h.absolutePos(pj), h.getBlockState(pj));
        ((MachineBlockEntity) h.getBlockEntity(pj)).energy().generate(30_000);
        BlockPos rf = new BlockPos(3, 3, 6);
        h.setBlock(rf, MachinesModule.REFINERY.block().get().defaultBlockState());
        MachinesModule.REFINERY.block().get().build(h.getLevel(), h.absolutePos(rf), h.getBlockState(rf));
        MachineBlockEntity refinery = h.getBlockEntity(rf);
        refinery.energy().generate(40_000);

        // --- oxidizer: electrolyzer facing north; oxygen leaves west into the liquefier, hydrogen east into a tank
        MachineBlockEntity electrolyzer = machine(h, MachinesModule.ELECTROLYZER, new BlockPos(7, 1, 2), Direction.NORTH, 40_000);
        electrolyzer.tank(0).fill(new FluidStack(Fluids.WATER, 4_000), IFluidHandler.FluidAction.EXECUTE);
        MachineBlockEntity liquefier = machine(h, MachinesModule.LIQUEFIER, new BlockPos(6, 1, 2), Direction.NORTH, 60_000);
        h.setBlock(new BlockPos(8, 1, 2), MachinesModule.PRESSURIZED_TANK.get());
        TankBlockEntity hydrogen = h.getBlockEntity(new BlockPos(8, 1, 2));

        h.succeedWhen(() -> {
            ItemStack plates = press.items().getStackInSlot(2);
            h.assertTrue(plates.is(EarthModule.ALUMINUM_PLATE.get()) && plates.getCount() >= 4, "aluminum plates: " + plates);
            FluidStack rp1 = refinery.tank(1).getFluid();
            h.assertTrue(rp1.is(FluidsModule.TAG_RP1) && rp1.getAmount() >= 500, "RP-1: " + rp1.getAmount());
            FluidStack lox = liquefier.tank(1).getFluid();
            h.assertTrue(lox.is(FluidsModule.TAG_LOX) && lox.getAmount() >= 500, "LOX: " + lox.getAmount());
            h.assertTrue(liquefier.tank(0).isEmpty() || liquefier.tank(0).getFluid().is(FluidsModule.TAG_OXYGEN), "liquefier got the wrong gas");
            h.assertTrue(hydrogen.tank().getFluid().is(FluidsModule.TAG_HYDROGEN), "hydrogen did not reach its tank");
        });
    }

    private IndustryChainTest() {
    }
}
