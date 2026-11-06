package com.pizzaparker07.spacebreaker.testmod.core;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    @GameTest(template = "empty")
    public static void celestialBodiesLoadWithRealValues(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        CelestialBody moon = Bodies.get(access, Bodies.MOON).orElseThrow(() -> new AssertionError("moon.json missing"));
        CelestialBody earth = Bodies.get(access, Bodies.EARTH).orElseThrow(() -> new AssertionError("earth.json missing"));
        helper.assertTrue(Math.abs(moon.physical().surfaceGravity() - 1.62) < 0.01, "Moon gravity should be 1.62 m/s²");
        helper.assertTrue(Math.abs(moon.relativeGravity() - 0.165) < 0.005, "Moon relative gravity ~0.165");
        helper.assertTrue(moon.atmosphere().isVacuum(), "Moon has no atmosphere");
        helper.assertTrue(earth.atmosphere().breathable(), "Earth air is breathable");
        double vEsc = earth.physical().escapeVelocity();
        helper.assertTrue(Math.abs(vEsc - 11186) < 50, "Earth escape velocity ~11.19 km/s, got " + vEsc);
        helper.assertTrue(Bodies.forDimension(access, Level.OVERWORLD).isPresent(), "overworld maps to Earth");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overworldEnvironmentIsEarthLike(GameTestHelper helper) {
        Level level = helper.getLevel();
        helper.assertTrue(Environment.canBreathe(level, helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1))), "overworld must be breathable");
        helper.assertTrue(Math.abs(Environment.gravityFactor(level) - 1.0) < 1e-6, "overworld gravity factor must be 1");
        helper.assertFalse(Environment.isVacuum(level), "overworld is not vacuum");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realismScalesLowGravity(GameTestHelper helper) {
        double moon = 0.165;
        helper.assertTrue(SbConfig.Realism.REALISTIC.scaleGravity(moon) == moon, "realistic keeps real gravity");
        double balanced = SbConfig.Realism.BALANCED.scaleGravity(moon);
        helper.assertTrue(balanced > 0.24 && balanced < 0.28, "balanced Moon gravity ~0.26, got " + balanced);
        helper.assertTrue(SbConfig.Realism.EASY.scaleGravity(1.0) == 1.0, "Earth stays 1.0 in every preset");
        helper.succeed();
    }

    private CoreGameTests() {
    }
}
