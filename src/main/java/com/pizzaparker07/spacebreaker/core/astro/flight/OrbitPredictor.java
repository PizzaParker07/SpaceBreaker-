package com.pizzaparker07.spacebreaker.core.astro.flight;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.StateVector;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Patched-conic trajectory prediction for the flight HUD: the current conic around the dominant body, and where the
 * craft enters a moon's sphere of influence, leaves its own, or hits the surface.
 */
public final class OrbitPredictor {
    public enum End {
        NONE, ENTER_SOI, LEAVE_SOI, IMPACT
    }

    /**
     * One piece of the predicted path.
     *
     * @param body     the body the conic is around
     * @param elements conic elements (mean anomaly at {@code start})
     * @param start    simulated time the segment starts
     * @param end      time it ends (horizon if nothing happens)
     * @param endEvent what ends it
     * @param next     the body whose sphere of influence comes next (for SOI events)
     */
    public record Segment(AstroBody body, OrbitalElements elements, double start, double end, End endEvent, AstroBody next) {
        public double periapsis() {
            return elements.periapsis();
        }

        public double apoapsis() {
            return elements.apoapsis();
        }
    }

    private OrbitPredictor() {
    }

    /** Sphere of influence of a body around its parent (infinite for a star). */
    public static double soi(BodySystem sys, AstroBody b) {
        Optional<AstroBody> p = sys.parent(b);
        if (p.isEmpty() || b.orbit() == null) return Double.POSITIVE_INFINITY;
        return Kepler.sphereOfInfluence(b.orbit().semiMajorAxis(), b.mass(), p.get().mass());
    }

    /**
     * Predicts up to {@code maxSegments} conics from a state relative to {@code body} at time {@code t}.
     *
     * @param horizon how far ahead to look, s
     * @param step    sampling step for event search, s (refined by bisection)
     */
    public static List<Segment> predict(BodySystem sys, AstroBody body, StateVector rel, double t, double horizon, double step,
                                        int maxSegments) {
        List<Segment> out = new ArrayList<>();
        AstroBody cur = body;
        StateVector s = rel;
        double t0 = t, tEnd = t + horizon;
        while (out.size() < maxSegments && t0 < tEnd) {
            OrbitalElements el = Kepler.elementsFromState(s, cur.mu());
            final AstroBody c = cur;
            final double start = t0;
            double soi = soi(sys, cur);
            List<AstroBody> moons = sys.children(cur.id());
            End event = End.NONE;
            AstroBody next = null;
            double hit = tEnd;
            double prev = t0;
            for (double tt = t0 + step; tt <= tEnd + 1e-9; tt += step) {
                Vec3 p = el.stateAt(tt - start).position();
                double r = p.length();
                if (r < c.radius()) {
                    hit = refine(prev, tt, x -> el.stateAt(x - start).position().length() < c.radius());
                    event = End.IMPACT;
                    break;
                }
                if (r > soi) {
                    hit = refine(prev, tt, x -> el.stateAt(x - start).position().length() > soi);
                    event = End.LEAVE_SOI;
                    next = sys.parent(c).orElse(null);
                    break;
                }
                AstroBody entered = null;
                for (AstroBody m : moons) {
                    if (m.orbit() == null) continue;
                    double ms = soi(sys, m);
                    if (p.sub(sys.relativePosition(m, tt)).length() < ms) {
                        entered = m;
                        break;
                    }
                }
                if (entered != null) {
                    final AstroBody em = entered;
                    final double ms = soi(sys, em);
                    hit = refine(prev, tt, x -> el.stateAt(x - start).position().sub(sys.relativePosition(em, x)).length() < ms);
                    event = End.ENTER_SOI;
                    next = em;
                    break;
                }
                prev = tt;
            }
            out.add(new Segment(cur, el, start, hit, event, next));
            if (event == End.NONE || event == End.IMPACT || next == null) break;
            // hand over to the next body's frame
            StateVector at = el.stateAt(hit - start);
            if (event == End.ENTER_SOI) {
                StateVector moon = next.orbit().stateAt(hit);
                s = new StateVector(at.position().sub(moon.position()), at.velocity().sub(moon.velocity()));
            } else {
                StateVector self = cur.orbit().stateAt(hit);
                s = new StateVector(at.position().add(self.position()), at.velocity().add(self.velocity()));
            }
            cur = next;
            t0 = hit;
        }
        return out;
    }

    private static double refine(double lo, double hi, java.util.function.DoublePredicate inside) {
        for (int i = 0; i < 60; i++) {
            double mid = 0.5 * (lo + hi);
            if (inside.test(mid)) hi = mid;
            else lo = mid;
        }
        return hi;
    }
}
