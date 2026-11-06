package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Thin bridge between the data-pack {@link CelestialBody} registry and the pure-Java astro library. */
public final class BodyAdapter {

    public static AstroBody of(ResourceLocation id, CelestialBody b) {
        CelestialBody.Orbit o = b.orbit();
        return AstroBody.fromDataUnits(id.toString(), b.parent().map(ResourceLocation::toString).orElse(null),
                AstroBody.Kind.byName(b.type().getSerializedName()), b.physical().massKg(), b.physical().radiusKm(),
                o.semiMajorAxisKm(), o.eccentricity(), o.inclinationDeg(), o.longitudeAscendingNodeDeg(),
                o.argumentPeriapsisDeg(), o.meanAnomalyDeg(), o.periodDays(), b.rotation().dayLengthHours(),
                b.rotation().tidallyLocked(), b.atmosphere().pressureKpa(), b.atmosphere().composition(),
                b.climate().dayTempC(), b.climate().nightTempC(), b.albedo(), b.landable());
    }

    /** All bodies of the registry as one system (several trees if a data pack adds other stars). */
    public static BodySystem system(Registry<CelestialBody> registry) {
        List<AstroBody> list = new ArrayList<>();
        registry.entrySet().forEach(e -> list.add(of(e.getKey().location(), e.getValue())));
        return new BodySystem(list);
    }

    public static Optional<BodySystem> system(RegistryAccess access) {
        return Bodies.registry(access).map(BodyAdapter::system);
    }

    private BodyAdapter() {
    }
}
