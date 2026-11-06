package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.PlanetModel;
import com.pizzaparker07.spacebreaker.core.astro.StellarModel;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GalaxyTest {
    private static final long SEED = 123456789L;

    @Test
    void rngIsDeterministicAndUniform() {
        Rng a = new Rng(42), b = new Rng(42);
        double sum = 0;
        for (int i = 0; i < 100000; i++) {
            double x = a.nextDouble();
            assertEquals(x, b.nextDouble());
            assertTrue(x >= 0 && x < 1);
            sum += x;
        }
        assertEquals(0.5, sum / 100000, 0.005);
        assertEquals(new Rng(7).nextLong(), new Rng(7).nextLong());
        assertNotEquals(Rng.hash(1, 2, 3), Rng.hash(1, 3, 2));
        Rng p = new Rng(9);
        double mean = 0;
        for (int i = 0; i < 20000; i++) {
            mean += p.poisson(3.5);
        }
        assertEquals(3.5, mean / 20000, 0.05);
        // SplitMix64 reference output; any change to the generator changes every world
        assertEquals(0xE220A8397B1DCDAFL, new Rng(0).nextLong());
    }

    @Test
    void densityModel() {
        assertEquals(0.1, GalaxyModel.density(GalaxyModel.SUN), 1e-9);
        // denser towards the centre, sparse far above the disk and beyond its edge
        assertTrue(GalaxyModel.density(0, 0, 0) > 5 * GalaxyModel.density(GalaxyModel.SUN));
        assertTrue(GalaxyModel.density(-8178, 0, 2000) < 0.01);
        assertTrue(GalaxyModel.density(-25000, 0, 0) < 0.001);
        // the bar is elongated along its axis
        double c = Math.cos(Math.toRadians(27)), s = Math.sin(Math.toRadians(27));
        assertTrue(GalaxyModel.density(2000 * c, 2000 * s, 0) > GalaxyModel.density(-2000 * s, 2000 * c, 0));
        // spiral arms: around a ring of radius 6 kpc the density varies with azimuth
        double min = Double.MAX_VALUE, max = 0;
        for (int i = 0; i < 360; i++) {
            double t = Math.toRadians(i);
            double d = GalaxyModel.density(6000 * Math.cos(t), 6000 * Math.sin(t), 0);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        assertTrue(max > 1.5 * min, "arms should stand out: " + min + " .. " + max);
    }

    @Test
    void sectorsAreDeterministicAndSeedDependent() {
        Galaxy g1 = new Galaxy(SEED), g2 = new Galaxy(SEED), other = new Galaxy(SEED + 1);
        Galaxy.SectorKey k = Galaxy.sectorOf(GalaxyModel.SUN.add(new Vec3(35, -12, 3)));
        assertEquals(g1.sector(k).stars(), g2.sector(k).stars());
        assertNotEquals(g1.sector(k).stars(), other.sector(k).stars());
        int n = g1.sector(k).stars().size();
        assertTrue(n > 50 && n < 160, "about 100 stars per sector near the Sun: " + n);
        for (StarSeed s : g1.sector(k).stars()) {
            assertEquals(k, Galaxy.sectorOf(s.position()));
            assertTrue(s.mass() >= 0.08 && s.mass() <= 150);
            assertEquals(s, g1.star(s.sx(), s.sy(), s.sz(), s.index()).orElseThrow());
        }
    }

    @Test
    void initialMassFunctionIsDominatedByRedDwarfs() {
        Rng rng = new Rng(5);
        int red = 0, sunLike = 0, massive = 0, n = 50000;
        for (int i = 0; i < n; i++) {
            double m = StarSystemGenerator.sampleMass(rng);
            if (m < 0.5) {
                red++;
            } else if (m < 1.5) {
                sunLike++;
            } else if (m > 8) {
                massive++;
            }
        }
        assertEquals(0.76, red / (double) n, 0.02);
        assertTrue(sunLike > massive * 10);
        assertTrue(massive > 0);
    }

    @Test
    void nearbyRealStars() {
        Galaxy g = new Galaxy(SEED);
        StarSeed proxima = g.fixedStar("proxima_centauri").orElseThrow();
        assertEquals(4.24, proxima.distanceFromSun() * AstroConstants.PARSEC / AstroConstants.LIGHT_YEAR, 0.01);
        assertEquals(0, g.sun().distanceFromSun(), 1e-9);
        StarSeed alpha = g.fixedStar("alpha_centauri").orElseThrow();
        assertEquals(0.06, alpha.position().distance(proxima.position()), 0.03, "Proxima sits ~13000 AU from Alpha Cen");
        // fixed stars are part of their sector and nothing generated sits on top of them or near the Sun
        for (StarSeed f : g.fixedStars()) {
            List<StarSeed> stars = g.sector(f.sx(), f.sy(), f.sz()).stars();
            assertTrue(stars.contains(f), f.fixedKey());
        }
        for (StarSeed s : g.starsWithin(GalaxyModel.SUN, 1.5)) {
            assertTrue(s.fixed(), "no fake star closer than Proxima: " + s);
        }
        assertTrue(g.system(g.sun()).isEmpty(), "the Sun's system comes from the data registry");
        Set<String> keys = new HashSet<>();
        for (NearbyStars.Entry e : NearbyStars.ALL) {
            assertTrue(keys.add(e.key()));
            assertFalse(e.nameTr().isEmpty());
        }
        assertEquals(40.7, NearbyStars.entry("trappist_1").orElseThrow().distanceLy(), 0.2);
    }

    @Test
    void trappist1HasItsSevenPlanets() {
        Galaxy g = new Galaxy(SEED);
        StarSystem t = g.system(g.fixedStar("trappist_1").orElseThrow()).orElseThrow();
        List<AstroBody> planets = t.planets();
        assertEquals(7, planets.size());
        double[] periods = {1.510826, 2.421937, 4.049219, 6.101013, 9.207540, 12.352446, 18.772866};
        for (int i = 0; i < 7; i++) {
            AstroBody p = t.bodies().require("spacebreaker:trappist_1_" + (char) ('b' + i));
            assertEquals(periods[i], p.orbit().period() / AstroConstants.DAY, periods[i] * 0.02, p.id());
            assertTrue(p.tidallyLocked(), p.id() + " is locked");
            assertTrue(p.landable());
        }
        // e, f and g sit in or near the habitable zone
        AstroBody e = t.bodies().require("spacebreaker:trappist_1_e");
        assertTrue(PlanetModel.inHabitableZone(t.primary().luminosity(), e.orbit().semiMajorAxis() / AstroConstants.AU));
        assertEquals(StellarModel.SpectralClass.M, t.primary().spectralClass());
    }

    @Test
    void otherRealSystems() {
        Galaxy g = new Galaxy(SEED);
        StarSystem prox = g.system(g.fixedStar("proxima_centauri").orElseThrow()).orElseThrow();
        AstroBody b = prox.bodies().require("spacebreaker:proxima_centauri_b");
        assertEquals(11.19, b.orbit().period() / AstroConstants.DAY, 0.15);
        assertTrue(b.tidallyLocked());
        assertEquals("Proxima Centauri b", prox.details(b.id()).nameEn());
        assertEquals(0.65, prox.details(b.id()).irradiance() / 1361, 0.05);

        StarSystem alpha = g.system(g.fixedStar("alpha_centauri").orElseThrow()).orElseThrow();
        AstroBody bStar = alpha.bodies().require("spacebreaker:alpha_centauri_b");
        assertTrue(bStar.isStar());
        assertEquals(79.91, bStar.orbit().period() / AstroConstants.YEAR, 0.1);
        for (AstroBody p : alpha.planets()) {
            assertTrue(p.orbit().semiMajorAxis() <= 2.5 * 2.3 * AstroConstants.AU, "planets stay stable around A");
        }
        StarSystem sirius = g.system(g.fixedStar("sirius").orElseThrow()).orElseThrow();
        assertEquals("Sirius A", sirius.nameEn());
        assertTrue(sirius.bodies().require("spacebreaker:sirius_b").radius() < 0.01 * AstroConstants.SOLAR_RADIUS, "white dwarf");
        StarSystem eps = g.system(g.fixedStar("epsilon_eridani").orElseThrow()).orElseThrow();
        assertEquals(AstroBody.Kind.GAS_GIANT, eps.bodies().require("spacebreaker:epsilon_eridani_b").kind());
        assertEquals("Barnard Yıldızı", g.system(g.fixedStar("barnards_star").orElseThrow()).orElseThrow().nameTr());
        // same seed -> same filled-in planets
        assertEquals(new Galaxy(SEED).system(g.fixedStar("tau_ceti").orElseThrow()).orElseThrow().planets(),
                g.system(g.fixedStar("tau_ceti").orElseThrow()).orElseThrow().planets());
    }

    @Test
    void generatedSystemsArePlausible() {
        Galaxy g = new Galaxy(SEED);
        int systems = 0, planets = 0, moons = 0, habitable = 0, giants = 0, remnants = 0;
        for (StarSeed s : g.starsWithin(GalaxyModel.SUN.add(new Vec3(60, 40, 0)), 15)) {
            StarSystem sys = g.system(s).orElseThrow();
            assertSame(sys, g.system(s).orElseThrow(), "systems are cached");
            assertEquals(sys.planets(), StarSystemGenerator.generate(s).planets(), "generation is deterministic");
            systems++;
            if (sys.kind() != StarKind.MAIN_SEQUENCE) {
                remnants++;
            }
            AstroBody star = sys.root();
            double prevA = 0;
            for (AstroBody p : sys.planets()) {
                planets++;
                double a = p.orbit().semiMajorAxis();
                assertTrue(a > prevA * 1.3, "planets are spaced apart");
                assertTrue(a > star.radius() * 3, "no planet inside its star");
                prevA = a;
                assertTrue(p.orbit().eccentricity() < 0.6);
                assertTrue(p.mass() > 0 && p.radius() > 0 && p.dayTempK() >= p.nightTempK() && p.nightTempK() > 0, p.id());
                assertEquals(p.landable(), !p.kind().isGiant());
                if (p.kind().isGiant()) {
                    giants++;
                }
                if (PlanetModel.inHabitableZone(sys.primary().luminosity(), a / AstroConstants.AU) && !p.kind().isGiant()) {
                    habitable++;
                }
                double hill = Kepler.hillRadius(a, p.orbit().eccentricity(), p.mass(), star.mass());
                for (AstroBody m : sys.bodies().children(p.id())) {
                    moons++;
                    assertTrue(m.orbit().semiMajorAxis() < hill / 3 + 1, "moon " + m.id() + " inside a third of the Hill sphere");
                    assertTrue(m.orbit().semiMajorAxis() > p.radius() * 2, "moon outside its planet");
                    assertEquals(AstroBody.Kind.MOON, m.kind());
                }
                StarSystem.Details d = sys.details(p.id());
                assertNotNull(d);
                assertTrue(d.nameEn().startsWith(StarSystemGenerator.designation(s.id())));
            }
        }
        assertTrue(systems > 500, "systems " + systems);
        assertTrue(planets > systems, "planets are common: " + planets + " / " + systems);
        assertTrue(moons > 0 && giants > 0 && habitable > 0, moons + " moons, " + giants + " giants, " + habitable + " HZ");
        assertTrue(remnants > 0, "some white dwarfs among old stars");
    }
}
