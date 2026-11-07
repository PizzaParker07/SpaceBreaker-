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

    public enum Point {
        PERIHELION, APHELION
    }

    /**
     * @param departureDv   burn in low Earth orbit, m/s
     * @param arrivalDv     velocity match at the comet, m/s
     * @param flightSeconds time of flight
     * @param departureTime simulated time of departure (s from J2000)
     * @param arrivalTime   simulated time of arrival
     * @param arrivalAu     distance from the Sun at arrival
     */
    public record Result(Point point, double departureDv, double arrivalDv, double flightSeconds, double departureTime,
                         double arrivalTime, double arrivalAu) {
        public double totalDv() {
            return departureDv + arrivalDv;
        }

        public double flightYears() {
            return flightSeconds / AstroConstants.YEAR;
        }
    }

    private CometRendezvous() {
    }

    /** Cheapest direct rendezvous at the first perihelion or aphelion passage at or after {@code after}. */
    public static Optional<Result> estimate(BodySystem sys, Comet comet, Point point, double after) {
        AstroBody earth = sys.require("spacebreaker:earth");
        double period = comet.periodSeconds();
        double passage = comet.nextPerihelion(after) + (point == Point.APHELION ? period / 2 : 0);
        double rArrive = (point == Point.PERIHELION ? comet.perihelionAu() : comet.aphelionAu()) * AstroConstants.AU;
        // transfer times: from a month up to twice the Hohmann time to the arrival distance
        double aT = 0.5 * (AstroConstants.AU + rArrive);
        double hohmann = Math.PI * Math.sqrt(aT * aT * aT / AstroConstants.MU_SUN);
        double minTof = 30 * AstroConstants.DAY, maxTof = Math.max(2 * hohmann, 400 * AstroConstants.DAY);
        double rLeo = earth.lowOrbitRadius(), vLeo = Math.sqrt(earth.mu() / rLeo);
        Result best = null;
        for (int ia = 0; ia < ARRIVAL_STEPS; ia++) {
            double tArr = passage + (ia / (ARRIVAL_STEPS - 1.0) * 2 - 1) * ARRIVAL_WINDOW_DAYS * AstroConstants.DAY;
            StateVector cs = Kepler.stateFromElements(comet.elements(), tArr);
            for (int it = 0; it < TOF_STEPS; it++) {
                double tof = minTof * Math.pow(maxTof / minTof, it / (TOF_STEPS - 1.0));
                double tDep = tArr - tof;
                Vec3 rE = sys.position(earth, tDep), vE = sys.velocity(earth, tDep);
                for (boolean prograde : new boolean[]{true, false}) {
                    Optional<Lambert.Solution> s = Lambert.solve(rE, cs.position(), tof, AstroConstants.MU_SUN, prograde);
                    if (s.isEmpty()) continue;
                    double vInf = s.get().v1().sub(vE).length();
                    double dep = Math.sqrt(vInf * vInf + 2 * earth.mu() / rLeo) - vLeo;
                    double arr = cs.velocity().sub(s.get().v2()).length();
                    if (best == null || dep + arr < best.totalDv()) {
                        best = new Result(point, dep, arr, tof, tDep, tArr, cs.position().length() / AstroConstants.AU);
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** The cheaper of the perihelion and aphelion rendezvous. */
    public static Optional<Result> best(BodySystem sys, Comet comet, double after) {
        Optional<Result> p = estimate(sys, comet, Point.PERIHELION, after), a = estimate(sys, comet, Point.APHELION, after);
        if (p.isEmpty()) return a;
        if (a.isEmpty()) return p;
        return p.get().totalDv() <= a.get().totalDv() ? p : a;
    }
}
