package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CrewArmBlockEntity extends BlockEntity {
    private BlockPos padCenter;
    private float swing;     // 0 = reaching the rocket, 1 = folded against the tower
    private float swingO;
    private boolean initialised;

    public CrewArmBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.CREW_ARM_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrewArmBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 10 != 0) {
            return;
        }
        if (be.padCenter == null || level.getGameTime() % 100 == 0) {
            LaunchPad.Result r = LaunchPad.findNear(level, pos, 5);
            be.padCenter = r == null ? null : r.center();
        }
        RocketEntity rocket = be.padCenter == null ? null : LaunchPad.rocketOn(level, be.padCenter);
        boolean extend = rocket != null && rocket.state() == FlightState.IDLE;
        if (state.getValue(CrewArmBlock.EXTENDED) != extend) {
            level.setBlock(pos, state.setValue(CrewArmBlock.EXTENDED, extend), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, extend ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrewArmBlockEntity be) {
        float target = state.getValue(CrewArmBlock.EXTENDED) ? 0F : 1F;
        if (!be.initialised) {
            be.swing = be.swingO = target;
            be.initialised = true;
            return;
        }
        be.swingO = be.swing;
        be.swing += Mth.clamp(target - be.swing, -0.025F, 0.025F);
    }

    /** 0 = extended to the rocket, 1 = retracted. */
    public float swing(float partialTick) {
        return Mth.lerp(partialTick, swingO, swing);
    }

    @Nullable
    public BlockPos padCenter() {
        return padCenter;
    }
}
