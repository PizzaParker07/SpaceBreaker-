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
        AstroBody body = AstroBody.fromDataUnits(id.toString(), b.parent().map(ResourceLocation::toString).orElse(null),
                AstroBody.Kind.byName(b.type().getSerializedName()), b.physical().massKg(), b.physical().radiusKm(),
                o.semiMajorAxisKm(), o.eccentricity(), o.inclinationDeg(), o.longitudeAscendingNodeDeg(),
                o.argumentPeriapsisDeg(), o.meanAnomalyDeg(), o.periodDays(), b.rotation().dayLengthHours(),
                b.rotation().tidallyLocked(), b.atmosphere().pressureKpa(), b.atmosphere().composition(),
                b.climate().dayTempC(), b.climate().nightTempC(), b.albedo(), b.landable(), b.geometricAlbedo());
        if (body.orbit() != null && (o.nodeRateDegPerYear() != 0 || o.periapsisArgRateDegPerYear() != 0)) {
            body = body.withOrbit(body.orbit().withSecularRates(o.nodeRateDegPerYear(), o.periapsisArgRateDegPerYear()));
        }
        return body;
    }

    /** Simulated orbital time (s since J2000) of a level, from its game time and the server's time scale. */
    public static double simTime(Level level) {
        return GameClock.simSeconds(level.getGameTime(), timeScale());
    }

    /** The server's orbitalTimeScale (real seconds per simulated hour), 1 when the config is not loaded. */
    public static double timeScale() {
        return SbConfig.SERVER_SPEC.isLoaded() ? SbConfig.TIME_SCALE.get() : 1.0;
    }

    /** Polar light and cold traps of a data-pack body (uses its axial tilt). */
    public static Optional<PolarIllumination> polarIllumination(RegistryAccess access, String id) {
        return Bodies.registry(access).flatMap(reg -> reg.getOptional(ResourceLocation.parse(id))).flatMap(c ->
                system(access).map(sys -> PolarIllumination.of(sys, id, c.rotation().axialTiltDeg())));
    }

    /** All bodies of the registry as one system (several trees if a data pack adds other stars). */
    public static BodySystem system(Registry<CelestialBody> registry) {
        List<AstroBody> list = new ArrayList<>();
        Map<String, Vec3> poles = new java.util.HashMap<>();
        registry.entrySet().forEach(e -> {
            list.add(of(e.getKey().location(), e.getValue()));
            CelestialBody.Rotation r = e.getValue().rotation();
            if (r.hasPole()) {
                poles.put(e.getKey().location().toString(), Frames.eclipticFromEquatorial(r.poleRaDeg(), r.poleDecDeg()));
            }
        });
        return new BodySystem(Frames.equatorialMoonOrbits(list, poles));
    }

    /**
     * How a registered body looks from {@code observer} (heliocentric, m) at simulated time {@code t}: spin axis,
     * rotation, Sun direction and phase, for drawing it as a model.
     */
    public static Optional<BodyLook> look(RegistryAccess access, ResourceLocation id, Vec3 observer, double t) {
        return Bodies.registry(access).flatMap(reg -> reg.getOptional(id).map(c -> look(system(reg), id, c, observer, t)));
    }

    /** {@link #look(RegistryAccess, ResourceLocation, Vec3, double)} with a body system the caller keeps (per frame). */
    public static BodyLook look(BodySystem sys, ResourceLocation id, CelestialBody c, Vec3 observer, double t) {
        CelestialBody.Rotation r = c.rotation();
        Vec3 pole = r.hasPole() ? Frames.eclipticFromEquatorial(r.poleRaDeg(), r.poleDecDeg()) : null;
        return BodyLook.of(sys, sys.require(id.toString()), pole, r.axialTiltDeg(), observer, t);
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
                Math.toDegrees(o.meanAnomalyAtEpoch()), Math.toDegrees(o.nodeRate()) * AstroConstants.YEAR,
                Math.toDegrees(o.periapsisArgRate()) * AstroConstants.YEAR);
        double irradiance = d == null ? 0 : d.irradiance();
        return new CelestialBody(BodyType.valueOf(b.kind().name()),
                Optional.ofNullable(b.parent()).map(ResourceLocation::parse), dimension, Optional.empty(),
                CelestialBody.Physical.of(b.mass(), b.radius() / 1000.0, d == null ? -1 : d.surfaceGravity()),
                orbit,
                new CelestialBody.Rotation(b.rotationPeriod() / 3600.0, d == null ? 0 : d.axialTiltDeg(), b.tidallyLocked()),
                new CelestialBody.Atmosphere(b.surfacePressureKpa(), false, b.composition()),
                new CelestialBody.Climate(b.dayTempK() - 273.15, b.nightTempK() - 273.15),
                d == null ? 0 : d.radiation(), irradiance, b.albedo(),
                d != null && d.hasRings() ? Optional.of(new CelestialBody.Rings(d.ringInnerKm(), d.ringOuterKm())) : Optional.empty(),
                b.geometricAlbedo());
    }

    private BodyAdapter() {
    }
}
