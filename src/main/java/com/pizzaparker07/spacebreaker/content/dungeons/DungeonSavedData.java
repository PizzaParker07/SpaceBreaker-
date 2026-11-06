package com.pizzaparker07.spacebreaker.content.dungeons;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Per-dimension record of dungeon instances that have been discovered (guardians already spawned). */
public final class DungeonSavedData extends SavedData {
    private static final String NAME = "spacebreaker_dungeons";
    private final Set<String> discovered = new HashSet<>();

    public static DungeonSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(DungeonSavedData::new, DungeonSavedData::load), NAME);
    }

    /** @return true if this instance was not discovered before */
    public boolean markDiscovered(String key) {
        if (discovered.add(key)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isDiscovered(String key) {
        return discovered.contains(key);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        discovered.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put("discovered", list);
        return tag;
    }

    private static DungeonSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonSavedData d = new DungeonSavedData();
        ListTag list = tag.getList("discovered", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            d.discovered.add(list.getString(i));
        }
        return d;
    }
}
