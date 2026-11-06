package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Gameplay Δv costs: the real value from the active {@link TransferModel}, scaled by the server's realism
 * setting (the solar system is shrunk for fun; REALISTIC keeps real numbers), minus small bonuses for
 * fins (steering losses on ascent) and RCS (fine control when landing).
 */
public final class Transfers {
    private static volatile TransferModel model = new SimpleTransfers();

    public static final double FIN_BONUS = 150;   // m/s real, ascent through an atmosphere
    public static final double RCS_BONUS = 60;    // m/s real, landings and ascents on airless bodies

    private Transfers() {
    }

    public static void setModel(TransferModel newModel) {
        model = newModel;
    }

    public static TransferModel model() {
        return model;
    }

    /** Scale applied to real Δv in each realism preset. */
    public static double scale(SbConfig.Realism realism) {
        return switch (realism) {
            case REALISTIC -> 1.0;
            case BALANCED -> 0.35;
            case EASY -> 0.25;
        };
    }

    public static List<Location> destinations(Location from) {
        List<Location> out = new ArrayList<>();
        for (Location to : Location.values()) {
            if (to != from && model.deltaV(from, to).isPresent()) {
                out.add(to);
            }
        }
        return out;
    }

    /** Gameplay Δv for a leg flown by this rocket (with stage {@code active} as its lowest stage). */
    public static OptionalDouble cost(Location from, Location to, RocketLayout layout, int active, SbConfig.Realism realism) {
        OptionalDouble real = model.deltaV(from, to);
        if (real.isEmpty()) {
            return real;
        }
        double dv = real.getAsDouble();
        if (from == Location.EARTH_SURFACE && layout != null && active < layout.stages().size()) {
            RocketLayout.Stage s = layout.stages().get(active);
            if (s.fins() || s.gridFins()) {
                dv -= FIN_BONUS;
            }
        }
        boolean airlessPowered = (from == Location.MOON_ORBIT && to == Location.MOON_SURFACE)
                || (from == Location.MOON_SURFACE && to == Location.MOON_ORBIT);
        if (airlessPowered && layout != null && layout.hasRcsFrom(active)) {
            dv -= RCS_BONUS;
        }
        return OptionalDouble.of(Math.max(0, dv) * scale(realism));
    }

    public static OptionalDouble cost(Location from, Location to, SbConfig.Realism realism) {
        return cost(from, to, null, 0, realism);
    }

    /** Required heat shield rating to come home through an atmosphere from here (0 = none needed). */
    public static int reentryRating(Location from, Location to) {
        if (to == Location.EARTH_SURFACE) {
            return from == Location.MOON_ORBIT ? 2 : 1;
        }
        if (to == Location.EARTH_ORBIT && from == Location.MOON_ORBIT) {
            return 2; // aerocapture
        }
        return 0;
    }
}
