package com.pizzaparker07.spacebreaker.compat.jei;

import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * One JEI category per processing kind. Layout (180 × 78): item inputs in a 3-wide grid, fluid inputs as tanks,
 * the arrow (the tool, if any, below it), item outputs and the bonus with its chance, fluid outputs as tanks; under
 * them the energy and time, and the research the recipe needs.
 */
final class ProcessingCategory implements IRecipeCategory<RecipeHolder<ProcessingRecipe>> {
    static final int WIDTH = 180;
    static final int HEIGHT = 86;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;

    private final RecipeViewerData.Category category;
    private final RecipeType<RecipeHolder<ProcessingRecipe>> type;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    @SuppressWarnings({"unchecked", "rawtypes"})
    ProcessingCategory(RecipeViewerData.Category category, IGuiHelper gui) {
        this.category = category;
        this.type = (RecipeType) RecipeType.create(category.uid().getNamespace(), category.uid().getPath(), RecipeHolder.class);
        this.icon = gui.createDrawableItemLike(category.machine().get());
        this.arrow = gui.createAnimatedRecipeArrow(40);
    }

    RecipeType<RecipeHolder<ProcessingRecipe>> type() {
        return type;
    }

    RecipeViewerData.Category category() {
        return category;
    }

    @Override
    public RecipeType<RecipeHolder<ProcessingRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(category.titleKey());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /** x of the arrow for this recipe (after the inputs). */
    private static int arrowX(ProcessingRecipe r) {
        int itemCols = Math.min(3, r.ingredients.size());
        int x = itemCols * 18;
        int tanks = (r.fluidInput.isPresent() ? 1 : 0) + (r.fluidInput2.isPresent() ? 1 : 0);
        x += tanks * (TANK_W + 4);
        return Math.max(x + 4, 24);
    }

    /** x positions of every fluid tank of a recipe (inputs then outputs), for the frames drawn behind them. */
    static java.util.List<Integer> tankXs(ProcessingRecipe r) {
        java.util.List<Integer> xs = new java.util.ArrayList<>();
        int x = Math.min(3, r.ingredients.size()) * 18;
        int inputs = (r.fluidInput.isPresent() ? 1 : 0) + (r.fluidInput2.isPresent() ? 1 : 0);
        for (int i = 0; i < inputs; i++) {
            xs.add(x + 1);
            x += TANK_W + 4;
        }
        int ox = arrowX(r) + 30;
        int oy = 1;
        for (int i = 0; i < r.results.size(); i++) {
            oy += 22;
            if (oy > 45) {
                oy = 1;
                ox += 26;
            }
        }
        if (r.bonus.isPresent()) {
            ox += 22;
        } else if (!r.results.isEmpty()) {
            ox += 26;
        }
        for (int i = 0; i < r.fluidResults.size(); i++) {
            xs.add(ox);
            ox += TANK_W + 4;
        }
        return xs;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessingRecipe> holder, IFocusGroup focuses) {
        ProcessingRecipe r = holder.value();
        // item inputs
        for (int i = 0; i < r.ingredients.size(); i++) {
            SizedIngredient ing = r.ingredients.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 1 + (i / 3) * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.asList(ing.getItems()));
        }
        int x = Math.min(3, r.ingredients.size()) * 18;
        for (SizedFluidIngredient f : r.fluidInput.stream().toList()) {
            tank(builder, RecipeIngredientRole.INPUT, x + 1, f);
            x += TANK_W + 4;
        }
        for (SizedFluidIngredient f : r.fluidInput2.stream().toList()) {
            tank(builder, RecipeIngredientRole.INPUT, x + 1, f);
            x += TANK_W + 4;
        }
        int ax = arrowX(r);
        r.tool.ifPresent(tool -> builder.addSlot(RecipeIngredientRole.CATALYST, ax + 4, 38)
                .setStandardSlotBackground()
                .addIngredients(tool)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(RecipeViewerData.KEY_TOOL).withStyle(ChatFormatting.GRAY))));
        // outputs
        int ox = ax + 30;
        int oy = 1;
        for (ItemStack out : r.results) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, ox, oy).setOutputSlotBackground().addItemStack(out);
            oy += 22;
            if (oy > 45) {
                oy = 1;
                ox += 26;
            }
        }
        if (r.bonus.isPresent()) {
            float chance = r.bonusChance;
            builder.addSlot(RecipeIngredientRole.OUTPUT, ox, oy).setStandardSlotBackground().addItemStack(r.bonus.get())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(RecipeViewerData.chanceLine(chance)));
            ox += 22;
        } else if (!r.results.isEmpty()) {
            ox += 26;
        }
        for (FluidStack f : r.fluidResults) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, ox, 1)
                    .setFluidRenderer(Math.max(1000, f.getAmount()), false, TANK_W, TANK_H);
            slot.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(f));
            ox += TANK_W + 4;
        }
    }

    private static void tank(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, SizedFluidIngredient f) {
        builder.addSlot(role, x, 1)
                .setFluidRenderer(Math.max(1000, f.amount()), false, TANK_W, TANK_H)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(f.getFluids()));
    }

    @Override
    public void draw(RecipeHolder<ProcessingRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ProcessingRecipe r = holder.value();
        for (int tx : tankXs(r)) {
            // tank frame: dark border, darker glass, like the machines' own screens
            g.fill(tx - 1, 0, tx + TANK_W + 1, TANK_H + 2, 0xFF373737);
            g.fill(tx, 1, tx + TANK_W, TANK_H + 1, 0xFF1E2228);
        }
        arrow.draw(g, arrowX(r), 19);
        Font font = Minecraft.getInstance().font;
        int y = HEIGHT - 28;
        g.drawString(font, RecipeViewerData.energyLine(r), 0, y, 0xFF404040, false);
        Component time = RecipeViewerData.timeLine(r);
        g.drawString(font, time, WIDTH - font.width(time), y, 0xFF404040, false);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            Component req = RecipeViewerData.requirementLine(RecipeViewerData.requirement(level.registryAccess(), holder));
            if (req != null) {
                int ly = y + 10;
                for (var line : font.split(req, WIDTH)) {
                    g.drawString(font, line, 0, ly, 0xFF8B1A1A, false);
                    ly += 9;
                }
            }
        }
    }

    @Override
    public ResourceLocation getRegistryName(RecipeHolder<ProcessingRecipe> holder) {
        return holder.id();
    }
}
