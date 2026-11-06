package com.pizzaparker07.spacebreaker.content.fluids;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** Insulated, pressure-rated canister for cryogenic liquids and gases (4 buckets). */
public class CryogenicCanisterItem extends Item {
    public static final int CAPACITY = 4000;

    public CryogenicCanisterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static FluidStack contents(ItemStack stack) {
        return stack.getOrDefault(FluidsModule.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).copy();
    }

    public static ItemStack filled(Item item, FluidStack fluid) {
        ItemStack stack = new ItemStack(item);
        stack.set(FluidsModule.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(fluid));
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * contents(stack).getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FluidStack f = contents(stack);
        for (SbFluid fluid : FluidsModule.ALL) {
            if (f.is(fluid.get())) {
                return fluid.tint & 0xFFFFFF;
            }
        }
        return Mth.color(0.4F, 0.7F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack f = contents(stack);
        if (f.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.spacebreaker.fluid_amount", f.getHoverName(), f.getAmount(), CAPACITY)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
