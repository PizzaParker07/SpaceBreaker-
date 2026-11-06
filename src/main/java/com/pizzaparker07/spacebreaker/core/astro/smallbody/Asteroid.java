package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;
import com.pizzaparker07.spacebreaker.core.astro.TerrainParams;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSystem;

import java.util.Map;

/**
 * A generated asteroid (or Trojan / Kuiper belt object).
 *
 * @param id              body id, e.g. {@code spacebreaker:asteroid/main_belt/1a2b3c}
 * @param nameEn          English name (proper name; designations are not translated)
 * @param nameTr          Turkish name
 * @param designation     catalogue designation, e.g. {@code SBA 12345}
 * @param region          population it belongs to
 * @param type            spectral class
 * @param diameterKm      volume-equivalent diameter
 * @param shape           ellipsoid semi-axes relative to the longest
 * @param rubblePile      a loose aggregate held by gravity (most bodies above ≈ 200 m)
 * @param porosity        void fraction, 0..1
 * @param density         bulk density, kg/m³
 * @param mass            kg
 * @param albedo          geometric albedo
 * @param rotationHours   spin period
 * @param orbit           heliocentric orbit (ecliptic J2000)
 * @param composition     mass fractions of the main materials
 * @param metalVeins      0..1, richness of metal veins for mining
 * @param pgmPpm          platinum-group metals, ppm by mass
 */
public record Asteroid(String id, String nameEn, String nameTr, String designation, Region region, SpectralType type,
                       double diameterKm, Shape shape, boolean rubblePile, double porosity, double density, double mass,
                       double albedo, double rotationHours, OrbitalElements orbit, Map<String, Double> composition,
                       double metalVeins, double pgmPpm) {
    /** Triaxial ellipsoid a ≥ b ≥ c as ratios b/a and c/a. */
    public record Shape(double ba, double ca) {
    }

    /** Speed of a person's standing jump, m/s: below this escape speed you can jump off the asteroid. */
    public static final double JUMP_SPEED = 3.0;

    public double radiusM() {
        return diameterKm * 500;
    }

    public double surfaceGravity() {
        return AstroConstants.G * mass / (radiusM() * radiusM());
    }

    public double escapeVelocity() {
        return Math.sqrt(2 * AstroConstants.G * mass / radiusM());
    }

    /** True when a jump would leave the asteroid for good. */
    public boolean canJumpOff() {
        return escapeVelocity() < JUMP_SPEED;
    }

    /** Equator speed from the spin (on very small fast rotators loose material is flung off). */
    public double spinSpeed() {
        return 2 * Math.PI * radiusM() / (rotationHours * 3600);
    }

    public double distanceAu() {
        return orbit.semiMajorAxis() / AstroConstants.AU;
    }

    /** The asteroid as a pure body record (parent = the Sun). */
    public AstroBody toAstroBody() {
        double flux = AstroConstants.SOLAR_CONSTANT / (distanceAu() * distanceAu());
        double teq = com.pizzaparker07.spacebreaker.core.astro.PlanetModel.equilibriumTemperature(flux, albedo * 0.4);
        double day = com.pizzaparker07.spacebreaker.core.astro.PlanetModel.daySideTemperature(flux, albedo * 0.4);
        return new AstroBody(id, "spacebreaker:sun", AstroBody.Kind.ASTEROID, mass, radiusM(), orbit, rotationHours * 3600,
                false, 0, Map.of(), Math.max(day * 0.9, teq), Math.max(30, teq * 0.6), albedo * 0.4, true, albedo);
    }

    /** Extra values for the CelestialBody conversion ({@code BodyAdapter.toCelestialBody(toAstroBody(), details(), dim)}). */
    public StarSystem.Details details() {
        double flux = AstroConstants.SOLAR_CONSTANT / (distanceAu() * distanceAu());
        return new StarSystem.Details(nameEn, nameTr, flux, 1.4 * Math.sqrt(flux / AstroConstants.SOLAR_CONSTANT + 0.05),
                0, surfaceGravity(), 0, 0);
    }

    /** Terrain for a mining world built on this asteroid. */
    public AsteroidTerrain terrain() {
        AstroBody b = toAstroBody();
        double flux = AstroConstants.SOLAR_CONSTANT / (distanceAu() * distanceAu());
        TerrainHints h = TerrainHints.of(b, null, flux, 4.5e9, id.hashCode());
        TerrainParams p = TerrainParams.of(b, h);
        // gravity holds fine regolith on large bodies; small ones are bare rock and boulders
        double rock = Math.max(0.15, Math.min(0.9, 0.9 - 0.25 * Math.log10(Math.max(diameterKm, 0.01) / 0.05)));
        double boulders = rubblePile ? Math.min(1, 0.4 + 0.3 * rock) : 0.2 * rock;
        double ice = type == SpectralType.D ? 0.35 : type == SpectralType.C && distanceAu() > 2.8 ? 0.08 : 0;
        return new AsteroidTerrain(h, p, rock, boulders, metalVeins, pgmPpm, ice);
    }
}
