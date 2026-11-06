package com.pizzaparker07.spacebreaker.content.suit;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table: Suit Upgrade Template + any suit piece + a module = the same piece with the module fitted
 * (all its other data, tanks, dye and enchantments kept). One JSON file covers every piece and module.
 */
public final class SuitUpgradeRecipe implements SmithingRecipe {
    public static final SuitUpgradeRecipe INSTANCE = new SuitUpgradeRecipe();
    public static final RecipeSerializer<SuitUpgradeRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final MapCodec<SuitUpgradeRecipe> codec = MapCodec.unit(INSTANCE);
        private final StreamCodec<RegistryFriendlyByteBuf, SuitUpgradeRecipe> stream = StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<SuitUpgradeRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SuitUpgradeRecipe> streamCodec() {
            return stream;
        }
    };

    private SuitUpgradeRecipe() {
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(SuitModule.SUIT_UPGRADE_TEMPLATE.get());
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() instanceof SpaceSuitItem;
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() instanceof SuitModuleItem;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isTemplateIngredient(input.template()) && result(input.base(), input.addition()) != null;
    }

    /** The upgraded piece, or null when the module does not fit (wrong slot or already fitted). */
    public static ItemStack result(ItemStack base, ItemStack addition) {
        if (!(base.getItem() instanceof SpaceSuitItem suit) || !(addition.getItem() instanceof SuitModuleItem module)) {
            return null;
        }
        if (!module.upgrade().fits(suit.getEquipmentSlot()) || SuitUpgrade.of(base) == module.upgrade()) {
            return null;
        }
        ItemStack out = base.copyWithCount(1);
        out.set(SuitModule.SUIT_UPGRADE.get(), module.upgrade());
        return out;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = result(input.base(), input.addition());
        return out == null ? ItemStack.EMPTY : out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(SuitModule.EVA_CHESTPLATE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
