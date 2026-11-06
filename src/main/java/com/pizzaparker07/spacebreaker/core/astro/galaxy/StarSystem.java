package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A complete star system: the star(s) and every planet and moon, as {@link AstroBody} values that convert
 * one-to-one into {@code CelestialBody} records ({@code BodyAdapter.toCelestialBodies}).
 *
 * @param seed     where it sits in the galaxy
 * @param rootId   id of the primary star
 * @param kind     kind of the primary star
 * @param primary  primary star properties (solar units)
 * @param bodies   all bodies, primary star first
 * @param details  per-body values that the body record does not carry
 */
public record StarSystem(StarSeed seed, String rootId, StarKind kind, StellarModel.Star primary, BodySystem bodies,
                         Map<String, Details> details, String designation, Optional<Binary> binary) {

    /**
     * A companion star (kept as data for now: it is not a body of the system).
     *
     * @param companion      companion star properties (solar units)
     * @param semiMajorAxisAu separation, AU
     * @param eccentricity   orbital eccentricity
     * @param periodDays     orbital period
     */
    public record Binary(StellarModel.Star companion, double semiMajorAxisAu, double eccentricity, double periodDays) {
        /** Planets around the primary are stable inside about this distance (Holman &amp; Wiegert), AU. */
        public double stableLimitAu() {
            return 0.3 * semiMajorAxisAu * (1 - eccentricity);
        }
    }

    /**
     * @param nameEn          English display name
     * @param nameTr          Turkish display name
     * @param irradiance      starlight at the body, W/m²
     * @param radiation       surface dose without shielding, mSv/day
     * @param axialTiltDeg    axial tilt
     * @param surfaceGravity  m/s²
     * @param ringInnerKm     inner ring radius from the body's centre (0: no rings)
     * @param ringOuterKm     outer ring radius
     */
    public record Details(String nameEn, String nameTr, double irradiance, double radiation, double axialTiltDeg,
                          double surfaceGravity, double ringInnerKm, double ringOuterKm) {
        public boolean hasRings() {
            return ringOuterKm > ringInnerKm && ringInnerKm > 0;
        }
    }

    /** Terrain parameters for the planet engine. */
    public TerrainHints terrainHints(String bodyId) {
        AstroBody b = bodies.require(bodyId);
        return TerrainHints.of(b, bodies.parent(b).orElse(null), details(bodyId).irradiance(), seed.ageYears(), seed.id());
    }

    public AstroBody root() {
        return bodies.require(rootId);
    }

    public Vec3 position() {
        return seed.position();
    }

    public List<AstroBody> planets() {
        return bodies.children(rootId).stream().filter(b -> !b.isStar()).toList();
    }

    public Details details(String bodyId) {
        return details.get(bodyId);
    }

    public String nameEn() {
        return details.get(rootId).nameEn();
    }

    public String nameTr() {
        return details.get(rootId).nameTr();
    }
}
