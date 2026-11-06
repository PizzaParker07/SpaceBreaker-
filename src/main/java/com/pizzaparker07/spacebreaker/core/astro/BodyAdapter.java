package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSystem;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

    /** Simulated orbital time (s since J2000) of a level, from its game time and the server's time scale. */
    public static double simTime(Level level) {
        return GameClock.simSeconds(level.getGameTime(), timeScale());
    }

    /** The server's orbitalTimeScale (real seconds per simulated hour), 1 when the config is not loaded. */
    public static double timeScale() {
        return SbConfig.SERVER_SPEC.isLoaded() ? SbConfig.TIME_SCALE.get() : 1.0;
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

    /**
     * Converts a generated (or real nearby) star system into celestial body records, keyed by body id.
     *
     * @param dimensions surface dimension for a body id, if it has one (the planet engine assigns these)
     */
    public static Map<ResourceLocation, CelestialBody> toCelestialBodies(StarSystem system,
                                                                         Function<String, Optional<ResourceKey<Level>>> dimensions) {
        Map<ResourceLocation, CelestialBody> out = new LinkedHashMap<>();
        for (AstroBody b : system.bodies().all()) {
            StarSystem.Details d = system.details(b.id());
            out.put(ResourceLocation.parse(b.id()), toCelestialBody(b, d, b.landable() ? dimensions.apply(b.id()) : Optional.empty()));
        }
        return out;
    }

    public static CelestialBody toCelestialBody(AstroBody b, StarSystem.Details d, Optional<ResourceKey<Level>> dimension) {
        OrbitalElements o = b.orbit();
        CelestialBody.Orbit orbit = o == null ? CelestialBody.Orbit.NONE : new CelestialBody.Orbit(
                o.semiMajorAxis() / 1000.0, o.eccentricity(), Math.toDegrees(o.inclination()), o.period() / AstroConstants.DAY,
                Math.toDegrees(o.longitudeAscendingNode()), Math.toDegrees(o.argumentOfPeriapsis()),
                Math.toDegrees(o.meanAnomalyAtEpoch()));
        double irradiance = d == null ? 0 : d.irradiance();
        return new CelestialBody(BodyType.valueOf(b.kind().name()),
                Optional.ofNullable(b.parent()).map(ResourceLocation::parse), dimension, Optional.empty(),
                CelestialBody.Physical.of(b.mass(), b.radius() / 1000.0, d == null ? -1 : d.surfaceGravity()),
                orbit,
                new CelestialBody.Rotation(b.rotationPeriod() / 3600.0, d == null ? 0 : d.axialTiltDeg(), b.tidallyLocked()),
                new CelestialBody.Atmosphere(b.surfacePressureKpa(), false, b.composition()),
                new CelestialBody.Climate(b.dayTempK() - 273.15, b.nightTempK() - 273.15),
                d == null ? 0 : d.radiation(), irradiance, b.albedo(), Optional.empty());
    }

    private BodyAdapter() {
    }
}
