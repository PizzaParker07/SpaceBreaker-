package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every place rockets can go, read from the {@code celestial_body} registry: an orbit around every body
 * except stars, and the surface of every body that names a {@code dimension}. Installed when a world loads
 * (server and client); until then only Earth and the Moon are known.
 */
public final class Locations {
    private static volatile List<Location> all = List.of(Location.EARTH_SURFACE, Location.EARTH_ORBIT, Location.MOON_ORBIT, Location.MOON_SURFACE);
    private static volatile Map<ResourceLocation, CelestialBody> bodies = Map.of();

    private Locations() {
    }

    public static void install(RegistryAccess access) {
        Optional<Registry<CelestialBody>> reg = Bodies.registry(access);
        if (reg.isEmpty()) {
            return;
        }
        Map<ResourceLocation, CelestialBody> b = new LinkedHashMap<>();
        List<Location> out = new ArrayList<>();
        reg.get().entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> order(reg.get(), e.getValue())))
                .forEach(e -> {
                    ResourceLocation id = e.getKey().location();
                    CelestialBody body = e.getValue();
                    if (body.type() == BodyType.STAR) {
                        return;
                    }
                    b.put(id, body);
                    Location surface = Location.of(id, body, false);
                    if (surface != null) {
                        out.add(surface);
                    }
                    out.add(Location.of(id, body, true));
                });
        bodies = Map.copyOf(b);
        all = List.copyOf(out);
    }

    /** Sort key: distance from the sun of the body (or of its planet), moons right after their planet. */
    private static double order(Registry<CelestialBody> reg, CelestialBody b) {
        double own = b.orbit().semiMajorAxisKm();
        Optional<CelestialBody> parent = b.parent().flatMap(reg::getOptional);
        if (parent.isPresent() && parent.get().type() != BodyType.STAR) {
            return parent.get().orbit().semiMajorAxisKm() + Math.min(own, 1e6) * 1e-3;
        }
        return own;
    }

    public static List<Location> all() {
        return all;
    }

    public static Optional<CelestialBody> body(ResourceLocation id) {
        return Optional.ofNullable(bodies.get(id));
    }

    public static Optional<Location> get(ResourceLocation body, boolean orbit) {
        for (Location l : all) {
            if (l.body().equals(body) && l.orbit() == orbit) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    /**
     * Where a rocket in this dimension is. In the shared orbit dimension the rocket tells which body it
     * circles ({@code orbitBody}).
     */
    public static Optional<Location> at(ResourceKey<Level> dimension, @Nullable ResourceLocation orbitBody) {
        if (Location.Orbits.SHARED.equals(dimension)) {
            return orbitBody == null ? Optional.empty() : get(orbitBody, true);
        }
        for (Location l : all) {
            if (l.dimension().equals(dimension)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    /** Width of each body's region in the shared orbit dimension (blocks along x). */
    public static final double SHARED_REGION = 8192.0;

    /** x of the centre of a body's region in the shared orbit dimension. */
    public static double sharedRegionX(ResourceLocation body) {
        return Math.floorMod(body.hashCode(), 512) * SHARED_REGION;
    }

    /** The body whose region of the shared orbit dimension contains this x. */
    public static Optional<ResourceLocation> bodyForSharedX(double x) {
        ResourceLocation best = null;
        double bestD = Double.MAX_VALUE;
        for (ResourceLocation b : bodies.keySet()) {
            double d = Math.abs(x - sharedRegionX(b));
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return bestD <= SHARED_REGION ? Optional.ofNullable(best) : Optional.empty();
    }

    /** The planet a moon belongs to (empty for planets). */
    public static Optional<ResourceLocation> parentPlanet(ResourceLocation body) {
        return body(body).flatMap(CelestialBody::parent).filter(bodies::containsKey);
    }
}
