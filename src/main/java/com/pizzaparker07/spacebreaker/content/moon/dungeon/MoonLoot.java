package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Loot tables of the Moon dungeons (JSON in data/spacebreaker/loot_table/chests/moon/).
 * {@link #TECH_DATA} is the shared hook for research data: the lab and probe chests roll it, and the
 * Research area fills it with tech disks ({@code spacebreaker:tech_disk} with a tech id component).
 */
public final class MoonLoot {
    public static final ResourceKey<LootTable> OUTPOST_LAB = key("chests/moon/outpost_lab");
    public static final ResourceKey<LootTable> OUTPOST_STORAGE = key("chests/moon/outpost_storage");
    public static final ResourceKey<LootTable> OUTPOST_COMMAND = key("chests/moon/outpost_command");
    public static final ResourceKey<LootTable> LAVA_TUBE_PROBE = key("chests/moon/lava_tube_probe");
    public static final ResourceKey<LootTable> TECH_DATA = key("chests/moon/tech_data");

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Spacebreaker.id(path));
    }

    private MoonLoot() {
    }
}
