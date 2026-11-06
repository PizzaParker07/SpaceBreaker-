package com.pizzaparker07.spacebreaker.content.machines.net;

import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidStack;

/** Cable and pipe tiers. Energy rates are FE/t per network, fluid rates mB/t per network. */
public enum ConduitType implements StringRepresentable {
    COPPER_CABLE("copper_cable", true, 512, 2),
    HV_CABLE("hv_cable", true, 8192, 3),
    FLUID_PIPE("fluid_pipe", false, 500, 3),
    GAS_PIPE("gas_pipe", false, 1000, 2);

    public final String id;
    public final boolean energy;
    public final int rate;
    /** Half thickness in pixels (core is 8±radius). */
    public final int radius;

    ConduitType(String id, boolean energy, int rate, int radius) {
        this.id = id;
        this.energy = energy;
        this.rate = rate;
        this.radius = radius;
    }

    public boolean accepts(FluidStack stack) {
        if (energy || stack.isEmpty()) {
            return false;
        }
        boolean gas = stack.getFluidType().isLighterThanAir();
        return this == GAS_PIPE ? gas : !gas;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
