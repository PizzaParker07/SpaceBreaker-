package com.pizzaparker07.spacebreaker.content.dungeons;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java hooks for dungeons, for things JSON cannot express (custom guardians, boss bars, traps...).
 * <pre>
 *   DungeonHooks.onFirstDiscovery(Spacebreaker.id("moon_crash_site"), (level, start, player, def) -> spawnMyCreatures(...));
 *   DungeonHooks.onEnter(Spacebreaker.id("moon_crash_site"), (level, start, player, def) -> ...);
 * </pre>
 * First-discovery hooks run once per generated structure (any player); enter hooks run each time a player walks in.
 * Use {@link #ANY} to hook every dungeon.
 */
public final class DungeonHooks {
    public static final ResourceLocation ANY = ResourceLocation.fromNamespaceAndPath("spacebreaker", "any");

    @FunctionalInterface
    public interface Hook {
        void run(ServerLevel level, StructureStart start, ServerPlayer player, DungeonDef def);
    }

    private static final Map<ResourceLocation, List<Hook>> FIRST = new HashMap<>();
    private static final Map<ResourceLocation, List<Hook>> ENTER = new HashMap<>();

    public static synchronized void onFirstDiscovery(ResourceLocation dungeon, Hook hook) {
        FIRST.computeIfAbsent(dungeon, k -> new ArrayList<>()).add(hook);
    }

    public static synchronized void onEnter(ResourceLocation dungeon, Hook hook) {
        ENTER.computeIfAbsent(dungeon, k -> new ArrayList<>()).add(hook);
    }

    static void fireFirst(ResourceLocation id, ServerLevel level, StructureStart start, ServerPlayer player, DungeonDef def) {
        fire(FIRST, id, level, start, player, def);
    }

    static void fireEnter(ResourceLocation id, ServerLevel level, StructureStart start, ServerPlayer player, DungeonDef def) {
        fire(ENTER, id, level, start, player, def);
    }

    private static void fire(Map<ResourceLocation, List<Hook>> map, ResourceLocation id, ServerLevel level, StructureStart start,
                             ServerPlayer player, DungeonDef def) {
        List<Hook> hooks = new ArrayList<>();
        synchronized (DungeonHooks.class) {
            hooks.addAll(map.getOrDefault(id, List.of()));
            hooks.addAll(map.getOrDefault(ANY, List.of()));
        }
        for (Hook h : hooks) {
            h.run(level, start, player, def);
        }
    }

    private DungeonHooks() {
    }
}
