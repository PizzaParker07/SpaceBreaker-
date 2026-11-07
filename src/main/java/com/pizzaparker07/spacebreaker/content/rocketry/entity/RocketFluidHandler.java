package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fill-only fluid access to a rocket: tank 0 takes RP-1 (c:rp1), tank 1 liquid oxygen (c:liquid_oxygen),
 * tank 2 liquid methane (c:liquid_methane) for methalox stages, tank 3 liquid hydrogen (c:liquid_hydrogen) for
 * hydrolox stages.
 */
public record RocketFluidHandler(RocketEntity rocket) implements IFluidHandler {
    @Override
    public int getTanks() {
        return 4;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return rocket.roomFor(tankFuel(tank));
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 1 ? stack.is(Propellant.LOX_TAG) : stack.is(java.util.Objects.requireNonNull(tankFuel(tank)).fuelTag());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return rocket.fill(resource, action.simulate());
    }

    /** Tank index to liquid: 0 RP-1, 1 liquid oxygen (null), 2 liquid methane, 3 liquid hydrogen. */
    @org.jetbrains.annotations.Nullable
    public static Propellant tankFuel(int tank) {
        return switch (tank) {
            case 1 -> null;
            case 2 -> Propellant.METHALOX;
            case 3 -> Propellant.HYDROLOX;
            default -> Propellant.KEROLOX;
        };
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
