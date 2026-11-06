package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.TransferPlanner;
import net.minecraft.core.RegistryAccess;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Δv costs from the astrophysics library ({@code core/astro}): ascent and descent with gravity and drag
 * losses, patched-conic Earth-Moon transfers, aerocapture on the way home. Values are computed once
 * from the celestial body data; routes the library cannot answer fall back to {@link SimpleTransfers}.
 */
public final class AstroTransferModel implements TransferModel {
    private static final String EARTH = "spacebreaker:earth";
    private static final String MOON = "spacebreaker:moon";

    private final Map<Location, Map<Location, Double>> table = new EnumMap<>(Location.class);
    private final Map<Location, Map<Location, Double>> times = new EnumMap<>(Location.class);
    private final TransferModel fallback = new SimpleTransfers();

    public AstroTransferModel(BodySystem sys) {
        AstroBody earth = sys.require(EARTH);
        AstroBody moon = sys.require(MOON);
        put(Location.EARTH_SURFACE, Location.EARTH_ORBIT, TransferPlanner.ascent(earth));
        put(Location.EARTH_ORBIT, Location.EARTH_SURFACE, TransferPlanner.descent(earth, true));
        TransferPlanner.Plan out = TransferPlanner.plan(sys, EARTH, MOON, false);
        put(Location.EARTH_ORBIT, Location.MOON_ORBIT, out.total());
        time(Location.EARTH_ORBIT, Location.MOON_ORBIT, out.transferTime());
        put(Location.MOON_ORBIT, Location.MOON_SURFACE, TransferPlanner.descent(moon, false));
        put(Location.MOON_SURFACE, Location.MOON_ORBIT, TransferPlanner.ascent(moon));
        TransferPlanner.Plan home = TransferPlanner.plan(sys, MOON, EARTH, true);
        put(Location.MOON_ORBIT, Location.EARTH_SURFACE, home.deltaV("departure") > 0 ? home.deltaV("departure") : home.total());
        put(Location.MOON_ORBIT, Location.EARTH_ORBIT, home.total());
        time(Location.MOON_ORBIT, Location.EARTH_ORBIT, home.transferTime());
        time(Location.MOON_ORBIT, Location.EARTH_SURFACE, home.transferTime());
    }

    private void time(Location from, Location to, double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) {
            times.computeIfAbsent(from, k -> new EnumMap<>(Location.class)).put(to, seconds);
        }
    }

    /** Real coast time of a transfer in seconds (planet-moon windows are always open), if known. */
    public OptionalDouble transferSeconds(Location from, Location to) {
        Double v = times.getOrDefault(from, Map.of()).get(to);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    private void put(Location from, Location to, double dv) {
        if (Double.isFinite(dv) && dv >= 0) {
            table.computeIfAbsent(from, k -> new EnumMap<>(Location.class)).put(to, dv);
        }
    }

    @Override
    public OptionalDouble deltaV(Location from, Location to) {
        Double v = table.getOrDefault(from, Map.of()).get(to);
        return v != null ? OptionalDouble.of(v) : fallback.deltaV(from, to);
    }

    /** Installs the library-backed model for this world's body data (keeps the simple table if it is missing). */
    public static void install(RegistryAccess access) {
        try {
            BodySystem sys = BodyAdapter.system(access).orElse(null);
            Transfers.setModel(sys != null && sys.get(EARTH).isPresent() && sys.get(MOON).isPresent()
                    ? new AstroTransferModel(sys) : new SimpleTransfers());
        } catch (RuntimeException e) {
            Transfers.setModel(new SimpleTransfers());
        }
    }
}
