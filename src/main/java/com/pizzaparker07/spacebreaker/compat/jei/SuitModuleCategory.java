package com.pizzaparker07.spacebreaker.compat.jei;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Smithing table: Suit Upgrade Template + suit piece + module = the piece with the module fitted. */
final class SuitModuleCategory implements IRecipeCategory<RecipeViewerData.SuitUpgradeDisplay> {
    static final RecipeType<RecipeViewerData.SuitUpgradeDisplay> TYPE =
            RecipeType.create(Spacebreaker.MOD_ID, RecipeViewerData.SUIT_MODULES, RecipeViewerData.SuitUpgradeDisplay.class);

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    SuitModuleCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Blocks.SMITHING_TABLE);
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeViewerData.SuitUpgradeDisplay> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(RecipeViewerData.KEY_SUIT_MODULES);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 108;
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeViewerData.SuitUpgradeDisplay d, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 2).setStandardSlotBackground().addItemStack(d.template());
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 2).setStandardSlotBackground().addItemStack(d.base());
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 2).setStandardSlotBackground().addItemStack(d.module());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 89, 2).setOutputSlotBackground().addItemStack(d.result());
    }

    @Override
    public void draw(RecipeViewerData.SuitUpgradeDisplay d, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        arrow.draw(g, 59, 2);
    }

    @Override
    public ResourceLocation getRegistryName(RecipeViewerData.SuitUpgradeDisplay d) {
        return d.id();
    }
}
