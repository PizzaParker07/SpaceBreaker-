package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.Lambert;
import com.pizzaparker07.spacebreaker.core.astro.StateVector;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.Optional;

/**
 * Direct rendezvous with a comet: leave low Earth orbit, fly a Lambert arc in 3-D (inclination and retrograde
 * motion included), and match the comet's velocity on arrival. The arrival is pinned to a perihelion or aphelion
 * passage (± {@link #ARRIVAL_WINDOW_DAYS}) and the departure date and flight time are searched for the cheapest
 * total. No gravity assists, single revolution, the comet's own gravity ignored (it is ≈ 1 m/s).
 */
public final class CometRendezvous {
    public static final double ARRIVAL_WINDOW_DAYS = 60;
    private static final int TOF_STEPS = 160, ARRIVAL_STEPS = 25;

    /** How a comet can be reached, for the map. */
    public enum Access {
        CHEMICAL("Reachable", "Ulaşılabilir"),
        NUCLEAR("Needs a nuclear engine", "Nükleer motor gerekir"),
        OBSERVATION_ONLY("Observation only", "Yalnız gözlem");

        public final String nameEn, nameTr;

        Access(String en, String tr) {
            nameEn = en;
            nameTr = tr;
        }
    }

    public enum Point {
        PERIHELION, APHELION,
        /** Anywhere along the orbit, departing after the start time, within a flight-time limit. */
        ANYWHERE
    }

    /**
     * @param cometId       {@link Comet#id()}
     * @param point         which end of the orbit the arrival is near
     * @param departureDv   burn in low Earth orbit, m/s
     * @param arrivalDv     velocity match at the comet, m/s
     * @param flightSeconds time of flight
     * @param departureTime simulated time of departure (s from J2000)
     * @param arrivalTime   simulated time of arrival
     * @param arrivalAu     distance from the Sun at arrival
     */
    public record Result(String cometId, Point point, double departureDv, double arrivalDv, double flightSeconds, double departureTime,
                         double arrivalTime, double arrivalAu) {
        public double totalDv() {
            return departureDv + arrivalDv;
        }

        public double flightYears() {
            return flightSeconds / AstroConstants.YEAR;
        }

        /** Within a mission budget: total Δv and flight time limits. */
        public boolean within(double maxDv, double maxYears) {
            return totalDv() <= maxDv && flightYears() <= maxYears;
        }
    }

    /**
     * Precomputed rendezvous options for the whole catalogue: for each comet, its next {@code passages} perihelion and
     * aphelion passages after a start time. Computing takes a few seconds; build it once (off the main thread) and
     * reuse it. Immutable and thread-safe.
     */
    public record Table(double after, int passages, java.util.Map<String, java.util.List<Result>> options) {
        public Table {
            options = java.util.Map.copyOf(options);
        }

        public static Table compute(BodySystem sys, double after, int passages) {
            return compute(sys, after, passages, DEFAULT_MAX_YEARS);
        }

        /** Flight-time limits for which the table keeps the cheapest anywhere-on-orbit option, years. */
        public static final double[] TABLE_FLIGHT_YEARS = {5, 10, 20, 40};

        /** As {@link #compute(BodySystem, double, int)}; {@code maxYears} is ignored (see {@link #TABLE_FLIGHT_YEARS}). */
        public static Table compute(BodySystem sys, double after, int passages, double maxYears) {
            java.util.Map<String, java.util.List<Result>> out = new java.util.LinkedHashMap<>();
            for (Comet c : Comet.CATALOGUE) {
                java.util.List<Result> list = new java.util.ArrayList<>();
                double t = after;
                for (int k = 0; k < passages; k++) {
                    for (Point p : new Point[]{Point.PERIHELION, Point.APHELION}) {
                        estimate(sys, c, p, t).ifPresent(list::add);
                    }
                    t = c.nextPerihelion(t) + c.periodSeconds() * 0.75; // move past this perihelion and aphelion
                }
                for (Result r : cheapestWithin(sys, c, after, TABLE_FLIGHT_YEARS)) {
                    if (r != null && !list.contains(r)) list.add(r);
                }
                list.sort(java.util.Comparator.comparingDouble(Result::arrivalTime));
                out.put(c.id(), java.util.List.copyOf(list));
            }
            return new Table(after, passages, out);
        }

        /** All options for a comet, in arrival order (empty for an unknown id). */
        public java.util.List<Result> of(String cometId) {
            return options.getOrDefault(cometId, java.util.List.of());
        }

        /** Options whose departure is still ahead at simulated time {@code now}. */
        public java.util.List<Result> upcoming(String cometId, double now) {
            return of(cometId).stream().filter(r -> r.departureTime() >= now).toList();
        }

        /** Cheapest upcoming option within the budget, if any. */
        public Optional<Result> cheapest(String cometId, double now, double maxDv, double maxYears) {
            return upcoming(cometId, now).stream().filter(r -> r.within(maxDv, maxYears))
                    .min(java.util.Comparator.comparingDouble(Result::totalDv));
        }

        /** Earliest-arriving upcoming option within the budget, if any. */
        public Optional<Result> soonest(String cometId, double now, double maxDv, double maxYears) {
            return upcoming(cometId, now).stream().filter(r -> r.within(maxDv, maxYears))
                    .min(java.util.Comparator.comparingDouble(Result::arrivalTime));
        }

        /**
         * Map classification for budgets of two engine generations: reachable with chemical rockets, only with a
         * nuclear thermal stage, or observation only.
         */
        public Access access(String cometId, double now, double chemicalDv, double nuclearDv, double maxYears) {
            if (reachable(cometId, now, chemicalDv, maxYears)) return Access.CHEMICAL;
            if (reachable(cometId, now, nuclearDv, maxYears)) return Access.NUCLEAR;
            return Access.OBSERVATION_ONLY;
        }

        /** Whether any upcoming option fits the budget: false means "observation only" for that budget. */
        public boolean reachable(String cometId, double now, double maxDv, double maxYears) {
            return cheapest(cometId, now, maxDv, maxYears).isPresent();
        }
    }

    private CometRendezvous() {
    }

    /** Cheapest direct rendezvous at the first perihelion or aphelion passage at or after {@code after}. */
    public static Optional<Result> estimate(BodySystem sys, Comet comet, Point point, double after) {
        if (point == Point.ANYWHERE) {
            return cheapestWithin(sys, comet, after, DEFAULT_MAX_YEARS);
        }
        double period = comet.periodSeconds();
        double passage = comet.nextPerihelion(after) + (point == Point.APHELION ? period / 2 : 0);
        double rArrive = (point == Point.PERIHELION ? comet.perihelionAu() : comet.aphelionAu()) * AstroConstants.AU;
        // transfer times: from a month up to twice the Hohmann time to the arrival distance
        double aT = 0.5 * (AstroConstants.AU + rArrive);
        double hohmann = Math.PI * Math.sqrt(aT * aT * aT / AstroConstants.MU_SUN);
        double[] arrivals = new double[ARRIVAL_STEPS];
        for (int ia = 0; ia < ARRIVAL_STEPS; ia++) {
            arrivals[ia] = passage + (ia / (ARRIVAL_STEPS - 1.0) * 2 - 1) * ARRIVAL_WINDOW_DAYS * AstroConstants.DAY;
        }
        return search(sys, comet, point, arrivals, 30 * AstroConstants.DAY, Math.max(2 * hohmann, 400 * AstroConstants.DAY),
                TOF_STEPS, Double.NEGATIVE_INFINITY);
    }

    /** Flight-time limit used by {@link Point#ANYWHERE} through {@link #estimate}, years. */
    public static final double DEFAULT_MAX_YEARS = 20;
    /** How far ahead {@link #cheapestWithin} looks for an arrival, years. */
    public static final double HORIZON_YEARS = 80;

    /**
     * Cheapest rendezvous anywhere along the orbit: departure after {@code after}, flight at most {@code maxYears},
     * arrival within one orbital period (at most {@link #HORIZON_YEARS}) of departure. This is the "best affordable
     * passage" for a mission planner: slow far-out points are cheap to match but take long to reach.
     */
    public static Optional<Result> cheapestWithin(BodySystem sys, Comet comet, double after, double maxYears) {
        return Optional.ofNullable(cheapestWithin(sys, comet, after, new double[]{maxYears})[0]);
    }

    /** {@link #cheapestWithin(BodySystem, Comet, double, double)} for several flight-time limits in one pass. */
    public static Result[] cheapestWithin(BodySystem sys, Comet comet, double after, double[] maxYears) {
        // fixed grids, so a longer allowance only ever adds candidates
        double span = (Math.min(comet.periodSeconds() / AstroConstants.YEAR, HORIZON_YEARS) + MAX_FLIGHT_YEARS) * AstroConstants.YEAR;
        int n = 360;
        double[] arrivals = new double[n];
        for (int i = 0; i < n; i++) {
            arrivals[i] = after + 30 * AstroConstants.DAY + span * i / (n - 1.0);
        }
        double[] limits = new double[maxYears.length];
        for (int i = 0; i < limits.length; i++) {
            limits[i] = Math.min(maxYears[i], MAX_FLIGHT_YEARS) * AstroConstants.YEAR;
        }
        return search(sys, comet, Point.ANYWHERE, arrivals, 30 * AstroConstants.DAY, MAX_FLIGHT_YEARS * AstroConstants.YEAR, 80,
                after, limits);
    }

    /** Longest flight {@link #cheapestWithin} considers, years. */
    public static final double MAX_FLIGHT_YEARS = 40;

    private static Optional<Result> search(BodySystem sys, Comet comet, Point point, double[] arrivals, double minTof,
                                           double maxTof, int tofSteps, double earliestDeparture) {
        return Optional.ofNullable(search(sys, comet, point, arrivals, minTof, maxTof, tofSteps, earliestDeparture,
                new double[]{maxTof})[0]);
    }

    /** One pass over the grids; the cheapest result for each flight-time limit (null where none). */
    private static Result[] search(BodySystem sys, Comet comet, Point point, double[] arrivals, double minTof,
                                   double maxTof, int tofSteps, double earliestDeparture, double[] tofLimits) {
        AstroBody earth = sys.require("spacebreaker:earth");
        double rLeo = earth.lowOrbitRadius(), vLeo = Math.sqrt(earth.mu() / rLeo);
        Result[] best = new Result[tofLimits.length];
        for (double tArr : arrivals) {
            StateVector cs = Kepler.stateFromElements(comet.elements(), tArr);
            for (int it = 0; it < tofSteps; it++) {
                double tof = minTof * Math.pow(maxTof / minTof, it / (tofSteps - 1.0));
                double tDep = tArr - tof;
                if (tDep < earliestDeparture) continue;
                Vec3 rE = sys.position(earth, tDep), vE = sys.velocity(earth, tDep);
                for (boolean prograde : new boolean[]{true, false}) {
                    Optional<Lambert.Solution> s = Lambert.solve(rE, cs.position(), tof, AstroConstants.MU_SUN, prograde);
                    if (s.isEmpty()) continue;
                    double vInf = s.get().v1().sub(vE).length();
                    double dep = Math.sqrt(vInf * vInf + 2 * earth.mu() / rLeo) - vLeo;
                    double arr = cs.velocity().sub(s.get().v2()).length();
                    Result r = null;
                    for (int k = 0; k < tofLimits.length; k++) {
                        if (tof <= tofLimits[k] * (1 + 1e-12) && (best[k] == null || dep + arr < best[k].totalDv())) {
                            if (r == null) {
                                r = new Result(comet.id(), point, dep, arr, tof, tDep, tArr, cs.position().length() / AstroConstants.AU);
                            }
                            best[k] = r;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * The trip home: leave the comet (a burn from its velocity onto a Lambert arc) and fly into Earth's atmosphere,
     * braking just above it only as much as the entry-speed limit requires.
     *
     * @param burnDv        burn at the comet, m/s
     * @param brakeDv       braking burn just above Earth's atmosphere to bring the entry speed down to the limit, m/s
     * @param entrySpeed    speed at the top of Earth's atmosphere after braking, m/s (√(v∞² + v_esc²) − brakeDv)
     * @param departureTime simulated time of leaving the comet (s from J2000)
     * @param arrivalTime   simulated time of entry
     * @param departureAu   comet's distance from the Sun at departure
     */
    public record Return(String cometId, double burnDv, double brakeDv, double entrySpeed, double flightSeconds,
                         double departureTime, double arrivalTime, double departureAu) {
        public double totalDv() {
            return burnDv + brakeDv;
        }

        public double flightYears() {
            return flightSeconds / AstroConstants.YEAR;
        }
    }

    /**
     * Cheapest return from a comet to Earth entry: leave at or after {@code after} (e.g. the rendezvous arrival, plus
     * any stay), fly at most {@code maxYears}, and enter no faster than {@code maxEntrySpeed} (m/s; use
     * {@code Double.POSITIVE_INFINITY} for no limit; Apollo returned at ≈ 11 km/s, Stardust at 12.9 km/s). A faster
     * arrival pays the difference as a braking burn, so the cheapest total wins. Riding a retrograde comet home
     * costs almost nothing but arrives at ≈ 68 km/s, so the brake matters.
     */
    public static Optional<Return> returnTrip(BodySystem sys, Comet comet, double after, double maxYears, double maxEntrySpeed) {
        AstroBody earth = sys.require("spacebreaker:earth");
        double years = Math.min(maxYears, MAX_FLIGHT_YEARS);
        double maxTof = years * AstroConstants.YEAR, minTof = 30 * AstroConstants.DAY;
        double span = Math.min(comet.periodSeconds(), HORIZON_YEARS * AstroConstants.YEAR);
        double vEsc2 = 2 * earth.mu() / (earth.radius() + 100e3);
        Return best = null;
        java.util.List<Return> candidates = new java.util.ArrayList<>();
        int n = 360, tofSteps = 80;
        for (int i = 0; i < n; i++) {
            double tDep = after + span * i / (n - 1.0);
            StateVector cs = Kepler.stateFromElements(comet.elements(), tDep);
            for (int it = 0; it < tofSteps; it++) {
                double tof = minTof * Math.pow(MAX_FLIGHT_YEARS * AstroConstants.YEAR / minTof, it / (tofSteps - 1.0));
                if (tof > maxTof * (1 + 1e-12)) break;
                double tArr = tDep + tof;
                Vec3 rE = sys.position(earth, tArr), vE = sys.velocity(earth, tArr);
                for (boolean prograde : new boolean[]{true, false}) {
                    Optional<Lambert.Solution> s = Lambert.solve(cs.position(), rE, tof, AstroConstants.MU_SUN, prograde);
                    if (s.isEmpty()) continue;
                    double burn = s.get().v1().sub(cs.velocity()).length();
                    double vInf = s.get().v2().sub(vE).length();
                    double entry = Math.sqrt(vInf * vInf + vEsc2);
                    double brake = Math.max(0, entry - maxEntrySpeed);
                    Return r = new Return(comet.id(), burn, brake, entry - brake, tof, tDep, tArr, cs.position().length() / AstroConstants.AU);
                    candidates.add(r);
                    if (best == null || burn + brake < best.totalDv()) {
                        best = r;
                    }
                }
            }
        }
        if (best == null) return Optional.empty();
        // refine the cheapest grid cells (several starts: the cost surface has many local minima)
        candidates.sort(java.util.Comparator.comparingDouble(Return::totalDv));
        Return result = best;
        int starts = 0;
        for (Return start : candidates) {
            if (starts >= REFINE_STARTS) break;
            starts++;
            Return cur = start;
            double dDep = span / (n - 1.0), dTof = start.flightSeconds() * 0.08;
            for (int iter = 0; iter < 60 && (dDep > 3600 || dTof > 3600); iter++) {
                Return next = cur;
                for (double[] step : new double[][]{{dDep, 0}, {-dDep, 0}, {0, dTof}, {0, -dTof}, {dDep, dTof}, {-dDep, -dTof}, {dDep, -dTof}, {-dDep, dTof}}) {
                    double tDep = cur.departureTime() + step[0], tof = cur.flightSeconds() + step[1];
                    if (tDep < after || tof < minTof || tof > maxTof * (1 + 1e-12)) continue;
                    Return r = returnAt(sys, earth, comet, tDep, tof, vEsc2, maxEntrySpeed);
                    if (r != null && r.totalDv() < next.totalDv()) next = r;
                }
                if (next == cur) {
                    dDep /= 2;
                    dTof /= 2;
                }
                cur = next;
            }
            if (cur.totalDv() < result.totalDv()) result = cur;
        }
        return Optional.of(result);
    }

    private static final int REFINE_STARTS = 40;

    private static Return returnAt(BodySystem sys, AstroBody earth, Comet comet, double tDep, double tof, double vEsc2, double maxEntry) {
        StateVector cs = Kepler.stateFromElements(comet.elements(), tDep);
        double tArr = tDep + tof;
        Vec3 rE = sys.position(earth, tArr), vE = sys.velocity(earth, tArr);
        Return best = null;
        for (boolean prograde : new boolean[]{true, false}) {
            Optional<Lambert.Solution> s = Lambert.solve(cs.position(), rE, tof, AstroConstants.MU_SUN, prograde);
            if (s.isEmpty()) continue;
            double burn = s.get().v1().sub(cs.velocity()).length();
            double vInf = s.get().v2().sub(vE).length();
            double entry = Math.sqrt(vInf * vInf + vEsc2), brake = Math.max(0, entry - maxEntry);
            if (best == null || burn + brake < best.totalDv()) {
                best = new Return(comet.id(), burn, brake, entry - brake, tof, tDep, tArr, cs.position().length() / AstroConstants.AU);
            }
        }
        return best;
    }

    /** The cheaper of the perihelion and aphelion rendezvous. */
    public static Optional<Result> best(BodySystem sys, Comet comet, double after) {
        Optional<Result> p = estimate(sys, comet, Point.PERIHELION, after), a = estimate(sys, comet, Point.APHELION, after);
        if (p.isEmpty()) return a;
        if (a.isEmpty()) return p;
        return p.get().totalDv() <= a.get().totalDv() ? p : a;
    }
}
