package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A data cartridge holding one technology (component {@code spacebreaker:tech}). Found in dungeons;
 * read it in a Research Lab to learn the technology. The label colour shows the tree branch.
 */
public class TechDiskItem extends Item {
    public TechDiskItem(Properties properties) {
        super(properties);
    }

    public static ResourceLocation tech(ItemStack stack) {
        return stack.get(ResearchModule.TECH.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation tech = tech(stack);
        if (tech == null) {
            return Component.translatable("item.spacebreaker.tech_disk.blank");
        }
        return Component.translatable("item.spacebreaker.tech_disk.named", Component.translatable(TechNode.nameKey(tech)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation tech = tech(stack);
        if (tech == null) {
            return;
        }
        String category = ResearchClientData.categoryOf(tech, context);
        if (category != null) {
            tooltip.add(Component.translatable(TechNode.categoryKey(category))
                    .withColor(ResearchTree.categoryColor(category)));
        }
        if (ResearchClientData.isKnownOnClient(tech)) {
            tooltip.add(Component.translatable("tooltip.spacebreaker.tech_disk.known").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.spacebreaker.tech_disk.hint").withStyle(ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal(tech.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Tint for layer 1 (the label), by branch. */
    public static int labelColor(ItemStack stack) {
        ResourceLocation tech = tech(stack);
        String category = tech == null ? null : ResearchClientData.categoryOf(tech, null);
        if (category == null) {
            return 0xFFE8E2D0;
        }
        return 0xFF000000 | ResearchTree.categoryColor(category);
    }
}
