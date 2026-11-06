package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Steel lattice section of the service tower (stack six or more next to the pad). */
public class ServiceTowerBlock extends Block {
    public ServiceTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
