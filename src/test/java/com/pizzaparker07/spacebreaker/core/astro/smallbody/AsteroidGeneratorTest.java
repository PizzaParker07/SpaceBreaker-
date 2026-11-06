package com.pizzaparker07.spacebreaker.core.astro.smallbody;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Kepler;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.Rng;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AsteroidGeneratorTest {
    private static final long SEED = 1234L;
    private static final List<Asteroid> BELT = AsteroidGenerator.population(SEED, Region.MAIN_BELT, 20000);

    @Test
    void deterministic() {
        assertEquals(AsteroidGenerator.generate(SEED, Region.MAIN_BELT, 17), AsteroidGenerator.generate(SEED, Region.MAIN_BELT, 17));
        assertNotEquals(AsteroidGenerator.generate(SEED, Region.MAIN_BELT, 17), AsteroidGenerator.generate(SEED + 1, Region.MAIN_BELT, 17));
        assertNotEquals(AsteroidGenerator.generate(SEED, Region.MAIN_BELT, 17).id(), AsteroidGenerator.generate(SEED, Region.TROJANS, 17).id());
    }

    @Test
    void sizesFollowThePowerLaw() {
        Rng rng = new Rng(3);
        int above1 = 0, n = 200000;
        for (int i = 0; i < n; i++) {
            double d = AsteroidGenerator.sampleDiameter(rng, 0.1, 100);
            assertTrue(d >= 0.1 && d <= 100);
            if (d > 1) {
                above1++;
            }
        }
        // N(>1 km) / N(>0.1 km) = 10^-2.5
        assertEquals(Math.pow(10, -2.5), above1 / (double) n, 0.0007);
        assertTrue(BELT.stream().allMatch(x -> x.diameterKm() >= 0.05 && x.diameterKm() <= 100));
    }

    @Test
    void orbitsAvoidTheKirkwoodGaps() {
        for (Asteroid a : BELT) {
            double au = a.distanceAu();
            assertTrue(au >= 2.1 && au <= 3.3);
            assertFalse(AsteroidGenerator.inKirkwoodGap(au), a.id() + " at " + au);
            assertTrue(a.orbit().eccentricity() <= 0.3 && a.orbit().inclination() <= Math.toRadians(40));
        }
        assertTrue(AsteroidGenerator.inKirkwoodGap(2.50) && AsteroidGenerator.inKirkwoodGap(3.28));
        assertFalse(AsteroidGenerator.inKirkwoodGap(2.7));
    }

    @Test
    void typesDependOnDistance() {
        int innerS = 0, innerC = 0, outerS = 0, outerC = 0;
        for (Asteroid a : BELT) {
            boolean inner = a.distanceAu() < 2.5, outer = a.distanceAu() > 3.0;
            if (inner && a.type() == SpectralType.S) innerS++;
            if (inner && a.type() == SpectralType.C) innerC++;
            if (outer && a.type() == SpectralType.S) outerS++;
            if (outer && a.type() == SpectralType.C) outerC++;
        }
        assertTrue(innerS > innerC, "S types dominate the inner belt");
        assertTrue(outerC > 3 * outerS, "C types dominate the outer belt");
        Map<SpectralType, Integer> count = new EnumMap<>(SpectralType.class);
        BELT.forEach(a -> count.merge(a.type(), 1, Integer::sum));
        assertEquals(0.07, count.get(SpectralType.M) / (double) BELT.size(), 0.015);
        assertTrue(count.getOrDefault(SpectralType.V, 0) > 0);
    }

    @Test
    void bulkPropertiesByType() {
        for (Asteroid a : BELT) {
            SpectralType t = a.type();
            assertTrue(a.albedo() >= t.albedoMin && a.albedo() <= t.albedoMax);
            assertEquals(t.grainDensity * (1 - a.porosity()), a.density(), 1e-6);
            assertTrue(a.mass() > 0 && a.shape().ba() <= 1 && a.shape().ca() <= a.shape().ba());
            assertTrue(a.metalVeins() >= 0 && a.metalVeins() <= 1 && a.pgmPpm() > 0);
        }
        double c = avg(SpectralType.C, Asteroid::albedo), s = avg(SpectralType.S, Asteroid::albedo);
        assertTrue(c < 0.1 && s > 0.15, "C dark, S bright");
        assertTrue(avg(SpectralType.M, Asteroid::density) > avg(SpectralType.S, Asteroid::density));
        assertTrue(avg(SpectralType.S, Asteroid::density) > avg(SpectralType.C, Asteroid::density));
        assertTrue(avg(SpectralType.M, Asteroid::pgmPpm) > 10 * avg(SpectralType.S, Asteroid::pgmPpm), "metal asteroids hold the platinum");
        // Vesta (V type, real bulk density 3456) sits within the V-type range for a low-porosity body
        assertEquals(3456, SpectralType.V.grainDensity * (1 - 0.03), 300);
        // rubble piles: C ≈ 1.2–1.6 g/cm³ (Bennu, Ryugu 1.19), S ≈ 1.9–2.7 (Itokawa 1.9, Eros 2.7)
        assertEquals(1450, rubble(SpectralType.C), 250);
        assertEquals(2050, rubble(SpectralType.S), 350);
    }

    @Test
    void spinBarrierAndTinyGravity() {
        for (Asteroid a : BELT) {
            if (a.rubblePile()) {
                assertTrue(a.rotationHours() >= AsteroidGenerator.SPIN_BARRIER_HOURS, "rubble piles cannot spin faster than ~2.2 h");
            }
        }
        assertTrue(BELT.stream().anyMatch(a -> !a.rubblePile() && a.rotationHours() < 1), "small monoliths can be fast rotators");
        Asteroid km = BELT.stream().filter(a -> Math.abs(a.diameterKm() - 1) < 0.1).findFirst().orElseThrow();
        assertTrue(km.escapeVelocity() < 1.2, "a 1 km asteroid: walking pace escapes it " + km.escapeVelocity());
        assertTrue(km.canJumpOff());
        Asteroid big = BELT.stream().max((x, y) -> Double.compare(x.diameterKm(), y.diameterKm())).orElseThrow();
        assertTrue(big.surfaceGravity() / big.density() > km.surfaceGravity() / km.density(), "g ∝ ρ·D");
        assertTrue(big.surfaceGravity() < 0.1, "even the biggest generated asteroid has tiny gravity");
    }

    @Test
    void trojansSitAtL4AndL5() {
        List<Asteroid> trojans = AsteroidGenerator.population(SEED, Region.TROJANS, 2000);
        double jupiterL = Math.toRadians(AsteroidGenerator.JUPITER_MEAN_LONGITUDE_DEG);
        int l4 = 0, l5 = 0;
        for (Asteroid a : trojans) {
            assertEquals(5.2, a.distanceAu(), 0.2);
            double lead = Math.toDegrees(Kepler.wrapSigned(a.orbit().meanLongitude(0) - jupiterL));
            assertTrue(Math.abs(Math.abs(lead) - 60) < 50, "near a Lagrange point: " + lead);
            if (lead > 0) l4++;
            else l5++;
            assertTrue(a.type() == SpectralType.D || a.type() == SpectralType.C, "dark primitive bodies");
        }
        assertEquals(0.5, l4 / (double) (l4 + l5), 0.05);
        assertEquals(AsteroidGenerator.generate(SEED, Region.TROJANS, 0).orbit().period(),
                Kepler.period(AsteroidGenerator.generate(SEED, Region.TROJANS, 0).orbit().semiMajorAxis(), AstroConstants.MU_SUN), 1e-3);
    }

    @Test
    void kuiperBeltIsIcy() {
        for (Asteroid a : AsteroidGenerator.population(SEED, Region.KUIPER_BELT, 500)) {
            assertTrue(a.distanceAu() >= 39 && a.distanceAu() <= 48);
            if (a.type() == SpectralType.D) {
                assertTrue(a.terrain().iceFraction() > 0.3);
            }
        }
    }

    @Test
    void terrainAndBodyRecords() {
        for (Asteroid a : BELT.subList(0, 500)) {
            AstroBody b = a.toAstroBody();
            assertEquals(AstroBody.Kind.ASTEROID, b.kind());
            assertTrue(b.landable() && !b.hasAtmosphere());
            assertEquals(a.surfaceGravity(), b.surfaceGravity(), a.surfaceGravity() * 1e-9);
            assertTrue(b.dayTempK() >= b.nightTempK());
            AsteroidTerrain t = a.terrain();
            assertNotEquals(TerrainHints.ColorFamily.GAS_BANDS, t.hints().colorFamily());
            assertTrue(t.params().craterDensity() > 0.8, "airless and old: heavily cratered");
            assertTrue(t.rockFraction() >= 0.15 && t.rockFraction() <= 0.9);
            assertEquals(a.metalVeins(), t.metalVeins());
        }
        Asteroid small = BELT.stream().filter(a -> a.diameterKm() < 0.1).findFirst().orElseThrow();
        Asteroid large = BELT.stream().max((x, y) -> Double.compare(x.diameterKm(), y.diameterKm())).orElseThrow();
        assertTrue(small.terrain().rockFraction() > large.terrain().rockFraction(), "big bodies keep fine regolith");
        Asteroid metal = BELT.stream().filter(a -> a.type() == SpectralType.M).findFirst().orElseThrow();
        assertTrue(metal.terrain().metalVeins() > 0.6);
    }

    private static double rubble(SpectralType t) {
        return BELT.stream().filter(a -> a.type() == t && a.rubblePile()).mapToDouble(Asteroid::density).average().orElseThrow();
    }

    private static double avg(SpectralType t, java.util.function.ToDoubleFunction<Asteroid> f) {
        return BELT.stream().filter(a -> a.type() == t).mapToDouble(f).average().orElseThrow();
    }
}
