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
 * @param targetPos  where the target's centre is in the world, blocks (for the on-screen marker), null for none
 * @param targetSize its drawn radius, blocks
 */
public record CockpitReadout(Component target, double relSpeed, double distance, boolean altitude, double dv, double dvMax,
                             boolean assist, double periapsis, double apoapsis, Component event, double eventIn,
                             double nodeDv, double nodeIn, net.minecraft.world.phys.Vec3 targetPos, double targetSize) {
    public CockpitReadout(Component target, double relSpeed, double distance, boolean altitude, double dv, double dvMax,
                          boolean assist, double periapsis, double apoapsis, Component event, double eventIn,
                          double nodeDv, double nodeIn) {
        this(target, relSpeed, distance, altitude, dv, dvMax, assist, periapsis, apoapsis, event, eventIn, nodeDv, nodeIn, null, 0);
    }

    /**
     * The same readout with the target marked on screen: its centre in world blocks (for a scene body, its
     * SpaceSceneLayout centre) and its drawn radius.
     */
    public CockpitReadout withTarget(net.minecraft.world.phys.Vec3 pos, double radius) {
        return new CockpitReadout(target, relSpeed, distance, altitude, dv, dvMax, assist, periapsis, apoapsis, event, eventIn,
                nodeDv, nodeIn, pos, radius);
    }

    /**
     * From what a craft syncs ({@link com.pizzaparker07.spacebreaker.core.astro.flight.FlightReadout}): the target's
     * name, and periapsis/apoapsis (as altitudes above the body) and the event ending the first conic of the path.
     */
    public static CockpitReadout of(com.pizzaparker07.spacebreaker.core.astro.flight.FlightReadout r) {
        double pe = Double.NaN, ap = Double.NaN, eventIn = Double.NaN;
        Component event = null;
        if (!r.path().isEmpty()) {
            var seg = r.path().get(0);
            double radius = seg.body().radius();
            pe = seg.periapsis() - radius;
            double apo = seg.apoapsis();
            ap = Double.isNaN(apo) || apo < 0 || Double.isInfinite(apo) ? Double.POSITIVE_INFINITY : apo - radius;
            event = switch (seg.endEvent()) {
                case ENTER_SOI -> seg.next() == null ? null
                        : Component.translatable("hud.spacebreaker.cockpit.event.soi_enter", name(seg.next().id()));
                case LEAVE_SOI -> Component.translatable("hud.spacebreaker.cockpit.event.soi_exit");
                case IMPACT -> Component.translatable("hud.spacebreaker.cockpit.event.impact");
                case NONE -> null;
            };
            eventIn = event == null ? Double.NaN : seg.end() - seg.start();
        }
        return new CockpitReadout(name(r.targetId()), r.relSpeed(), r.distance(), r.distanceIsAltitude(), r.dv(), r.dvMax(),
                r.assist(), pe, ap, event, eventIn, r.hasNode() ? r.nodeDv() : Double.NaN, r.nodeInSeconds());
    }

    /** A body id ("spacebreaker:moon" or "moon") as its translated name; anything else as written. */
    public static Component name(String id) {
        if (id == null || id.isEmpty()) {
            return Component.empty();
        }
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "spacebreaker";
        String key = "body." + ns + "." + path;
        return net.minecraft.locale.Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(id);
    }

    /** Share of the Δv left, 0..1. */
    public float dvFraction() {
        return dvMax > 0 ? (float) Math.max(0, Math.min(1, dv / dvMax)) : 0F;
    }

    public boolean hasNode() {
        return !Double.isNaN(nodeDv);
    }
}
