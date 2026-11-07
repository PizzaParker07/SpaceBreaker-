package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The docking ports of one dimension, so a rocket arriving in orbit knows where the station is even when its
 * chunks are not loaded. Ports add themselves when loaded and leave when broken.
 */
public class DockingPorts extends SavedData {
    private static final String NAME = "spacebreaker_docking_ports";

    /** @param owner name of the player who built it ("" if unknown) */
    public record Port(BlockPos pos, Direction facing, String owner) {
    }

    private final Map<Long, Port> ports = new LinkedHashMap<>();

    public static DockingPorts get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(DockingPorts::new, DockingPorts::load), NAME);
    }

    private static DockingPorts load(CompoundTag tag, HolderLookup.Provider registries) {
        DockingPorts d = new DockingPorts();
        ListTag list = tag.getList("ports", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockPos pos = BlockPos.of(t.getLong("pos"));
            d.ports.put(pos.asLong(), new Port(pos, Direction.from3DDataValue(t.getInt("facing")), t.getString("owner")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Port p : ports.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", p.pos().asLong());
            t.putInt("facing", p.facing().get3DDataValue());
            t.putString("owner", p.owner());
            list.add(t);
        }
        tag.put("ports", list);
        return tag;
    }

    public void add(BlockPos pos, Direction facing) {
        Port old = ports.get(pos.asLong());
        if (old == null || old.facing() != facing) {
            ports.put(pos.asLong(), new Port(pos.immutable(), facing, old == null ? "" : old.owner()));
            changed();
        }
    }

    /** Who built the port (the player placing it, or using the habitat kit). */
    public void setOwner(BlockPos pos, String owner) {
        Port old = ports.get(pos.asLong());
        if (old != null && !old.owner().equals(owner)) {
            ports.put(pos.asLong(), new Port(old.pos(), old.facing(), owner));
            changed();
        }
    }

    public void remove(BlockPos pos) {
        if (ports.remove(pos.asLong()) != null) {
            changed();
        }
    }

    private void changed() {
        setDirty();
        Stations.markDirty();
    }

    public java.util.Collection<Port> all() {
        return java.util.Collections.unmodifiableCollection(ports.values());
    }

    /** The port nearest to {@code from} among those that pass {@code filter}. */
    public Optional<Port> nearest(Vec3 from, Predicate<Port> filter) {
        return ports.values().stream().filter(filter).min(Comparator.comparingDouble(p -> p.pos().getCenter().distanceToSqr(from)));
    }

    public int size() {
        return ports.size();
    }
}
