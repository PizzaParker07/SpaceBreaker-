package com.pizzaparker07.spacebreaker.content.machines;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Reactor fuel for a nuclear thermal rocket: uranium carbide kernels in a tungsten cermet clad, the kind of hexagonal
 * element a solid-core engine runs its hydrogen through. Fresh fuel is only weakly radioactive, so unlike an RTG pellet it gives
 * no dose while carried; the engine itself is the radiation source while it burns.
 */
public class NuclearFuelElementItem extends Item {
    public NuclearFuelElementItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.nuclear_fuel_element").withStyle(ChatFormatting.GRAY));
        Integer left = stack.get(MachinesModule.FUEL_BURN_LEFT.get());
        if (left != null) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.nuclear_fuel_element.left", percentLeft(stack)).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Burn left in percent: 100 for a fresh element, 1..99 for a partly burnt one. */
    public static int percentLeft(ItemStack stack) {
        Integer left = stack.get(MachinesModule.FUEL_BURN_LEFT.get());
        if (left == null) {
            return 100;
        }
        // a partly burnt element never reads 100 %: floor, and at least 1 % while anything is left
        return Math.max(1, Math.min(99, (int) Math.floor(100.0 * left / com.pizzaparker07.spacebreaker.content.machines.machine.FissionReactorBlockEntity.BURN_TICKS)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(MachinesModule.FUEL_BURN_LEFT.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * percentLeft(stack) / 100F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9CD84A;
    }
}
