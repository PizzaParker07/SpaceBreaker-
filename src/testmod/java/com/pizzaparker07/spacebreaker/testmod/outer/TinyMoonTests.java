package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.TinyWorld;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.BeltStructures;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.neptune.Proteus;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The tiny irregular worlds (the moons Nix, Hydra, Proteus and Nereid; the near-Earth asteroids Bennu, Ryugu, Eros and Itokawa): each body with a dimension has a world, a planet-engine terrain much
 * smaller and lumpier than a round moon's, very weak gravity, its own ground at the surface, a lander, and refuelling
 * only where the ice is clean water ice.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TinyMoonTests {
    private static final List<TinyWorld> WORLDS = List.of(PlutoMoonlets.NIX, PlutoMoonlets.HYDRA, Proteus.PROTEUS,
            com.pizzaparker07.spacebreaker.content.neptune.Nereid.NEREID, com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.BENNU,
            com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.RYUGU, com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.EROS,
            com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.ITOKAWA, com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV,
            com.pizzaparker07.spacebreaker.content.belt.Comets.ENCKE, com.pizzaparker07.spacebreaker.content.belt.Comets.HALLEY);

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void tinyMoonsAreWorlds(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var refuel = TagKey.create(Bodies.REGISTRY, Spacebreaker.id("refuel"));
        var height = LevelHeightAccessor.create(-64, 384);
        double triton = WorldTests.profile(helper, "triton").poleDistance();
        for (TinyWorld w : WORLDS) {
            CelestialBody b = Bodies.forDimension(access, w.level).orElseThrow(() -> new AssertionError(w.name + " has no body"));
            helper.assertTrue(b.physical().surfaceGravity() < 0.1, w.name + " has almost no gravity: " + b.physical().surfaceGravity());
            PlanetChunkGenerator gen = WorldTests.generator(helper, w.name, 4L);
            helper.assertTrue(gen.terrain().profile().poleDistance() < triton, w.name + " is much smaller than Triton");
            // the surface is the world's own soil or rock somewhere near the landing site
            int own = 0;
            for (int i = 0; i < 16; i++) {
                int x = i * 37 - 300;
                int z = i * 23 - 180;
                var top = gen.getBaseColumn(x, z, height, null).getBlock(gen.surfaceY(x, z, height));
                own += top.is(w.soil.get()) || top.is(w.rock.get()) ? 1 : 0;
            }
            helper.assertTrue(own >= 8, w.name + " is covered in its own ground: " + own + "/16");
            helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(
                    BeltStructures.landerKey(LanderPiece.Variant.valueOf(w.name.toUpperCase(java.util.Locale.ROOT)))), w.name + " has a lander");
            var holder = Bodies.registry(access).orElseThrow().getHolder(ResourceKey.create(Bodies.REGISTRY, Spacebreaker.id(w.name))).orElseThrow();
            boolean refuels = holder.is(refuel);
            boolean water = w == PlutoMoonlets.NIX || w == PlutoMoonlets.HYDRA || w == com.pizzaparker07.spacebreaker.content.neptune.Nereid.NEREID
                    || com.pizzaparker07.spacebreaker.content.belt.Comets.all().contains(w);
            helper.assertTrue(refuels == water, w.name + (refuels ? " refuels" : " does not refuel"));
        }
        // Proteus: Pharos scoops out its west side
        var t = WorldTests.generator(helper, "proteus", 4L).terrain();
        var pharos = Proteus.pharos();
        double floor = BasinFeature.offset(t, pharos, pharos.x(), BasinFeature.centreZ(t, pharos));
        helper.assertTrue(floor < -20, "Pharos is a deep basin: " + floor);
        // the near-Earth asteroids' own shapes
        var ryugu = WorldTests.generator(helper, "ryugu", 4L).terrain();
        var ridge = com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.ryuguRidge();
        double crest = com.pizzaparker07.spacebreaker.content.belt.world.RidgeFeature.offset(ryugu, ridge, 0, 0);
        double off = com.pizzaparker07.spacebreaker.content.belt.world.RidgeFeature.offset(ryugu, ridge, 0, ryugu.profile().poleDistance() * 0.5);
        helper.assertTrue(crest > 12 && off == 0, "Ryugu's equatorial ridge: " + crest + " / " + off);
        var bennuRidge = com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.bennuRidge();
        helper.assertTrue(bennuRidge.height() < ridge.height(), "Bennu's bulge is lower than Ryugu's ridge");
        var itokawa = WorldTests.generator(helper, "itokawa", 4L).terrain();
        var sea = com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.musesSea();
        double sz = BasinFeature.centreZ(itokawa, sea);
        double a = BasinFeature.offset(itokawa, sea, sea.x(), sz);
        double b = BasinFeature.offset(itokawa, sea, sea.x() + sea.radius() * 0.4, sz);
        helper.assertTrue(a < 0 && Math.abs(a - b) < 2, "the Muses Sea is a flat, low plain: " + a + " / " + b);
        var eros = WorldTests.generator(helper, "eros", 4L).terrain();
        var himeros = com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.himeros();
        helper.assertTrue(BasinFeature.offset(eros, himeros, himeros.x(), BasinFeature.centreZ(eros, himeros)) < -15, "Himeros is a deep saddle");
        helper.assertTrue(WorldTests.profile(helper, "eros").poleDistance() > WorldTests.profile(helper, "bennu").poleDistance(),
                "Eros is far bigger than Bennu");
        helper.succeed();
    }
}
