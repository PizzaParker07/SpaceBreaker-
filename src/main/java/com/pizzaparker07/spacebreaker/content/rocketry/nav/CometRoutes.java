package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.CometRendezvous;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.Optional;

/**
 * Flights to comets from astro's realistic rendezvous estimates (CometRendezvous): a transfer from low Earth orbit
 * to the comet at a perihelion or aphelion passage, matching its speed there, inclination and retrograde orbits
 * included. Each comet uses its cheapest passage within {@link #MAX_DV} and {@link #MAX_YEARS}; a comet with no
 * passage inside that budget (Hale-Bopp) is for observation only. The table is computed once per world, from the
 * J2000 start of the simulated clock, so the numbers are the same for every player and for the progression model.
 */
public final class CometRoutes {
    /** Real Δv budget for a comet rendezvous, m/s (from low Earth orbit, departure + speed match). */
    public static final double MAX_DV = 25_000;
    /** Longest one-way flight still offered, years. */
    public static final double MAX_YEARS = 50;
    /** Above this real Δv a comet needs a nuclear thermal engine in practice (the map says so). */
    public static final double NUCLEAR_HINT_DV = 15_000;
    /** Perihelion and aphelion passages considered from the start of the clock. */
    public static final int PASSAGES = 2;

    private static volatile CometRendezvous.Table table;

    private CometRoutes() {
    }

    /** Builds the table for this world's bodies (a second or two; once, when the transfer model is installed). */
    public static void install(BodySystem sys) {
        try {
            table = CometRendezvous.Table.compute(sys, 0, PASSAGES);
        } catch (RuntimeException e) {
            table = null;
        }
    }

    /** Whether the rendezvous table is known (otherwise comets fall back to the simple model). */
    public static boolean ready() {
        return table != null;
    }

    /** The comet behind a body id, by its catalogue name ("spacebreaker:halley" → halley). */
    public static boolean isComet(ResourceLocation body) {
        return com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(body.getPath()).isPresent();
    }

    /** The passage flights use for this comet: the cheapest within the budget, if any. */
    public static Optional<CometRendezvous.Result> route(ResourceLocation body) {
        CometRendezvous.Table t = table;
        if (t == null) {
            return Optional.empty();
        }
        return t.of(body.getPath()).stream().filter(r -> r.within(MAX_DV, MAX_YEARS))
                .min(Comparator.comparingDouble(CometRendezvous.Result::totalDv));
    }

    /** A comet no passage of which fits the budget: it can be watched, not visited. */
    public static boolean observationOnly(ResourceLocation body) {
        return ready() && isComet(body) && route(body).isEmpty();
    }

    /** Whether reaching this comet takes more than chemical rockets can sensibly give (the map hints at nuclear). */
    public static boolean needsNuclear(ResourceLocation body) {
        return route(body).map(r -> r.totalDv() > NUCLEAR_HINT_DV).orElse(false);
    }
}
