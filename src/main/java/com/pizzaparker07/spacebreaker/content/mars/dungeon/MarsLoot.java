package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Loot tables of the inner planets' dungeons (JSON in data/spacebreaker/loot_table/dungeons/&lt;world&gt;/). */
public final class MarsLoot {
    public static final ResourceKey<LootTable> STATION_COMMAND = Dungeons.lootTable("mars", "station_command");
    public static final ResourceKey<LootTable> STATION_LAB = Dungeons.lootTable("mars", "station_lab");
    public static final ResourceKey<LootTable> STATION_ISRU = Dungeons.lootTable("mars", "station_isru");
    public static final ResourceKey<LootTable> STATION_STORAGE = Dungeons.lootTable("mars", "station_storage");
    public static final ResourceKey<LootTable> RIVERBED_PROBE = Dungeons.lootTable("mars", "riverbed_probe");
    public static final ResourceKey<LootTable> LANDER_DATA = Dungeons.lootTable("mars", "lander_data");
    public static final ResourceKey<LootTable> PHOBOS_PROBE = Dungeons.lootTable("phobos", "probe_vault");
    public static final ResourceKey<LootTable> STICKNEY_LANDER = Dungeons.lootTable("phobos", "stickney_lander");
    public static final ResourceKey<LootTable> DEIMOS_RELAY = Dungeons.lootTable("deimos", "relay_cache");
    public static final ResourceKey<LootTable> DEIMOS_CACHE = Dungeons.lootTable("deimos", "ice_cache");

    private MarsLoot() {
    }
}
