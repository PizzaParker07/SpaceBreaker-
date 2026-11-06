package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Cold-gas Maneuvering Unit. Clipped onto the EVA torso's backpack it lets the wearer fly with jump/sneak
 * (full control in free fall, a weak lift in low gravity). Fuel is compressed nitrogen in thrust ticks,
 * refilled at the Oxygen Filler.
 */
public class ManeuveringUnitItem extends Item {
    /** Two minutes of continuous thrust. */
    public static final int CAPACITY = 20 * 120;

    public ManeuveringUnitItem(Properties properties) {
        super(properties.component(SuitModule.GAS_FUEL.get(), 0));
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(SuitModule.GAS_FUEL.get(), 0);
    }

    public static int fuelPercent(ItemStack stack) {
        return Math.round(100F * fuel(stack) / CAPACITY);
    }

    public static ItemStack filled(ManeuveringUnitItem item) {
        ItemStack stack = new ItemStack(item);
        stack.set(SuitModule.GAS_FUEL.get(), CAPACITY);
        return stack;
    }

    public static int fill(ItemStack stack, int amount) {
        int have = fuel(stack);
        int add = Math.max(0, Math.min(amount, CAPACITY - have));
        if (add > 0) {
            stack.set(SuitModule.GAS_FUEL.get(), have + add);
        }
        return add;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * fuel(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0B040;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.mmu.fuel", fuelPercent(stack)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.spacebreaker.mmu.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
