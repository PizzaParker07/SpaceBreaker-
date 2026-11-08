package com.pizzaparker07.spacebreaker.content.machines.net;

import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Cable and pipe tiers. Energy rates are FE/t per network, fluid rates mB/t per network, item rates items per pull
 * (every {@link #ITEM_INTERVAL} ticks) per pulling end.
 */
public enum ConduitType implements StringRepresentable {
    COPPER_CABLE("copper_cable", true, 512, 2),
    HV_CABLE("hv_cable", true, 8192, 3),
    FLUID_PIPE("fluid_pipe", false, 500, 2),
    GAS_PIPE("gas_pipe", false, 1000, 2),
    ITEM_DUCT("item_duct", false, 8, 3);

    /** Ticks between item pulls: 8 items every half second = 16 items/s per pulling end. */
    public static final int ITEM_INTERVAL = 10;

    public final String id;
    public final boolean energy;
    public final int rate;
    /** Half thickness in pixels (core is 8±radius). */
    public final int radius;
    /** Carries items between inventories instead of energy or fluid. */
    public final boolean items;

    ConduitType(String id, boolean energy, int rate, int radius) {
        this.id = id;
        this.energy = energy;
        this.rate = rate;
        this.radius = radius;
        this.items = "item_duct".equals(id);
    }

    public boolean accepts(FluidStack stack) {
        if (energy || items || stack.isEmpty()) {
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
