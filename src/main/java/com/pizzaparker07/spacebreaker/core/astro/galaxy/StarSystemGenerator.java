package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.PlanetModel;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;

import java.util.Locale;

/**
 * Deterministic generation of stars and their planets and moons from a {@link StarSeed}.
 * Planets are spaced in roughly geometric orbits (period ratios ≈ 1.4–2.3, like real compact systems),
 * rocky inside the snow line, giants and icy worlds beyond it; moons stay inside a third of the Hill sphere.
 */
public final class StarSystemGenerator {
    public static final int MAX_PLANETS = 10;
    public static final String ID_PREFIX = "spacebreaker:star/";
    private static final double ROMAN_LIMIT = 10;

    /** Initial mass function (Kroupa 2001) between 0.08 and 150 M☉. */
    public static double sampleMass(Rng rng) {
        double lo = 0.08, mid = 0.5, hi = 150;
        double n1 = (Math.pow(lo, -0.3) - Math.pow(mid, -0.3)) / 0.3;
        double n2 = 0.5 * (Math.pow(mid, -1.3) - Math.pow(hi, -1.3)) / 1.3;
        double u = rng.nextDouble();
        if (rng.nextDouble() < n1 / (n1 + n2)) {
            // invert ∫ m^-1.3
            double a = Math.pow(lo, -0.3), b = Math.pow(mid, -0.3);
            return Math.pow(a - u * (a - b), -1 / 0.3);
        }
        double a = Math.pow(mid, -1.3), b = Math.pow(hi, -1.3);
        return Math.pow(a - u * (a - b), -1 / 1.3);
    }

    /** Designation of a generated star, e.g. {@code SB 1F3K-9QX}. */
    public static String designation(long id) {
        String s = Long.toString(id >>> 24 & 0xFFFFFFFFFL, 36).toUpperCase(Locale.ROOT);
        while (s.length() < 7) {
            s = "0" + s;
        }
        return "SB " + s.substring(0, 4) + "-" + s.substring(4);
    }

    public static String rootId(long id) {
        return ID_PREFIX + Long.toHexString(id);
    }

    /** What a seed's star is today. */
    public record StarState(StarKind kind, StellarModel.Star star) {
    }

    /** The star alone (cheap); remnants for stars older than their lifetime. */
    public static StarState starOf(StarSeed seed) {
        double life = StellarModel.lifetimeYears(seed.mass());
        if (seed.ageYears() <= life) {
            return new StarState(StarKind.MAIN_SEQUENCE, StellarModel.mainSequence(seed.mass()));
        }
        double since = seed.ageYears() - life;
        if (seed.mass() < 8) {
            double m = Math.min(1.35, 0.109 * seed.mass() + 0.394);
            double t = Math.max(4000, 30000 * Math.pow(1 + since / 1e8, -0.4));
            double r = 0.012 * Math.pow(m / 0.6, -1.0 / 3.0);
            double l = r * r * Math.pow(t / AstroConstants.SOLAR_TEMPERATURE, 4);
            return new StarState(StarKind.WHITE_DWARF, StellarModel.catalogue(m, l, r, t));
        }
        if (seed.mass() < 25) {
            return new StarState(StarKind.NEUTRON_STAR, StellarModel.catalogue(1.4, 1e-6, 1.7e-5, 1e6));
        }
        double m = seed.mass() * 0.3;
        return new StarState(StarKind.BLACK_HOLE, StellarModel.catalogue(m, 1e-12, 4.24e-6 * m, 1));
    }

    public static StarSystem generate(StarSeed seed) {
        StarState st = starOf(seed);
        StarKind kind = st.kind();
        StellarModel.Star star = st.star();
        String name = StarNames.properName(seed.id());
        SystemBuilder b = new SystemBuilder(seed, rootId(seed.id()), kind, star, name, name);
        b.designation(designation(seed.id()));
        double maxAu = Double.POSITIVE_INFINITY;
        StarSystem.Binary binary = binaryOf(seed, star);
        if (binary != null) {
            b.binary(binary);
            maxAu = binary.stableLimitAu();
        }
        if (kind.hasPlanets()) {
            addPlanets(b, new Rng(Rng.hash(seed.id(), 0x504C414EL)), maxAu, name, name);
        }
        return b.build();
    }

    /** Multiplicity grows with mass (≈ 27 % of red dwarfs, ≈ 45 % of Sun-like stars, ≈ 60 % above 1.5 M☉). */
    public static double binaryFraction(double mass) {
        return mass < 0.5 ? 0.27 : mass < 1.5 ? 0.45 : 0.6;
    }

    /** The companion of a generated star, or null for a single star. */
    static StarSystem.Binary binaryOf(StarSeed seed, StellarModel.Star primary) {
        Rng rng = new Rng(Rng.hash(seed.id(), 0x42494E41L));
        if (!rng.chance(binaryFraction(seed.mass()))) {
            return null;
        }
        double m2 = Math.max(0.08, seed.mass() * rng.range(0.1, 1.0));
        // separations: log-normal around ~50 AU (Raghavan et al. 2010)
        double a = Math.pow(10, 1.7 + 1.5 * rng.gaussian());
        a = Math.max(0.05, Math.min(a, 20000));
        double e = a < 1 ? Math.min(0.3, rng.rayleigh(0.05)) : rng.range(0, 0.8);
        double period = Kepler.period(a * AstroConstants.AU, AstroConstants.G * (primary.mass() + m2) * AstroConstants.SOLAR_MASS);
        return new StarSystem.Binary(StellarModel.mainSequence(m2), a, e, period / AstroConstants.DAY);
    }

    /**
     * Adds generated planets around the builder's primary.
     *
     * @param maxAu stability limit (binaries), AU
     */
    static void addPlanets(SystemBuilder b, Rng rng, double maxAu, String nameEn, String nameTr) {
        addPlanets(b, rng, maxAu, nameEn, nameTr, 0, 0);
    }

    /** Variant that starts outside already-known planets: {@code startAu} and letters from {@code letterOffset}. */
    static void addPlanets(SystemBuilder b, Rng rng, double maxAu, String nameEn, String nameTr, double startAu, int letterOffset) {
        StellarModel.Star star = b.primary();
        double m = star.mass();
        double l = star.luminosity();
        boolean dwarf = star.radius() < 0.05;
        int count = Math.min(MAX_PLANETS, rng.poisson(dwarf ? 1.0 : 2.5 + 2.0 * Math.min(m, 1.5)));
        double snow = 2.7 * Math.sqrt(l);
        double minA = Math.max(5 * star.radius() * AstroConstants.SOLAR_RADIUS / AstroConstants.AU, 0.008);
        double a = startAu > 0 ? startAu * rng.range(1.4, 2.2) : Math.max(minA, rng.logRange(0.02, 0.4) * Math.sqrt(m));
        double prevApo = startAu > 0 ? startAu : 0;
        for (int i = 0; i < count; i++) {
            // orbits never cross: each periapsis clears the previous apoapsis by 15 %
            a = Math.max(a, prevApo * 1.25);
            double ecc = Math.min(0.5, rng.rayleigh(0.05));
            if (prevApo > 0) {
                ecc = Math.max(0, Math.min(ecc, 1 - prevApo * 1.15 / a));
            }
            if (a * (1 + ecc) > maxAu || a > 400) {
                break;
            }
            char letter = (char) ('b' + letterOffset + i);
            if (letter > 'z') {
                break;
            }
            double giantChance = a > snow ? 0.35 * Math.min(m, 2) : a < 0.1 ? 0.03 * Math.min(m, 2) : 0.01;
            double me;
            PlanetModel.Composition comp;
            if (rng.chance(giantChance)) {
                me = rng.logRange(8, 1500);
                comp = me > 40 ? PlanetModel.Composition.GAS_GIANT
                        : rng.chance(0.7) ? PlanetModel.Composition.ICE_GIANT : PlanetModel.Composition.GAS_GIANT;
            } else if (a < snow) {
                me = rng.logRange(0.03, 8);
                comp = a < 0.3 && rng.chance(0.15) ? PlanetModel.Composition.IRON_RICH
                        : rng.chance(0.15) ? PlanetModel.Composition.OCEAN : PlanetModel.Composition.ROCKY;
            } else {
                me = rng.logRange(0.002, 3);
                comp = rng.chance(0.25) ? PlanetModel.Composition.OCEAN : PlanetModel.Composition.ICY;
            }
            double re = PlanetModel.radiusFor(me, comp);
            String id = b.rootId() + "/" + letter;
            AstroBody p = b.body(id, b.rootId(), nameEn + " " + letter, nameTr + " " + letter, me * AstroConstants.EARTH_MASS,
                    re * AstroConstants.EARTH_RADIUS, comp, a * AstroConstants.AU, ecc,
                    Math.toRadians(Math.abs(rng.gaussian()) * 1.5), rng.range(0, 2 * Math.PI), rng.range(0, 2 * Math.PI),
                    rng.range(0, 2 * Math.PI), rng.logRange(8, 60), Math.abs(rng.gaussian()) * 20, rng);
            addRings(b, p, comp, rng);
            addMoons(b, p, comp, rng, nameEn + " " + letter, nameTr + " " + letter, a > snow);
            prevApo = a * (1 + ecc);
            a *= rng.logRange(1.4, 2.3);
        }
    }

    /** Giants often have rings inside their Roche limit for ice (density 1000 kg/m³). */
    private static void addRings(SystemBuilder b, AstroBody planet, PlanetModel.Composition comp, Rng rng) {
        boolean giant = comp == PlanetModel.Composition.GAS_GIANT || comp == PlanetModel.Composition.ICE_GIANT;
        if (!giant || !rng.chance(0.4)) {
            return;
        }
        double roche = rocheLimitIce(planet);
        double inner = planet.radius() * rng.range(1.1, 1.4);
        double outer = Math.min(roche, inner * rng.range(1.3, 2.2));
        if (outer > inner * 1.05) {
            b.rings(planet.id(), inner / 1000, outer / 1000);
        }
    }

    /** Fluid Roche limit for an icy body around a planet, m. */
    public static double rocheLimitIce(AstroBody planet) {
        return 2.44 * planet.radius() * Math.cbrt(planet.density() / 1000.0);
    }

    private static void addMoons(SystemBuilder b, AstroBody planet, PlanetModel.Composition comp, Rng rng,
                                 String nameEn, String nameTr, boolean cold) {
        boolean giant = comp == PlanetModel.Composition.GAS_GIANT || comp == PlanetModel.Composition.ICE_GIANT;
        int count = giant ? Math.min(6, rng.poisson(2.5)) : planet.mass() > 0.3 * AstroConstants.EARTH_MASS && rng.chance(0.3) ? 1 : 0;
        if (count == 0) {
            return;
        }
        AstroBody star = b.find(b.rootId());
        double hill = Kepler.hillRadius(planet.orbit().semiMajorAxis(), planet.orbit().eccentricity(), planet.mass(), star.mass());
        double limit = hill / 3;
        double a = Math.max(planet.radius() * rng.range(4, 8), rocheLimitIce(planet) * 1.2);
        for (int i = 0; i < count && a < limit; i++) {
            double me = giant ? rng.logRange(1e-5, 0.03) : planet.mass() / AstroConstants.EARTH_MASS * rng.logRange(1e-3, 0.03);
            PlanetModel.Composition mc = cold ? (rng.chance(0.3) ? PlanetModel.Composition.OCEAN : PlanetModel.Composition.ICY)
                    : PlanetModel.Composition.ROCKY;
            double re = PlanetModel.radiusFor(me, mc);
            String roman = roman(i + 1);
            b.body(planet.id() + "/" + roman.toLowerCase(Locale.ROOT), planet.id(), nameEn + " " + roman, nameTr + " " + roman,
                    me * AstroConstants.EARTH_MASS, re * AstroConstants.EARTH_RADIUS, mc, a, rng.rayleigh(0.01),
                    Math.toRadians(Math.abs(rng.gaussian())), rng.range(0, 2 * Math.PI), rng.range(0, 2 * Math.PI),
                    rng.range(0, 2 * Math.PI), 24, 0, rng);
            a *= rng.logRange(1.5, 2.6);
        }
    }

    static String roman(int n) {
        String[] r = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= ROMAN_LIMIT ? r[n - 1] : Integer.toString(n);
    }

    private StarSystemGenerator() {
    }
}
