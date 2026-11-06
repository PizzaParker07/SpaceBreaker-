package com.pizzaparker07.spacebreaker.content.rocketry.design;

import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;

import java.util.ArrayList;
import java.util.List;

/**
 * The rocket equation and friends. Stages are numbered from the bottom; {@code active} is the lowest stage
 * still attached. {@code fuel[i]} is the propellant left in stage i, in kg.
 */
public final class RocketMath {
    /** How much of a ground-launched first stage burn happens at sea-level pressure (Isp blend). */
    public static final double ASCENT_ATMOSPHERE = 0.5;

    private RocketMath() {
    }

    /** Tsiolkovsky: Δv = Isp · g0 · ln(m0 / mf). */
    public static double deltaV(double isp, double m0, double mf) {
        if (isp <= 0 || mf <= 0 || m0 <= mf) {
            return 0;
        }
        return isp * RocketPart.G0 * Math.log(m0 / mf);
    }

    /** Propellant needed to give mass m0 a velocity change dv. */
    public static double propellantFor(double isp, double m0, double dv) {
        if (isp <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return m0 * (1 - Math.exp(-dv / (isp * RocketPart.G0)));
    }

    /** Mass of everything above stage {@code index} (upper stages with their fuel, and the payload). */
    public static double massAbove(RocketLayout layout, int index, double[] fuel) {
        double m = layout.payload().massKg();
        for (int i = index + 1; i < layout.stages().size(); i++) {
            m += layout.stages().get(i).dryKg() + fuel[i];
        }
        return m;
    }

    /** Current mass of the vehicle with stage {@code active} as its lowest stage. */
    public static double mass(RocketLayout layout, int active, double[] fuel) {
        if (active >= layout.stages().size()) {
            return layout.payload().massKg();
        }
        return massAbove(layout, active, fuel) + layout.stages().get(active).dryKg() + fuel[active];
    }

    /** Δv the given stage still holds. atmosphere: 0 = vacuum, 1 = sea level. */
    public static double stageDeltaV(RocketLayout layout, int index, double[] fuel, double atmosphere) {
        RocketLayout.Stage s = layout.stages().get(index);
        if (!s.hasEngine()) {
            return 0;
        }
        double mf = massAbove(layout, index, fuel) + s.dryKg();
        return deltaV(s.isp(atmosphere), mf + fuel[index], mf);
    }

    /** Total Δv from the active stage up; the active stage uses the given atmosphere blend, the rest vacuum. */
    public static double totalDeltaV(RocketLayout layout, int active, double[] fuel, double firstStageAtmosphere) {
        double dv = 0;
        for (int i = active; i < layout.stages().size(); i++) {
            dv += stageDeltaV(layout, i, fuel, i == active ? firstStageAtmosphere : 0);
        }
        return dv;
    }

    /** Thrust-to-weight ratio of the active stage in a gravity field g (m/s²). */
    public static double twr(RocketLayout layout, int active, double[] fuel, double gravity) {
        if (active >= layout.stages().size() || gravity <= 0) {
            return 0;
        }
        double thrustN = layout.stages().get(active).thrustKn() * 1000.0;
        return thrustN / (mass(layout, active, fuel) * gravity);
    }

    /** Outcome of a burn: whether the full Δv was delivered, and which stages were dropped on the way. */
    public record Burn(boolean complete, double delivered, int activeStage, List<Integer> separated, double[] fuel) {
    }

    /**
     * Burns {@code dv} starting with stage {@code active}, staging whenever a stage runs dry.
     * Never mutates the given fuel array; the result carries the new one.
     */
    public static Burn burn(RocketLayout layout, int active, double[] fuelIn, double dv, double firstStageAtmosphere) {
        double[] fuel = fuelIn.clone();
        List<Integer> separated = new ArrayList<>();
        double delivered = 0;
        double remaining = dv;
        int stage = active;
        boolean first = true;
        while (remaining > 1e-6 && stage < layout.stages().size()) {
            RocketLayout.Stage s = layout.stages().get(stage);
            double atmosphere = first ? firstStageAtmosphere : 0;
            if (s.hasEngine() && fuel[stage] > 1e-6) {
                double isp = s.isp(atmosphere);
                double m0 = mass(layout, stage, fuel);
                double need = propellantFor(isp, m0, remaining);
                if (need <= fuel[stage]) {
                    fuel[stage] -= need;
                    delivered += remaining;
                    remaining = 0;
                    break;
                }
                double got = deltaV(isp, m0, m0 - fuel[stage]);
                fuel[stage] = 0;
                delivered += got;
                remaining -= got;
            }
            separated.add(stage);
            stage++;
            first = false;
        }
        return new Burn(remaining <= 1e-6, delivered, stage, List.copyOf(separated), fuel);
    }

    /** How long a stage can burn at full thrust, in seconds. */
    public static double burnTime(RocketLayout.Stage s, double propellantKg, double atmosphere) {
        if (!s.hasEngine()) {
            return 0;
        }
        double flow = s.thrustKn() * 1000.0 / (s.isp(atmosphere) * RocketPart.G0);
        return propellantKg / flow;
    }
}
