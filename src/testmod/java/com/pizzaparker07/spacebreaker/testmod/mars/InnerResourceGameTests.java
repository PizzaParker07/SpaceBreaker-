package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The raw materials of the inner planets go into the industry's crusher and come out as tagged, usable inputs. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InnerResourceGameTests {
    private static void crushes(GameTestHelper helper, String id, ItemLike input, Item output) {
        var holder = helper.getLevel().getRecipeManager().byKey(Spacebreaker.id("crushing/" + id))
                .orElseThrow(() -> new AssertionError("no crushing recipe " + id));
        helper.assertTrue(holder.value() instanceof ProcessingRecipe, id + " is a machine recipe");
        ProcessingRecipe r = (ProcessingRecipe) holder.value();
        helper.assertTrue(r.ingredients.get(0).test(new ItemStack(input)), id + " takes " + input);
        helper.assertTrue(r.results.get(0).is(output), id + " gives " + output);
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void innerPlanetMaterialsFeedTheCrusher(GameTestHelper helper) {
        crushes(helper, "perchlorate_soil", MarsModule.PERCHLORATE_SOIL.get(), MarsModule.PERCHLORATE_SALT.get());
        crushes(helper, "martian_ice", MarsModule.MARTIAN_ICE.get(), Items.ICE);
        crushes(helper, "dusty_ice", MarsModule.DUSTY_ICE.get(), Items.ICE);
        crushes(helper, "sulfur_crust", VenusModule.SULFUR_CRUST.get(), VenusModule.SULFUR.get());
        crushes(helper, "hollow_deposit", MercuryModule.HOLLOW_DEPOSIT.get(), VenusModule.SULFUR.get());
        helper.assertTrue(new ItemStack(MarsModule.PERCHLORATE_SALT.get()).is(MarsModule.DUSTS_PERCHLORATE), "perchlorate is c:dusts/perchlorate");
        helper.assertTrue(new ItemStack(MarsModule.MARTIAN_ICE.get()).is(MarsModule.ICES_WATER), "martian ice is c:ices/water");
        helper.assertTrue(new ItemStack(Items.ICE).is(MarsModule.ICES_WATER), "plain ice is c:ices/water");
        helper.assertTrue(new ItemStack(VenusModule.SULFUR.get()).is(MarsModule.DUSTS_SULFUR), "sulfur is c:dusts/sulfur");
        helper.assertTrue(new ItemStack(MercuryModule.MERCURY_REGOLITH.get()).is(MarsModule.REGOLITHS_SOLAR_WIND), "mercury regolith holds solar wind");
        helper.succeed();
    }

    /**
     * Mercury has no air, so a rocket must refuel there to come home: polar crater ice → water → hydrogen + oxygen,
     * graphite + oxygen → CO₂, CO₂ + hydrogen → methane, then liquefied methane and oxygen. Every step must exist.
     */
    @GameTest(template = WorldTests.EMPTY)
    public static void mercuryCanMakeMethalox(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var burn = recipes.byKey(Spacebreaker.id("chemical_reaction/graphite_combustion"))
                .orElseThrow(() -> new AssertionError("no graphite combustion"));
        ProcessingRecipe r = (ProcessingRecipe) burn.value();
        helper.assertTrue(r.ingredients.get(0).test(new ItemStack(MercuryModule.GRAPHITE.get())), "burns graphite");
        helper.assertTrue(r.fluidResults.get(0).is(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.CARBON_DIOXIDE.get()), "makes CO2");
        for (String id : new String[]{"melting/water_ice", "electrolysis/water", "sabatier/methane", "liquefaction/methane", "liquefaction/oxygen"}) {
            helper.assertTrue(recipes.byKey(Spacebreaker.id(id)).isPresent(), "missing step " + id);
        }
        helper.assertTrue(new ItemStack(com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_ICE.get()).is(MarsModule.ICES_WATER),
                "Mercury's polar crater ice melts into water");
        var mercury = WorldTests.profile(helper, "mercury");
        helper.assertTrue(mercury.materials().ice().map(i -> i.is(com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_ICE.get())).orElse(false),
                "Mercury's shadowed craters hold water ice");
        helper.succeed();
    }

    /** Every inner-planet creature is worth something: each has a loot table with at least one useful drop. */
    @GameTest(template = WorldTests.EMPTY)
    public static void everyInnerCreatureDropsSomething(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String id : new String[]{"rust_beetle", "stonegrazer", "canyon_lurker", "polar_worm", "dust_devil", "cloud_jelly", "acid_ray",
                "terminator_crawler", "hollow_snail"}) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Spacebreaker.id("entities/" + id));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    id + " drops nothing");
        }
        helper.succeed();
    }

    /** The Cloud Jelly's gas sac lets you sink slowly. */
    @GameTest(template = WorldTests.EMPTY)
    public static void liftGasSacSlowsYourFall(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(VenusModule.LIFT_GAS_SAC.get(), 2));
        VenusModule.LIFT_GAS_SAC.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "slow falling after use");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "one sac used up");
        helper.succeed();
    }

    private InnerResourceGameTests() {
    }
}
