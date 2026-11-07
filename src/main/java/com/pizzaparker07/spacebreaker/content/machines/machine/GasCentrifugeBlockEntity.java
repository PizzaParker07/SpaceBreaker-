package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gas centrifuge: spins uranium hexafluoride made from yellowcake and splits it into a little enriched uranium and
 * a lot of depleted uranium. One centrifuge alone is slow. Real plants run thousands in cascades, each stage feeding
 * the next; here every working centrifuge beside this one (up to four, on the same level) adds half a stage of speed.
 */
public class GasCentrifugeBlockEntity extends ProcessingMachineBlockEntity {
    /** Extra progress per tick for each working neighbour in the cascade. */
    public static final double STAGE_BONUS = 0.5;

    private double carry;

    public GasCentrifugeBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec, pos, state);
    }

    /** Whether this centrifuge is spinning a recipe right now (what a neighbour counts as a cascade stage). */
    public boolean spinning() {
        return current != null && status == 0 && progress > 0;
    }

    /** Working centrifuges beside this one, on the four horizontal sides. */
    public int cascade(Level level, BlockPos pos) {
        int n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(d)) instanceof GasCentrifugeBlockEntity other && other.spinning()) {
                n++;
            }
        }
        return n;
    }

    /** Speed of a centrifuge with this many working neighbours, relative to one on its own. */
    public static double speed(int neighbours) {
        return 1 + STAGE_BONUS * Math.max(0, Math.min(4, neighbours));
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        boolean worked = super.tickServer(level, pos, state);
        if (worked && progress > 0 && current != null) {
            carry += speed(cascade(level, pos)) - 1;
            int extra = (int) carry;
            if (extra > 0) {
                carry -= extra;
                // never skip past the end: the next tick's step finishes the batch
                progress = Math.min(progress + extra, current.time - 1);
            }
        } else if (!worked) {
            carry = 0;
        }
        return worked;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("cascade_carry", carry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        carry = tag.getDouble("cascade_carry");
    }
}
