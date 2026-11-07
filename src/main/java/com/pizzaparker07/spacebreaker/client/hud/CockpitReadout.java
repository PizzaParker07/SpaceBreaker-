package com.pizzaparker07.spacebreaker.client.hud;

import net.minecraft.network.chat.Component;

/**
 * What a spacecraft shows its pilot, supplied by the craft's client code through {@link CockpitHud#setSource}. Plain
 * numbers in real units (metres, m/s, seconds), so the HUD never depends on how the flight is simulated.
 *
 * @param target     what speed and distance are measured against (a body, a station, another craft)
 * @param relSpeed   speed relative to the target, m/s; positive when moving away, negative when closing in
 * @param distance   distance to the target (or altitude above it), m
 * @param altitude   {@code distance} is an altitude above the target's surface
 * @param dv         Δv left in the tanks, m/s
 * @param dvMax      Δv with full tanks, m/s
 * @param assist     flight assist (velocity damping) is on
 * @param periapsis  lowest point of the current orbit above the surface, m (NaN when unknown)
 * @param apoapsis   highest point, m (NaN when unknown, +∞ on an escape path)
 * @param event      what ends the current path (entering a moon's sphere, leaving, impact), null for none
 * @param eventIn    seconds until that event
 * @param nodeDv     Δv of the planned manoeuvre node, m/s (NaN when none)
 * @param nodeIn     seconds until the node
 */
public record CockpitReadout(Component target, double relSpeed, double distance, boolean altitude, double dv, double dvMax,
                             boolean assist, double periapsis, double apoapsis, Component event, double eventIn,
                             double nodeDv, double nodeIn) {
    /** Share of the Δv left, 0..1. */
    public float dvFraction() {
        return dvMax > 0 ? (float) Math.max(0, Math.min(1, dv / dvMax)) : 0F;
    }

    public boolean hasNode() {
        return !Double.isNaN(nodeDv);
    }
}
