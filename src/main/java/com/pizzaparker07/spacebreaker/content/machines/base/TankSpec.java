package com.pizzaparker07.spacebreaker.content.machines.base;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Predicate;

public record TankSpec(int capacity, boolean input, Predicate<FluidStack> filter) {
    public static TankSpec in(int capacity, Predicate<FluidStack> filter) {
        return new TankSpec(capacity, true, filter);
    }

    public static TankSpec out(int capacity) {
        return new TankSpec(capacity, false, f -> true);
    }
}
