package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * What the star map tells the crew about a trip, in plain terms: the steps of the route, the fuel it takes
 * against the fuel on board, whether there is enough left to come home, and what to watch out for. Pure
 * logic (no client classes), so it is shared by the screen and the tests.
 */
public final class StarMapData {
    /** Dangers of a world, shown as icons on the map. */
    public enum Hazard {
        NO_AIR, THIN_AIR, CRUSHING_AIR, HOT, COLD, RADIATION;

        public String key() {
            return "gui.spacebreaker.star_map.hazard." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Why the Go button is off (empty when it is on). */
    public enum Block {
        NONE, HERE, NOT_IN_ORBIT, NOT_BUILT, NO_ROUTE, NEEDS_HEAT_SHIELD, NOT_ENOUGH_FUEL, BUSY, OBSERVATION_ONLY;

        public String key() {
            return "gui.spacebreaker.star_map.block." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * One trip.
     *
     * @param next         the stop the Go button flies to now (the destination itself, or the first stop on the way)
     * @param route        the stops from here to the destination (here first), for the route line
     * @param needed       fuel the whole route takes, as Δv in m/s (scaled by the realism setting)
     * @param have         fuel on board, as Δv in m/s
     * @param enough       whether the rocket can fly the whole route with what it has
     * @param returnHome   whether, after arriving, there is still enough to get back to Earth
     * @param refuelThere  whether propellant can be made at the destination (on its surface), so the way home
     *                     can be fuelled there
     * @param heatShield   heat shield rating the route needs (0 = none)
     * @param shieldOk     whether the rocket's heat shield is good enough
     * @param legsMissing  landing on an airless world without landing legs
     * @param block        why the trip cannot be started right now
     */
    public record Trip(Location to, Location next, List<Location> route, double needed, double have, boolean enough, boolean returnHome,
                       boolean refuelThere,
                       int heatShield, boolean shieldOk, boolean legsMissing, Block block, double boilOff, boolean needsNuclear) {
        public double shortBy() {
            return Math.max(0, needed - have);
        }

        public boolean canGo() {
            return block == Block.NONE;
        }
    }

    public static final double THIN_AIR_KPA = 50;
    public static final double CRUSHING_AIR_KPA = 1000;
    public static final double HOT_C = 60;
    public static final double COLD_C = -60;
    public static final double RADIATION_MSV_PER_DAY = 0.5;

    private StarMapData() {
    }

    public static Set<Hazard> hazards(CelestialBody b) {
        Set<Hazard> out = EnumSet.noneOf(Hazard.class);
        double p = b.atmosphere().pressureKpa();
        if (b.atmosphere().isVacuum() || p < 0.1) {
            out.add(Hazard.NO_AIR);
        } else if (p > CRUSHING_AIR_KPA) {
            out.add(Hazard.CRUSHING_AIR);
        } else if (p < THIN_AIR_KPA || !b.atmosphere().breathable()) {
            out.add(Hazard.THIN_AIR);
        }
        if (b.climate().dayTempC() > HOT_C) {
            out.add(Hazard.HOT);
        }
        if (b.climate().nightTempC() < COLD_C) {
            out.add(Hazard.COLD);
        }
        if (b.radiation() > RADIATION_MSV_PER_DAY) {
            out.add(Hazard.RADIATION);
        }
        return out;
    }

    /** The stops from {@code here} to {@code to}: climb to orbit first, then fly to the destination's orbit, then land. */
    public static List<Location> route(Location here, Location to) {
        List<Location> out = new ArrayList<>();
        out.add(here);
        if (here.equals(to)) {
            return out;
        }
        if (!here.orbit()) {
            Locations.get(here.body(), true).filter(o -> !o.equals(to)).ifPresent(out::add);
        }
        Location last = out.get(out.size() - 1);
        if (!to.orbit() && !last.body().equals(to.body())) {
            Locations.get(to.body(), true).ifPresent(out::add);
        }
        out.add(to);
        return out;
    }

    /**
     * The legs the rocket actually flies: from a surface it climbs to orbit first; from orbit it burns straight
     * to the destination when there is a direct route, otherwise to the destination's orbit and then down.
     */
    public static List<Location> flightPath(Location here, Location to) {
        List<Location> out = new ArrayList<>();
        out.add(here);
        if (here.equals(to)) {
            return out;
        }
        if (!here.orbit() && !(to.orbit() && to.body().equals(here.body()))) {
            Locations.get(here.body(), true).ifPresent(out::add);
        }
        Location last = out.get(out.size() - 1);
        if (!last.equals(to) && Transfers.real(last, to).isEmpty() && !to.orbit()) {
            Locations.get(to.body(), true).filter(o -> !o.equals(last)).ifPresent(out::add);
        }
        if (!out.get(out.size() - 1).equals(to)) {
            out.add(to);
        }
        return out;
    }

    /**
     * Everything the map shows for a trip from {@code here}.
     *
     * @param inOrbit  whether the rocket is parked in orbit (the only time the crew can pick a destination)
     * @param built    whether the destination's dimension exists on this server
     */
    public static Trip trip(RocketLayout layout, int active, double[] fuel, double have, Location here, Location to,
                            SbConfig.Realism realism, boolean inOrbit, boolean built) {
        List<Location> path = flightPath(here, to);
        double needed = 0;
        boolean routed = true;
        int shield = 0;
        int stage = active;
        for (int i = 0; i + 1 < path.size(); i++) {
            OptionalDouble c = Transfers.cost(path.get(i), path.get(i + 1), layout, stage, realism);
            if (c.isEmpty()) {
                routed = false;
                break;
            }
            needed += c.getAsDouble();
            shield = Math.max(shield, Transfers.reentryRating(path.get(i), path.get(i + 1)));
        }
        // fuel alone; the heat shield is its own reason below
        MissionPlanner.Plan plan = routed ? MissionPlanner.fly(layout, active, fuel, path, realism, false) : null;
        boolean enough = plan != null && plan.possible();
        // share of the propellant on board that boils off on the coasts there (cryogenic propellant only)
        double onBoard = 0;
        for (int i = Math.max(0, active); i < fuel.length; i++) {
            onBoard += fuel[i];
        }
        double boilOff = plan == null || onBoard <= 0 ? 0 : plan.boilOffKg() / onBoard;
        boolean shieldOk = layout.payload().heatShield() >= shield;
        boolean home = enough && shieldOk && MissionPlanner.canReturnAfter(layout, active, fuel, here, to, realism);
        boolean legs = !to.orbit() && !to.isEarth() && !to.atmosphere() && !layout.hasLegsFrom(active);
        Block block;
        if (here.equals(to)) {
            block = Block.HERE;
        } else if (CometRoutes.observationOnly(to.body())) {
            block = Block.OBSERVATION_ONLY;
        } else if (!built) {
            block = Block.NOT_BUILT;
        } else if (!routed) {
            block = Block.NO_ROUTE;
        } else if (!inOrbit) {
            block = Block.NOT_IN_ORBIT;
        } else if (!shieldOk) {
            block = Block.NEEDS_HEAT_SHIELD;
        } else if (!enough) {
            block = Block.NOT_ENOUGH_FUEL;
        } else {
            block = Block.NONE;
        }
        // a comet only a nuclear thermal engine reaches sensibly, and no nuclear stage on board
        boolean nuclearOnBoard = false;
        for (int i = Math.max(0, active); i < layout.stages().size(); i++) {
            nuclearOnBoard |= layout.stages().get(i).propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR;
        }
        boolean needsNuclear = !enough && !nuclearOnBoard && CometRoutes.needsNuclear(to.body());
        Location next = path.size() > 1 ? path.get(1) : to;
        return new Trip(to, next, route(here, to), needed, have, enough, home, Locations.canRefuel(to.body()), shield, shieldOk, legs, block, boilOff, needsNuclear);
    }

    /** The map's group of near-Earth asteroids (not a real body): one node on the solar system view. */
    public static final ResourceLocation NEAR_EARTH_ASTEROIDS = ResourceLocation.fromNamespaceAndPath("spacebreaker", "near_earth_asteroids");
    /** The map's group of comets (one node on the solar system view; the comets themselves are destinations). */
    public static final ResourceLocation COMETS = ResourceLocation.fromNamespaceAndPath("spacebreaker", "comets");

    public static boolean comet(ResourceLocation body) {
        return Locations.comets().contains(body);
    }

    /**
     * What the map shows about a comet now.
     *
     * @param sunAu            distance from the Sun, AU
     * @param nextPerihelion   date of the next closest pass to the Sun
     * @param level            how active it is (asleep far out, a bright tail near the Sun)
     * @param periodYears      orbital period
     */
    public record CometInfo(double sunAu, java.time.LocalDate nextPerihelion, com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level level,
                            double periodYears, double perihelionAu) {
    }

    /** The comet card at simulated time {@code t} (seconds from J2000), if the astro library knows the comet. */
    public static java.util.Optional<CometInfo> cometInfo(ResourceLocation body, double t) {
        return com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(body.getPath()).map(c -> {
            var activity = com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(c, t);
            double periodDays = c.periodSeconds() / 86400.0;
            double nowDays = t / 86400.0;
            double k = Math.ceil((nowDays - c.perihelionDays()) / periodDays);
            double next = c.perihelionDays() + k * periodDays;
            java.time.LocalDate date = java.time.LocalDate.of(2000, 1, 1).plusDays((long) Math.floor(next + 0.5));
            return new CometInfo(activity.distanceAu(), date, activity.level(), c.periodYears(), c.perihelionAu());
        });
    }

    /** A near-Earth asteroid comes closer to the Sun than 1.3 AU (the usual definition). */
    public static final double NEA_PERIHELION_KM = 1.3 * 149_597_870.7;

    /** An asteroid circling the Sun whose perihelion is inside 1.3 AU (Bennu, Ryugu, Itokawa, Eros; not Vesta). */
    public static boolean nearEarthAsteroid(ResourceLocation body) {
        return Locations.parentPlanet(body).isEmpty() && Locations.body(body).filter(b -> b.type() == com.pizzaparker07.spacebreaker.core.body.BodyType.ASTEROID)
                .map(b -> b.orbit().semiMajorAxisKm() * (1 - b.orbit().eccentricity()) < NEA_PERIHELION_KM).orElse(false);
    }

    /**
     * Planets (and dwarf planets, big asteroids) in order from the sun; the near-Earth asteroids show as one group
     * where the first of them would be. Moons, and the members of the group, are listed by {@link #moons}.
     */
    public static List<ResourceLocation> planets() {
        List<ResourceLocation> out = new ArrayList<>();
        for (Location l : Locations.all()) {
            ResourceLocation b = nearEarthAsteroid(l.body()) ? NEAR_EARTH_ASTEROIDS : l.body();
            if (Locations.parentPlanet(l.body()).isEmpty() && !comet(l.body()) && !out.contains(b)) {
                out.add(b);
            }
        }
        if (!Locations.comets().isEmpty()) {
            out.add(COMETS); // last: their orbits reach out past the planets
        }
        return out;
    }

    public static List<ResourceLocation> moons(ResourceLocation planet) {
        List<ResourceLocation> out = new ArrayList<>();
        if (COMETS.equals(planet)) {
            return Locations.comets();
        }
        if (NEAR_EARTH_ASTEROIDS.equals(planet)) {
            for (Location l : Locations.all()) {
                if (nearEarthAsteroid(l.body()) && !out.contains(l.body())) {
                    out.add(l.body());
                }
            }
            return out;
        }
        for (Location l : Locations.all()) {
            if (Locations.parentPlanet(l.body()).filter(planet::equals).isPresent() && !out.contains(l.body())) {
                out.add(l.body());
            }
        }
        return out;
    }

    /** The planet a body belongs to (itself for planets): the map focuses on this system. */
    public static ResourceLocation system(ResourceLocation body) {
        return comet(body) ? COMETS : nearEarthAsteroid(body) ? NEAR_EARTH_ASTEROIDS : Locations.parentPlanet(body).orElse(body);
    }
}
