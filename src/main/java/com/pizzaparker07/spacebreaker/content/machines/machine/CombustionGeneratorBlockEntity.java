package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Burns furnace fuel to make power. Combustion needs oxygen: it only runs where the air is breathable
 * (or inside an oxygenated, sealed room), so it is useless on the Moon.
 */
public class CombustionGeneratorBlockEntity extends MachineBlockEntity {
    public static final int OUTPUT = 40;
    public static final MachineLayout LAYOUT = new MachineLayout()
            .energy(10, 17).slot(79, 52, MachineLayout.Role.FUEL).progress(80, 33, MachineLayout.Progress.FLAME);

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.COMBUSTION_GENERATOR_BE.get(), pos, state, LAYOUT, 40_000, 0, 1_000, List.of());
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        boolean burning = false;
        if (!Environment.canBreathe(level, pos)) {
            status = 3;
            progress = 0;
        } else {
            if (progress <= 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                ItemStack fuel = items.getStackInSlot(0);
                int burn = fuel.getBurnTime(RecipeType.SMELTING);
                if (burn > 0) {
                    maxProgress = burn;
                    progress = burn;
                    ItemStack remainder = fuel.getCraftingRemainingItem();
                    items.extractItem(0, 1, false);
                    if (items.getStackInSlot(0).isEmpty() && !remainder.isEmpty()) {
                        items.setStackInSlot(0, remainder);
                    }
                }
            }
            if (progress > 0) {
                progress--;
                energy.generate(OUTPUT);
                burning = true;
                status = 0;
            } else {
                status = items.getStackInSlot(0).isEmpty() ? 5 : 0;
            }
        }
        List<Direction> sides = new ArrayList<>();
        Direction front = state.getValue(MachineBlock.FACING);
        for (Direction d : Direction.values()) {
            if (d != front) {
                sides.add(d);
            }
        }
        pushEnergy(level, pos, sides, 1_000);
        return burning;
    }
}
