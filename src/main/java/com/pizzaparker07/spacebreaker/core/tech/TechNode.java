package com.pizzaparker07.spacebreaker.core.tech;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * One technology in the research tree, loaded from {@code data/<ns>/spacebreaker/tech/<name>.json}.
 * Technologies are learned by researching data recovered from dungeons, probes and experiments.
 *
 * @param category  tree branch, e.g. "propulsion", "life_support", "industry", "astronomy"
 * @param tier      rough progression tier (1 = Earth/Moon ...)
 * @param requires  technologies that must be known first
 * @param icon      item shown in the research screen
 * @param unlocks   what it enables: recipe ids, item ids or feature ids (interpreted by the systems that check them)
 */
public record TechNode(String category, int tier, List<ResourceLocation> requires, Optional<ResourceLocation> icon,
                       List<ResourceLocation> unlocks) {
    public static final Codec<TechNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("category").forGetter(TechNode::category),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(TechNode::tier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(TechNode::requires),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(TechNode::icon),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(TechNode::unlocks)
    ).apply(i, TechNode::new));
}
