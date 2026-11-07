package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Answers "can this rocket get there, and can it get back?" by simulating the burns with staging. */
public final class MissionPlanner {
    /** Result of flying a sequence of legs. */
    /** {@code boilOffKg}: propellant lost to boil-off on the coasts flown. */
    public record Plan(boolean possible, int legsFlown, double[] fuel, int activeStage, String failure, double boilOffKg) {
    }

    /** One step of the standard Moon mission, for the assembler's checklist. */
    public record Milestone(Location location, boolean reached, double deltaV) {
    }

    private MissionPlanner() {
    }

    /** Fly legs one after another from {@code start}; stops at the first leg that cannot be completed. */
    public static Plan fly(RocketLayout layout, int active, double[] fuel, List<Location> route, SbConfig.Realism realism) {
        return fly(layout, active, fuel, route, realism, true);
    }

    /**
     * As {@link #fly(RocketLayout, int, double[], List, SbConfig.Realism)}; with {@code checkShield} false only the
     * fuel is checked (so the map can tell "not enough fuel" from "needs a heat shield").
     */
    public static Plan fly(RocketLayout layout, int active, double[] fuel, List<Location> route, SbConfig.Realism realism, boolean checkShield) {
        double[] f = fuel.clone();
        int stage = active;
        double boiled = 0;
        for (int i = 0; i + 1 < route.size(); i++) {
            Location from = route.get(i);
            Location to = route.get(i + 1);
            OptionalDouble cost = Transfers.cost(from, to, layout, stage, realism);
            if (cost.isEmpty()) {
                return new Plan(false, i, f, stage, "no_route", boiled);
            }
            int rating = Transfers.reentryRating(from, to);
            if (checkShield && rating > 0 && layout.payload().heatShield() < rating) {
                return new Plan(false, i, f, stage, "heat_shield", boiled);
            }
            boolean reentry = to.isEarth() && !to.orbit();
            if (reentry) {
                // the capsule is released before reentry; only the deorbit burn uses the stack
                RocketMath.Burn burn = RocketMath.burn(layout, stage, f, cost.getAsDouble(), 0);
                if (!burn.complete()) {
                    return new Plan(false, i, f, stage, "delta_v", boiled);
                }
                f = burn.fuel();
                stage = layout.stages().size();
                continue;
            }
            double atmosphere = from.atmosphere() && !from.orbit() ? RocketMath.ASCENT_ATMOSPHERE : 0;
            RocketMath.Burn burn = RocketMath.burn(layout, stage, f, cost.getAsDouble(), atmosphere);
            if (!burn.complete()) {
                return new Plan(false, i, f, stage, "delta_v", boiled);
            }
            f = burn.fuel();
            stage = burn.activeStage();
            // the long coast after the burn: cryogenic propellant left in the tanks boils off
            boiled += BoilOff.apply(layout, stage, f, Transfers.coastSeconds(from, to) / BoilOff.SECONDS_PER_DAY, realism);
        }
        return new Plan(true, route.size() - 1, f, stage, "", boiled);
    }

    /** The classic mission from the pad: orbit, Moon orbit, landing, ascent, home. */
    public static final List<Location> MOON_MISSION = List.of(Location.EARTH_SURFACE, Location.EARTH_ORBIT,
            Location.MOON_ORBIT, Location.MOON_SURFACE, Location.MOON_ORBIT, Location.EARTH_SURFACE);

    /** For each step of the Moon mission: reachable or not, and its Δv. */
    public static List<Milestone> milestones(RocketLayout layout, SbConfig.Realism realism) {
        List<Milestone> out = new ArrayList<>();
        double[] full = layout.fullFuel();
        for (int n = 1; n < MOON_MISSION.size(); n++) {
            Plan plan = fly(layout, 0, full, MOON_MISSION.subList(0, n + 1), realism);
            Location from = MOON_MISSION.get(n - 1);
            Location to = MOON_MISSION.get(n);
            double dv = Transfers.cost(from, to, layout, 0, realism).orElse(0);
            out.add(new Milestone(to, plan.possible(), dv));
        }
        return out;
    }

    /** Whether the rocket, from where it is, can still make it home to Earth's surface. */
    public static boolean canReturnHome(RocketLayout layout, int active, double[] fuel, Location here, SbConfig.Realism realism) {
        return fly(layout, active, fuel, homeRoute(here), realism).possible();
    }

    /** The way home to Earth's surface: surface → own orbit → (direct entry) Earth. */
    public static List<Location> homeRoute(Location here) {
        if (here.equals(Location.EARTH_SURFACE)) {
            return List.of(here);
        }
        if (here.orbit()) {
            return List.of(here, Location.EARTH_SURFACE);
        }
        Location orbit = Locations.get(here.body(), true).orElse(here);
        return List.of(here, orbit, Location.EARTH_SURFACE);
    }

    /** Whether going to {@code to} would still leave enough to come home afterwards. */
    public static boolean canReturnAfter(RocketLayout layout, int active, double[] fuel, Location here, Location to, SbConfig.Realism realism) {
        List<Location> route = new ArrayList<>();
        route.add(here);
        route.addAll(homeRoute(to));
        return fly(layout, active, fuel, route, realism).possible();
    }
}
