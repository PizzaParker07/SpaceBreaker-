package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * A vehicle with a pressurised cabin (a crewed rover, ...): implemented by the vehicle's entity. While its cabin holds
 * air for a passenger, life support treats that passenger as in a shirt-sleeve cabin: breathable air (no helmet or
 * tank needed), the cabin's temperature instead of the outside, outside radiation cut by the hull, normal pressure,
 * and the helmet HUD shows "pressurised". Checked on the server every tick, so keep it cheap.
 */
public interface PressurizedCabin {
    /** Whether the cabin holds air for this passenger right now (hatch shut, power on, air left, ...). */
    boolean cabinPressurised(Player passenger);

    /** Cabin air temperature in °C. */
    default double cabinTemperatureC(Player passenger) {
        return 21.0;
    }

    /** Share of the outside radiation dose rate that gets through the hull (0..1). */
    default double cabinRadiationFactor(Player passenger) {
        return 0.3;
    }

    /** The pressurised cabin the player sits in now (the vehicle, or one it is mounted on), if any. */
    static Optional<PressurizedCabin> holding(Player player) {
        for (Entity v = player.getVehicle(); v != null; v = v.getVehicle()) {
            if (v instanceof PressurizedCabin cabin && cabin.cabinPressurised(player)) {
                return Optional.of(cabin);
            }
        }
        return Optional.empty();
    }
}
