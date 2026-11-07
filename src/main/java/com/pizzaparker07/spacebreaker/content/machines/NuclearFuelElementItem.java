package com.pizzaparker07.spacebreaker.content.machines;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Reactor fuel for a nuclear thermal rocket: uranium carbide kernels in a tungsten cermet clad, the kind of element a
 * NERVA-style engine runs its hydrogen through. Fresh fuel is only weakly radioactive, so unlike an RTG pellet it gives
 * no dose while carried; the engine itself is the radiation source while it burns.
 */
public class NuclearFuelElementItem extends Item {
    public NuclearFuelElementItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.nuclear_fuel_element").withStyle(ChatFormatting.GRAY));
    }
}
