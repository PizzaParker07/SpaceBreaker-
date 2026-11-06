package com.pizzaparker07.spacebreaker.content.machines.recipe;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Datagen builder for {@link ProcessingRecipe}s, usable by any area: {@code ProcessingRecipeBuilder.of(MachinesModule.FABRICATING)...}. */
public final class ProcessingRecipeBuilder {
    private final ProcessingRecipe.Kind kind;
    private final List<SizedIngredient> ingredients = new ArrayList<>();
    private Optional<Ingredient> tool = Optional.empty();
    private Optional<SizedFluidIngredient> fluidInput = Optional.empty();
    private Optional<SizedFluidIngredient> fluidInput2 = Optional.empty();
    private final List<ItemStack> results = new ArrayList<>();
    private Optional<ItemStack> bonus = Optional.empty();
    private float bonusChance;
    private final List<FluidStack> fluidResults = new ArrayList<>();
    private int time = 200;
    private int energy = 20;
    private int priority;

    private ProcessingRecipeBuilder(ProcessingRecipe.Kind kind) {
        this.kind = kind;
    }

    public static ProcessingRecipeBuilder of(ProcessingRecipe.Kind kind) {
        return new ProcessingRecipeBuilder(kind);
    }

    public ProcessingRecipeBuilder in(TagKey<Item> tag, int count) {
        ingredients.add(SizedIngredient.of(tag, count));
        return this;
    }

    public ProcessingRecipeBuilder in(TagKey<Item> tag) {
        return in(tag, 1);
    }

    public ProcessingRecipeBuilder in(ItemLike item, int count) {
        ingredients.add(SizedIngredient.of(item, count));
        return this;
    }

    public ProcessingRecipeBuilder in(ItemLike item) {
        return in(item, 1);
    }

    public ProcessingRecipeBuilder tool(ItemLike item) {
        tool = Optional.of(Ingredient.of(item));
        return this;
    }

    /** First call sets the first fluid input, a second call the second one. */
    public ProcessingRecipeBuilder fluidIn(TagKey<Fluid> tag, int amount) {
        if (fluidInput.isEmpty()) {
            fluidInput = Optional.of(SizedFluidIngredient.of(tag, amount));
        } else {
            fluidInput2 = Optional.of(SizedFluidIngredient.of(tag, amount));
        }
        return this;
    }

    public ProcessingRecipeBuilder out(ItemLike item, int count) {
        results.add(new ItemStack(item, count));
        return this;
    }

    public ProcessingRecipeBuilder out(ItemLike item) {
        return out(item, 1);
    }

    public ProcessingRecipeBuilder bonus(ItemLike item, int count, float chance) {
        bonus = Optional.of(new ItemStack(item, count));
        bonusChance = chance;
        return this;
    }

    public ProcessingRecipeBuilder fluidOut(Fluid fluid, int amount) {
        fluidResults.add(new FluidStack(fluid, amount));
        return this;
    }

    public ProcessingRecipeBuilder time(int ticks) {
        this.time = ticks;
        return this;
    }

    public ProcessingRecipeBuilder energy(int perTick) {
        this.energy = perTick;
        return this;
    }

    public ProcessingRecipeBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public ProcessingRecipe build() {
        ProcessingRecipe r = new ProcessingRecipe(kind, List.copyOf(ingredients), tool, fluidInput, fluidInput2, List.copyOf(results), bonus, bonusChance,
                List.copyOf(fluidResults), time, energy);
        r.priority = priority;
        return r;
    }

    public void save(RecipeOutput out, ResourceLocation id) {
        out.accept(id, build(), null);
    }

    /** Saves as spacebreaker:&lt;kind&gt;/&lt;name&gt;. */
    public void save(RecipeOutput out, String name) {
        save(out, Spacebreaker.id(kind.name() + "/" + name));
    }
}
