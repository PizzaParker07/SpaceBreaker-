package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometRendezvous;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.Optional;

/**
 * Flights to comets from astro's realistic estimates (CometRendezvous): there, a transfer from low Earth orbit that
 * matches the comet's speed anywhere along its orbit (inclination and retrograde orbits included), the cheapest within
 * {@link #MAX_DV} and {@link #MAX_YEARS}; home, a flight from the comet straight into Earth's air with a braking burn
 * when it would arrive faster than {@link #MAX_ENTRY_SPEED}. A comet with no passage inside the budget (Hale-Bopp) is
 * for observation only. Worked out once per world from the J2000 start of the simulated clock, so the numbers are the
 * same for every player and for the progression model.
 */
public final class CometRoutes {
    /** Real Δv budget for a comet rendezvous, m/s (from low Earth orbit, departure + speed match). */
    public static final double MAX_DV = 25_000;
    /** Longest one-way flight still offered, years (each way). */
    public static final double MAX_YEARS = 20;
    /** Above this real Δv a comet needs a nuclear thermal engine in practice (the map says so). */
    public static final double NUCLEAR_HINT_DV = 15_000;
    /** Perihelion and aphelion passages considered from the start of the clock. */
    public static final int PASSAGES = 2;
    /** Time spent at the comet before the flight home can leave, seconds. */
    public static final double STAY_SECONDS = 30 * 86_400.0;
    /** Fastest a capsule may hit Earth's air on the way home, m/s; faster arrivals brake first. */
    public static final double MAX_ENTRY_SPEED = 15_000;

    /** One comet's flights: there, and straight home into Earth's air. */
    public record Route(CometRendezvous.Result out, java.util.Optional<CometRendezvous.Return> home) {
    }

    private static volatile java.util.Map<String, Route> routes;

    private CometRoutes() {
    }

    /**
     * Works out every comet's flights for this world's bodies (several seconds: the rendezvous table, then one return
     * search per comet). The server does it once when the transfer model is installed; the client in the background.
     */
    public static void install(BodySystem sys) {
        try {
            CometRendezvous.Table table = CometRendezvous.Table.compute(sys, 0, PASSAGES);
            java.util.Map<String, Route> out = new java.util.HashMap<>();
            for (var comet : com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.CATALOGUE) {
                table.of(comet.id()).stream().filter(r -> r.within(MAX_DV, MAX_YEARS))
                        .min(Comparator.comparingDouble(CometRendezvous.Result::totalDv))
                        .ifPresent(there -> out.put(comet.id(), new Route(there, CometRendezvous.returnTrip(sys, comet,
                                there.arrivalTime() + STAY_SECONDS, MAX_YEARS, MAX_ENTRY_SPEED))));
            }
            routes = java.util.Map.copyOf(out);
        } catch (RuntimeException e) {
            routes = null;
        }
    }

    /** Whether the comet flights are known (until then comets are left out of the transfer model's cache). */
    public static boolean ready() {
        return routes != null;
    }

    /** The comet behind a body id, by its catalogue name ("spacebreaker:halley" → halley). */
    public static boolean isComet(ResourceLocation body) {
        return com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(body.getPath()).isPresent();
    }

    /** The comet's flights, if it can be visited at all. */
    public static Optional<Route> of(ResourceLocation body) {
        java.util.Map<String, Route> r = routes;
        return r == null ? Optional.empty() : Optional.ofNullable(r.get(body.getPath()));
    }

    /** The flight there: the cheapest passage within the budget, if any. */
    public static Optional<CometRendezvous.Result> route(ResourceLocation body) {
        return of(body).map(Route::out);
    }

    /** A comet no passage of which fits the budget: it can be watched, not visited. */
    public static boolean observationOnly(ResourceLocation body) {
        return ready() && isComet(body) && of(body).isEmpty();
    }

    /** Whether reaching this comet takes more than chemical rockets can sensibly give (the map hints at nuclear). */
    public static boolean needsNuclear(ResourceLocation body) {
        return route(body).map(r -> r.totalDv() > NUCLEAR_HINT_DV).orElse(false);
    }
}
