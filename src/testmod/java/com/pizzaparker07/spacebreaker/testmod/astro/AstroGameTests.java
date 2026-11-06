package com.pizzaparker07.spacebreaker.testmod.astro;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.astro.AstroBody;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.TransferPlanner;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

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

    private AstroGameTests() {
    }
}
