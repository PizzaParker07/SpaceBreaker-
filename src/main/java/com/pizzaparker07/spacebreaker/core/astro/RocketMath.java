package com.pizzaparker07.spacebreaker.core.astro;

import java.util.List;

/**
 * Rocket equation helpers for the Rocketry area. Masses in kg, thrust in N, Isp in seconds, Δv in m/s.
 * Stages are listed bottom (first to fire) to top.
 */
public final class RocketMath {

    /**
     * One stage.
     *
     * @param dryMass       structure + engines of this stage, kg
     * @param propellantMass propellant, kg
     * @param thrust        total thrust, N
     * @param isp           specific impulse in the conditions it fires in, s
     */
    public record Stage(double dryMass, double propellantMass, double thrust, double isp) {
        public double wetMass() {
            return dryMass + propellantMass;
        }

        public double exhaustVelocity() {
            return isp * AstroConstants.G0;
        }

        public double massFlow() {
            return RocketMath.massFlow(thrust, isp);
        }
    }

    /** Summary of one stage within a stack. */
    public record StageResult(double deltaV, double initialMass, double finalMass, double burnTime,
                              double initialTwr, double finalTwr) {
    }

    /** Tsiolkovsky: Δv = Isp · g0 · ln(m0/mf). */
    public static double deltaV(double isp, double wetMass, double dryMass) {
        if (dryMass <= 0 || wetMass < dryMass) {
            throw new IllegalArgumentException("need wetMass ≥ dryMass > 0");
        }
        return isp * AstroConstants.G0 * Math.log(wetMass / dryMass);
    }

    /** Mass ratio m0/mf needed for a Δv. */
    public static double massRatio(double dv, double isp) {
        return Math.exp(dv / (isp * AstroConstants.G0));
    }

    /** Propellant mass needed to give a final (dry + payload) mass mf a Δv. */
    public static double propellantFor(double dv, double isp, double finalMass) {
        return finalMass * (massRatio(dv, isp) - 1);
    }

    /** Mass flow rate, kg/s. */
    public static double massFlow(double thrust, double isp) {
        return thrust / (isp * AstroConstants.G0);
    }

    /** Burn time to consume a propellant mass at full thrust, s. */
    public static double burnTime(double propellantMass, double thrust, double isp) {
        return propellantMass / massFlow(thrust, isp);
    }

    /** Burn time to deliver a Δv starting from mass m0, s. */
    public static double burnTimeForDeltaV(double dv, double m0, double thrust, double isp) {
        double mf = m0 / massRatio(dv, isp);
        return burnTime(m0 - mf, thrust, isp);
    }

    /** Thrust-to-weight ratio at a local gravity g (m/s²). */
    public static double thrustToWeight(double thrust, double mass, double g) {
        return thrust / (mass * g);
    }

    /** Effective Isp of several engines firing together. */
    public static double combinedIsp(double[] thrusts, double[] isps) {
        double t = 0, flow = 0;
        for (int i = 0; i < thrusts.length; i++) {
            t += thrusts[i];
            flow += massFlow(thrusts[i], isps[i]);
        }
        return t / (flow * AstroConstants.G0);
    }

    /** Linear interpolation of Isp between sea level and vacuum by ambient pressure (kPa). */
    public static double ispAtPressure(double ispSea, double ispVac, double pressureKpa) {
        double f = Math.max(0, Math.min(1, pressureKpa / 101.325));
        return ispVac + (ispSea - ispVac) * f;
    }

    /**
     * Per-stage results for a serial stack with a payload on top. Each stage carries every stage above it.
     *
     * @param g local gravity for the TWR figures, m/s²
     */
    public static StageResult[] stack(List<Stage> stagesBottomUp, double payload, double g) {
        StageResult[] out = new StageResult[stagesBottomUp.size()];
        double above = payload;
        double[] carried = new double[stagesBottomUp.size()];
        for (int i = stagesBottomUp.size() - 1; i >= 0; i--) {
            carried[i] = above;
            above += stagesBottomUp.get(i).wetMass();
        }
        for (int i = 0; i < stagesBottomUp.size(); i++) {
            Stage s = stagesBottomUp.get(i);
            double m0 = s.wetMass() + carried[i];
            double mf = s.dryMass() + carried[i];
            double dv = s.propellantMass() > 0 ? deltaV(s.isp(), m0, mf) : 0;
            double bt = s.thrust() > 0 ? burnTime(s.propellantMass(), s.thrust(), s.isp()) : Double.POSITIVE_INFINITY;
            out[i] = new StageResult(dv, m0, mf, bt, thrustToWeight(s.thrust(), m0, g), thrustToWeight(s.thrust(), mf, g));
        }
        return out;
    }

    /** Total Δv of a serial stack. */
    public static double totalDeltaV(List<Stage> stagesBottomUp, double payload) {
        double sum = 0;
        for (StageResult r : stack(stagesBottomUp, payload, AstroConstants.G0)) {
            sum += r.deltaV();
        }
        return sum;
    }

    private RocketMath() {
    }
}
