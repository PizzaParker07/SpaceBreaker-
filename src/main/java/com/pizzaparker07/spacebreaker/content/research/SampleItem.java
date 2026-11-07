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
            case "mercury" -> 0x6E6A66;    // dark grey regolith
            case "phobos", "deimos", "ceres" -> 0x4E4A46; // carbon-rich dust
            case "vesta" -> 0x8C8478;
            case "io" -> 0xD6C24A;         // sulfur
            case "europa", "enceladus", "triton" -> 0xC8D6DE; // ice
            case "ganymede", "callisto" -> 0x7E7266;
            case "titan" -> 0xA87A3A;      // tholin orange
            case "pluto", "charon" -> 0xC9A98E;
            case "miranda" -> 0x8E9AA6;    // cool blue-grey
            case "ariel" -> 0xD8E2EA;      // bright blue-white frost
            case "titania" -> 0x7A726A;    // dark warm grey
            case "umbriel" -> 0x3E3B3A;    // soot-dark carbon
            case "oberon" -> 0x8A7268;     // reddish brown-grey
            case "nix" -> 0x9A8C88;        // grey ice, reddish tholin stain
            case "hydra", "nereid" -> 0xDDE3E8; // bright water ice
            case "proteus" -> 0x45403C;    // soot-dark crust
            case "bennu", "ryugu" -> 0x2E2B29; // coal-dark rubble
            case "eros", "itokawa" -> 0x8A7C68; // stony
            case "churyumov_gerasimenko", "encke", "halley" -> 0x26231F; // charcoal-dark comet dust
            case "suborbital_flight" -> 0x6FA0D8;
            case "orbital_flight" -> 0x3E5FA8;
            case "orbital_survey" -> 0x4F8A8B;
            case "probe_landing" -> 0xB0703A;
            default -> 0xA0A0A0;
        };
    }
}
