package com.pizzaparker07.spacebreaker.testmod.mars;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InnerOrbitGameTests {
    private static final String[] BODIES = {"venus", "mercury", "mars"};
    private static final String[] GUIDE = {"mars", "mars_resources", "mars_life", "mars_moons", "venus", "venus_clouds", "mercury", "inner_orbits"};

    @GameTest(template = WorldTests.EMPTY)
    public static void innerPlanetOrbitsAreSpace(GameTestHelper helper) {
        for (String b : BODIES) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(b + "_orbit"));
            var stem = WorldTests.stem(helper, b + "_orbit");
            helper.assertTrue(stem.type().value().effectsLocation().equals(Spacebreaker.id(b + "_orbit")), b + " orbit sky effects");
            CelestialBody body = Bodies.forDimension(helper.getLevel().registryAccess(), key)
                    .orElseThrow(() -> new AssertionError(b + " orbit has no body"));
            helper.assertTrue(body.dimension().map(d -> d.location().getPath().equals(b)).orElse(false), b + " orbit belongs to " + b);
            // the environment treats a body's orbit dimension as vacuum (Bodies.isOrbit)
            helper.assertTrue(body.orbitDimension().filter(key::equals).isPresent(), b + " orbit is the body's orbit dimension");
        }
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void innerPlanetGuidePagesPointAtRealThings(GameTestHelper helper) {
        var techs = helper.getLevel().registryAccess().registryOrThrow(Tech.REGISTRY);
        for (String lang : new String[]{"en_us", "tr_tr"}) {
            for (String name : GUIDE) {
                String path = "/assets/spacebreaker/guide/" + lang + "/" + name + ".json";
                try (InputStream in = Spacebreaker.class.getResourceAsStream(path)) {
                    helper.assertTrue(in != null, "missing guide page " + path);
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    helper.assertTrue("inner_planets".equals(o.get("chapter").getAsString()), name + " chapter");
                    ResourceLocation tech = ResourceLocation.parse(o.get("requires_tech").getAsString());
                    helper.assertTrue(techs.containsKey(tech), name + " requires unknown tech " + tech);
                    helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(o.get("icon").getAsString())), name + " icon");
                    for (JsonElement p : o.getAsJsonArray("pages")) {
                        JsonObject page = p.getAsJsonObject();
                        if (page.has("items")) {
                            for (JsonElement i : page.getAsJsonArray("items")) {
                                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(i.getAsString())),
                                        name + " shows unknown item " + i.getAsString());
                            }
                        }
                    }
                } catch (java.io.IOException e) {
                    throw new AssertionError(e);
                }
            }
        }
        helper.succeed();
    }

    private InnerOrbitGameTests() {
    }
}
