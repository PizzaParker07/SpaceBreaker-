package com.pizzaparker07.spacebreaker.core.body;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** The data-pack registry of celestial bodies, and lookups from a dimension to its body. */
public final class Bodies {
    public static final ResourceKey<Registry<CelestialBody>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("celestial_body"));

    public static final ResourceKey<CelestialBody> SUN = key("sun");
    public static final ResourceKey<CelestialBody> EARTH = key("earth");
    public static final ResourceKey<CelestialBody> MOON = key("moon");

    private static final Map<Registry<CelestialBody>, Map<ResourceKey<Level>, ResourceLocation>> BY_DIMENSION = new WeakHashMap<>();

    public static ResourceKey<CelestialBody> key(String name) {
        return ResourceKey.create(REGISTRY, Spacebreaker.id(name));
    }

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, CelestialBody.CODEC, CelestialBody.CODEC);
    }

    public static Optional<Registry<CelestialBody>> registry(RegistryAccess access) {
        return access.registry(REGISTRY);
    }

    public static Optional<CelestialBody> get(RegistryAccess access, ResourceKey<CelestialBody> key) {
        return registry(access).flatMap(r -> r.getOptional(key));
    }

    /** The body whose surface (or orbit) is this dimension, if any. */
    public static Optional<CelestialBody> forDimension(RegistryAccess access, ResourceKey<Level> dimension) {
        return registry(access).flatMap(reg -> {
            Map<ResourceKey<Level>, ResourceLocation> index;
            synchronized (BY_DIMENSION) {
                index = BY_DIMENSION.computeIfAbsent(reg, Bodies::index);
            }
            ResourceLocation id = index.get(dimension);
            return id == null ? Optional.empty() : reg.getOptional(id);
        });
    }

    public static Optional<CelestialBody> forLevel(Level level) {
        return forDimension(level.registryAccess(), level.dimension());
    }

    /** True when the level is the orbital space around a body rather than its surface. */
    public static boolean isOrbit(Level level) {
        return forLevel(level).flatMap(CelestialBody::orbitDimension).filter(level.dimension()::equals).isPresent();
    }

    private static Map<ResourceKey<Level>, ResourceLocation> index(Registry<CelestialBody> reg) {
        Map<ResourceKey<Level>, ResourceLocation> map = new HashMap<>();
        reg.entrySet().forEach(e -> {
            e.getValue().dimension().ifPresent(d -> map.put(d, e.getKey().location()));
            e.getValue().orbitDimension().ifPresent(d -> map.put(d, e.getKey().location()));
        });
        return map;
    }

    private Bodies() {
    }
}
