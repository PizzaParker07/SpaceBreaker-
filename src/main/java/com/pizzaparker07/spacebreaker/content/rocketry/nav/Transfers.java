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

    /** How long the coast between two places really takes, in seconds (0 when the model does not know). */
    public static double coastSeconds(Location from, Location to) {
        return model instanceof AstroTransferModel astro ? astro.transferSeconds(from, to).orElse(0) : 0;
    }

    /**
     * The one place where gameplay Δv is derived from real Δv: cost = real Δv (from the active
     * {@link TransferModel}, i.e. the core/astro TransferPlanner, see docs/ASTRO.md) × this factor,
     * minus the fin/RCS bonuses. REALISTIC uses real numbers; BALANCED and EASY shrink the solar system
     * so a buildable three-stage kerosene rocket can fly a Moon mission.
     */
    public static double scale(SbConfig.Realism realism) {
        return switch (realism) {
            case REALISTIC -> 1.0;
            case BALANCED -> 0.35;
            case EASY -> 0.25;
        };
    }

    public static List<Location> destinations(Location from) {
        List<Location> out = new ArrayList<>();
        for (Location to : Locations.all()) {
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
        boolean sameBody = from.body().equals(to.body());
        if (sameBody && !from.orbit() && to.orbit() && layout != null && active < layout.stages().size()
                && model instanceof AstroTransferModel astro) {
            // the climb's gravity loss depends on this rocket's lift-off thrust-to-weight
            double g = Locations.body(from.body()).map(b -> b.physical().surfaceGravity()).orElse(0.0);
            double twr = com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath.twr(layout, active, layout.fullFuel(), g);
            if (twr > 0) {
                dv = astro.ascent(from, twr).orElse(dv);
            }
        }
        if (sameBody && !from.orbit() && from.atmosphere() && layout != null && active < layout.stages().size()) {
            RocketLayout.Stage s = layout.stages().get(active);
            if (s.fins() || s.gridFins()) {
                dv -= FIN_BONUS;
            }
        }
        boolean airlessPowered = sameBody && from.orbit() != to.orbit() && !from.atmosphere();
        if (airlessPowered && layout != null && layout.hasRcsFrom(active)) {
            dv -= RCS_BONUS;
        }
        return OptionalDouble.of(Math.max(0, dv) * scale(realism));
    }

    /** Real (unscaled) Δv of a route, for display next to the gameplay value. */
    public static OptionalDouble real(Location from, Location to) {
        return model.deltaV(from, to);
    }

    public static OptionalDouble cost(Location from, Location to, SbConfig.Realism realism) {
        return cost(from, to, null, 0, realism);
    }

    /**
     * Heat shield rating needed for a leg (0 = none): 1 to land from a low orbit through an atmosphere,
     * 2 for a direct entry or aerocapture coming from another body, and for very dense atmospheres.
     */
    public static int reentryRating(Location from, Location to) {
        boolean sameBody = from.body().equals(to.body());
        if (!to.orbit() && to.atmosphere()) {
            if (!sameBody) {
                return 2;
            }
            return from.orbit() ? (to.pressureKpa() > 1000 ? 2 : 1) : 0;
        }
        if (to.orbit() && to.atmosphere() && !sameBody) {
            return 2; // aerocapture
        }
        return 0;
    }
}
