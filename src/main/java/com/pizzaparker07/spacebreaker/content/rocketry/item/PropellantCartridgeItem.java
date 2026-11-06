package com.pizzaparker07.spacebreaker.content.rocketry.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Creative/testing item: use it on a rocket to fill every tank with RP-1 and LOX at once. */
public class PropellantCartridgeItem extends Item {
    public PropellantCartridgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.propellant_cartridge").withStyle(ChatFormatting.GRAY));
    }
}
