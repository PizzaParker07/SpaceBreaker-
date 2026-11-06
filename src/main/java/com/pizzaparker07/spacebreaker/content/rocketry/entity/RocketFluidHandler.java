package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Fill-only fluid access to a rocket: tank 0 takes RP-1 (c:rp1), tank 1 liquid oxygen (c:liquid_oxygen). */
public record RocketFluidHandler(RocketEntity rocket) implements IFluidHandler {
    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return rocket.roomFor(tank == 1);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 ? stack.is(RocketryModule.RP1_TAG) : stack.is(RocketryModule.LOX_TAG);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        boolean ox = resource.is(RocketryModule.LOX_TAG);
        if (!ox && !resource.is(RocketryModule.RP1_TAG)) {
            return 0;
        }
        return rocket.fill(ox, resource.getAmount(), action.simulate());
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
