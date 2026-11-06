package com.pizzaparker07.spacebreaker.content.machines.base;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

/** A machine tank: input tanks accept fluids from outside, output tanks can only be drained. */
public class MachineTank extends FluidTank {
    public final boolean input;
    private final Runnable onChange;

    public MachineTank(int capacity, boolean input, Predicate<FluidStack> filter, Runnable onChange) {
        super(capacity, filter);
        this.input = input;
        this.onChange = onChange;
    }

    @Override
    protected void onContentsChanged() {
        onChange.run();
    }

    /** Machine-internal fill that ignores the input/output role but respects the filter and capacity. */
    public int fillInternal(FluidStack stack, FluidAction action) {
        return super.fill(stack, action);
    }

    public float fraction() {
        return capacity == 0 ? 0F : (float) getFluidAmount() / capacity;
    }
}
