package com.pizzaparker07.spacebreaker.compat.jei;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration (optional): JEI finds this class through its {@link JeiPlugin} annotation, so nothing here is
 * loaded unless JEI is installed. Shows every Spacebreaker processing recipe with its machine, energy, time and
 * required research, the suit-module smithing combinations, and info pages for rocket assembly and the atmosphere
 * processor. The data comes from {@link RecipeViewerData}, shared with the EMI plugin.
 */
@JeiPlugin
public final class SpacebreakerJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Spacebreaker.id("jei");
    private final List<ProcessingCategory> categories = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        categories.clear();
        for (RecipeViewerData.Category c : RecipeViewerData.CATEGORIES) {
            ProcessingCategory category = new ProcessingCategory(c, gui);
            categories.add(category);
            registration.addRecipeCategories(category);
        }
        registration.addRecipeCategories(new SuitModuleCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (ProcessingCategory c : categories) {
                registration.addRecipes(c.type(), RecipeViewerData.recipes(level.getRecipeManager(), c.category()));
            }
        }
        registration.addRecipes(SuitModuleCategory.TYPE, RecipeViewerData.suitUpgrades());
        for (RecipeViewerData.InfoPage page : RecipeViewerData.INFO_PAGES) {
            registration.addIngredientInfo(page.item().get(), Component.translatable(page.key()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ProcessingCategory c : categories) {
            registration.addRecipeCatalysts(c.type(), c.category().machine().get());
        }
        registration.addRecipeCatalysts(SuitModuleCategory.TYPE, Blocks.SMITHING_TABLE);
        registration.addRecipeCatalysts(RecipeTypes.SMELTING, RecipeViewerData.EXTRA_SMELTER.get());
    }
}
