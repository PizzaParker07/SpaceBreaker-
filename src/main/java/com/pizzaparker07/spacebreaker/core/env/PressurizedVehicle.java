package com.pizzaparker07.spacebreaker.core.env;

import net.minecraft.world.entity.LivingEntity;

/**
 * A vehicle with a closed, pressurised cabin (the pressurised rover). Life support asks the vehicle a player is riding:
 * while the cabin holds air, the rider breathes the cabin's oxygen instead of the suit's, sits at cabin temperature
 * and takes only part of the outside radiation, and needs no helmet.
 */
public interface PressurizedVehicle {
    /** Whether the cabin holds breathable air right now (oxygen in the tank, life support powered). */
    boolean cabinPressurized();

    /** Cabin air temperature, °C. */
    default double cabinTemperatureC() {
        return 21.0;
    }

    /** Fraction of the outside radiation dose that gets through the cabin walls (0..1). */
    default double radiationFactor() {
        return 1.0;
    }

    /**
     * One rider breathes {@code breathingTicks} of oxygen from the cabin supply (the suit's unit: one tick of
     * walking-rate breathing). Returns false when the cabin cannot supply it, and the suit's own rules apply.
     */
    boolean breathe(LivingEntity rider, double breathingTicks);
}
