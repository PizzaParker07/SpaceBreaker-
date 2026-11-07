package com.pizzaparker07.spacebreaker.content.rocketry.design;

import com.pizzaparker07.spacebreaker.core.config.SbConfig;

/**
 * Cryogenic boil-off: liquid hydrogen (and, much more slowly, oxygen and methane) warms up and escapes from the
 * tanks during long coasts and while a rocket waits in orbit. Never on the pad, where the umbilical keeps
 * topping the tanks up. The rate is per stage propellant (see Propellant#boilOffPerDay) and scaled by the
 * realism setting, like the Δv costs.
 */
public final class BoilOff {
    public static final double SECONDS_PER_DAY = 86_400;

    private BoilOff() {
    }

    /** How much of the real boil-off rate a realism setting applies. */
    public static double scale(SbConfig.Realism realism) {
        return switch (realism) {
            case REALISTIC -> 1.0;
            case BALANCED -> 0.2;
            case EASY -> 0.1;
        };
    }

    /**
     * Boils off {@code days} worth from every stage still on the rocket (from {@code active} up), in place.
     * Returns the propellant lost in kg.
     */
    public static double apply(RocketLayout layout, int active, double[] fuel, double days, SbConfig.Realism realism) {
        if (days <= 0) {
            return 0;
        }
        double lost = 0;
        for (int i = Math.max(0, active); i < fuel.length && i < layout.stages().size(); i++) {
            double rate = layout.stages().get(i).propellant().boilOffPerDay() * scale(realism);
            if (rate <= 0 || fuel[i] <= 0) {
                continue;
            }
            double keep = Math.pow(1 - rate, days);
            lost += fuel[i] * (1 - keep);
            fuel[i] *= keep;
        }
        return lost;
    }
}
