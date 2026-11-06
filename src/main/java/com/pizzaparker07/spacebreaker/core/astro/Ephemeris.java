package com.pizzaparker07.spacebreaker.core.astro;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * Cached "where is everything now" for one star system: body positions at the current simulated time and
 * launch windows between bodies. Positions are recomputed only when the time changes; windows are kept until
 * they open (then the next one is found). Pure Java — {@link OrbitService} wires it to a running server.
 * Thread-safe.
 */
public final class Ephemeris {
    /** A window counts as open while the phase angle is within this of the Hohmann angle, rad. */
    public static final double DEFAULT_TOLERANCE = Math.toRadians(2.0);

    /**
     * A launch window between two bodies.
     *
     * @param plan          the Hohmann plan (Δv, transfer time, required phase angle, synodic period)
     * @param opensAt       simulated time (s since J2000) of the next departure at the exact phase angle
     * @param currentPhase  phase angle now, rad
     * @param open          true when departing now is within the tolerance (always true for planet ↔ moon)
     * @param halfWidth     how long (s) the window stays inside the tolerance on either side of {@code opensAt}
     */
    public record Window(TransferPlanner.Plan plan, double opensAt, double currentPhase, boolean open, double halfWidth) {
        /** Seconds of simulated time from {@code now} until the window opens (0 if open). */
        public double waitFrom(double now) {
            return open ? 0 : Math.max(0, opensAt - halfWidth - now);
        }
    }

    private final BodySystem system;
    private final DoubleSupplier clock;
    private final double tolerance;
    private final Map<String, Vec3> positions = new HashMap<>();
    private final Map<String, Window> windows = new HashMap<>();
    private double positionsTime = Double.NaN;

    public Ephemeris(BodySystem system, DoubleSupplier simClock) {
        this(system, simClock, DEFAULT_TOLERANCE);
    }

    public Ephemeris(BodySystem system, DoubleSupplier simClock, double tolerance) {
        this.system = system;
        this.clock = simClock;
        this.tolerance = tolerance;
    }

    public BodySystem system() {
        return system;
    }

    /** Current simulated time, s since J2000. */
    public double now() {
        return clock.getAsDouble();
    }

    /** Position relative to the system's star, m. */
    public synchronized Vec3 position(String id) {
        double t = now();
        if (t != positionsTime) {
            positions.clear();
            positionsTime = t;
        }
        return positions.computeIfAbsent(id, k -> system.position(system.require(k), t));
    }

    /** Vector from body {@code from} to body {@code to}, m. */
    public Vec3 offset(String from, String to) {
        return position(to).sub(position(from));
    }

    public double distance(String a, String b) {
        return offset(a, b).length();
    }

    /** Distance from the star, AU (for sunlight and temperature effects). */
    public double starDistanceAu(String id) {
        return position(id).sub(position(system.root(system.require(id)).id())).length() / AstroConstants.AU;
    }

    /** The current or next launch window from {@code from} to {@code to}. */
    public synchronized Window window(String from, String to) {
        double t = now();
        String key = from + "|" + to;
        Window w = windows.get(key);
        if (w == null || t > w.opensAt() + w.halfWidth() || t < w.opensAt() - w.plan().synodicPeriod()) {
            w = compute(from, to, t);
            windows.put(key, w);
        }
        double phase = TransferPlanner.currentPhase(system, from, to, t);
        boolean always = w.plan().synodicPeriod() == 0 || isMoonLink(from, to);
        boolean open = always || Math.abs(t - w.opensAt()) <= w.halfWidth();
        return new Window(w.plan(), always ? t : w.opensAt(), phase, open, w.halfWidth());
    }

    private Window compute(String from, String to, double t) {
        TransferPlanner.Plan plan = TransferPlanner.plan(system, from, to, false);
        if (isMoonLink(from, to)) {
            return new Window(plan, t, 0, true, Double.POSITIVE_INFINITY);
        }
        // start a little in the past so a window that is open right now is found, not the next one
        double halfWidth = tolerance / (2 * Math.PI / plan.synodicPeriod());
        double start = t - halfWidth;
        double opens = start + TransferPlanner.timeToWindow(system, from, to, start);
        return new Window(plan, opens, 0, false, halfWidth);
    }

    private boolean isMoonLink(String from, String to) {
        AstroBody a = system.require(from), b = system.require(to);
        return from.equals(b.parent()) || to.equals(a.parent()) || from.equals(to);
    }

    /** Forgets cached windows (e.g. after the time scale changed). */
    public synchronized void invalidate() {
        windows.clear();
        positions.clear();
        positionsTime = Double.NaN;
    }
}
