package com.pizzaparker07.spacebreaker.testmod.sky;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetPoints;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Planet points in the sky (pure maths of the client sky, checked on the server). */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SkyGameTests {
    private static PlanetPoints.Dot find(List<PlanetPoints.Dot> dots, String id) {
        return dots.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
    }

    @GameTest(template = "empty")
    public static void planetsInEarthSky(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        for (double t : new double[]{0, 3.0e7, 9.0e7}) {
            List<PlanetPoints.Dot> dots = PlanetPoints.compute(sys, "spacebreaker:earth", t);
            PlanetPoints.Dot venus = find(dots, "spacebreaker:venus");
            PlanetPoints.Dot jupiter = find(dots, "spacebreaker:jupiter");
            helper.assertTrue(venus != null, "Venus is always bright enough to see from Earth (t=" + t + ")");
            helper.assertTrue(jupiter != null, "Jupiter is bright enough to see from Earth (t=" + t + ")");
            double elongation = Math.toDegrees(Math.acos(venus.local().y));
            helper.assertTrue(elongation < 48, "Venus never strays more than ~47 deg from the Sun, got " + elongation);
            helper.assertTrue(Math.abs(venus.local().x) < 0.15, "planets stay near the ecliptic (Venus " + venus.local().x + ")");
            helper.assertTrue(find(dots, "spacebreaker:moon") == null, "the Moon is a disc, not a point");
            helper.assertTrue(find(dots, "spacebreaker:sun") == null, "the Sun is not a point");
            // Venus (about -4) always outshines Jupiter (about -2): a bigger, brighter point
            helper.assertTrue(venus.size() >= jupiter.size() && venus.alpha() >= jupiter.alpha(),
                    "Venus outshines Jupiter (t=" + t + "): " + venus.size() + " vs " + jupiter.size());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueEarthInMarsSky(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        List<PlanetPoints.Dot> dots = PlanetPoints.compute(sys, "spacebreaker:mars", 0);
        PlanetPoints.Dot earth = find(dots, "spacebreaker:earth");
        helper.assertTrue(earth != null, "Earth is a bright point in the Martian sky");
        helper.assertTrue(earth.b() >= earth.r(), "the pale blue dot is blue-white");
        helper.assertTrue(Math.toDegrees(Math.acos(earth.local().y)) < 48, "Earth stays near the Sun as seen from Mars");
        helper.succeed();
    }

    /**
     * Comets in the sky: an active comet near perihelion is seen from Earth with its tail pointing away from the sun,
     * Halley at its 2061 perihelion is a great comet, and comets far out at aphelion stay dormant and hidden.
     */
    @GameTest(template = "empty")
    public static void cometsShowNearPerihelion(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        var halley = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.HALLEY;
        double perihelion = halley.nextPerihelion(0);
        var seen = com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.compute(sys, "spacebreaker:earth", perihelion).stream()
                .filter(c -> c.id().equals(halley.id())).findFirst().orElse(null);
        helper.assertTrue(seen != null, "Halley is in Earth's sky at its 2061 perihelion");
        helper.assertTrue(seen.great(), "and it is a great comet: " + seen.level());
        helper.assertTrue(seen.tip().y < seen.head().y, "the tail points away from the sun (head " + seen.head() + ", tip " + seen.tip() + ")");
        helper.assertTrue(seen.headSize() > 0 && seen.alpha() > 0.5F, "a great comet is bright");
        double aphelion = perihelion + halley.periodSeconds() / 2;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.compute(sys, "spacebreaker:earth", aphelion).stream()
                .noneMatch(c -> c.id().equals(halley.id())), "Halley is dormant and hidden near aphelion");
        helper.succeed();
    }

    /** On 67P the coma glows in the sky near perihelion and not at all far out, where the comet sleeps. */
    @GameTest(template = "empty")
    public static void comaGlowsOnAnActiveComet(GameTestHelper helper) {
        var c = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.CHURYUMOV_GERASIMENKO;
        double peri = c.nextPerihelion(0);
        float near = com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.comaGlow(
                com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(c, peri));
        float far = com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.comaGlow(
                com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(c, peri + c.periodSeconds() / 2));
        helper.assertTrue(near > 0.05F && near <= 0.36F, "a faint coma glow near perihelion: " + near);
        helper.assertTrue(far == 0F, "no glow at aphelion: " + far);
        helper.assertTrue(com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId("churyumov_gerasimenko").isPresent(),
                "the 67P world's body id finds its comet");
        helper.succeed();
    }

    /**
     * Eclipses from the Moon: the total lunar eclipse of 2000-01-21 (greatest at 04:44 UTC) is, seen from the Moon, a
     * total eclipse of the Sun by Earth; it is found by the search and its middle lies within a few hours of the real one.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void earthEclipsesTheSunFromTheMoon(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        double greatest = 1701776.0; // 2000-01-21 04:44 UTC in seconds after J2000 (TT)
        double best = 0;
        double bestT = 0;
        for (double dt = -12 * 3600; dt <= 12 * 3600; dt += 300) {
            double c = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.coverage(sys, "spacebreaker:moon", "spacebreaker:earth", greatest + dt);
            if (c > best) {
                best = c;
                bestT = dt;
            }
        }
        helper.assertTrue(best > 0.99, "Earth fully hides the Sun from the Moon: " + best);
        helper.assertTrue(Math.abs(bestT) < 4 * 3600, "within a few hours of the real eclipse: " + bestT / 3600 + " h");
        double start = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.next(sys, "spacebreaker:moon", "spacebreaker:earth",
                greatest - 5 * 86400, 10 * 86400, 1800);
        helper.assertTrue(!Double.isNaN(start) && Math.abs(start - greatest) < 6 * 3600, "the search finds it: " + (start - greatest) / 3600 + " h");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.coverage(sys, "spacebreaker:moon", "spacebreaker:earth",
                greatest + 7 * 86400) == 0, "a week later the Sun is clear");
        helper.succeed();
    }

    private SkyGameTests() {
    }
}
