package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission-level Δv estimates between bodies using patched conics: ascent, descent, escape, and
 * low-orbit-to-low-orbit transfers (Hohmann at the level of the common parent, with the Oberth effect at
 * departure and arrival and optional aerobraking). Orbits are treated as circular and coplanar, which is
 * the usual Δv-map simplification; plane changes are not included.
 */
public final class TransferPlanner {
    /** Default lift-off thrust-to-weight used by the Δv map. */
    public static final double DEFAULT_TWR = 1.5;
    /** Reference drag loss of an Earth launch, m/s. */
    public static final double EARTH_DRAG_LOSS = 130.0;

    /** One burn (or one group of burns) of a plan. */
    public record Leg(String label, double deltaV) {
    }

    /**
     * A transfer plan between the low orbits of two bodies.
     *
     * @param legs           burns in order
     * @param transferTime   coast time of the transfer, s
     * @param phaseAngle     required angle of the target ahead of the origin at departure, rad (level of the common parent)
     * @param synodicPeriod  time between launch windows, s
     */
    public record Plan(String from, String to, List<Leg> legs, double transferTime, double phaseAngle,
                       double synodicPeriod) {
        public double total() {
            double s = 0;
            for (Leg l : legs) {
                s += l.deltaV();
            }
            return s;
        }

        public double deltaV(String label) {
            for (Leg l : legs) {
                if (l.label().equals(label)) {
                    return l.deltaV();
                }
            }
            return 0;
        }
    }

    // ------------------------------------------------------------------ surface <-> low orbit

    /** Ideal (lossless) Δv from the equator to the default low orbit, minus the surface rotation speed. */
    public static double ascentIdeal(AstroBody b) {
        double mu = b.mu();
        double r0 = b.radius();
        double r1 = b.lowOrbitRadius();
        double at = (r0 + r1) / 2;
        double lift = Kepler.visViva(mu, r0, at);
        double circ = Kepler.circularSpeed(mu, r1) - Kepler.visViva(mu, r1, at);
        return Math.max(0, lift + circ - b.equatorialSpeed());
    }

    /**
     * Gravity loss estimate for a launch with lift-off thrust-to-weight {@code twr} (local gravity):
     * 0.25 · v_circ / TWR (Earth at TWR 1.3 ≈ 1.5 km/s, Moon at TWR 2 ≈ 0.2 km/s). Infinite when TWR ≤ 1.
     */
    public static double gravityLoss(AstroBody b, double twr) {
        if (twr <= 1) {
            return Double.POSITIVE_INFINITY;
        }
        return 0.25 * Kepler.circularSpeed(b.mu(), b.radius()) / twr;
    }

    /** Drag loss estimate: 130 m/s on Earth, scaled by √(pressure) and √(scale height). */
    public static double dragLoss(AstroBody b) {
        if (!b.hasAtmosphere()) {
            return 0;
        }
        return EARTH_DRAG_LOSS * Math.sqrt(b.surfacePressureKpa() / 101.325) * Math.sqrt(b.scaleHeight() / 8500.0);
    }

    /** Surface to default low orbit including gravity and drag losses. */
    public static double ascent(AstroBody b, double twr) {
        return ascentIdeal(b) + gravityLoss(b, twr) + dragLoss(b);
    }

    public static double ascent(AstroBody b) {
        return ascent(b, DEFAULT_TWR);
    }

    /** Surface air density estimate from pressure, temperature and composition, kg/m³. */
    public static double surfaceDensity(AstroBody b) {
        double m = b.meanMolarMass() * 1e-3;
        return b.surfacePressureKpa() * 1000.0 * m / (8.314462618 * b.meanTempK());
    }

    /** Terminal speed under a standard landing parachute (≈ 8 m/s on Earth), m/s. */
    public static double parachuteTerminalSpeed(AstroBody b) {
        if (!b.hasAtmosphere()) {
            return Double.POSITIVE_INFINITY;
        }
        double rhoEarth = 1.225;
        return 8.0 * Math.sqrt(rhoEarth / surfaceDensity(b)) * Math.sqrt(b.surfaceGravity() / AstroConstants.G0);
    }

    /**
     * Low orbit to the surface. Airless: a powered descent mirroring the ascent. With an atmosphere and
     * {@code useAtmosphere}: a deorbit burn, aerobraking, parachutes and, if the parachute alone lands faster
     * than 10 m/s, a powered descent (1.5 × terminal speed + 15 % of the surface orbital speed).
     */
    public static double descent(AstroBody b, boolean useAtmosphere, double twr) {
        if (!b.hasAtmosphere() || !useAtmosphere) {
            return ascentIdeal(b) + gravityLoss(b, twr);
        }
        double deorbit = Transfers.hohmann(b.mu(), b.lowOrbitRadius(), b.radius()).dv1();
        double vt = parachuteTerminalSpeed(b);
        // where parachutes alone land too fast (thin air), a powered descent with hover and divert follows:
        // ≈ 1.5 × terminal speed plus 15 % of the surface orbital speed (Mars ≈ 0.6 km/s, like real landers)
        double landing = vt > 10 ? 1.5 * vt + 0.15 * Kepler.circularSpeed(b.mu(), b.radius()) : 0;
        return deorbit + landing;
    }

    public static double descent(AstroBody b, boolean useAtmosphere) {
        return descent(b, useAtmosphere, DEFAULT_TWR);
    }

    /**
     * A surface ↔ low-orbit leg with its timing.
     *
     * @param deltaV        Δv, m/s (same as {@link #ascent} / {@link #descent})
     * @param burnSeconds   powered flight time
     * @param coastSeconds  unpowered time (descent: the half orbit from the deorbit burn down to the surface,
     *                      plus atmospheric entry where there is air)
     */
    public record SurfaceLeg(double deltaV, double burnSeconds, double coastSeconds) {
        public double totalSeconds() {
            return burnSeconds + coastSeconds;
        }
    }

    /** Average thrust-to-weight over a burn is higher than at lift-off as propellant is used: ≈ 1.3×. */
    static final double TWR_GROWTH = 1.3;
    /** Entry, parachutes and landing through an atmosphere, s. */
    static final double ENTRY_SECONDS = 600;

    /** Ascent from the surface to the default low orbit (Moon at TWR 2: ≈ 1.9 km/s, ≈ 7.5 min like the Apollo ascent stage). */
    public static SurfaceLeg ascentLeg(AstroBody b, double twr) {
        double dv = ascent(b, twr);
        double burn = Double.isFinite(dv) ? dv / (b.surfaceGravity() * twr * TWR_GROWTH) : Double.POSITIVE_INFINITY;
        return new SurfaceLeg(dv, burn, 0);
    }

    /**
     * Descent from the default low orbit: a short deorbit burn, a half-orbit coast down (Moon ≈ 56 min, like
     * Apollo between descent orbit insertion and powered descent), then the powered landing (or entry and
     * parachutes where there is air).
     */
    public static SurfaceLeg descentLeg(AstroBody b, boolean useAtmosphere, double twr) {
        double dv = descent(b, useAtmosphere, twr);
        double a = (b.lowOrbitRadius() + b.radius()) / 2;
        double coast = Math.PI * Math.sqrt(a * a * a / b.mu());
        double burn;
        if (b.hasAtmosphere() && useAtmosphere) {
            double landing = Math.max(0, dv - Transfers.hohmann(b.mu(), b.lowOrbitRadius(), b.radius()).dv1());
            burn = landing / (b.surfaceGravity() * twr * TWR_GROWTH);
            coast += ENTRY_SECONDS;
        } else {
            burn = dv / (b.surfaceGravity() * twr * TWR_GROWTH);
        }
        return new SurfaceLeg(dv, burn, coast);
    }

    /** From the default low orbit to escape. */
    public static double escape(AstroBody b) {
        return Transfers.escapeFromCircular(b.mu(), b.lowOrbitRadius());
    }

    // ------------------------------------------------------------------ orbit <-> orbit

    /** Low orbit of {@code from} to low orbit of {@code to}. */
    public static Plan plan(BodySystem sys, String fromId, String toId, boolean aerobrake) {
        AstroBody a = sys.require(fromId);
        AstroBody b = sys.require(toId);
        if (a == b) {
            return new Plan(fromId, toId, List.of(), 0, 0, 0);
        }
        if (b.parent() != null && b.parent().equals(a.id())) {
            return down(a, b, aerobrake);
        }
        if (a.parent() != null && a.parent().equals(b.id())) {
            return up(a, b, aerobrake);
        }
        AstroBody c = sys.commonAncestor(a, b).orElseThrow(() -> new IllegalArgumentException("no common parent"));
        if (c == a || c == b) {
            throw new IllegalArgumentException("transfers to a grandchild are not supported: " + fromId + " -> " + toId);
        }
        List<AstroBody> chainA = chainBelow(sys, a, c);
        List<AstroBody> chainB = chainBelow(sys, b, c);
        AstroBody ta = chainA.get(chainA.size() - 1);
        AstroBody tb = chainB.get(chainB.size() - 1);
        double r1 = ta.orbit().semiMajorAxis();
        double r2 = tb.orbit().semiMajorAxis();
        Transfers.Transfer h = Transfers.hohmann(c.mu(), r1, r2);
        double vDep = excessAtBody(chainA, h.dv1());
        double vArr = excessAtBody(chainB, h.dv2());
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg("departure", Transfers.departure(a.mu(), a.lowOrbitRadius(), vDep)));
        legs.add(new Leg("capture", capture(b, vArr, aerobrake)));
        double syn = Transfers.synodicPeriod(ta.orbit().period(), tb.orbit().period());
        return new Plan(fromId, toId, legs, h.time(), Transfers.hohmannPhaseAngle(c.mu(), r1, r2), syn);
    }

    /**
     * The cheaper of the direct transfer ({@link #plan}) and a gravity-assist tour ({@link GravityAssist}), e.g.
     * Earth ↔ Mercury via Venus: far less Δv, years longer. The plan's {@code synodicPeriod} and
     * {@code phaseAngle} then describe the window of the first leg (Earth → Venus).
     */
    public static Plan planWithAssists(BodySystem sys, String fromId, String toId, boolean aerobrake) {
        Plan direct = plan(sys, fromId, toId, aerobrake);
        return GravityAssist.tour(sys, fromId, toId, aerobrake)
                .map(GravityAssist.Tour::plan)
                .filter(t -> t.total() < direct.total())
                .orElse(direct);
    }

    /** Hyperbolic excess speed vInf needed at the top of the chain, carried down to the first body. */
    private static double excessAtBody(List<AstroBody> chainBodyFirst, double vInfTop) {
        double v = vInfTop;
        for (int i = chainBodyFirst.size() - 1; i >= 1; i--) {
            AstroBody x = chainBodyFirst.get(i);      // the body we must leave/arrive at with v
            AstroBody y = chainBodyFirst.get(i - 1);  // its moon, one level further down
            double r = y.orbit().semiMajorAxis();
            double needed = Math.sqrt(v * v + 2 * x.mu() / r);
            v = Math.abs(needed - Kepler.circularSpeed(x.mu(), r));
        }
        return v;
    }

    private static List<AstroBody> chainBelow(BodySystem sys, AstroBody b, AstroBody ancestor) {
        List<AstroBody> out = new ArrayList<>();
        for (AstroBody x : sys.ancestry(b)) {
            if (x == ancestor) {
                break;
            }
            out.add(x);
        }
        return out;
    }

    private static double capture(AstroBody b, double vInf, boolean aerobrake) {
        if (aerobrake && b.hasAtmosphere()) {
            return Transfers.captureAerobrake(b.mu(), b.lowOrbitRadius(), b.radius() + b.atmosphereTop());
        }
        return Transfers.captureCircular(b.mu(), b.lowOrbitRadius(), vInf);
    }

    /** Parent low orbit to a moon's low orbit. */
    private static Plan down(AstroBody parent, AstroBody moon, boolean aerobrake) {
        double r1 = parent.lowOrbitRadius();
        double r2 = moon.orbit().semiMajorAxis();
        Transfers.Transfer h = Transfers.hohmann(parent.mu(), r1, r2);
        List<Leg> legs = List.of(new Leg("departure", h.dv1()), new Leg("capture", capture(moon, h.dv2(), aerobrake)));
        double syn = Transfers.synodicPeriod(Kepler.period(r1, parent.mu()), moon.orbit().period());
        return new Plan(parent.id(), moon.id(), legs, h.time(), Transfers.hohmannPhaseAngle(parent.mu(), r1, r2), syn);
    }

    /** A moon's low orbit back to its parent's low orbit. */
    private static Plan up(AstroBody moon, AstroBody parent, boolean aerobrake) {
        double r1 = moon.orbit().semiMajorAxis();
        double r2 = parent.lowOrbitRadius();
        Transfers.Transfer h = Transfers.hohmann(parent.mu(), r1, r2);
        double dep = Transfers.departure(moon.mu(), moon.lowOrbitRadius(), h.dv1());
        double cap = aerobrake && parent.hasAtmosphere()
                ? Transfers.captureAerobrake(parent.mu(), r2, parent.radius() + parent.atmosphereTop())
                : h.dv2();
        List<Leg> legs = List.of(new Leg("departure", dep), new Leg("capture", cap));
        double syn = Transfers.synodicPeriod(Kepler.period(r2, parent.mu()), moon.orbit().period());
        return new Plan(moon.id(), parent.id(), legs, h.time(), Transfers.hohmannPhaseAngle(parent.mu(), r1, r2), syn);
    }

    // ------------------------------------------------------------------ launch windows

    /**
     * Current phase angle (target ahead of origin, rad in (−π, π]) at the level where the transfer happens,
     * measured as the difference of ecliptic longitudes at simulated time t.
     */
    public static double currentPhase(BodySystem sys, String fromId, String toId, double t) {
        AstroBody[] pair = transferPair(sys, fromId, toId);
        if (pair[0] == null) {
            return 0;
        }
        Vec3 pa = sys.relativePosition(pair[0], t);
        Vec3 pb = sys.relativePosition(pair[1], t);
        return Kepler.wrapSigned(Math.atan2(pb.y(), pb.x()) - Math.atan2(pa.y(), pa.x()));
    }

    /** Simulated seconds from t until the next Hohmann departure window. Parent↔moon transfers return 0 (always open). */
    public static double timeToWindow(BodySystem sys, String fromId, String toId, double t) {
        AstroBody[] pair = transferPair(sys, fromId, toId);
        if (pair[0] == null) {
            return 0;
        }
        Plan p = plan(sys, fromId, toId, false);
        double n1 = pair[0].orbit().meanMotion();
        double n2 = pair[1].orbit().meanMotion();
        double rate = n2 - n1;
        double guess = Transfers.timeToPhase(currentPhase(sys, fromId, toId, t), p.phaseAngle(), n1, n2);
        // refine against the real (eccentric) positions; the mean-motion estimate can be off by weeks
        double wait = refineWindow(sys, fromId, toId, t, guess, p.phaseAngle(), rate);
        return wait >= 0 ? wait : refineWindow(sys, fromId, toId, t, wait + p.synodicPeriod(), p.phaseAngle(), rate);
    }

    private static double refineWindow(BodySystem sys, String fromId, String toId, double t, double guess,
                                       double required, double rate) {
        double wait = guess;
        for (int i = 0; i < 20; i++) {
            double err = Kepler.wrapSigned(currentPhase(sys, fromId, toId, t + wait) - required);
            if (Math.abs(err) < 1e-7) {
                break;
            }
            wait -= err / rate;
        }
        return wait;
    }

    /** The two bodies orbiting the common parent whose alignment sets the window, or {null, null} for parent↔moon. */
    private static AstroBody[] transferPair(BodySystem sys, String fromId, String toId) {
        AstroBody a = sys.require(fromId);
        AstroBody b = sys.require(toId);
        AstroBody c = sys.commonAncestor(a, b).orElseThrow();
        if (c == a || c == b) {
            return new AstroBody[]{null, null};
        }
        List<AstroBody> ca = chainBelow(sys, a, c);
        List<AstroBody> cb = chainBelow(sys, b, c);
        return new AstroBody[]{ca.get(ca.size() - 1), cb.get(cb.size() - 1)};
    }

    private TransferPlanner() {
    }
}
