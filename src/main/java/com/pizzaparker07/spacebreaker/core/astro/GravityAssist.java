package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gravity-assist tours to the inner Solar System, the way MESSENGER and BepiColombo reached Mercury: a flyby
 * of an intermediate planet (Venus) lowers the perihelion to the target's orbit, then repeated resonant flybys
 * of the target itself, each followed by a small deep-space burn, shrink the arrival speed before capture.
 * <p>
 * Patched conics with the usual "v∞ leveraging" rule of thumb: a deep-space manoeuvre changes the flyby
 * hyperbolic excess speed by about {@link #LEVERAGE_GAIN} times its own Δv. Coast times add one resonant loop
 * (two orbits of the flyby body) per pumping phase and per target flyby. Good to ~10–20 % against real mission
 * budgets; much cheaper than a direct Hohmann transfer, but years longer.
 */
public final class GravityAssist {
    /** Δv∞ obtained per unit of deep-space Δv (v∞ leveraging, Sims &amp; Longuski: 2–3). */
    public static final double LEVERAGE_GAIN = 2.5;
    /** Share of the target-arrival v∞ that the target flybys cannot remove. */
    public static final double RESIDUAL_FRACTION = 0.3;
    /** Arrival v∞ never drops below this with flybys alone, m/s. */
    public static final double MIN_ARRIVAL_VINF = 1500;
    /** How much v∞ one target flyby loop takes off, m/s. */
    public static final double VINF_PER_FLYBY = 1300;
    /** Tours are offered only when both ends orbit inside this distance, AU (Earth/Mars ↔ Mercury via Venus). */
    public static final double INNER_LIMIT_AU = 1.6;

    /**
     * A tour.
     *
     * @param plan           legs and coast time (departure, flyby_pumping, target_flybys, capture)
     * @param flybyBody      the intermediate planet (e.g. Venus)
     * @param targetFlybys   number of resonant flybys of the inner body
     */
    public record Tour(TransferPlanner.Plan plan, String flybyBody, int targetFlybys) {
    }

    /**
     * Tour between two planets of the same star where one lies inside the orbit of a third planet that lies
     * between them (Earth ↔ Mercury via Venus). Empty when no such route exists.
     */
    public static Optional<Tour> tour(BodySystem sys, String fromId, String toId, boolean aerobrake) {
        AstroBody from = sys.require(fromId), to = sys.require(toId);
        if (from.parent() == null || !from.parent().equals(to.parent()) || from.orbit() == null || to.orbit() == null) {
            return Optional.empty();
        }
        AstroBody star = sys.require(from.parent());
        if (!star.isStar()) {
            return Optional.empty();
        }
        boolean inward = to.orbit().semiMajorAxis() < from.orbit().semiMajorAxis();
        AstroBody outer = inward ? from : to, inner = inward ? to : from;
        double aO = outer.orbit().semiMajorAxis(), aT = inner.orbit().semiMajorAxis();
        if (aO > INNER_LIMIT_AU * AstroConstants.AU) {
            return Optional.empty(); // the leveraging calibration only covers the inner Solar System
        }
        AstroBody flyby = null;
        for (AstroBody b : sys.children(star.id())) {
            if (b.isStar() || b.orbit() == null || b.kind() == AstroBody.Kind.ASTEROID || b.kind() == AstroBody.Kind.COMET || b.kind() == AstroBody.Kind.DWARF) {
                continue;
            }
            double a = b.orbit().semiMajorAxis();
            if (a < aO && a > aT && (flyby == null || b.mass() > flyby.mass())) {
                flyby = b;
            }
        }
        if (flyby == null) {
            return Optional.empty();
        }
        double mu = star.mu();
        double aF = flyby.orbit().semiMajorAxis();
        Transfers.Transfer of = Transfers.hohmann(mu, aO, aF);
        double vInfOuter = of.dv1();          // at the outer planet
        double vInfFlyby = of.dv2();          // arriving at the flyby planet
        // a retrograde flyby must leave us slow enough at aF to fall to the target's orbit
        double vNeeded = Kepler.visViva(mu, aF, (aF + aT) / 2);
        double vInfRequired = Kepler.circularSpeed(mu, aF) - vNeeded;
        double pumping = Math.max(0, vInfRequired - vInfFlyby) / LEVERAGE_GAIN;
        // arrival at the target on the flyby-target ellipse
        double vInfTarget = Kepler.visViva(mu, aT, (aF + aT) / 2) - Kepler.circularSpeed(mu, aT);
        double vInfFinal = Math.min(vInfTarget, Math.max(MIN_ARRIVAL_VINF, RESIDUAL_FRACTION * vInfTarget));
        double cranking = (vInfTarget - vInfFinal) / LEVERAGE_GAIN;
        int flybys = (int) Math.ceil((vInfTarget - vInfFinal) / VINF_PER_FLYBY);

        double outerBurn = inward ? Transfers.departure(outer.mu(), outer.lowOrbitRadius(), vInfOuter)
                : captureAt(outer, vInfOuter, aerobrake);
        double innerBurn = inward ? captureAt(inner, vInfFinal, aerobrake)
                : Transfers.departure(inner.mu(), inner.lowOrbitRadius(), vInfFinal);
        List<TransferPlanner.Leg> legs = new ArrayList<>();
        legs.add(new TransferPlanner.Leg("departure", inward ? outerBurn : innerBurn));
        if (inward) {
            legs.add(new TransferPlanner.Leg("flyby_pumping", pumping));
            legs.add(new TransferPlanner.Leg("target_flybys", cranking));
        } else {
            legs.add(new TransferPlanner.Leg("target_flybys", cranking));
            legs.add(new TransferPlanner.Leg("flyby_pumping", pumping));
        }
        legs.add(new TransferPlanner.Leg("capture", inward ? innerBurn : outerBurn));

        double pF = flyby.orbit().period(), pT = inner.orbit().period();
        double ellipseHalf = Math.PI * Math.sqrt(Math.pow((aF + aT) / 2, 3) / mu);
        double time = of.time() + (pumping > 0 ? 2 * pF : 0) + ellipseHalf + flybys * 2 * pT;
        double syn = Transfers.synodicPeriod(outer.orbit().period(), flyby.orbit().period());
        double phase = Transfers.hohmannPhaseAngle(mu, aO, aF);
        TransferPlanner.Plan plan = new TransferPlanner.Plan(fromId, toId, List.copyOf(legs), time, phase, syn);
        return Optional.of(new Tour(plan, flyby.id(), flybys));
    }

    private static double captureAt(AstroBody b, double vInf, boolean aerobrake) {
        if (aerobrake && b.hasAtmosphere()) {
            return Transfers.captureAerobrake(b.mu(), b.lowOrbitRadius(), b.radius() + b.atmosphereTop());
        }
        return Transfers.captureCircular(b.mu(), b.lowOrbitRadius(), vInf);
    }

    private GravityAssist() {
    }
}
