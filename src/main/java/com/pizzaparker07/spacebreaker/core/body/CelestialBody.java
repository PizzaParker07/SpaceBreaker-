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
 * @param albedo          Bond albedo (fraction of sunlight reflected), 0.3 when unknown
 * @param rings           ring system, if any (for sky rendering and the star map)
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
        double solarIrradiance,
        double albedo,
        Optional<Rings> rings
) {
    public static final double DEFAULT_ALBEDO = 0.3;

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
            Codec.DOUBLE.optionalFieldOf("solar_irradiance", EARTH_IRRADIANCE).forGetter(CelestialBody::solarIrradiance),
            Codec.DOUBLE.optionalFieldOf("albedo", DEFAULT_ALBEDO).forGetter(CelestialBody::albedo),
            Rings.CODEC.optionalFieldOf("rings").forGetter(CelestialBody::rings)
    ).apply(i, CelestialBody::new));

    /** Constructor without the optional albedo and rings (kept for code written before they existed). */
    public CelestialBody(BodyType type, Optional<ResourceLocation> parent, Optional<ResourceKey<Level>> dimension,
                         Optional<ResourceKey<Level>> orbitDimension, Physical physical, Orbit orbit, Rotation rotation,
                         Atmosphere atmosphere, Climate climate, double radiation, double solarIrradiance) {
        this(type, parent, dimension, orbitDimension, physical, orbit, rotation, atmosphere, climate, radiation,
                solarIrradiance, DEFAULT_ALBEDO, Optional.empty());
    }

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

    /**
     * Keplerian orbit around the parent. Distances in km, period in days, angles in degrees.
     * The orientation angles and the mean anomaly refer to the J2000 epoch (ecliptic frame for planets,
     * the parent's equator/Laplace plane for moons) and default to 0 when unknown.
     */
    public record Orbit(double semiMajorAxisKm, double eccentricity, double inclinationDeg, double periodDays,
                        double longitudeAscendingNodeDeg, double argumentPeriapsisDeg, double meanAnomalyDeg) {
        public static final Orbit NONE = new Orbit(0, 0, 0, 0);
        public static final Codec<Orbit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("semi_major_axis_km").forGetter(Orbit::semiMajorAxisKm),
                Codec.DOUBLE.optionalFieldOf("eccentricity", 0.0).forGetter(Orbit::eccentricity),
                Codec.DOUBLE.optionalFieldOf("inclination_deg", 0.0).forGetter(Orbit::inclinationDeg),
                Codec.DOUBLE.fieldOf("period_days").forGetter(Orbit::periodDays),
                Codec.DOUBLE.optionalFieldOf("longitude_ascending_node_deg", 0.0).forGetter(Orbit::longitudeAscendingNodeDeg),
                Codec.DOUBLE.optionalFieldOf("argument_periapsis_deg", 0.0).forGetter(Orbit::argumentPeriapsisDeg),
                Codec.DOUBLE.optionalFieldOf("mean_anomaly_deg", 0.0).forGetter(Orbit::meanAnomalyDeg)
        ).apply(i, Orbit::new));

        /** Orbit without orientation angles (kept for code written before they existed). */
        public Orbit(double semiMajorAxisKm, double eccentricity, double inclinationDeg, double periodDays) {
            this(semiMajorAxisKm, eccentricity, inclinationDeg, periodDays, 0, 0, 0);
        }
    }

    /** A ring system, radii from the body's centre in km. */
    public record Rings(double innerRadiusKm, double outerRadiusKm) {
        public static final Codec<Rings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("inner_radius_km").forGetter(Rings::innerRadiusKm),
                Codec.DOUBLE.fieldOf("outer_radius_km").forGetter(Rings::outerRadiusKm)
        ).apply(i, Rings::new));
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
    /**
     * @param haze optional visible haze of a thick atmosphere (fog colour and visibility at the surface); read by the
     *             world's sky effects and by shader-pack compatibility, so packs can reproduce it
     */
    public record Atmosphere(double pressureKpa, boolean breathable, Map<String, Double> composition, Optional<Haze> haze) {
        public static final Atmosphere VACUUM = new Atmosphere(0, false, Map.of());
        public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("pressure_kpa").forGetter(Atmosphere::pressureKpa),
                Codec.BOOL.optionalFieldOf("breathable", false).forGetter(Atmosphere::breathable),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("composition", Map.of()).forGetter(Atmosphere::composition),
                Haze.CODEC.optionalFieldOf("haze").forGetter(Atmosphere::haze)
        ).apply(i, Atmosphere::new));

        public Atmosphere(double pressureKpa, boolean breathable, Map<String, Double> composition) {
            this(pressureKpa, breathable, composition, Optional.empty());
        }

        public boolean isVacuum() {
            return pressureKpa < 0.1;
        }
    }

    /**
     * Haze of a thick atmosphere.
     *
     * @param colour           fog colour at the surface in full daylight (RGB)
     * @param highColour       fog colour high up, inside the cloud deck (RGB)
     * @param visibilityBlocks how far you can see at the surface, blocks
     */
    public record Haze(int colour, int highColour, double visibilityBlocks) {
        private static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(str -> {
            try {
                return com.mojang.serialization.DataResult.success(Integer.parseInt(str.startsWith("#") ? str.substring(1) : str, 16));
            } catch (NumberFormatException e) {
                return com.mojang.serialization.DataResult.error(() -> "not a hex colour: " + str);
            }
        }, c -> String.format("#%06x", c));
        public static final Codec<Haze> CODEC = RecordCodecBuilder.create(i -> i.group(
                HEX.fieldOf("colour").forGetter(Haze::colour),
                HEX.optionalFieldOf("high_colour").forGetter(h -> Optional.of(h.highColour())),
                Codec.DOUBLE.optionalFieldOf("visibility_blocks", 64.0).forGetter(Haze::visibilityBlocks)
        ).apply(i, (c, hc, v) -> new Haze(c, hc.orElse(c), v)));

        /** The colour as r, g, b in 0..1. */
        public static float[] rgb(int c) {
            return new float[]{(c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F};
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
