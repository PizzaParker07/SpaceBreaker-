package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The station modules built in one dimension (where and which), so the star map can say how big a station is and
 * whether it has a shipyard without its chunks being loaded.
 */
public final class StationParts extends SavedData {
    private static final String NAME = "spacebreaker_station_parts";
    private final Map<Long, StationModule> parts = new LinkedHashMap<>();

    public static StationParts get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(StationParts::new, StationParts::load), NAME);
    }

    private static StationParts load(CompoundTag tag, HolderLookup.Provider registries) {
        StationParts p = new StationParts();
        ListTag list = tag.getList("parts", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                p.parts.put(t.getLong("pos"), StationModule.valueOf(t.getString("type")));
            } catch (IllegalArgumentException ignored) {
                // a module type that no longer exists
            }
        }
        return p;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        parts.forEach((pos, type) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", pos);
            t.putString("type", type.name());
            list.add(t);
        });
        tag.put("parts", list);
        return tag;
    }

    void add(BlockPos pos, StationModule type) {
        parts.put(pos.asLong(), type);
        setDirty();
        Stations.markDirty();
    }

    /** Modules within {@code reach} blocks of a position: how many, and whether one is a shipyard. */
    public int[] near(BlockPos pos, int reach) {
        int n = 0, yard = 0;
        for (Map.Entry<Long, StationModule> e : parts.entrySet()) {
            if (BlockPos.of(e.getKey()).closerThan(pos, reach)) {
                n++;
                if (e.getValue() == StationModule.SHIPYARD) {
                    yard = 1;
                }
            }
        }
        return new int[]{n, yard};
    }
}
