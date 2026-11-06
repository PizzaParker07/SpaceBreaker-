package com.pizzaparker07.spacebreaker.content.research;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-wide research state: technologies shared with each scoreboard team (so members who were offline learn them
 * on login) and technologies finished by a lab while its researcher was offline.
 */
public final class ResearchSavedData extends SavedData {
    private static final String NAME = "spacebreaker_research";

    private final Map<String, Set<ResourceLocation>> teams = new HashMap<>();
    private final Map<UUID, Set<ResourceLocation>> pending = new HashMap<>();

    public static ResearchSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ResearchSavedData::new, ResearchSavedData::load), NAME);
    }

    public void addTeamTech(String team, ResourceLocation tech) {
        if (teams.computeIfAbsent(team, k -> new HashSet<>()).add(tech)) {
            setDirty();
        }
    }

    public Set<ResourceLocation> teamTech(String team) {
        return teams.getOrDefault(team, Set.of());
    }

    public void addPending(UUID player, ResourceLocation tech) {
        if (pending.computeIfAbsent(player, k -> new HashSet<>()).add(tech)) {
            setDirty();
        }
    }

    /** Removes and returns what was waiting for this player. */
    public Set<ResourceLocation> takePending(UUID player) {
        Set<ResourceLocation> out = pending.remove(player);
        if (out != null) {
            setDirty();
            return out;
        }
        return Set.of();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        teams.forEach((name, set) -> t.put(name, write(set)));
        tag.put("teams", t);
        CompoundTag p = new CompoundTag();
        pending.forEach((uuid, set) -> p.put(uuid.toString(), write(set)));
        tag.put("pending", p);
        return tag;
    }

    private static ResearchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ResearchSavedData data = new ResearchSavedData();
        CompoundTag t = tag.getCompound("teams");
        for (String name : t.getAllKeys()) {
            data.teams.put(name, read(t.getList(name, Tag.TAG_STRING)));
        }
        CompoundTag p = tag.getCompound("pending");
        for (String key : p.getAllKeys()) {
            try {
                data.pending.put(UUID.fromString(key), read(p.getList(key, Tag.TAG_STRING)));
            } catch (IllegalArgumentException ignored) {
                // corrupt entry: skip
            }
        }
        return data;
    }

    private static ListTag write(Set<ResourceLocation> set) {
        ListTag list = new ListTag();
        set.stream().sorted().forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    private static Set<ResourceLocation> read(ListTag list) {
        Set<ResourceLocation> set = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                set.add(id);
            }
        }
        return set;
    }
}
