package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/** Textbook Δv budget for the Earth-Moon system (m/s), used when the astrophysics data is not available. */
public final class SimpleTransfers implements TransferModel {
    private final Map<Location, Map<Location, Double>> table = new HashMap<>();

    public SimpleTransfers() {
        put(Location.EARTH_SURFACE, Location.EARTH_ORBIT, 9400);  // launch to a 200 km orbit
        put(Location.EARTH_ORBIT, Location.EARTH_SURFACE, 120);   // deorbit burn, the heat shield does the rest
        put(Location.EARTH_ORBIT, Location.MOON_ORBIT, 4000);     // trans-lunar injection 3150 + lunar orbit insertion 850
        put(Location.MOON_ORBIT, Location.MOON_SURFACE, 1870);    // powered descent from a 100 km orbit
        put(Location.MOON_SURFACE, Location.MOON_ORBIT, 1870);    // ascent
        put(Location.MOON_ORBIT, Location.EARTH_SURFACE, 850);    // trans-Earth injection, direct reentry
        put(Location.MOON_ORBIT, Location.EARTH_ORBIT, 1300);     // trans-Earth injection + aerocapture trim
    }

    private void put(Location from, Location to, double dv) {
        table.computeIfAbsent(from, k -> new HashMap<>()).put(to, dv);
    }

    @Override
    public OptionalDouble deltaV(Location from, Location to) {
        Double v = table.getOrDefault(from, Map.of()).get(to);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }
}
