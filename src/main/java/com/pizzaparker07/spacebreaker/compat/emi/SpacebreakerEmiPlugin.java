package com.pizzaparker07.spacebreaker.compat.emi;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * EMI integration (optional): EMI finds this class through its {@link EmiEntrypoint} annotation, so it is never loaded
 * without EMI. Same content as the JEI plugin, from {@link RecipeViewerData}.
 */
@EmiEntrypoint
public final class SpacebreakerEmiPlugin implements EmiPlugin {
    /** EMI expects ids of recipes that are not in the recipe manager to start with '/'. */
    static net.minecraft.resources.ResourceLocation synthetic(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "/" + id.getPath());
    }

    @Override
    public void register(EmiRegistry registry) {
        for (RecipeViewerData.Category c : RecipeViewerData.CATEGORIES) {
            EmiStack machine = EmiStack.of(c.machine().get());
            EmiRecipeCategory category = new EmiRecipeCategory(c.uid(), machine);
            registry.addCategory(category);
            registry.addWorkstation(category, machine);
            for (var holder : RecipeViewerData.recipes(registry.getRecipeManager(), c)) {
                registry.addRecipe(new EmiProcessingRecipe(category, holder));
            }
        }
        EmiStack smithing = EmiStack.of(Blocks.SMITHING_TABLE);
        EmiRecipeCategory suit = new EmiRecipeCategory(Spacebreaker.id(RecipeViewerData.SUIT_MODULES), smithing);
        registry.addCategory(suit);
        registry.addWorkstation(suit, smithing);
        for (RecipeViewerData.SuitUpgradeDisplay d : RecipeViewerData.suitUpgrades()) {
            registry.addRecipe(new BasicEmiRecipe(suit, synthetic(d.id()), 108, 18) {
                {
                    inputs.add(EmiStack.of(d.template()));
                    inputs.add(EmiStack.of(d.base()));
                    inputs.add(EmiStack.of(d.module()));
                    outputs.add(EmiStack.of(d.result()));
                }

                @Override
                public void addWidgets(WidgetHolder w) {
                    w.addSlot(inputs.get(0), 0, 0);
                    w.addSlot(inputs.get(1), 18, 0);
                    w.addSlot(inputs.get(2), 36, 0);
                    w.addTexture(EmiTexture.EMPTY_ARROW, 58, 1);
                    w.addSlot(outputs.get(0), 88, 0).recipeContext(this);
                }
            });
        }
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(RecipeViewerData.EXTRA_SMELTER.get()));
        for (RecipeViewerData.InfoPage page : RecipeViewerData.INFO_PAGES) {
            registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(page.item().get())), List.of(Component.translatable(page.key())),
                    synthetic(Spacebreaker.id("info/" + page.key().substring(page.key().lastIndexOf('.') + 1)))));
        }
    }
}
