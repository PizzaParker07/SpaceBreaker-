package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.Comets;
import com.pizzaparker07.spacebreaker.content.belt.block.CometJetBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 67P: a tiny dark world of comet dust over ice, with jets that rise only in sunlight and under open sky. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CometTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void cometJetsNeedSunAndOpenSky(GameTestHelper helper) {
        // sun high, jet at the top of its column: active; something above it, or the sun down: not
        helper.assertTrue(CometJetBlock.active(1.2F, 65, 64), "a jet rises in sunlight");
        helper.assertFalse(CometJetBlock.active(1.2F, 70, 64), "but not under a roof");
        helper.assertFalse(CometJetBlock.active(-0.5F, 65, 64), "nor at night");
        helper.assertFalse(CometJetBlock.active(0.01F, 65, 64), "nor with the sun on the horizon");
        helper.assertTrue(Comets.JET.get().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL), "dug with a shovel");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void churyumovIsATinyDarkIceWorld(GameTestHelper helper) {
        PlanetChunkGenerator gen = WorldTests.generator(helper, "churyumov_gerasimenko", 6L);
        helper.assertTrue(gen.terrain().profile().poleDistance() < WorldTests.profile(helper, "nix").poleDistance(), "smaller than Nix");
        var height = net.minecraft.world.level.LevelHeightAccessor.create(-64, 384);
        int dust = 0;
        for (int i = 0; i < 16; i++) {
            int x = i * 29 - 200;
            int z = i * 17 - 120;
            var top = gen.getBaseColumn(x, z, height, null).getBlock(gen.surfaceY(x, z, height));
            dust += top.is(Comets.CHURYUMOV.soil.get()) || top.is(Comets.CHURYUMOV.rock.get()) ? 1 : 0;
        }
        helper.assertTrue(dust >= 8, "covered in comet dust over ice: " + dust + "/16");
        for (var c : new com.pizzaparker07.spacebreaker.content.belt.TinyWorld[]{Comets.ENCKE, Comets.HALLEY, Comets.TEMPEL, Comets.HALE_BOPP}) {
            PlanetChunkGenerator g = WorldTests.generator(helper, c.name, 6L);
            int own = 0;
            for (int i = 0; i < 16; i++) {
                int x = i * 29 - 200;
                int z = i * 17 - 120;
                var top = g.getBaseColumn(x, z, height, null).getBlock(g.surfaceY(x, z, height));
                own += top.is(c.soil.get()) || top.is(c.rock.get()) ? 1 : 0;
            }
            helper.assertTrue(own >= 8, c.name + " is covered in its own dust and ice: " + own + "/16");
        }
        helper.assertTrue(WorldTests.profile(helper, "halley").poleDistance() > WorldTests.profile(helper, "encke").poleDistance(),
                "Halley is the bigger nucleus");
        helper.assertTrue(WorldTests.profile(helper, "hale_bopp").poleDistance() > WorldTests.profile(helper, "halley").poleDistance(),
                "Hale-Bopp is the giant among them");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void cometDustFeedsTheRegolithHeater(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (var c : Comets.all()) {
            helper.assertTrue(new net.minecraft.world.item.ItemStack(c.soil.get())
                    .is(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.CARBONACEOUS_REGOLITHS), c.name + " dust is carbonaceous regolith");
            var id = Spacebreaker.id(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.REGOLITH_PROCESSING.name() + "/"
                    + c.soil.getId().getPath());
            var holder = recipes.byKey(id).orElseThrow(() -> new AssertionError("no heater recipe " + id));
            helper.assertTrue(holder.value() instanceof com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe,
                    id + " is a processing recipe");
        }
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void cometJetsFollowTheCometsActivity(GameTestHelper helper) {
        helper.assertTrue(CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.DORMANT) == 0, "asleep far from the Sun");
        helper.assertTrue(CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.WEAK) > 0
                && CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.WEAK)
                < CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.ACTIVE), "weak, then active");
        helper.assertTrue(CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.GREAT)
                > CometJetBlock.strength(com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.ACTIVE), "a great comet blows hardest");
        // along a real orbit: at perihelion 67P is awake, at aphelion asleep
        var c = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.CHURYUMOV_GERASIMENKO;
        double peri = c.nextPerihelion(0);
        var near = com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(c, peri).level();
        var far = com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(c, peri + c.periodSeconds() / 2).level();
        helper.assertTrue(CometJetBlock.strength(near) >= 1 && CometJetBlock.strength(far) == 0, "jets at perihelion (" + near + "), none at aphelion (" + far + ")");
        // and in the world right now (sky time): logged so a showcase can be read against it
        helper.assertTrue(CometJetBlock.strength(helper.getLevel()) == 1F, "a world that is not a comet keeps full jets");
        helper.succeed();
    }
}
