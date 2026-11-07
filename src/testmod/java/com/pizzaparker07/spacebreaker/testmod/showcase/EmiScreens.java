package com.pizzaparker07.spacebreaker.testmod.showcase;

import dev.emi.emi.api.EmiApi;
import net.minecraft.resources.ResourceLocation;

/** EMI calls for the showcase (only loaded when an EMI shot runs). */
final class EmiScreens {
    private EmiScreens() {
    }

    static void showCategory(ResourceLocation uid) {
        EmiApi.getRecipeManager().getCategories().stream().filter(c -> c.getId().equals(uid)).findFirst()
                .ifPresentOrElse(EmiApi::displayRecipeCategory, () -> ShowcaseDriver.log("no EMI category " + uid));
    }

    /** Recipes that make the item with this id. */
    static void showRecipesFor(ResourceLocation itemId) {
        EmiApi.displayRecipes(dev.emi.emi.api.stack.EmiStack.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)));
    }

    static void showInfo(ResourceLocation recipeId) {
        EmiApi.getRecipeManager().getRecipes().stream().filter(r -> recipeId.equals(r.getId())).findFirst()
                .ifPresentOrElse(EmiApi::displayRecipe, () -> ShowcaseDriver.log("no EMI recipe " + recipeId));
    }
}
