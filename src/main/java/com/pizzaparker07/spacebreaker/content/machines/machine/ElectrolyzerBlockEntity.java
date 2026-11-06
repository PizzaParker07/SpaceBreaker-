package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electrolyzer with separate outlet manifolds, so the two gases never mix:
 * hydrogen leaves on the left side and oxygen on the right side (as seen standing in front of it).
 */
public class ElectrolyzerBlockEntity extends ProcessingMachineBlockEntity {
    public ElectrolyzerBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec, pos, state);
    }

    @Override
    protected boolean canPush(int tank, Direction side) {
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return switch (tank) {
            case 1 -> side == facing.getClockWise();          // hydrogen: viewer's left
            case 2 -> side == facing.getCounterClockWise();   // oxygen: viewer's right
            default -> false;
        };
    }
}
