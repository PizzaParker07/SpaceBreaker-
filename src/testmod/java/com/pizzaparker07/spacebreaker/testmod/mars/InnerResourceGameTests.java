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

    private InnerResourceGameTests() {
    }
}
