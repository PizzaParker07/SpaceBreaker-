package com.pizzaparker07.spacebreaker.content.machines.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** What a machine offers a recipe: its input slots, the tool (die) slot and the input tank. */
public record MachineInput(List<ItemStack> items, ItemStack tool, FluidStack fluid) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && fluid.isEmpty();
    }
}
