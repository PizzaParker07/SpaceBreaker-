package com.pizzaparker07.spacebreaker.content.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** {@code spacebreaker:dungeon_discovered}: a player entered a dungeon. Optional {@code dungeon} and {@code world} filters. */
public class DungeonDiscoveredTrigger extends SimpleCriterionTrigger<DungeonDiscoveredTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation dungeon, ResourceLocation world) {
        trigger(player, i -> i.dungeon().map(dungeon::equals).orElse(true) && i.world().map(world::equals).orElse(true));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> dungeon,
                           Optional<ResourceLocation> world) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.optionalFieldOf("dungeon").forGetter(Instance::dungeon),
                ResourceLocation.CODEC.optionalFieldOf("world").forGetter(Instance::world)
        ).apply(i, Instance::new));
    }
}
