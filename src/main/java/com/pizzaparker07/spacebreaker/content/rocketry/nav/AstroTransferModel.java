package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import net.minecraft.resources.ResourceLocation;
import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.TransferPlanner;
import net.minecraft.core.RegistryAccess;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Δv for any pair of places from the astrophysics library ({@code core/astro}):
 * <ul>
 *   <li>surface → own low orbit: ascent with gravity and drag losses;</li>
 *   <li>low orbit → own surface: powered descent, or deorbit + aerobraking + parachutes where there is air;</li>
 *   <li>low orbit → low orbit of another body: patched-conic transfer (aerocapture where the target has air);</li>
 *   <li>low orbit → surface of another body with air: departure burn and a direct entry.</li>
 * </ul>
 * Results are cached; anything the library cannot answer falls back to {@link SimpleTransfers}.
 */
public final class AstroTransferModel implements TransferModel {
    /** Thrust-to-weight assumed for the timing of landings and launches (a typical lander, Apollo-like). */
    static final double SURFACE_TWR = 2.0;

    private final BodySystem sys;
    private final TransferModel fallback = new SimpleTransfers();
    private final Map<String, OptionalDouble> dv = new ConcurrentHashMap<>();
    private final Map<String, OptionalDouble> times = new ConcurrentHashMap<>();

    public AstroTransferModel(BodySystem sys) {
        this.sys = sys;
    }

    private static String key(Location a, Location b) {
        return a.id() + ">" + b.id();
    }

    @Override
    public OptionalDouble deltaV(Location from, Location to) {
        if (from.equals(to)) {
            return OptionalDouble.empty();
        }
        // comets: realistic rendezvous, and none at all for the ones that are only to be watched
        if (CometRoutes.ready() && (CometRoutes.isComet(from.body()) || CometRoutes.isComet(to.body())) && !from.body().equals(to.body())) {
            return dv.computeIfAbsent(key(from, to), k -> comet(from, to));
        }
        OptionalDouble v = dv.computeIfAbsent(key(from, to), k -> compute(from, to));
        return v.isPresent() ? v : fallback.deltaV(from, to);
    }

    private OptionalDouble compute(Location from, Location to) {
        try {
            String a = from.body().toString();
            String b = to.body().toString();
            if (sys.get(a).isEmpty() || sys.get(b).isEmpty()) {
                return OptionalDouble.empty();
            }
            AstroBody fromBody = sys.require(a);
            if (from.body().equals(to.body())) {
                // timed surface legs (astro): ascent ≈ 7.5 min on the Moon, descent ≈ 1 h with the half-orbit coast
                if (!from.orbit() && to.orbit()) {
                    times.put(key(from, to), finite(TransferPlanner.ascentLeg(fromBody, SURFACE_TWR).totalSeconds()));
                    return finite(TransferPlanner.ascent(fromBody));
                }
                if (from.orbit() && !to.orbit()) {
                    times.put(key(from, to), finite(TransferPlanner.descentLeg(fromBody, to.atmosphere(), SURFACE_TWR).totalSeconds()));
                    return finite(TransferPlanner.descent(fromBody, to.atmosphere()));
                }
                return OptionalDouble.empty();
            }
            if (!from.orbit()) {
                return OptionalDouble.empty(); // reach orbit first
            }
            if (to.orbit()) {
                // the cheaper of a direct transfer and a tour of gravity assists (inner system: Mercury via Venus)
                TransferPlanner.Plan p = TransferPlanner.planWithAssists(sys, a, b, to.atmosphere());
                times.put(key(from, to), finite(p.transferTime()));
                return finite(p.total());
            }
            if (to.atmosphere()) {
                TransferPlanner.Plan p = TransferPlanner.planWithAssists(sys, a, b, true);
                times.put(key(from, to), finite(p.transferTime()));
                double departure = p.deltaV("departure");
                return finite(departure > 0 ? departure : p.total());
            }
            return OptionalDouble.empty();
        } catch (RuntimeException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Orbit to orbit between a comet and anywhere else: the comet's rendezvous (departure from low orbit plus the
     * speed match, the same both ways). From a comet straight down into an atmosphere (home to Earth): only the
     * burn away from the comet. Other surfaces go through their own orbit first.
     */
    private OptionalDouble comet(Location from, Location to) {
        if (!from.orbit()) {
            return OptionalDouble.empty();
        }
        boolean leaving = CometRoutes.isComet(from.body()) && !CometRoutes.isComet(to.body());
        if (!to.orbit() && !(leaving && to.atmosphere())) {
            return OptionalDouble.empty();
        }
        ResourceLocation comet = leaving ? from.body() : to.body();
        return CometRoutes.route(comet).map(r -> {
            times.put(key(from, to), OptionalDouble.of(r.flightSeconds()));
            // straight home into the air: only the burn away from the comet; the atmosphere does the braking
            return OptionalDouble.of(to.orbit() ? r.totalDv() : r.arrivalDv());
        }).orElse(OptionalDouble.empty());
    }

    private static OptionalDouble finite(double v) {
        return Double.isFinite(v) && v >= 0 ? OptionalDouble.of(v) : OptionalDouble.empty();
    }

    /**
     * Real Δv to climb from a surface to its low orbit for a rocket lifting off with this thrust-to-weight
     * ratio: a sluggish rocket loses more to gravity, a punchy one less (clamped to 1.05–4).
     */
    public OptionalDouble ascent(Location from, double twr) {
        try {
            var body = sys.get(from.body().toString());
            if (body.isEmpty() || from.orbit()) {
                return OptionalDouble.empty();
            }
            return finite(TransferPlanner.ascent(body.get(), Math.max(1.05, Math.min(4.0, twr))));
        } catch (RuntimeException e) {
            return OptionalDouble.empty();
        }
    }

    /** Real coast time of a transfer in seconds, if known. */
    public OptionalDouble transferSeconds(Location from, Location to) {
        deltaV(from, to);
        return times.getOrDefault(key(from, to), OptionalDouble.empty());
    }

    /** Installs the places and the library-backed model for this world's body data. */
    public static void install(RegistryAccess access) {
        Locations.install(access);
        try {
            BodySystem sys = BodyAdapter.system(access).orElse(null);
            boolean astro = sys != null && sys.get("spacebreaker:earth").isPresent();
            if (astro) {
                CometRoutes.install(sys);
            }
            Transfers.setModel(astro ? new AstroTransferModel(sys) : new SimpleTransfers());
        } catch (RuntimeException e) {
            Transfers.setModel(new SimpleTransfers());
        }
    }
}
