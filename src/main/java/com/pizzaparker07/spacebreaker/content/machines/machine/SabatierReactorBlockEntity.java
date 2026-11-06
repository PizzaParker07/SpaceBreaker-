package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sabatier reactor (CO2 + 4 H2 -> CH4 + 2 H2O over a nickel catalyst). Gases and water leave through every
 * side except the front; gas pipes only take the methane and fluid pipes only the water.
 */
public class SabatierReactorBlockEntity extends ProcessingMachineBlockEntity {
    public SabatierReactorBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec, pos, state);
    }

    @Override
    protected boolean canPush(int tank, Direction side) {
        return side != getBlockState().getValue(com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock.FACING);
    }
}
