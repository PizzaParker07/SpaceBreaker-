package com.pizzaparker07.spacebreaker.content.earth;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Shapeless crafting where damageable tools (e.g. shears) are not used up: they stay in the grid with one
 * point of durability less. Used for the hand-made fallbacks of the industry chain (copper wire by hand).
 */
public class ToolShapelessRecipe extends ShapelessRecipe {
    public ToolShapelessRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }

    static ToolShapelessRecipe of(ShapelessRecipe r) {
        return new ToolShapelessRecipe(r.getGroup(), r.category(), r.getResultItem(null), r.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isDamageableItem()) {
                ItemStack tool = stack.copyWithCount(1);
                tool.setDamageValue(tool.getDamageValue() + 1);
                out.set(i, tool.getDamageValue() >= tool.getMaxDamage() ? ItemStack.EMPTY : tool);
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EarthModule.TOOL_SHAPELESS.get();
    }

    public static final class Serializer implements RecipeSerializer<ToolShapelessRecipe> {
        private static final MapCodec<ToolShapelessRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
                .xmap(ToolShapelessRecipe::of, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, ToolShapelessRecipe> STREAM = RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
                .map(ToolShapelessRecipe::of, r -> r);

        @Override
        public MapCodec<ToolShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolShapelessRecipe> streamCodec() {
            return STREAM;
        }
    }
}
