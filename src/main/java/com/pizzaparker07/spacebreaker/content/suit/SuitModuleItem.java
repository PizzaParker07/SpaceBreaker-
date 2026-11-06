package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A suit upgrade module, fitted at a smithing table with a Suit Upgrade Template. */
public class SuitModuleItem extends Item {
    private final SuitUpgrade upgrade;

    public SuitModuleItem(SuitUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public SuitUpgrade upgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(upgrade.descriptionKey() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.module.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
