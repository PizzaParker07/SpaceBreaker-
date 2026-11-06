package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.PlanetModel;
import com.pizzaparker07.spacebreaker.core.astro.TerrainHints;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Physical limits of generated systems, names, binaries, rings and terrain hints. */
class GeneratorTest {
    private static final long SEED = 987654321L;

    private static List<StarSystem> sample() {
        Galaxy g = new Galaxy(SEED);
        List<StarSystem> out = new ArrayList<>();
        for (StarSeed s : g.starsWithin(GalaxyModel.SUN.add(new Vec3(-30, 70, 5)), 18)) {
            out.add(g.system(s).orElseThrow());
        }
        return out;
    }

    @Test
    void sameSeedSameUniverse() {
        Galaxy a = new Galaxy(SEED), b = new Galaxy(SEED);
        Vec3 c = GalaxyModel.SUN.add(new Vec3(100, 0, 0));
        List<StarSeed> sa = a.starsWithin(c, 8), sb = b.starsWithin(c, 8);
        assertEquals(sa, sb);
        for (int i = 0; i < Math.min(20, sa.size()); i++) {
            StarSystem x = a.system(sa.get(i)).orElseThrow(), y = b.system(sb.get(i)).orElseThrow();
            assertEquals(x.planets(), y.planets());
            assertEquals(x.binary(), y.binary());
            assertEquals(x.details(), y.details());
            assertEquals(x.nameEn(), y.nameEn());
        }
    }

    @Test
    void orbitsNeverCross() {
        int checked = 0;
        for (StarSystem sys : sample()) {
            List<AstroBody> ps = sys.planets().stream()
                    .sorted((p, q) -> Double.compare(p.orbit().semiMajorAxis(), q.orbit().semiMajorAxis())).toList();
            for (int i = 1; i < ps.size(); i++) {
                double apo = ps.get(i - 1).orbit().apoapsis(), peri = ps.get(i).orbit().periapsis();
                assertTrue(peri > apo * 1.1, sys.rootId() + ": " + ps.get(i).id() + " crosses " + ps.get(i - 1).id());
                checked++;
            }
            for (AstroBody p : ps) {
                assertTrue(p.mass() > 0 && p.radius() > 0 && p.orbit().eccentricity() >= 0 && p.orbit().eccentricity() < 1);
            }
        }
        assertTrue(checked > 200, "checked " + checked);
    }

    @Test
    void binariesAreCommonAndKeepPlanetsStable() {
        int binaries = 0, systems = 0;
        for (StarSystem sys : sample()) {
            systems++;
            if (sys.binary().isPresent()) {
                binaries++;
                StarSystem.Binary b = sys.binary().get();
                assertTrue(b.companion().mass() >= 0.08 && b.companion().mass() <= sys.seed().mass() + 1e-9);
                assertTrue(b.periodDays() > 0 && b.eccentricity() >= 0 && b.eccentricity() < 1);
                for (AstroBody p : sys.planets()) {
                    assertTrue(p.orbit().apoapsis() / AstroConstants.AU <= b.stableLimitAu() + 1e-9, "S-type stability");
                }
            }
        }
        assertEquals(0.33, binaries / (double) systems, 0.08, "mostly red dwarfs: ~1/3 multiple");
        assertEquals(0.27, StarSystemGenerator.binaryFraction(0.2));
        assertEquals(0.45, StarSystemGenerator.binaryFraction(1.0));
    }

    @Test
    void ringsSitInsideTheRocheLimitAndMoonsOutside() {
        int ringed = 0;
        for (StarSystem sys : sample()) {
            for (AstroBody p : sys.planets()) {
                StarSystem.Details d = sys.details(p.id());
                if (d.hasRings()) {
                    ringed++;
                    assertTrue(p.kind().isGiant(), "only giants get rings");
                    assertTrue(d.ringInnerKm() * 1000 > p.radius());
                    assertTrue(d.ringOuterKm() * 1000 <= StarSystemGenerator.rocheLimitIce(p) + 1);
                    for (AstroBody m : sys.bodies().children(p.id())) {
                        assertTrue(m.orbit().periapsis() > d.ringOuterKm() * 1000, "moon " + m.id() + " inside the rings");
                    }
                }
            }
        }
        assertTrue(ringed > 0, "some giants have rings");
    }

    @Test
    void sunLikeStarsHaveASensibleHabitableZone() {
        int sunLike = 0;
        for (StarSystem sys : sample()) {
            double m = sys.primary().mass();
            if (sys.kind() == StarKind.MAIN_SEQUENCE && m > 0.9 && m < 1.1) {
                sunLike++;
                double l = sys.primary().luminosity();
                double in = PlanetModel.habitableZoneInner(l), out = PlanetModel.habitableZoneOuter(l);
                assertTrue(in > 0.7 && in < 1.2 && out > 1.0 && out < 1.8, "HZ " + in + "–" + out);
                // an Earth twin in the middle of that zone gets an Earth-like equilibrium temperature
                double teq = PlanetModel.equilibriumTemperature(l, (in + out) / 2, 0.3);
                assertTrue(teq > 200 && teq < 280, "teq " + teq);
            }
        }
        assertTrue(sunLike > 0);
    }

    @Test
    void namesArePronounceableAndStable() {
        Set<String> names = new HashSet<>();
        for (long id = 1; id <= 2000; id++) {
            String n = StarNames.properName(id);
            assertEquals(n, StarNames.properName(id));
            assertTrue(n.matches("[A-Z][a-z]{2,15}"), n);
            assertFalse(n.toLowerCase().matches(".*[^aeiouy]{4}.*"), "too many consonants in a row: " + n);
            names.add(n);
        }
        assertTrue(names.size() > 1700, "names should rarely repeat: " + names.size());
        assertTrue(StarSystemGenerator.designation(123456789L).matches("SB [0-9A-Z]{4}-[0-9A-Z]{3,}"));
    }

    @Test
    void generatedTerrainHints() {
        int water = 0, ice = 0, total = 0;
        for (StarSystem sys : sample()) {
            for (AstroBody b : sys.bodies().all()) {
                if (b.isStar()) {
                    continue;
                }
                TerrainHints h = sys.terrainHints(b.id());
                assertEquals(h, sys.terrainHints(b.id()));
                assertEquals(b.kind().isGiant(), h.colorFamily() == TerrainHints.ColorFamily.GAS_BANDS);
                assertTrue(h.glaciation() >= 0 && h.glaciation() <= 1 && h.volcanism() >= 0 && h.volcanism() <= 1);
                total++;
                if (h.liquid() == TerrainHints.Liquid.WATER) {
                    water++;
                }
                if (h.glaciation() > 0.9) {
                    ice++;
                }
            }
        }
        assertTrue(total > 300 && ice > 0, total + " bodies, " + ice + " ice worlds, " + water + " water worlds");
    }
}
