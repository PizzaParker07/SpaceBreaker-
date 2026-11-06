package com.pizzaparker07.spacebreaker.core.tech;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * One technology in the research tree, loaded from {@code data/<ns>/spacebreaker/tech/<name>.json}.
 * Technologies are learned by researching data recovered from dungeons, probes and experiments.
 * Name and description come from the language keys {@code tech.<ns>.<path>} and {@code tech.<ns>.<path>.desc}.
 *
 * @param category     tree branch: "propulsion", "life_support", "industry", "astronomy", "construction", "exploration"
 * @param tier         progression phase (1 = Earth/Moon, 2 = inner system, 3 = outer system ...)
 * @param requires     technologies that must be known first
 * @param icon         item shown in the research screen
 * @param unlocks      what it enables: recipe ids, item ids or feature ids (interpreted by the systems that check them,
 *                     see {@link TechRequirement})
 * @param researchTime ticks a Research Lab needs to analyse it (0 = derived from the tier)
 * @param energy       FE per tick the lab draws while analysing it (0 = derived from the tier)
 * @param samples      sample kinds (usually celestial body ids, e.g. {@code spacebreaker:moon}, or experiment ids)
 *                     whose analysis can reveal this technology; empty = only from a tech disk
 */
public record TechNode(String category, int tier, List<ResourceLocation> requires, Optional<ResourceLocation> icon,
                       List<ResourceLocation> unlocks, int researchTime, int energy, List<ResourceLocation> samples) {
    public static final Codec<TechNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("category").forGetter(TechNode::category),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(TechNode::tier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(TechNode::requires),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(TechNode::icon),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(TechNode::unlocks),
            Codec.INT.optionalFieldOf("research_time", 0).forGetter(TechNode::researchTime),
            Codec.INT.optionalFieldOf("energy", 0).forGetter(TechNode::energy),
            ResourceLocation.CODEC.listOf().optionalFieldOf("samples", List.of()).forGetter(TechNode::samples)
    ).apply(i, TechNode::new));

    /** Original five-field form (kept for source compatibility). */
    public TechNode(String category, int tier, List<ResourceLocation> requires, Optional<ResourceLocation> icon,
                    List<ResourceLocation> unlocks) {
        this(category, tier, requires, icon, unlocks, 0, 0, List.of());
    }

    /** Lab analysis time in ticks: explicit value, else 30 s per tier. */
    public int effectiveResearchTime() {
        return researchTime > 0 ? researchTime : 600 * Math.max(1, tier);
    }

    /** Lab power draw in FE/t: explicit value, else 10 / 24 / 60 ... by tier. */
    public int effectiveEnergy() {
        if (energy > 0) {
            return energy;
        }
        return switch (Math.max(1, tier)) {
            case 1 -> 10;
            case 2 -> 24;
            default -> 60;
        };
    }

    public static String nameKey(ResourceLocation id) {
        return "tech." + id.getNamespace() + "." + id.getPath();
    }

    public static String descriptionKey(ResourceLocation id) {
        return nameKey(id) + ".desc";
    }

    public static String categoryKey(String category) {
        return "tech_category.spacebreaker." + category;
    }
}
