package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The landing beacons of one dimension, so a rocket coming down from orbit can steer for a base even when its
 * chunks are not loaded. Beacons add themselves when placed and leave when broken.
 */
public class LandingBeacons extends SavedData {
    private static final String NAME = "spacebreaker_landing_beacons";

    /** Beacon position → name of the player who placed it ("" if unknown). */
    private final Map<Long, String> beacons = new LinkedHashMap<>();

    public static LandingBeacons get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(LandingBeacons::new, LandingBeacons::load), NAME);
    }

    private static LandingBeacons load(CompoundTag tag, HolderLookup.Provider registries) {
        LandingBeacons b = new LandingBeacons();
        long[] pos = tag.getLongArray("beacons");
        var owners = tag.getList("owners", 8);
        for (int i = 0; i < pos.length; i++) {
            b.beacons.put(pos[i], i < owners.size() ? owners.getString(i) : "");
        }
        return b;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("beacons", new LongArrayTag(beacons.keySet().stream().mapToLong(Long::longValue).toArray()));
        var owners = new net.minecraft.nbt.ListTag();
        beacons.values().forEach(o -> owners.add(net.minecraft.nbt.StringTag.valueOf(o)));
        tag.put("owners", owners);
        return tag;
    }

    public void add(BlockPos pos) {
        if (!beacons.containsKey(pos.asLong())) {
            beacons.put(pos.asLong(), "");
            changed();
        }
    }

    public void setOwner(BlockPos pos, String owner) {
        if (beacons.containsKey(pos.asLong()) && !owner.equals(beacons.get(pos.asLong()))) {
            beacons.put(pos.asLong(), owner);
            changed();
        }
    }

    public void remove(BlockPos pos) {
        if (beacons.remove(pos.asLong()) != null) {
            changed();
        }
    }

    private void changed() {
        setDirty();
        Stations.markDirty();
    }

    /** Every beacon as a port-like entry (position and builder), for grouping into bases. */
    public java.util.List<DockingPorts.Port> all() {
        return beacons.entrySet().stream().map(e -> new DockingPorts.Port(BlockPos.of(e.getKey()), net.minecraft.core.Direction.UP, e.getValue())).toList();
    }

    /** The beacon nearest to {@code from} (horizontally). */
    public Optional<BlockPos> nearest(Vec3 from) {
        return beacons.keySet().stream().map(BlockPos::of)
                .min(Comparator.comparingDouble(p -> Math.pow(p.getX() + 0.5 - from.x, 2) + Math.pow(p.getZ() + 0.5 - from.z, 2)));
    }

    public int size() {
        return beacons.size();
    }
}
