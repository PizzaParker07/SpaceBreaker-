package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.recipe.MachineInput;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** Electric arc furnace: alloys and refractory metals, and anything a furnace can smelt, twice as fast. */
public class ArcFurnaceBlockEntity extends ProcessingMachineBlockEntity {
    public static final int SMELT_ENERGY = 30;

    public ArcFurnaceBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec, pos, state);
    }

    @Override
    protected @Nullable ProcessingRecipe findRecipe(Level level, MachineInput input) {
        ProcessingRecipe r = super.findRecipe(level, input);
        if (r != null) {
            return r;
        }
        ItemStack single = ItemStack.EMPTY;
        for (ItemStack s : input.items()) {
            if (!s.isEmpty()) {
                if (!single.isEmpty()) {
                    return null;
                }
                single = s;
            }
        }
        if (single.isEmpty()) {
            return null;
        }
        var smelt = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(single), level);
        if (smelt.isEmpty()) {
            return null;
        }
        SmeltingRecipe sr = smelt.get().value();
        ItemStack out = sr.getResultItem(level.registryAccess()).copy();
        return new ProcessingRecipe(spec.recipe().get(), List.of(new SizedIngredient(sr.getIngredients().get(0), 1)), Optional.empty(),
                Optional.empty(), List.of(out), Optional.empty(), 0F, List.of(), Math.max(20, sr.getCookingTime() / 2), SMELT_ENERGY);
    }
}
