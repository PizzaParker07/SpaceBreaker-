package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.OrbitalElements;
import com.pizzaparker07.spacebreaker.core.astro.PlanetModel;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles star systems from physical parameters, filling everything the data format needs
 * (temperatures, atmospheres, rotation, irradiance, radiation) from {@link PlanetModel}.
 */
final class SystemBuilder {
    private static final double EARTH_MASS = AstroConstants.EARTH_MASS;
    private static final double EARTH_RADIUS = AstroConstants.EARTH_RADIUS;

    private final StarSeed seed;
    private final String rootId;
    private final StarKind kind;
    private final StellarModel.Star primary;
    private final List<AstroBody> bodies = new ArrayList<>();
    private final Map<String, StarSystem.Details> details = new LinkedHashMap<>();
    private final Map<String, Double> starDistanceAu = new HashMap<>();
    private final double ageYears;
    private String designation;
    private StarSystem.Binary binary;
    private boolean discovered;

    SystemBuilder(StarSeed seed, String rootId, StarKind kind, StellarModel.Star primary, String nameEn, String nameTr) {
        this.seed = seed;
        this.rootId = rootId;
        this.kind = kind;
        this.primary = primary;
        this.ageYears = seed.ageYears();
        this.designation = nameEn;
        bodies.add(star(rootId, null, primary, null));
        details.put(rootId, new StarSystem.Details(nameEn, nameTr, 0, 0, 0, gravity(primary), 0, 0));
    }

    String rootId() {
        return rootId;
    }

    StellarModel.Star primary() {
        return primary;
    }

    /** A companion star orbiting the primary. */
    void companion(String id, String nameEn, String nameTr, StellarModel.Star s, double aAu, double e, double periodDays) {
        OrbitalElements orbit = elements(aAu * AstroConstants.AU, e, 0, 0, 0, 0, periodDays * AstroConstants.DAY);
        bodies.add(star(id, rootId, s, orbit));
        details.put(id, new StarSystem.Details(nameEn, nameTr, 0, 0, 0, gravity(s), 0, 0));
    }

    private static double gravity(StellarModel.Star s) {
        return AstroConstants.G * s.mass() * AstroConstants.SOLAR_MASS / Math.pow(s.radius() * AstroConstants.SOLAR_RADIUS, 2);
    }

    private static AstroBody star(String id, String parent, StellarModel.Star s, OrbitalElements orbit) {
        return new AstroBody(id, parent, AstroBody.Kind.STAR, s.mass() * AstroConstants.SOLAR_MASS,
                s.radius() * AstroConstants.SOLAR_RADIUS, orbit, 25 * AstroConstants.DAY, false, 0, Map.of(),
                s.temperature(), s.temperature(), 0, false);
    }

    /** Planet around the primary (or a moon around {@code parentId}); returns the new body. Distances in m. */
    AstroBody body(String id, String parentId, String nameEn, String nameTr, double massKg, double radiusM,
                   PlanetModel.Composition comp, double a, double e, double inc, double lan, double argp, double m0,
                   double rotationHours, double axialTilt, Rng rng) {
        AstroBody parent = find(parentId);
        double mu = AstroConstants.G * (parent.mass() + massKg);
        double period = Kepler.period(a, mu);
        OrbitalElements orbit = new OrbitalElements(mu, a, e, inc, lan, argp, m0);
        double distAu = parent.isStar() ? a / AstroConstants.AU : starDistanceAu.getOrDefault(parentId, 1.0);
        starDistanceAu.put(id, distAu);
        double flux = PlanetModel.fluxSolar(primary.luminosity(), distAu);
        boolean giant = comp == PlanetModel.Composition.GAS_GIANT || comp == PlanetModel.Composition.ICE_GIANT;
        double albedo = switch (comp) {
            case IRON_RICH -> 0.12;
            case ROCKY -> 0.3;
            case OCEAN -> 0.4;
            case ICY -> 0.6;
            case ICE_GIANT -> 0.3;
            case GAS_GIANT -> 0.34;
        };
        double teq = PlanetModel.equilibriumTemperature(flux, albedo);
        double vEsc = Math.sqrt(2 * AstroConstants.G * massKg / radiusM);
        double pressure = 0;
        Map<String, Double> air = Map.of();
        double greenhouse = 0;
        if (giant) {
            pressure = 100;
            air = comp == PlanetModel.Composition.GAS_GIANT ? Map.of("H2", 0.9, "He", 0.1)
                    : Map.of("H2", 0.82, "He", 0.15, "CH4", 0.03);
        } else if (massKg > 0.008 * EARTH_MASS
                && PlanetModel.retainsGas(vEsc, PlanetModel.exosphereTemperature(teq, flux), 28)
                && rng.chance(0.85)) {
            double me = massKg / EARTH_MASS;
            pressure = 101.325 * Math.pow(me, 1.3) * rng.logRange(0.01, 30);
            if (teq > 330) {
                air = Map.of("CO2", 0.96, "N2", 0.035, "SO2", 0.005);
                greenhouse = 0.96;
            } else if (teq > 160) {
                double co2 = rng.logRange(0.0004, 0.2);
                air = Map.of("N2", 0.98 - co2, "CO2", co2, "Ar", 0.02);
                greenhouse = co2 * 25; // water vapour feedback in temperate climates
            } else {
                air = Map.of("N2", 0.95, "CH4", 0.05);
                greenhouse = 0.02;
            }
        }
        double surface = PlanetModel.surfaceTemperature(teq, PlanetModel.opticalDepth(pressure, Math.min(1, greenhouse)));
        boolean locked = parent.isStar()
                ? PlanetModel.tidallyLocked(a, massKg, radiusM, parent.mass(), ageYears)
                : PlanetModel.tidallyLocked(a, massKg, radiusM, parent.mass(), Math.max(ageYears, 1e9));
        double day = locked ? period / 3600.0 : rotationHours;
        // thin or no air: big day/night swings; thick air evens them out
        double swing = giant ? 0 : 0.45 / (1 + pressure / 10.0);
        double dayK = surface * (1 + (locked && parent.isStar() ? 1.6 : 1) * swing * 0.6);
        double nightK = Math.max(Math.min(30, surface), surface * (1 - swing));
        double radiation = giant ? 0 : 1.4 * Math.sqrt(flux / AstroConstants.SOLAR_CONSTANT + 0.05) / (1 + pressure / 20.0);
        AstroBody b = new AstroBody(id, parentId, kindFor(massKg, comp, parent.isStar()), massKg, radiusM, orbit,
                day * 3600.0, locked, pressure, air, dayK, nightK, albedo, !giant);
        bodies.add(b);
        details.put(id, new StarSystem.Details(nameEn, nameTr, flux, radiation, axialTilt, AstroConstants.G * massKg / (radiusM * radiusM), 0, 0, discovered));
        return b;
    }

    private static AstroBody.Kind kindFor(double massKg, PlanetModel.Composition comp, boolean aroundStar) {
        if (comp == PlanetModel.Composition.GAS_GIANT) {
            return AstroBody.Kind.GAS_GIANT;
        }
        if (comp == PlanetModel.Composition.ICE_GIANT) {
            return AstroBody.Kind.ICE_GIANT;
        }
        if (!aroundStar) {
            return AstroBody.Kind.MOON;
        }
        return massKg < 0.01 * EARTH_MASS ? AstroBody.Kind.DWARF : AstroBody.Kind.TERRESTRIAL;
    }

    /** Body from mass and radius in Earth units and the semi-major axis in AU (or km for moons). */
    AstroBody planet(String id, String nameEn, String nameTr, double massEarths, double radiusEarths, double aAu,
                     double e, double incDeg, Rng rng) {
        if (radiusEarths <= 0) { // unknown: estimate from the mass
            PlanetModel.Composition guess = massEarths < 10 ? PlanetModel.Composition.ROCKY
                    : massEarths < 50 ? PlanetModel.Composition.ICE_GIANT : PlanetModel.Composition.GAS_GIANT;
            radiusEarths = PlanetModel.radiusFor(massEarths, guess);
        }
        double massKg = massEarths * EARTH_MASS;
        double radius = radiusEarths * EARTH_RADIUS;
        PlanetModel.Composition comp = PlanetModel.composition(massEarths, PlanetModel.density(massKg, radius));
        return body(id, rootId, nameEn, nameTr, massKg, radius, comp, aAu * AstroConstants.AU, e,
                Math.toRadians(incDeg), rng.range(0, 2 * Math.PI), rng.range(0, 2 * Math.PI), rng.range(0, 2 * Math.PI),
                rng.logRange(8, 60), rng.range(0, 30), rng);
    }

    AstroBody find(String id) {
        for (AstroBody b : bodies) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        throw new IllegalArgumentException(id);
    }

    double starDistanceAu(String id) {
        return starDistanceAu.getOrDefault(id, 0.0);
    }

    void designation(String d) {
        this.designation = d;
    }

    /** Bodies added from now on are real, confirmed ones (true) or generator fill-ins (false, the default). */
    void discovered(boolean d) {
        this.discovered = d;
    }

    void binary(StarSystem.Binary b) {
        this.binary = b;
    }

    /** Gives a body a ring system (radii in km from its centre). */
    void rings(String id, double innerKm, double outerKm) {
        StarSystem.Details d = details.get(id);
        details.put(id, new StarSystem.Details(d.nameEn(), d.nameTr(), d.irradiance(), d.radiation(), d.axialTiltDeg(),
                d.surfaceGravity(), innerKm, outerKm, d.discovered()));
    }

    StarSystem build() {
        return new StarSystem(seed, rootId, kind, primary, new BodySystem(bodies), Map.copyOf(details), designation,
                java.util.Optional.ofNullable(binary));
    }

    static OrbitalElements elements(double a, double e, double inc, double lan, double argp, double m0, double period) {
        double n = 2 * Math.PI / period;
        return new OrbitalElements(n * n * a * a * a, a, e, inc, lan, argp, m0);
    }
}
