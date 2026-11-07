package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds no data: it only gives the client a place to draw the turning beam of a lit warning light. */
public class WarningLightBlockEntity extends BlockEntity {
    public WarningLightBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.WARNING_LIGHT_BE.get(), pos, state);
    }
}
