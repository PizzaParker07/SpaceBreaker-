package com.pizzaparker07.spacebreaker.content.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A dungeon, loaded from {@code data/<ns>/spacebreaker/dungeon/<name>.json}. Ties a worldgen structure to the research
 * system: discovery advancement, guardians and (through its chests' loot tables) tech disks.
 * <pre>
 * {
 *   "structure": "spacebreaker:earth_observatory",   // worldgen structure id
 *   "world": "spacebreaker:earth",                   // celestial body (informative; used by advancements)
 *   "guardians": [ { "entity": "minecraft:zombie", "count": 2 } ],
 *   "guardian_radius": 8,                            // blocks around the structure centre
 *   "loot_tables": ["spacebreaker:dungeons/earth/observatory_archive"]  // informative (guide, tests)
 * }
 * </pre>
 * The display name is the language key {@code dungeon.<ns>.<path>}.
 */
public record DungeonDef(ResourceLocation structure, ResourceLocation world, List<Guardian> guardians, int guardianRadius,
                         List<ResourceLocation> lootTables) {
    public record Guardian(ResourceLocation entity, int count, Optional<ResourceLocation> mainHand) {
        public static final Codec<Guardian> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("entity").forGetter(Guardian::entity),
                Codec.intRange(1, 16).optionalFieldOf("count", 1).forGetter(Guardian::count),
                ResourceLocation.CODEC.optionalFieldOf("main_hand").forGetter(Guardian::mainHand)
        ).apply(i, Guardian::new));
    }

    public static final Codec<DungeonDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(DungeonDef::structure),
            ResourceLocation.CODEC.fieldOf("world").forGetter(DungeonDef::world),
            Guardian.CODEC.listOf().optionalFieldOf("guardians", List.of()).forGetter(DungeonDef::guardians),
            Codec.intRange(0, 64).optionalFieldOf("guardian_radius", 8).forGetter(DungeonDef::guardianRadius),
            ResourceLocation.CODEC.listOf().optionalFieldOf("loot_tables", List.of()).forGetter(DungeonDef::lootTables)
    ).apply(i, DungeonDef::new));

    public static String nameKey(ResourceLocation id) {
        return "dungeon." + id.getNamespace() + "." + id.getPath();
    }
}
