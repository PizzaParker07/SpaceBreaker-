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

    /**
     * Planets are cubes by default now: a settings file from before (version 0) that still says VOXEL, the old default,
     * moves to CUBE once; any other saved style stays, and once the file is at the current version a VOXEL picked on
     * purpose stays VOXEL.
     */
    @GameTest(template = "empty")
    public static void planetStyleMovesToCubeOnce(GameTestHelper helper) {
        var cube = com.pizzaparker07.spacebreaker.client.sky.SkyConfig.PlanetStyle.CUBE;
        var voxel = com.pizzaparker07.spacebreaker.client.sky.SkyConfig.PlanetStyle.VOXEL;
        var round = com.pizzaparker07.spacebreaker.client.sky.SkyConfig.PlanetStyle.ROUND;
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.sky.SkyConfig.migratedStyle(voxel, 0) == cube, "old VOXEL becomes CUBE");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.sky.SkyConfig.migratedStyle(round, 0) == round, "old ROUND stays");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.sky.SkyConfig.migratedStyle(voxel, 1) == voxel, "VOXEL chosen after the switch stays");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.sky.SkyConfig.migratedStyle(cube, 0) == cube, "CUBE stays");
        helper.assertTrue(com.pizzaparker07.spacebreaker.client.sky.SkyConfig.PLANET_STYLE.getDefault() == cube, "CUBE is the default");
        helper.succeed();
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
     * Comet brightness follows the real apparent magnitude: Hale-Bopp at its 1997 perihelion (about -0.7) is at full
     * brightness, Halley in 1986 (a few magnitudes, near the naked-eye limit) in between, and 67P (about 11, a
     * telescope comet) only a faint smudge.
     */
    @GameTest(template = "empty")
    public static void cometBrightnessFollowsMagnitude(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        var hb = sighting(helper, sys, com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.HALE_BOPP);
        var halley = sighting(helper, sys, com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.HALLEY);
        var cg = sighting(helper, sys, com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.CHURYUMOV_GERASIMENKO);
        String all = "Hale-Bopp " + hb.magnitude() + ", Halley " + halley.magnitude() + ", 67P " + cg.magnitude();
        helper.assertTrue(hb.magnitude() < 1 && hb.alpha() > 0.99F, "Hale-Bopp is a great naked-eye comet: " + all);
        helper.assertTrue(halley.magnitude() > 1 && halley.magnitude() < 7 && halley.alpha() > 0.15F && halley.alpha() < 0.99F,
                "Halley 1986 is modest: " + all);
        helper.assertTrue(cg.magnitude() > 8 && cg.alpha() < 0.2F, "67P needs a telescope: " + all);
        helper.assertTrue(hb.alpha() > halley.alpha() && halley.alpha() > cg.alpha(), "brighter comets are drawn brighter: " + all);
        helper.succeed();
    }

    private static com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.Sighting sighting(GameTestHelper helper,
            com.pizzaparker07.spacebreaker.core.astro.BodySystem sys, com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet c) {
        double t = c.perihelionDays() * 86400.0;
        return com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.compute(sys, "spacebreaker:earth", t).stream()
                .filter(s -> s.id().equals(c.id())).findFirst()
                .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException(c.id() + " is in the sky at perihelion"));
    }

    /**
     * The Earth–Moon travel scene: Earth sits at the scene origin at its drawn radius, the Moon about 2,200 blocks out and
     * never smaller than the minimum radius, Earth's pole leans about 23° from the scene's up (ecliptic north), and the
     * ecliptic-to-scene axes round-trip.
     */
    @GameTest(template = "empty")
    public static void spaceSceneLayout(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var sys = BodyAdapter.system(access).orElseThrow();
        var reg = com.pizzaparker07.spacebreaker.core.body.Bodies.registry(access).orElseThrow();
        double t = 7.0e8;
        var earthPos = sys.position(sys.require("spacebreaker:earth"), t);
        var bodies = com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout.layout(sys,
                com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale.EARTH_MOON, "spacebreaker:earth",
                List.of("spacebreaker:earth", "spacebreaker:moon"), t,
                id -> BodyAdapter.look(sys, net.minecraft.resources.ResourceLocation.parse(id), reg.get(net.minecraft.resources.ResourceLocation.parse(id)), earthPos, t));
        var earth = bodies.get(0);
        var moon = bodies.get(1);
        helper.assertTrue(earth.center().length() < 1e-6 && earth.radius() == 128, "Earth at the origin, 128 blocks: " + earth);
        double d = moon.center().length();
        helper.assertTrue(d > 2000 && d < 2400, "the Moon about 2,200 blocks out: " + d);
        helper.assertTrue(moon.radius() >= com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout.MOON_MIN_RADIUS, "the Moon reads as a landable target: " + moon.radius());
        helper.assertTrue(Math.abs(moon.center().y) < d * 0.1, "the Moon stays near the ecliptic plane (scene horizontal): " + moon.center().y);
        double tilt = Math.toDegrees(Math.acos(earth.pole().y));
        helper.assertTrue(Math.abs(tilt - 23.4) < 1.0, "Earth's pole leans 23.4 degrees from ecliptic north: " + tilt);
        var v = new com.pizzaparker07.spacebreaker.core.astro.Vec3(1, 2, 3);
        var back = com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout.fromScene(
                com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout.toScene(v));
        helper.assertTrue(back.sub(v).length() < 1e-9, "scene axes round-trip");
        helper.succeed();
    }

    /**
     * Blocky planets: the voxel ball is closed (every face points outward, as many faces on each side), the cube has six
     * full pixel faces, the day/night line is a hard step with the night side at the night brightness, and ring tiles stay
     * between the ring's edges.
     */
    @GameTest(template = "empty")
    public static void blockyBodiesAreSoundShapes(GameTestHelper helper) {
        for (int n : new int[]{6, 10, 16, 128}) {
            var vox = com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.body(
                    com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.VOXEL, n);
            int[] perSide = new int[6];
            for (var f : vox) {
                helper.assertTrue(f.normal().dot(f.center()) > 0, "voxel faces point outward (n=" + n + ")");
                helper.assertTrue(f.center().length() < 1.25F, "the ball stays about one radius: " + f.center().length());
                helper.assertTrue(Math.abs(new org.joml.Vector3f(f.u()).cross(f.v()).normalize().dot(f.normal()) - 1) < 1e-4,
                        "u x v is the outward normal");
                int side = f.normal().x > 0 ? 0 : f.normal().x < 0 ? 1 : f.normal().y > 0 ? 2 : f.normal().y < 0 ? 3 : f.normal().z > 0 ? 4 : 5;
                perSide[side]++;
            }
            for (int k = 1; k < 6; k++) {
                helper.assertTrue(perSide[k] == perSide[0], "symmetric ball (n=" + n + "): " + java.util.Arrays.toString(perSide));
            }
            var cube = com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.body(
                    com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.CUBE, n);
            helper.assertTrue(cube.size() == 6 * n * n, "six full faces of " + n + "x" + n + ": " + cube.size());
        }
        org.joml.Vector3f sun = new org.joml.Vector3f(0, 0, 1);
        org.joml.Vector3f toward = new org.joml.Vector3f(0, 0, 1);
        org.joml.Vector3f away = new org.joml.Vector3f(0, 0, -1);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.light(toward, toward, sun, 0.07F) == 1F, "full day facing the sun");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.light(away, away, sun, 0.07F) == 0.07F, "night side at the night brightness");
        float edge = com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.light(new org.joml.Vector3f(1, 0, 0), new org.joml.Vector3f(1, 0, 0), sun, 0.07F);
        helper.assertTrue(edge > 0.07F && edge < 1F, "one step of twilight on the terminator: " + edge);
        for (var f : com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.ring(1.24F, 2.27F, 40)) {
            float r = (float) Math.hypot(f.center().x, f.center().z);
            helper.assertTrue(r >= 1.24F && r <= 2.27F && f.surface().x >= 0 && f.surface().x <= 1, "ring tile inside the ring: " + r);
        }
        helper.succeed();
    }

    /**
     * From Mars: Phobos and Deimos are not planet points (Mars' sky draws them as discs), and the Moon never hides in
     * Earth's point but shows beside it on its true side; Earth, when brighter than magnitude -1, shows earlier at dusk.
     */
    @GameTest(template = "empty")
    public static void marsSkyPointsAreClean(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        int pairs = 0;
        int boosted = 0;
        for (int day = 0; day <= 750; day += 15) {
            List<PlanetPoints.Dot> dots = PlanetPoints.compute(sys, "spacebreaker:mars", day * 86400.0);
            helper.assertTrue(find(dots, "spacebreaker:phobos") == null && find(dots, "spacebreaker:deimos") == null,
                    "no Phobos or Deimos points from Mars (day " + day + ")");
            PlanetPoints.Dot earth = find(dots, "spacebreaker:earth");
            PlanetPoints.Dot moon = find(dots, "spacebreaker:moon");
            if (earth != null && moon != null) {
                pairs++;
                double sep = Math.acos(Math.min(1, earth.local().dot(moon.local()))) * 100;
                helper.assertTrue(sep >= earth.size() + moon.size() + PlanetPoints.PAIR_GAP - 1e-3,
                        "the Moon clears Earth's point (day " + day + "): " + sep);
            }
            if (earth != null && earth.twilight() > 1F) {
                boosted++;
                helper.assertTrue(earth.shown(0.5F) > earth.alpha() * 0.5F, "boosted in twilight");
            }
        }
        helper.assertTrue(pairs > 0, "Earth and the Moon are seen together from Mars at some point");
        helper.assertTrue(boosted > 0, "near opposition Earth (brighter than -1) shows early at dusk");
        // the push keeps the true side: a moon just east (+x) of its planet stays east
        List<PlanetPoints.Dot> two = new java.util.ArrayList<>(List.of(
                new PlanetPoints.Dot("spacebreaker:earth", new org.joml.Vector3f(0, 1, 0), 0.6F, 1, 1, 1, 1, 1),
                new PlanetPoints.Dot("spacebreaker:moon", new org.joml.Vector3f(0.001F, 1, 0).normalize(), 0.25F, 1, 1, 1, 1, 1)));
        PlanetPoints.separatePairs(sys, two);
        org.joml.Vector3f m = two.get(1).local();
        helper.assertTrue(m.x > 0 && Math.abs(m.z) < 1e-4, "pushed along its true direction: " + m);
        helper.assertTrue(Math.acos(m.y) * 100 >= 0.6 + 0.25 + PlanetPoints.PAIR_GAP - 1e-3, "and just clear: " + Math.acos(m.y) * 100);
        helper.succeed();
    }

    /**
     * Lunar horizon glow: only in the first degrees of twilight (none by day or deep in the night), brightest a few
     * degrees after sunset, and only on about a third of sunsets and sunrises, the same on every client.
     */
    @GameTest(template = "empty")
    public static void horizonGlowIsRareAndBrief(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.strength(5) == 0, "none by day");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.strength(-13) == 0, "none deep in the night");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.strength(-5) > 0.99, "brightest 5 degrees after sunset");
        double mid = com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.strength(-8.5);
        helper.assertTrue(mid > 0.1 && mid < 0.9, "fading out: " + mid);
        int shown = 0;
        int n = 3000;
        for (int day = 0; day < n; day++) {
            if (com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.shows(42, day, true)) {
                shown++;
            }
        }
        helper.assertTrue(shown > n / 4 && shown < n * 2 / 5, "about one sunset in three: " + shown + "/" + n);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.shows(42, 17, true)
                == com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.shows(42, 17, true), "deterministic");
        // over whole days on a 24000-tick clock, the glow appears only in twilight
        var clock = new com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime.Clock(24000, 0);
        int lit = 0;
        for (long t = 0; t < 24000L * 30; t += 20) {
            double b = com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.at(clock, t, 0, 42);
            if (b > 0) {
                lit++;
                double el = com.pizzaparker07.spacebreaker.content.worldgen.SkyMath.sunElevation(clock.timeOfDay(t), 0);
                helper.assertTrue(el < 0 && el > -12, "glow only in twilight, sun at " + el);
            }
        }
        helper.assertTrue(lit > 0 && lit < 24000L * 30 / 20 / 20, "rare and brief: " + lit);
        helper.succeed();
    }

    /**
     * Earthshine: under a full Earth overhead the lunar night is clearly lit, under a quarter Earth much less, under a
     * crescent Earth low in the sky almost not at all, and on the far side, where Earth never rises, not at all.
     */
    @GameTest(template = "empty")
    public static void earthshineFollowsThePhase(GameTestHelper helper) {
        double sub = com.pizzaparker07.spacebreaker.content.worldgen.SkyMath.SUB_PARENT_LONGITUDE;
        java.util.function.DoubleBinaryOperator at = (lon, tod) ->
                com.pizzaparker07.spacebreaker.content.worldgen.Earthshine.brightness(0, lon, tod, 0);
        double full = at.applyAsDouble(sub, 0.5); // local midnight: the Sun behind us, Earth full overhead
        double quarter = at.applyAsDouble(sub, 0.25);
        double max = com.pizzaparker07.spacebreaker.content.worldgen.Earthshine.MAX;
        helper.assertTrue(full > 0.9 * max, "full Earth overhead: " + full);
        helper.assertTrue(quarter > 0.2 * full && quarter < 0.45 * full, "quarter Earth gives a third or so: " + quarter / full);
        helper.assertTrue(at.applyAsDouble(sub, 0) < 0.001 * max, "new Earth at noon gives nothing");
        // near the limb, Earth low in the sky: the darkest night is under a thin crescent
        double darkest = Double.MAX_VALUE;
        for (double tod = 0; tod < 1; tod += 0.005) {
            if (com.pizzaparker07.spacebreaker.content.worldgen.SkyMath.sunDirection(tod, 0).y < -0.05) {
                darkest = Math.min(darkest, at.applyAsDouble(sub + 75, tod));
            }
        }
        helper.assertTrue(darkest < 0.05 * full, "crescent Earth: almost dark " + darkest / full);
        for (double tod = 0; tod < 1; tod += 0.05) {
            helper.assertTrue(at.applyAsDouble(sub + 180, tod) == 0, "no earthshine on the far side");
            helper.assertTrue(at.applyAsDouble(sub + 120, tod) == 0, "none where Earth is below the horizon");
        }
        helper.succeed();
    }

    /**
     * Eclipses from the Moon: the total lunar eclipse of 2000-01-21 (greatest at 04:44 UTC) is, seen from the Moon, a
     * total eclipse of the Sun by Earth, and so is the one of 2022-11-08; each is found by the search and its middle lies within a few hours of the real one.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void earthEclipsesTheSunFromTheMoon(GameTestHelper helper) {
        var sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        // 2000-01-21 04:44 UTC and 2022-11-08 10:59 UTC, in seconds after J2000 (TT); the second needs the
        // Moon's nodal precession to line up 23 years later
        assertEclipse(helper, sys, 1701776.0, "2000-01-21");
        assertEclipse(helper, sys, 721177220.0, "2022-11-08");
        helper.succeed();
    }

    private static void assertEclipse(GameTestHelper helper, com.pizzaparker07.spacebreaker.core.astro.BodySystem sys, double greatest, String label) {
        double best = 0;
        double bestT = 0;
        for (double dt = -12 * 3600; dt <= 12 * 3600; dt += 300) {
            double c = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.coverage(sys, "spacebreaker:moon", "spacebreaker:earth", greatest + dt);
            if (c > best) {
                best = c;
                bestT = dt;
            }
        }
        helper.assertTrue(best > 0.99, label + ": Earth fully hides the Sun from the Moon: " + best);
        helper.assertTrue(Math.abs(bestT) < 4 * 3600, label + ": within a few hours of the real eclipse: " + bestT / 3600 + " h");
        double start = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.next(sys, "spacebreaker:moon", "spacebreaker:earth",
                greatest - 5 * 86400, 10 * 86400, 1800);
        helper.assertTrue(!Double.isNaN(start) && Math.abs(start - greatest) < 6 * 3600, label + ": the search finds it: " + (start - greatest) / 3600 + " h");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.coverage(sys, "spacebreaker:moon", "spacebreaker:earth",
                greatest + 7 * 86400) == 0, label + ": a week later the Sun is clear");
    }

    /**
     * The eclipse presentation window: a short eclipse (minutes at the default time scale) is shown for a minute of real
     * time around its middle, fading in and out over ten seconds; a long one keeps its real length.
     */
    @GameTest(template = "empty")
    public static void eclipsesShowForAtLeastAMinute(GameTestHelper helper) {
        double spr = 3600; // default: one simulated hour per real second
        var e = new com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.Event(0, 3 * 3600, 1.0);
        double mid = e.middle();
        java.util.function.DoubleUnaryOperator at = real -> com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.presentation(e, mid + real * spr, spr);
        helper.assertTrue(Math.abs(at.applyAsDouble(0) - 1) < 1e-9, "full at the middle");
        helper.assertTrue(Math.abs(at.applyAsDouble(19) - 1) < 1e-9 && Math.abs(at.applyAsDouble(-19) - 1) < 1e-9, "full for 40 s");
        double fade = at.applyAsDouble(25);
        helper.assertTrue(fade > 0.1 && fade < 0.9, "fading 25 s from the middle: " + fade);
        helper.assertTrue(at.applyAsDouble(31) == 0 && at.applyAsDouble(-31) == 0, "over after a minute");
        // slow time scale: a 3-hour eclipse takes 3 hours of real time and is shown all along
        double slow = 1;
        double mid2 = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.presentation(e, e.middle(), slow);
        double edge = com.pizzaparker07.spacebreaker.content.worldgen.Eclipses.presentation(e, e.start() + 60, slow);
        helper.assertTrue(mid2 == 1 && edge > 0, "a long eclipse keeps its real length");
        helper.succeed();
    }

    private SkyGameTests() {
    }
}
