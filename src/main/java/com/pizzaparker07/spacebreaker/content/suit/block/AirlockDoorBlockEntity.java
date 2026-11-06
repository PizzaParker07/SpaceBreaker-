package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the client-side slide animation of an airlock door half. */
public class AirlockDoorBlockEntity extends BlockEntity {
    /** Fraction of the stroke per tick: the doors take half a second. */
    static final float SPEED = 0.1F;
    private float progress = -1F;
    private float progressO;

    public AirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(SuitModule.AIRLOCK_DOOR_BE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AirlockDoorBlockEntity be) {
        float target = state.getValue(AirlockDoorBlock.OPEN) ? 1F : 0F;
        if (be.progress < 0) {
            be.progress = be.progressO = target;
            return;
        }
        be.progressO = be.progress;
        be.progress += Mth.clamp(target - be.progress, -SPEED, SPEED);
    }

    /** 0 = closed, 1 = open, eased. */
    public float getOpenness(float partialTick) {
        float p = progress < 0 ? (getBlockState().getValue(AirlockDoorBlock.OPEN) ? 1F : 0F) : Mth.lerp(partialTick, progressO, progress);
        return p * p * (3F - 2F * p);
    }
}
