package com.pizzaparker07.spacebreaker.testmod.astro;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.Ephemeris;
import com.pizzaparker07.spacebreaker.core.astro.GameClock;
import com.pizzaparker07.spacebreaker.core.astro.OrbitService;
import com.pizzaparker07.spacebreaker.core.astro.SkyEphemeris;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.Asteroid;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.AsteroidGenerator;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.Region;
import com.pizzaparker07.spacebreaker.core.astro.TransferPlanner;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.Galaxy;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSeed;
import com.pizzaparker07.spacebreaker.core.astro.galaxy.StarSystem;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AstroGameTests {
    private static final List<String> BODIES = List.of("sun", "mercury", "venus", "earth", "moon", "mars", "phobos",
            "deimos", "ceres", "vesta", "jupiter", "io", "europa", "ganymede", "callisto", "saturn", "titan",
            "enceladus", "uranus", "miranda", "neptune", "triton", "pluto", "charon");

    @GameTest(template = "empty")
    public static void everyCelestialBodyLoads(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<CelestialBody> reg = Bodies.registry(access).orElseThrow(() -> new AssertionError("celestial_body registry missing"));
        for (String name : BODIES) {
            CelestialBody b = reg.getOptional(Spacebreaker.id(name)).orElseThrow(() -> new AssertionError(name + ".json did not load"));
            helper.assertTrue(b.physical().massKg() > 0 && b.physical().radiusKm() > 0, name + " has no size");
            b.parent().ifPresent(p -> helper.assertTrue(reg.containsKey(p), name + " has unknown parent " + p));
            helper.assertTrue(b.type() == BodyType.STAR || b.orbit().periodDays() > 0, name + " has no orbit");
            boolean giant = b.type() == BodyType.GAS_GIANT || b.type() == BodyType.ICE_GIANT;
            helper.assertTrue(giant != b.landable() || b.type() == BodyType.STAR, name + " landable flag wrong");
            if (b.landable() && !name.equals("earth")) {
                ResourceKey<net.minecraft.world.level.Level> dim = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(name));
                helper.assertTrue(b.dimension().orElseThrow().equals(dim), name + " should use dimension spacebreaker:" + name);
                helper.assertTrue(Bodies.forDimension(access, dim).isPresent(), name + " not found by dimension");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalBodyFieldsLoad(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        CelestialBody earth = Bodies.get(access, Bodies.EARTH).orElseThrow();
        CelestialBody saturn = Bodies.get(access, Bodies.key("saturn")).orElseThrow();
        CelestialBody mars = Bodies.get(access, Bodies.key("mars")).orElseThrow();
        CelestialBody sun = Bodies.get(access, Bodies.SUN).orElseThrow();
        helper.assertTrue(Math.abs(earth.albedo() - 0.306) < 1e-9, "Earth albedo");
        helper.assertTrue(sun.albedo() == CelestialBody.DEFAULT_ALBEDO, "albedo defaults when missing");
        helper.assertTrue(saturn.rings().isPresent() && saturn.rings().get().outerRadiusKm() > 130000, "Saturn rings");
        helper.assertTrue(mars.rings().isEmpty(), "Mars has no rings");
        helper.assertTrue(Math.abs(mars.orbit().longitudeAscendingNodeDeg() - 49.56) < 0.01, "Mars node");
        helper.assertTrue(Math.abs(mars.physical().surfaceGravity() - 3.71) < 0.01, "Mars gravity");
        helper.assertTrue(mars.atmosphere().composition().get("CO2") > 0.9, "Mars air is CO2");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void astroLibraryRunsOnRegistryData(GameTestHelper helper) {
        BodySystem sys = BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        AstroBody earth = sys.require("spacebreaker:earth");
        helper.assertTrue(Math.abs(earth.escapeVelocity() - 11186) < 20, "Earth escape velocity " + earth.escapeVelocity());
        TransferPlanner.Plan p = TransferPlanner.plan(sys, "spacebreaker:earth", "spacebreaker:mars", false);
        helper.assertTrue(Math.abs(p.deltaV("departure") - 3600) < 100, "trans-Mars injection " + p.deltaV("departure"));
        helper.assertTrue(Math.abs(p.transferTime() / 86400 - 259) < 2, "Hohmann time " + p.transferTime() / 86400);
        helper.assertTrue(sys.distanceToStar(sys.require("spacebreaker:titan"), 0) > 9e11, "Titan is out at Saturn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void generatedSystemsBecomeCelestialBodies(GameTestHelper helper) {
        Galaxy galaxy = new Galaxy(helper.getLevel().getSeed());
        StarSystem trappist = galaxy.system(galaxy.fixedStar("trappist_1").orElseThrow()).orElseThrow();
        Map<ResourceLocation, CelestialBody> bodies = BodyAdapter.toCelestialBodies(trappist, id -> Optional.empty());
        helper.assertTrue(bodies.size() == 8, "TRAPPIST-1 + 7 planets, got " + bodies.size());
        CelestialBody e = bodies.get(ResourceLocation.parse("spacebreaker:trappist_1_e"));
        helper.assertTrue(Math.abs(e.orbit().periodDays() - 6.10) < 0.15, "TRAPPIST-1 e period " + e.orbit().periodDays());
        helper.assertTrue(e.parent().orElseThrow().equals(ResourceLocation.parse("spacebreaker:trappist_1")), "parent link");
        // every generated record survives a round trip through the data-pack codec
        for (StarSeed s : galaxy.starsWithin(galaxy.sun().position().add(new Vec3(40, 0, 0)), 6)) {
            StarSystem sys = galaxy.system(s).orElseThrow();
            for (Map.Entry<ResourceLocation, CelestialBody> en : BodyAdapter.toCelestialBodies(sys, id -> Optional.empty()).entrySet()) {
                JsonElement json = CelestialBody.CODEC.encodeStart(JsonOps.INSTANCE, en.getValue()).getOrThrow();
                CelestialBody back = CelestialBody.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                helper.assertTrue(back.equals(en.getValue()), "codec round trip changed " + en.getKey());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void orbitServiceGivesPositionsAndWindows(GameTestHelper helper) {
        OrbitService orbits = OrbitService.get(helper.getLevel().getServer());
        helper.assertTrue(orbits == OrbitService.get(helper.getLevel().getServer()), "one service per server");
        double au = orbits.ephemeris().starDistanceAu("spacebreaker:mars");
        helper.assertTrue(au > 1.38 && au < 1.67, "Mars between perihelion and aphelion: " + au);
        Ephemeris.Window w = orbits.window("spacebreaker:earth", "spacebreaker:mars");
        long ticks = orbits.ticksUntil(w);
        helper.assertTrue(w.open() == (ticks == 0), "open windows have no wait");
        helper.assertTrue(ticks <= GameClock.ticks(800 * 86400.0, BodyAdapter.timeScale()), "next Mars window within a synodic period");
        helper.assertTrue(orbits.window("spacebreaker:earth", "spacebreaker:moon").open(), "the Moon is always reachable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skyFromTheOverworld(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        CelestialBody jupiter = Bodies.get(access, Bodies.key("jupiter")).orElseThrow();
        helper.assertTrue(Math.abs(jupiter.geometricAlbedo() - 0.538) < 1e-9, "geometric albedo loads");
        helper.assertTrue(Bodies.get(access, Bodies.SUN).orElseThrow().geometricAlbedo() < 0, "unknown by default");
        BodySystem sys = BodyAdapter.system(access).orElseThrow();
        List<SkyEphemeris.SkyObject> sky = SkyEphemeris.visible(sys, "spacebreaker:earth", BodyAdapter.simTime(helper.getLevel()), 6);
        helper.assertTrue(sky.get(0).id().equals("spacebreaker:sun") && sky.get(0).apparentMagnitude() < -26, "the Sun first");
        helper.assertTrue(sky.stream().anyMatch(o -> o.id().equals("spacebreaker:moon")), "the Moon is in the sky");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void asteroidsBecomeCelestialBodies(GameTestHelper helper) {
        for (Asteroid a : AsteroidGenerator.population(helper.getLevel().getSeed(), Region.MAIN_BELT, 50)) {
            CelestialBody c = BodyAdapter.toCelestialBody(a.toAstroBody(), a.details(), Optional.empty());
            helper.assertTrue(c.type() == BodyType.ASTEROID, "asteroid type");
            helper.assertTrue(Math.abs(c.physical().surfaceGravity() - a.surfaceGravity()) < 1e-9, "gravity carried over");
            JsonElement json = CelestialBody.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
            CelestialBody back = CelestialBody.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            helper.assertTrue(back.equals(c), "codec round trip changed " + a.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyNearbySystemConvertsWithoutRegistering(GameTestHelper helper) {
        Galaxy galaxy = new Galaxy(helper.getLevel().getSeed());
        int before = Bodies.registry(helper.getLevel().registryAccess()).orElseThrow().size();
        int converted = 0;
        for (StarSeed seed : galaxy.fixedStars()) {
            Optional<StarSystem> sys = galaxy.system(seed);
            if (sys.isEmpty()) {
                continue; // the Sun: data registry
            }
            for (Map.Entry<ResourceLocation, CelestialBody> en : BodyAdapter.toCelestialBodies(sys.get(), id -> Optional.empty()).entrySet()) {
                CelestialBody c = en.getValue();
                AstroBody a = sys.get().bodies().require(en.getKey().toString());
                helper.assertTrue(Math.abs(c.physical().radiusKm() * 1000 - a.radius()) < 1, "radius kept for " + en.getKey());
                helper.assertTrue(Math.abs(c.physical().massKg() - a.mass()) <= a.mass() * 1e-9, "mass kept for " + en.getKey());
                helper.assertTrue(c.climate().dayTempC() >= c.climate().nightTempC(), "climate order for " + en.getKey());
                helper.assertTrue(c.dimension().isEmpty(), "no dimension until the planet engine opens one");
                JsonElement json = CelestialBody.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
                helper.assertTrue(CelestialBody.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(c), "codec round trip " + en.getKey());
                converted++;
            }
        }
        helper.assertTrue(converted > 100, "converted " + converted + " bodies");
        helper.assertTrue(Bodies.registry(helper.getLevel().registryAccess()).orElseThrow().size() == before, "nothing was registered");
        helper.succeed();
    }

    private AstroGameTests() {
    }
}
