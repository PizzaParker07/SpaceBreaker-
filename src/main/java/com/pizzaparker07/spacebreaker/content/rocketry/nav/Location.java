package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * A place a rocket can be: the surface of a celestial body or a low orbit around it. Built from the
 * {@code celestial_body} data, so every body with a {@code dimension} can be landed on and every body can
 * be orbited — new bodies from other areas or data packs appear on the star map by themselves.
 * Identity is (body, orbit); the other fields are derived from the body data.
 *
 * @param body        celestial body id
 * @param orbit       true for the low orbit, false for the surface
 * @param dimension   where the rocket is in this place (bodies without their own orbit dimension share {@link Orbits#SHARED})
 * @param pressureKpa surface pressure of the body (0 = airless)
 */
public record Location(ResourceLocation body, boolean orbit, ResourceKey<Level> dimension, double pressureKpa) {
    public static final ResourceLocation EARTH = Spacebreaker.id("earth");
    public static final ResourceLocation MOON = Spacebreaker.id("moon");

    public static final Location EARTH_SURFACE = new Location(EARTH, false, Level.OVERWORLD, 101.325);
    public static final Location EARTH_ORBIT = new Location(EARTH, true, Orbits.EARTH_ORBIT, 101.325);
    public static final Location MOON_ORBIT = new Location(MOON, true, Orbits.MOON_ORBIT, 0);
    public static final Location MOON_SURFACE = new Location(MOON, false, Orbits.MOON, 0);

    /** Builds the location of a body from its data, or null if it cannot be visited this way. */
    @Nullable
    public static Location of(ResourceLocation id, CelestialBody b, boolean orbit) {
        double p = b.atmosphere().pressureKpa();
        if (orbit) {
            return new Location(id, true, b.orbitDimension().orElse(Orbits.SHARED), p);
        }
        return b.dimension().map(d -> new Location(id, false, d, p)).orElse(null);
    }

    public ResourceKey<CelestialBody> bodyKey() {
        return ResourceKey.create(Bodies.REGISTRY, body);
    }

    /** Whether the surface has an atmosphere you launch through, brake in and land under parachutes in. */
    public boolean atmosphere() {
        return pressureKpa >= 0.1;
    }

    public boolean isEarth() {
        return EARTH.equals(body);
    }

    /** Stable text id, e.g. {@code spacebreaker:mars/orbit}. */
    public String id() {
        return body + (orbit ? "/orbit" : "/surface");
    }

    /** Body name: from the language files when a body has one, else its id capitalised. */
    public static Component bodyName(ResourceLocation body) {
        String path = body.getPath();
        String fallback = path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1).replace('_', ' ');
        return Component.translatableWithFallback("body." + body.getNamespace() + "." + path, fallback);
    }

    public Component name() {
        if (orbit) {
            return Component.translatable("location.spacebreaker.orbit_of", bodyName(body));
        }
        return isEarth() ? Component.translatable("location.spacebreaker.earth_surface") : bodyName(body);
    }

    /** Kept for older call sites: the translation key of the name (only exact for Earth and the Moon). */
    public String translationKey() {
        return "location.spacebreaker." + body.getPath() + (orbit ? "_orbit" : "_surface");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Location l && l.orbit == orbit && l.body.equals(body);
    }

    @Override
    public int hashCode() {
        return body.hashCode() * 31 + (orbit ? 1 : 0);
    }

    @Override
    public String toString() {
        return id().toLowerCase(Locale.ROOT);
    }

    /** Parses {@link #id()} back (body data is looked up by {@link Locations}). */
    public static Optional<Location> parse(String id) {
        int slash = id.lastIndexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }
        ResourceLocation body = ResourceLocation.tryParse(id.substring(0, slash));
        if (body == null) {
            return Optional.empty();
        }
        return Locations.get(body, id.endsWith("/orbit"));
    }

    /** Dimension keys used by rocketry. */
    public static final class Orbits {
        public static final ResourceKey<Level> EARTH_ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("earth_orbit"));
        public static final ResourceKey<Level> MOON_ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon_orbit"));
        public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon"));
        /** Orbit space shared by every body without an orbit dimension of its own (the rocket remembers which body). */
        public static final ResourceKey<Level> SHARED = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("orbit"));

        private Orbits() {
        }
    }
}
