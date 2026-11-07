package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** JEI calls for the showcase (only loaded when a JEI shot runs). */
final class JeiScreens {
    private JeiScreens() {
    }

    static void showCategory(ResourceLocation uid) {
        var rt = ShowcaseJeiPlugin.runtime;
        if (rt == null) {
            ShowcaseDriver.log("JEI is not running (use -PwithJei=true)");
            return;
        }
        rt.getRecipeManager().getRecipeType(uid).ifPresentOrElse(
                type -> rt.getRecipesGui().showTypes(List.of(type)),
                () -> ShowcaseDriver.log("no JEI category " + uid));
    }

    /** Recipes that use the item with this id. */
    static void showUsesOf(ResourceLocation itemId) {
        var rt = ShowcaseJeiPlugin.runtime;
        if (rt == null) {
            return;
        }
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId));
        rt.getRecipesGui().show(rt.getJeiHelpers().getFocusFactory().createFocus(
                mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack));
    }

    /** Recipes that make the item with this id. */
    static void showRecipesFor(ResourceLocation itemId) {
        var rt = ShowcaseJeiPlugin.runtime;
        if (rt == null) {
            return;
        }
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId));
        rt.getRecipesGui().show(rt.getJeiHelpers().getFocusFactory().createFocus(
                mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack));
    }

    /** The information pages (ours: rocket assembler, atmosphere processor). */
    static void showInfo() {
        var rt = ShowcaseJeiPlugin.runtime;
        if (rt != null) {
            rt.getRecipesGui().showTypes(List.of(mezz.jei.api.constants.RecipeTypes.INFORMATION));
        }
    }
}
