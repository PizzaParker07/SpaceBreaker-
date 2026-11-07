package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Brute-force rocket designer: every assembler-legal design (up to three stages, one engine, one to three tanks,
 * an optional radial part and a separator per stage; heat shield, crew capsule and parachute on top) built from
 * the parts the player can use, smallest first. Used to answer "can the best rocket buildable at this point fly
 * this mission?".
 */
final class RocketSearch {
    /**
     * A design that can lift off from Earth, with its vacuum Δv. The layout is rebuilt on demand rather than kept: the
     * search holds hundreds of thousands of candidates, and their layouts were most of the memory.
     */
    record Candidate(RocketDesign design, Function<ResourceLocation, RocketPart> lookup, double totalDv) {
        RocketLayout layout() {
            return RocketSearch.layout(design, lookup);
        }
    }

    /** Outcome of a mission check: the smallest design that flies it, or null. */
    record Result(Candidate rocket, String mode, double need) {
    }

    /** Earth surface gravity, a little low, and the TWR the callers need (1.05): only designs no caller can launch are dropped. */
    private static final double LAUNCH_G = 0.99 * com.pizzaparker07.spacebreaker.core.body.CelestialBody.EARTH_GRAVITY;
    private static final double MIN_TWR = 1.05;

    /**
     * What-if only (see {@code DensityWhatIfTests}): multiplies the propellant a stage's tanks hold by a factor per
     * propellant. Empty in every normal run.
     */
    static final java.util.Map<Propellant, Double> DENSITY = new java.util.EnumMap<>(Propellant.class);

    /** A layout, with {@link #DENSITY} applied to its stages. */
    static RocketLayout layout(RocketDesign design, Function<ResourceLocation, RocketPart> lookup) {
        RocketLayout l = RocketLayout.of(design, lookup);
        if (DENSITY.isEmpty()) {
            return l;
        }
        List<RocketLayout.Stage> stages = new ArrayList<>();
        try {
            // copy each stage record with only propellantKg scaled, whatever other components the record has
            var comps = RocketLayout.Stage.class.getRecordComponents();
            Class<?>[] types = new Class<?>[comps.length];
            for (int i = 0; i < comps.length; i++) {
                types[i] = comps[i].getType();
            }
            var ctor = RocketLayout.Stage.class.getDeclaredConstructor(types);
            for (RocketLayout.Stage s : l.stages()) {
                double f = s.propellant() == null ? 1.0 : DENSITY.getOrDefault(s.propellant(), 1.0);
                Object[] args = new Object[comps.length];
                for (int i = 0; i < comps.length; i++) {
                    args[i] = comps[i].getAccessor().invoke(s);
                    if (comps[i].getName().equals("propellantKg")) {
                        args[i] = s.propellantKg() * f;
                    }
                }
                stages.add(ctor.newInstance(args));
            }
            var c = RocketLayout.class.getDeclaredConstructor(RocketDesign.class, List.class, List.class, RocketLayout.Payload.class, List.class);
            c.setAccessible(true);
            return c.newInstance(l.design(), l.parts(), List.copyOf(stages), l.payload(), l.problems());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot rebuild a rocket layout for the density what-if", e);
        }
    }

    /** What-if only: parts the search must not use (e.g. the nuclear thermal engine, to see what chemistry alone reaches). */
    static final java.util.Set<ResourceLocation> EXCLUDED = new java.util.HashSet<>();

    /** Upper stages may also use the insulated versions of every tank set (when those parts exist and are unlocked). */
    static boolean INSULATED = true;

    private static final List<List<String>> TANK_SETS = List.of(
            List.of("fuel_tank_small"), List.of("fuel_tank_medium"), List.of("fuel_tank_large"),
            List.of("fuel_tank_large", "fuel_tank_medium"), List.of("fuel_tank_large", "fuel_tank_large"),
            List.of("fuel_tank_large", "fuel_tank_large", "fuel_tank_medium"),
            List.of("fuel_tank_large", "fuel_tank_large", "fuel_tank_large"));

    private RocketSearch() {
    }

    /** All designs from these parts whose stage propellants are in {@code fuels}, fewest parts first. */
    static List<Candidate> designs(Function<ResourceLocation, RocketPart> lookup, Set<ResourceLocation> parts, Set<Propellant> fuels) {
        return designs(lookup, parts, fuels, null);
    }

    /** With {@code uncrewed} set, the payload is that satellite or probe (optionally on a heat shield) instead of a capsule. */
    static List<Candidate> designs(Function<ResourceLocation, RocketPart> lookup, Set<ResourceLocation> parts, Set<Propellant> fuels,
                                   ResourceLocation uncrewed) {
        List<Candidate> out = new ArrayList<>();
        if (uncrewed == null ? !parts.contains(Spacebreaker.id("crew_capsule")) : !parts.contains(uncrewed)) {
            return out;
        }
        List<ResourceLocation> engines = new ArrayList<>();
        Map<ResourceLocation, RocketPart> byId = new HashMap<>();
        for (ResourceLocation id : parts) {
            RocketPart p = lookup.apply(id);
            if (p != null) {
                byId.put(id, p);
                if (p.isEngine() && fuels.contains(p.propellant()) && !EXCLUDED.contains(id)) {
                    engines.add(id);
                }
            }
        }
        if (engines.isEmpty()) {
            return out;
        }
        List<List<ResourceLocation>> tanks = new ArrayList<>();
        for (List<String> set : TANK_SETS) {
            List<ResourceLocation> ids = set.stream().map(Spacebreaker::id).toList();
            if (parts.containsAll(ids)) {
                tanks.add(ids);
            }
        }
        if (tanks.isEmpty()) {
            return out;
        }
        ResourceLocation separator = lightest(byId, PartType.DECOUPLER, PartType.INTERSTAGE);
        ResourceLocation fins = lightest(byId, PartType.FINS, PartType.GRID_FINS);
        ResourceLocation rcs = lightest(byId, PartType.RCS);
        List<List<ResourceLocation>> payloads = new ArrayList<>();
        ResourceLocation capsule = Spacebreaker.id("crew_capsule");
        ResourceLocation shield = parts.contains(Spacebreaker.id("heat_shield")) ? Spacebreaker.id("heat_shield") : null;
        ResourceLocation chute = parts.contains(Spacebreaker.id("parachute")) ? Spacebreaker.id("parachute") : null;
        for (int s = 0; s < 2 && uncrewed == null; s++) {
            for (int c = 0; c < 2; c++) {
                if ((s == 1 && shield == null) || (c == 1 && chute == null)) {
                    continue;
                }
                List<ResourceLocation> p = new ArrayList<>();
                if (s == 1) {
                    p.add(shield);
                }
                p.add(capsule);
                if (c == 1) {
                    p.add(chute);
                }
                payloads.add(p);
            }
        }
        if (uncrewed != null) {
            // the satellite or probe sits in the capsule slot, so a heat shield may go under it (AssemblerSlots)
            payloads.add(List.of(uncrewed));
            if (shield != null) {
                payloads.add(List.of(shield, uncrewed));
            }
        }
        List<List<ResourceLocation>> lower = stageOptions(engines, tanks, fins, rcs, true);
        // every caller launches from Earth and skips designs with launch TWR below 1.05: a bottom stage that cannot lift
        // even itself with the lightest payload never can with more stages on top (same thrust, more mass), so drop it
        // here (vacuum-only engines like the nuclear thermal engine would otherwise multiply the search)
        lower.removeIf(option -> {
            for (List<ResourceLocation> payload : payloads) {
                List<ResourceLocation> all = new ArrayList<>(option);
                all.addAll(payload);
                RocketLayout l = layout(new RocketDesign(all), lookup);
                if (!l.problems().isEmpty() || RocketMath.twr(l, 0, l.fullFuel(), LAUNCH_G) >= MIN_TWR) {
                    return false; // keep: either it can lift off, or this check cannot judge it
                }
            }
            return true;
        });
        List<List<ResourceLocation>> upperTanks = new ArrayList<>(tanks);
        if (INSULATED) {
            // insulated tanks only matter against boil-off on long coasts: only the top stage (the one that coasts
            // longest and flies home) gets them, which keeps the search from growing more than twofold
            for (List<String> set : TANK_SETS) {
                List<ResourceLocation> ids = set.stream().map(t -> Spacebreaker.id(t + "_insulated")).toList();
                if (parts.containsAll(ids)) {
                    upperTanks.add(ids);
                }
            }
        }
        List<List<ResourceLocation>> upper = stageOptions(engines, tanks, null, rcs, false);
        List<List<ResourceLocation>> top = stageOptions(engines, upperTanks, null, rcs, false);
        int maxStages = separator == null ? 1 : 3;
        for (int n = 1; n <= maxStages; n++) {
            stack(new ArrayList<>(), 0, n, lower, upper, top, separator, payloads, lookup, out);
        }
        out.sort(Comparator.comparingInt((Candidate c) -> c.design().parts().size()).thenComparingDouble(c -> -c.totalDv()));
        return out;
    }

    private static void stack(List<ResourceLocation> below, int stage, int stages, List<List<ResourceLocation>> lower,
                              List<List<ResourceLocation>> upper, List<List<ResourceLocation>> top, ResourceLocation separator, List<List<ResourceLocation>> payloads,
                              Function<ResourceLocation, RocketPart> lookup, List<Candidate> out) {
        if (stage == stages) {
            for (List<ResourceLocation> payload : payloads) {
                List<ResourceLocation> all = new ArrayList<>(below);
                all.addAll(payload);
                RocketDesign design = new RocketDesign(all);
                RocketLayout layout = layout(design, lookup);
                if (layout.problems().isEmpty() && RocketMath.twr(layout, 0, layout.fullFuel(), LAUNCH_G) >= MIN_TWR) {
                    out.add(new Candidate(design, lookup, RocketMath.totalDeltaV(layout, 0, layout.fullFuel(), 0)));
                }
            }
            return;
        }
        for (List<ResourceLocation> option : stage == 0 ? lower : stage == stages - 1 ? top : upper) {
            List<ResourceLocation> next = new ArrayList<>(below);
            next.addAll(option);
            if (stage < stages - 1) {
                next.add(separator);
            }
            stack(next, stage + 1, stages, lower, upper, top, separator, payloads, lookup, out);
        }
    }

    private static List<List<ResourceLocation>> stageOptions(List<ResourceLocation> engines, List<List<ResourceLocation>> tanks,
                                                             ResourceLocation fins, ResourceLocation rcs, boolean bottom) {
        List<List<ResourceLocation>> out = new ArrayList<>();
        for (ResourceLocation engine : engines) {
            for (List<ResourceLocation> t : tanks) {
                List<ResourceLocation> base = new ArrayList<>();
                base.add(engine);
                base.addAll(t);
                out.add(base);
                if (bottom && fins != null) {
                    List<ResourceLocation> f = new ArrayList<>(base);
                    f.add(fins);
                    out.add(f);
                }
                if (!bottom && rcs != null) {
                    List<ResourceLocation> r = new ArrayList<>(base);
                    r.add(rcs);
                    out.add(r);
                }
            }
        }
        return out;
    }

    private static ResourceLocation lightest(Map<ResourceLocation, RocketPart> parts, PartType... types) {
        ResourceLocation best = null;
        double mass = Double.MAX_VALUE;
        for (var e : parts.entrySet()) {
            for (PartType t : types) {
                if (e.getValue().type() == t && e.getValue().massKg() < mass) {
                    best = e.getKey();
                    mass = e.getValue().massKg();
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ routes

    /** Cheapest route (by gameplay Δv) between two locations over the transfer graph. */
    static List<Location> route(Location from, Location to, SbConfig.Realism realism) {
        Map<Location, Double> dist = new HashMap<>();
        Map<Location, Location> prev = new HashMap<>();
        List<Location> open = new ArrayList<>(Locations.all());
        if (!open.contains(from)) {
            open.add(from);
        }
        dist.put(from, 0.0);
        while (!open.isEmpty()) {
            Location best = null;
            for (Location l : open) {
                if (dist.containsKey(l) && (best == null || dist.get(l) < dist.get(best))) {
                    best = l;
                }
            }
            if (best == null || best.equals(to)) {
                break;
            }
            open.remove(best);
            for (Location n : Transfers.destinations(best)) {
                var c = Transfers.cost(best, n, realism);
                if (c.isEmpty()) {
                    continue;
                }
                double d = dist.get(best) + c.getAsDouble();
                if (d < dist.getOrDefault(n, Double.MAX_VALUE)) {
                    dist.put(n, d);
                    prev.put(n, best);
                }
            }
        }
        if (!dist.containsKey(to)) {
            return List.of();
        }
        List<Location> path = new ArrayList<>();
        for (Location l = to; l != null; l = prev.get(l)) {
            path.add(0, l);
        }
        return path;
    }

    static double need(List<Location> route, SbConfig.Realism realism) {
        double sum = 0;
        for (int i = 0; i + 1 < route.size(); i++) {
            sum += Transfers.cost(route.get(i), route.get(i + 1), realism).orElse(1e9);
        }
        return sum;
    }

    /**
     * Smallest design that flies there and back on what it carries from the pad ("round trip"), or, when
     * {@code fuelsThere} is given, that lands and flies home after refuelling its remaining stages there.
     */
    static Result mission(List<Candidate> designs, Location target, SbConfig.Realism realism, double gLaunch, double gTarget,
                          Set<Propellant> fuelsThere) {
        List<Location> out = route(Location.EARTH_SURFACE, target, realism);
        if (out.isEmpty()) {
            return null;
        }
        List<Location> home = MissionPlanner.homeRoute(target);
        List<Location> round = new ArrayList<>(out);
        round.addAll(home.subList(1, home.size()));
        double needRound = need(round, realism);
        for (Candidate c : designs) {
            if (c.totalDv() < needRound - 500) { // vacuum Δv is an upper bound; bonuses are below 500 m/s
                continue;
            }
            RocketLayout l = c.layout();
            double[] full = l.fullFuel();
            if (RocketMath.twr(l, 0, full, gLaunch) < 1.05) {
                continue;
            }
            if (MissionPlanner.fly(l, 0, full, round, realism).possible()) {
                return new Result(c, "round_trip", needRound);
            }
        }
        if (fuelsThere == null || fuelsThere.isEmpty() || home.size() < 2) {
            return null;
        }
        for (Candidate c : designs) {
            RocketLayout l = c.layout();
            double[] full = l.fullFuel();
            if (RocketMath.twr(l, 0, full, gLaunch) < 1.05) {
                continue;
            }
            MissionPlanner.Plan there = MissionPlanner.fly(l, 0, full, out, realism);
            if (!there.possible()) {
                continue;
            }
            int stage = there.activeStage();
            if (stage >= l.stages().size()) {
                continue;
            }
            double[] refuel = full.clone();
            boolean ok = true;
            for (int i = 0; i < refuel.length; i++) {
                if (i < stage) {
                    refuel[i] = 0;
                } else if (i < l.stages().size() && l.stages().get(i).propellant() != null
                        && !fuelsThere.contains(l.stages().get(i).propellant())) {
                    ok = false;
                }
            }
            if (!ok || (!target.orbit() && RocketMath.twr(l, stage, refuel, gTarget) < 1.05)) {
                continue;
            }
            if (MissionPlanner.fly(l, stage, refuel, home, realism).possible()) {
                return new Result(c, "refuel_there", need(out, realism));
            }
        }
        return null;
    }
}
