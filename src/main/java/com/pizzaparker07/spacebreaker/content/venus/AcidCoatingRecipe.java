package com.pizzaparker07.spacebreaker.content.venus;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Crafting: one piece of armour + a fluoropolymer coating = the same armour, now resistant to the acid clouds. */
public class AcidCoatingRecipe extends CustomRecipe {
    public AcidCoatingRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack[] find(CraftingInput input) {
        ItemStack armour = ItemStack.EMPTY;
        int coatings = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.is(VenusModule.ACID_COATING.get())) {
                coatings++;
            } else if (s.getItem() instanceof ArmorItem && armour.isEmpty() && !Boolean.TRUE.equals(s.get(VenusModule.ACID_COATED.get()))) {
                armour = s;
            } else {
                return null;
            }
        }
        return coatings == 1 && !armour.isEmpty() ? new ItemStack[]{armour} : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] f = find(input);
        if (f == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = f[0].copyWithCount(1);
        out.set(VenusModule.ACID_COATED.get(), true);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VenusModule.ACID_COATING_RECIPE.get();
    }
}
