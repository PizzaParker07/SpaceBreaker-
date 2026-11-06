package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.MultiblockMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Distillation tower: crude oil in at the base; RP-1, light gases and plastic feedstock out. */
public class RefineryBlockEntity extends ProcessingMachineBlockEntity {
    public RefineryBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec, pos, state);
    }

    @Override
    protected int extraCheck(Level level, BlockPos pos) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MultiblockMachineBlock mb && !mb.isComplete(level, pos, state)) {
            return 6;
        }
        return 0;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        boolean working = super.tickServer(level, pos, state);
        if (level.getGameTime() % 4 == 0 && state.getBlock() instanceof MultiblockMachineBlock mb) {
            for (BlockPos p : mb.partPositions(pos, state.getValue(MachineBlock.FACING))) {
                pushFluids(level, p, 200);
            }
        }
        return working;
    }
}
