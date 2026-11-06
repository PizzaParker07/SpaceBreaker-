package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Places a rocket can be in Phase 1. More bodies come with the Astrophysics library. */
public enum Location {
    EARTH_SURFACE("earth_surface", Bodies.EARTH, false, Level.OVERWORLD, true),
    EARTH_ORBIT("earth_orbit", Bodies.EARTH, true, Orbits.EARTH_ORBIT, false),
    MOON_ORBIT("moon_orbit", Bodies.MOON, true, Orbits.MOON_ORBIT, false),
    MOON_SURFACE("moon_surface", Bodies.MOON, false, Orbits.MOON, false);

    private final String id;
    private final ResourceKey<CelestialBody> body;
    private final boolean orbit;
    private final ResourceKey<Level> dimension;
    private final boolean atmosphere;

    Location(String id, ResourceKey<CelestialBody> body, boolean orbit, ResourceKey<Level> dimension, boolean atmosphere) {
        this.id = id;
        this.body = body;
        this.orbit = orbit;
        this.dimension = dimension;
        this.atmosphere = atmosphere;
    }

    public String id() {
        return id;
    }

    public ResourceKey<CelestialBody> body() {
        return body;
    }

    public boolean orbit() {
        return orbit;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** Whether the surface has an atmosphere you launch through and reenter. */
    public boolean atmosphere() {
        return atmosphere;
    }

    public String translationKey() {
        return "location.spacebreaker." + id;
    }

    public static Optional<Location> of(ResourceKey<Level> dimension) {
        for (Location l : values()) {
            if (l.dimension.equals(dimension)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    public static Location byId(int ordinal) {
        Location[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }

    /** Dimension keys of the places rocketry flies to. */
    public static final class Orbits {
        public static final ResourceKey<Level> EARTH_ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("earth_orbit"));
        public static final ResourceKey<Level> MOON_ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon_orbit"));
        public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon"));

        private Orbits() {
        }
    }
}
