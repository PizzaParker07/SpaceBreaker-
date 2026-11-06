package com.pizzaparker07.spacebreaker.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * {@code spacebreaker:tech_learned}: fires when a player learns a technology.
 * Optional {@code tech} (exact id), {@code category} and {@code min_known} (total number of known technologies).
 */
public class TechLearnedTrigger extends SimpleCriterionTrigger<TechLearnedTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation tech, String category, int knownCount) {
        trigger(player, i -> i.matches(tech, category, knownCount));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> tech, Optional<String> category,
                           Optional<Integer> minKnown) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.optionalFieldOf("tech").forGetter(Instance::tech),
                Codec.STRING.optionalFieldOf("category").forGetter(Instance::category),
                Codec.INT.optionalFieldOf("min_known").forGetter(Instance::minKnown)
        ).apply(i, Instance::new));

        boolean matches(ResourceLocation id, String cat, int count) {
            return tech.map(id::equals).orElse(true) && category.map(c -> c.equals(cat)).orElse(true)
                    && minKnown.map(m -> count >= m).orElse(true);
        }
    }
}
