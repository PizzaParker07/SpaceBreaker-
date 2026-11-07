package com.pizzaparker07.spacebreaker.testmod.compat;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.Compat;
import com.pizzaparker07.spacebreaker.compat.CompatConfig;
import com.pizzaparker07.spacebreaker.compat.CompatMixinPlugin;
import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import com.pizzaparker07.spacebreaker.compat.SpaceWorlds.Kind;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Renderer compatibility: world classification, weather isolation and optional-mod gating. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompatGameTests {
    private static ResourceKey<Level> dim(String path) {
        return ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(path));
    }

    private static void expect(GameTestHelper helper, RegistryAccess access, String path, Kind kind) {
        Kind got = SpaceWorlds.classify(access, dim(path));
        helper.assertTrue(got == kind, path + " should be " + kind + ", got " + got);
    }

    @GameTest(template = "empty")
    public static void spaceWorldsAreClassified(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        helper.assertTrue(SpaceWorlds.classify(access, Level.OVERWORLD) == Kind.VANILLA, "the Overworld (Earth) keeps the pack's sky");
        helper.assertTrue(SpaceWorlds.classify(access, Level.NETHER) == Kind.VANILLA, "the Nether is vanilla");
        helper.assertTrue(SpaceWorlds.classify(access, Level.END) == Kind.VANILLA, "the End is vanilla");
        expect(helper, access, "moon", Kind.AIRLESS);
        expect(helper, access, "phobos", Kind.AIRLESS);
        expect(helper, access, "deimos", Kind.AIRLESS);
        expect(helper, access, "mercury", Kind.AIRLESS);
        expect(helper, access, "pluto", Kind.AIRLESS);
        expect(helper, access, "earth_orbit", Kind.ORBIT);
        expect(helper, access, "moon_orbit", Kind.ORBIT);
        expect(helper, access, "orbit", Kind.ORBIT);
        expect(helper, access, "mars", Kind.THIN_ATMOSPHERE);
        expect(helper, access, "venus", Kind.THICK_ATMOSPHERE);
        expect(helper, access, "titan", Kind.THICK_ATMOSPHERE);
        expect(helper, access, "no_such_world", Kind.VANILLA);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everySpaceDimensionHasASky(GameTestHelper helper) {
        // every dimension of ours that exists on the server must be classified, or shader packs show it as Overworld
        for (ServerLevel level : helper.getLevel().getServer().getAllLevels()) {
            if (level.dimension().location().getNamespace().equals(Spacebreaker.MOD_ID)) {
                Kind kind = SpaceWorlds.classify(level);
                helper.assertTrue(kind.ownSky(), level.dimension().location() + " has no celestial body: add it to its body JSON");
                helper.assertFalse(kind.vanillaWeather(), level.dimension().location() + " must not show Overworld rain");
            }
        }
        // and every body with a surface dimension maps back to itself
        Bodies.registry(helper.getLevel().registryAccess()).ifPresent(reg -> reg.entrySet().forEach(e -> {
            CelestialBody body = e.getValue();
            body.dimension().filter(d -> !d.equals(Level.OVERWORLD)).ifPresent(d ->
                    helper.assertTrue(SpaceWorlds.classify(body, d).ownSky(), e.getKey().location() + " surface is not classified"));
            body.orbitDimension().ifPresent(d ->
                    helper.assertTrue(SpaceWorlds.classify(body, d) == Kind.ORBIT, e.getKey().location() + " orbit is not ORBIT"));
        }));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kindsDescribeTheirWorlds(GameTestHelper helper) {
        helper.assertTrue(Kind.ORBIT.airless() && Kind.AIRLESS.airless(), "orbit and airless worlds have no air");
        helper.assertFalse(Kind.THIN_ATMOSPHERE.airless(), "Mars has air");
        helper.assertFalse(Kind.VANILLA.ownSky(), "vanilla worlds keep the pack's sky");
        helper.assertTrue(Kind.VANILLA.vanillaWeather(), "vanilla worlds keep weather");
        for (Kind k : Kind.values()) {
            if (k != Kind.VANILLA) {
                helper.assertTrue(k.ownSky() && !k.vanillaWeather(), k + " must draw its own sky without Overworld weather");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverWeatherIsUntouched(GameTestHelper helper) {
        // the weather fix is client-only: server-side rain in space worlds keeps vanilla behaviour
        ServerLevel moon = helper.getLevel().getServer().getLevel(dim("moon"));
        if (moon == null) {
            helper.succeed();
            return;
        }
        float before = moon.getRainLevel(1F);
        moon.setRainLevel(0.7F);
        helper.assertTrue(Math.abs(moon.getRainLevel(1F) - 0.7F) < 1e-4, "server rain level must not be overridden");
        moon.setRainLevel(before);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shaderMixinsNeedIris(GameTestHelper helper) {
        CompatMixinPlugin plugin = new CompatMixinPlugin();
        String target = "net.minecraft.client.renderer.DimensionSpecialEffects";
        boolean iris = Compat.shaderModInstalled();
        helper.assertTrue(plugin.shouldApplyMixin(target, "com.pizzaparker07.spacebreaker.compat.mixin.iris.PackCloudsMixin") == iris,
                "shader-pack mixins must only apply when Iris is installed");
        helper.assertTrue(plugin.shouldApplyMixin("net.minecraft.world.level.Level", "com.pizzaparker07.spacebreaker.compat.mixin.ClientWeatherMixin"),
                "the weather mixin always applies");
        helper.assertTrue(CompatConfig.skyMode() == CompatConfig.SkyMode.AUTO, "default sky mode is AUTO");
        helper.assertTrue(CompatConfig.hidePackClouds(), "pack clouds are hidden by default");
        helper.succeed();
    }
}
