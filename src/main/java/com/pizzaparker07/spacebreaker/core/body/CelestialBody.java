package com.pizzaparker07.spacebreaker.core.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;

/**
 * A star, planet, moon, asteroid or station, described with real physical units.
 * Loaded from data packs: {@code data/<namespace>/spacebreaker/celestial_body/<name>.json}.
 * Real Solar System bodies and procedurally generated ones share this exact format.
 * Gameplay scaling (gravity, day length, travel time) is applied on top by {@code core.env.Environment}
 * and the server config — the data always holds real values.
 *
 * @param type            kind of body
 * @param parent          body this one orbits (empty for a star)
 * @param dimension       the surface dimension, if the body can be landed on
 * @param orbitDimension  the orbital space dimension around it, if any
 * @param physical        mass, radius, surface gravity
 * @param orbit           orbit around the parent
 * @param rotation        day length and axial tilt
 * @param atmosphere      surface pressure and composition
 * @param climate         surface temperatures
 * @param radiation       surface dose rate without shielding, mSv per day
 * @param solarIrradiance sunlight at the body, W/m² (Earth = 1361)
 */
public record CelestialBody(
        BodyType type,
        Optional<ResourceLocation> parent,
        Optional<ResourceKey<Level>> dimension,
        Optional<ResourceKey<Level>> orbitDimension,
        Physical physical,
        Orbit orbit,
        Rotation rotation,
        Atmosphere atmosphere,
        Climate climate,
        double radiation,
        double solarIrradiance
) {
    public static final double EARTH_GRAVITY = 9.80665;
    public static final double EARTH_IRRADIANCE = 1361.0;

    public static final Codec<CelestialBody> CODEC = RecordCodecBuilder.create(i -> i.group(
            BodyType.CODEC.fieldOf("type").forGetter(CelestialBody::type),
            ResourceLocation.CODEC.optionalFieldOf("parent").forGetter(CelestialBody::parent),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(CelestialBody::dimension),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("orbit_dimension").forGetter(CelestialBody::orbitDimension),
            Physical.CODEC.fieldOf("physical").forGetter(CelestialBody::physical),
            Orbit.CODEC.optionalFieldOf("orbit", Orbit.NONE).forGetter(CelestialBody::orbit),
            Rotation.CODEC.fieldOf("rotation").forGetter(CelestialBody::rotation),
            Atmosphere.CODEC.optionalFieldOf("atmosphere", Atmosphere.VACUUM).forGetter(CelestialBody::atmosphere),
            Climate.CODEC.fieldOf("climate").forGetter(CelestialBody::climate),
            Codec.DOUBLE.optionalFieldOf("radiation_msv_per_day", 0.0).forGetter(CelestialBody::radiation),
            Codec.DOUBLE.optionalFieldOf("solar_irradiance", EARTH_IRRADIANCE).forGetter(CelestialBody::solarIrradiance)
    ).apply(i, CelestialBody::new));

    public boolean landable() {
        return dimension.isPresent();
    }

    /** Surface gravity relative to Earth (1.0 = 9.81 m/s²). */
    public double relativeGravity() {
        return physical.surfaceGravity() / EARTH_GRAVITY;
    }

    /**
     * @param massKg         mass in kilograms
     * @param radiusKm       mean radius in kilometres
     * @param surfaceGravity m/s²; derived from mass and radius when omitted
     */
    public record Physical(double massKg, double radiusKm, double surfaceGravity) {
        public static final double G = 6.67430e-11;
        public static final Codec<Physical> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("mass_kg").forGetter(Physical::massKg),
                Codec.DOUBLE.fieldOf("radius_km").forGetter(Physical::radiusKm),
                Codec.DOUBLE.optionalFieldOf("surface_gravity", -1.0).forGetter(Physical::surfaceGravity)
        ).apply(i, Physical::of));

        public static Physical of(double massKg, double radiusKm, double surfaceGravity) {
            if (surfaceGravity <= 0) {
                double r = radiusKm * 1000.0;
                surfaceGravity = G * massKg / (r * r);
            }
            return new Physical(massKg, radiusKm, surfaceGravity);
        }

        /** Escape velocity in m/s. */
        public double escapeVelocity() {
            return Math.sqrt(2 * G * massKg / (radiusKm * 1000.0));
        }
    }

    /** Keplerian orbit around the parent. Distances in km, period in days. */
    public record Orbit(double semiMajorAxisKm, double eccentricity, double inclinationDeg, double periodDays) {
        public static final Orbit NONE = new Orbit(0, 0, 0, 0);
        public static final Codec<Orbit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("semi_major_axis_km").forGetter(Orbit::semiMajorAxisKm),
                Codec.DOUBLE.optionalFieldOf("eccentricity", 0.0).forGetter(Orbit::eccentricity),
                Codec.DOUBLE.optionalFieldOf("inclination_deg", 0.0).forGetter(Orbit::inclinationDeg),
                Codec.DOUBLE.fieldOf("period_days").forGetter(Orbit::periodDays)
        ).apply(i, Orbit::new));
    }

    /** Solar day length in hours (what you experience on the surface) and axial tilt. */
    public record Rotation(double dayLengthHours, double axialTiltDeg, boolean tidallyLocked) {
        public static final Codec<Rotation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("day_length_hours").forGetter(Rotation::dayLengthHours),
                Codec.DOUBLE.optionalFieldOf("axial_tilt_deg", 0.0).forGetter(Rotation::axialTiltDeg),
                Codec.BOOL.optionalFieldOf("tidally_locked", false).forGetter(Rotation::tidallyLocked)
        ).apply(i, Rotation::new));
    }

    /** Surface pressure in kPa (Earth = 101.3) and composition as volume fractions. */
    public record Atmosphere(double pressureKpa, boolean breathable, Map<String, Double> composition) {
        public static final Atmosphere VACUUM = new Atmosphere(0, false, Map.of());
        public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("pressure_kpa").forGetter(Atmosphere::pressureKpa),
                Codec.BOOL.optionalFieldOf("breathable", false).forGetter(Atmosphere::breathable),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("composition", Map.of()).forGetter(Atmosphere::composition)
        ).apply(i, Atmosphere::new));

        public boolean isVacuum() {
            return pressureKpa < 0.1;
        }
    }

    /** Typical surface temperatures in °C at local noon and before dawn. */
    public record Climate(double dayTempC, double nightTempC) {
        public static final Codec<Climate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("day_temp_c").forGetter(Climate::dayTempC),
                Codec.DOUBLE.fieldOf("night_temp_c").forGetter(Climate::nightTempC)
        ).apply(i, Climate::new));
    }
}
