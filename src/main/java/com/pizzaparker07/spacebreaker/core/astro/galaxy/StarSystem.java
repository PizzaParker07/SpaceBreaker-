package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.List;
import java.util.Map;

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
                         Map<String, Details> details) {

    /**
     * @param nameEn          English display name
     * @param nameTr          Turkish display name
     * @param irradiance      starlight at the body, W/m²
     * @param radiation       surface dose without shielding, mSv/day
     * @param axialTiltDeg    axial tilt
     * @param surfaceGravity  m/s²
     */
    public record Details(String nameEn, String nameTr, double irradiance, double radiation, double axialTiltDeg,
                          double surfaceGravity) {
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
