package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pressurised oxygen bottle for the EVA suit. Contents are measured in breathing ticks. */
public class OxygenTankItem extends Item {
    /** Five minutes of breathing. */
    public static final int SMALL_CAPACITY = 20 * 60 * 5;

    private final int capacity;

    public OxygenTankItem(Properties properties, int capacity) {
        super(properties.component(SuitModule.OXYGEN.get(), 0));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static int getOxygen(ItemStack stack) {
        return stack.getOrDefault(SuitModule.OXYGEN.get(), 0);
    }

    public static ItemStack filled(OxygenTankItem item) {
        ItemStack stack = new ItemStack(item);
        stack.set(SuitModule.OXYGEN.get(), item.capacity);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * getOxygen(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) getOxygen(stack) / capacity;
        return f > 0.25F ? 0x36B1C0 : Mth.hsvToRgb(0.02F, 0.8F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int ticks = getOxygen(stack);
        tooltip.add(Component.translatable("tooltip.spacebreaker.oxygen", time(ticks), time(capacity)).withStyle(ChatFormatting.AQUA));
    }

    private static String time(int ticks) {
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
