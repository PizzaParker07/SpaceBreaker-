package com.pizzaparker07.spacebreaker.compat.emi;

import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** A Spacebreaker processing recipe in EMI; same layout and data as the JEI category. */
final class EmiProcessingRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 86;
    private static final int TANK_W = 18;
    private static final int TANK_H = 54;

    private final RecipeHolder<ProcessingRecipe> holder;

    EmiProcessingRecipe(EmiRecipeCategory category, RecipeHolder<ProcessingRecipe> holder) {
        super(category, holder.id(), WIDTH, HEIGHT);
        this.holder = holder;
        ProcessingRecipe r = holder.value();
        for (SizedIngredient i : r.ingredients) {
            inputs.add(NeoForgeEmiIngredient.of(i));
        }
        r.fluidInput.ifPresent(f -> inputs.add(NeoForgeEmiIngredient.of(f)));
        r.fluidInput2.ifPresent(f -> inputs.add(NeoForgeEmiIngredient.of(f)));
        r.tool.ifPresent(t -> catalysts.add(EmiIngredient.of(t)));
        for (ItemStack out : r.results) {
            outputs.add(EmiStack.of(out));
        }
        r.bonus.ifPresent(b -> outputs.add(EmiStack.of(b).setChance(r.bonusChance)));
        for (FluidStack f : r.fluidResults) {
            outputs.add(NeoForgeEmiStack.of(f));
        }
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        ProcessingRecipe r = holder.value();
        for (int i = 0; i < r.ingredients.size(); i++) {
            w.addSlot(NeoForgeEmiIngredient.of(r.ingredients.get(i)), (i % 3) * 18, (i / 3) * 18);
        }
        int x = Math.min(3, r.ingredients.size()) * 18;
        for (SizedFluidIngredient f : r.fluidInput.stream().toList()) {
            w.addTank(NeoForgeEmiIngredient.of(f), x, 0, TANK_W, TANK_H, Math.max(1000, f.amount()));
            x += TANK_W + 2;
        }
        for (SizedFluidIngredient f : r.fluidInput2.stream().toList()) {
            w.addTank(NeoForgeEmiIngredient.of(f), x, 0, TANK_W, TANK_H, Math.max(1000, f.amount()));
            x += TANK_W + 2;
        }
        int ax = Math.max(x + 4, 24);
        w.addFillingArrow(ax, 18, r.time * 50);
        r.tool.ifPresent(t -> w.addSlot(EmiIngredient.of(t), ax + 3, 38).catalyst(true)
                .appendTooltip(Component.translatable(RecipeViewerData.KEY_TOOL)));
        int ox = ax + 30;
        int oy = 0;
        for (ItemStack out : r.results) {
            w.addSlot(EmiStack.of(out), ox, oy).recipeContext(this);
            oy += 22;
            if (oy > 44) {
                oy = 0;
                ox += 22;
            }
        }
        if (r.bonus.isPresent()) {
            w.addSlot(EmiStack.of(r.bonus.get()).setChance(r.bonusChance), ox, oy).recipeContext(this);
            ox += 22;
        } else if (!r.results.isEmpty()) {
            ox += 22;
        }
        for (FluidStack f : r.fluidResults) {
            w.addTank(NeoForgeEmiStack.of(f), ox, 0, TANK_W, TANK_H, Math.max(1000, f.getAmount())).recipeContext(this);
            ox += TANK_W + 2;
        }
        var font = Minecraft.getInstance().font;
        int y = HEIGHT - 28;
        w.addText(RecipeViewerData.energyLine(r), 0, y, 0xFF404040, false);
        Component time = RecipeViewerData.timeLine(r);
        w.addText(time, WIDTH - font.width(time), y, 0xFF404040, false);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            Component req = RecipeViewerData.requirementLine(RecipeViewerData.requirement(level.registryAccess(), holder));
            if (req != null) {
                int ly = y + 10;
                for (var line : font.split(req, WIDTH)) {
                    w.addText(line, 0, ly, 0xFF8B1A1A, false);
                    ly += 9;
                }
            }
        }
    }
}
