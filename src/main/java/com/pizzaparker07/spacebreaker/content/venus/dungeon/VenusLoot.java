package com.pizzaparker07.spacebreaker.content.venus.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Loot tables of the Venus dungeon (JSON in data/spacebreaker/loot_table/dungeons/venus/). */
public final class VenusLoot {
    public static final ResourceKey<LootTable> AEROSTAT_LAB = Dungeons.lootTable("venus", "aerostat_lab");
    public static final ResourceKey<LootTable> AEROSTAT_STORES = Dungeons.lootTable("venus", "aerostat_stores");

    private VenusLoot() {
    }
}
