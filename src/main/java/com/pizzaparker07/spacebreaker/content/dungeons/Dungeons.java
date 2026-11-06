package com.pizzaparker07.spacebreaker.content.dungeons;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared dungeon framework, for every world. To add a dungeon:
 * <ol>
 *   <li>Worldgen: a structure (usually {@code minecraft:jigsaw}) + template pool + structure set + biome tag, and the
 *       NBT templates (we generate them by code in {@code tools/structures}). Chests carry a {@code LootTable}.</li>
 *   <li>Loot: tables under {@code spacebreaker:dungeons/<world>/<name>}; roll tech disks with the
 *       {@code spacebreaker:random_tech_disk} function on {@code spacebreaker:tech_disk}.</li>
 *   <li>A {@link DungeonDef} in {@code data/<ns>/spacebreaker/dungeon/<name>.json}: discovery advancement, guardians.</li>
 *   <li>Optional Java behaviour through {@link DungeonHooks}; names via the language key {@code dungeon.<ns>.<name>}.</li>
 * </ol>
 */
public final class Dungeons {
    public static final ResourceKey<Registry<DungeonDef>> REGISTRY = ResourceKey.createRegistryKey(Spacebreaker.id("dungeon"));

    /** Earth's first dungeon: the abandoned observatory and rocket test stand. */
    public static final ResourceLocation EARTH_OBSERVATORY = Spacebreaker.id("earth_observatory");

    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, DungeonDef.CODEC, DungeonDef.CODEC);
    }

    /** Loot table key for {@code spacebreaker:dungeons/<world>/<name>}. */
    public static ResourceKey<LootTable> lootTable(String world, String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Spacebreaker.id("dungeons/" + world + "/" + name));
    }

    public static Map<ResourceLocation, DungeonDef> all(RegistryAccess access) {
        Map<ResourceLocation, DungeonDef> out = new LinkedHashMap<>();
        access.registry(REGISTRY).ifPresent(r -> r.entrySet().forEach(e -> out.put(e.getKey().location(), e.getValue())));
        return out;
    }

    public record Found(ResourceLocation id, DungeonDef def, StructureStart start) {
    }

    /** The dungeon whose structure bounds contain {@code pos}, if any. */
    public static Optional<Found> find(ServerLevel level, BlockPos pos) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<ResourceLocation, DungeonDef> e : all(level.registryAccess()).entrySet()) {
            Structure structure = structures.get(e.getValue().structure());
            if (structure == null) {
                continue;
            }
            StructureStart start = level.structureManager().getStructureAt(pos, structure);
            if (start.isValid()) {
                return Optional.of(new Found(e.getKey(), e.getValue(), start));
            }
        }
        return Optional.empty();
    }

    private Dungeons() {
    }
}
