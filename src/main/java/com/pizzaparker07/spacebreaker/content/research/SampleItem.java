package com.pizzaparker07.spacebreaker.content.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A field sample (rock and soil from a world) or an experiment record (flight / probe data).
 * The kind ({@code spacebreaker:sample}) decides which technologies analysing it can reveal.
 */
public class SampleItem extends Item {
    private final boolean experiment;

    public SampleItem(Properties properties, boolean experiment) {
        super(properties);
        this.experiment = experiment;
    }

    public static ResourceLocation kind(ItemStack stack) {
        return stack.get(ResearchModule.SAMPLE.get());
    }

    /** Language key of a sample kind: {@code sample.<ns>.<path>}. */
    public static String kindKey(ResourceLocation kind) {
        return "sample." + kind.getNamespace() + "." + kind.getPath();
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation kind = kind(stack);
        String base = experiment ? "item.spacebreaker.experiment_data" : "item.spacebreaker.field_sample";
        if (kind == null) {
            return Component.translatable(base);
        }
        return Component.translatable(base + ".named", Component.translatable(kindKey(kind)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.sample.hint").withStyle(ChatFormatting.GRAY));
    }

    /** Tint of layer 1 (the sample material / tape label). */
    public static int tint(ItemStack stack) {
        ResourceLocation kind = kind(stack);
        if (kind == null) {
            return 0xFFA0A0A0;
        }
        return 0xFF000000 | switch (kind.getPath()) {
            case "earth" -> 0x8A6A4A;      // brown soil
            case "moon" -> 0x9A9894;       // grey regolith
            case "mars" -> 0xB5552E;       // rust
            case "venus" -> 0xC9A757;      // sulphur yellow
            case "meteorite" -> 0x5A4E48;  // fusion crust
            case "suborbital_flight" -> 0x6FA0D8;
            case "orbital_flight" -> 0x3E5FA8;
            default -> 0xA0A0A0;
        };
    }
}
