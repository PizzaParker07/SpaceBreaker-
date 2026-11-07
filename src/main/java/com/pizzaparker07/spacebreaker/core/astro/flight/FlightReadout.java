package com.pizzaparker07.spacebreaker.core.astro.flight;

import java.util.List;

/**
 * What a cockpit display shows, as plain data a server can build and sync (no client types). The HUD turns
 * {@code targetId} into a translated name.
 *
 * @param targetId           body id ("spacebreaker:moon") or entity id the readout refers to
 * @param relSpeed           speed relative to the target, m/s; positive when receding
 * @param distance           distance to the target, m (altitude above its surface when {@code distanceIsAltitude})
 * @param distanceIsAltitude whether {@code distance} is an altitude
 * @param dv                 Δv left, m/s
 * @param dvMax              Δv when full, m/s
 * @param assist             flight assist on
 * @param path               predicted conics (may be empty)
 * @param nodeDv             Δv of the next planned manoeuvre, m/s (NaN when none)
 * @param nodeInSeconds      time until it, s (NaN when none)
 */
public record FlightReadout(String targetId, double relSpeed, double distance, boolean distanceIsAltitude, double dv,
                            double dvMax, boolean assist, List<OrbitPredictor.Segment> path, double nodeDv,
                            double nodeInSeconds) {
    public FlightReadout {
        path = path == null ? List.of() : List.copyOf(path);
    }

    public boolean hasNode() {
        return !Double.isNaN(nodeDv) && !Double.isNaN(nodeInSeconds);
    }

    /** Fraction of Δv left, 0..1. */
    public double dvFraction() {
        return dvMax <= 0 ? 0 : Math.max(0, Math.min(1, dv / dvMax));
    }
}
