package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shipyard berths: where a spacecraft is built, parks and docks at a station (see docs/ASTRO.md, "Spaceflight
 * layout"). Every shipyard module adds one berth; the list is kept per dimension so it is known even when the station's
 * chunks are not loaded.
 * <p>
 * Craft limits: hull at most 13 × 13 × 29 blocks (one block of clearance inside the 15 × 15 × 31 shipyard), one
 * docking port at the nose.
 */
public final class Berths extends SavedData {
    private static final String NAME = "spacebreaker_berths";
    /** The largest hull a berth takes, blocks (width, height, length). */
    public static final int MAX_WIDTH = 13, MAX_HEIGHT = 13, MAX_LENGTH = 29;
    /** How far outside a berth a position still finds it, blocks. */
    public static final double REACH = 16;

    /**
     * One berth.
     *
     * @param bounds     the space a craft may take, world blocks
     * @param portPos    the berth's docking port (the craft's nose port meets it)
     * @param portFacing which way the port faces, out of the station into the berth
     * @param stationId  the station it belongs to
     */
    public record Berth(AABB bounds, BlockPos portPos, Direction portFacing, UUID stationId) {
    }

    private final List<Berth> berths = new ArrayList<>();
    /** Docked craft (entity UUID) and the port of the berth they are in. */
    private final Map<UUID, BlockPos> docked = new HashMap<>();

    public static Berths get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(Berths::new, Berths::load), NAME);
    }

    private static Berths load(CompoundTag tag, HolderLookup.Provider registries) {
        Berths b = new Berths();
        ListTag list = tag.getList("berths", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            AABB box = new AABB(t.getDouble("x0"), t.getDouble("y0"), t.getDouble("z0"), t.getDouble("x1"), t.getDouble("y1"), t.getDouble("z1"));
            b.berths.add(new Berth(box, BlockPos.of(t.getLong("port")), Direction.from3DDataValue(t.getInt("facing")), t.getUUID("station")));
        }
        ListTag ships = tag.getList("docked", 10);
        for (int i = 0; i < ships.size(); i++) {
            CompoundTag t = ships.getCompound(i);
            b.docked.put(t.getUUID("ship"), BlockPos.of(t.getLong("port")));
        }
        return b;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Berth b : berths) {
            CompoundTag t = new CompoundTag();
            t.putDouble("x0", b.bounds().minX);
            t.putDouble("y0", b.bounds().minY);
            t.putDouble("z0", b.bounds().minZ);
            t.putDouble("x1", b.bounds().maxX);
            t.putDouble("y1", b.bounds().maxY);
            t.putDouble("z1", b.bounds().maxZ);
            t.putLong("port", b.portPos().asLong());
            t.putInt("facing", b.portFacing().get3DDataValue());
            t.putUUID("station", b.stationId());
            list.add(t);
        }
        tag.put("berths", list);
        ListTag ships = new ListTag();
        docked.forEach((ship, port) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("ship", ship);
            t.putLong("port", port.asLong());
            ships.add(t);
        });
        tag.put("docked", ships);
        return tag;
    }

    void add(Berth b) {
        berths.removeIf(o -> o.portPos().equals(b.portPos()));
        berths.add(b);
        setDirty();
    }

    /** Forgets the berth whose port this was (the shipyard was taken apart). */
    public void removePort(BlockPos port) {
        if (berths.removeIf(o -> o.portPos().equals(port))) {
            docked.values().removeIf(port::equals);
            setDirty();
        }
    }

    public List<Berth> all() {
        return List.copyOf(berths);
    }

    /** The berth at or nearest to a position (within {@link #REACH} blocks of it), if any. */
    public static Optional<Berth> find(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        Vec3 p = Vec3.atCenterOf(pos);
        Berth best = null;
        double bestD = Double.MAX_VALUE;
        for (Berth b : get(server).berths) {
            if (b.bounds().inflate(REACH).contains(p)) {
                double d = b.bounds().getCenter().distanceToSqr(p);
                if (d < bestD) {
                    bestD = d;
                    best = b;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Docks a craft: server side, its nose is placed one block out from the berth's port, the craft turned to face the
     * port (back along {@code portFacing}), and it stops moving. It counts as docked until {@link #undock}.
     */
    public static void dock(Entity ship, Berth berth) {
        if (!(ship.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 front = Vec3.atCenterOf(berth.portPos()).add(Vec3.atLowerCornerOf(berth.portFacing().getNormal()));
        ship.setYRot(berth.portFacing().getOpposite().toYRot());
        ship.setDeltaMovement(Vec3.ZERO);
        ship.moveTo(front.x, front.y, front.z, ship.getYRot(), ship.getXRot());
        Berths b = get(server);
        b.docked.put(ship.getUUID(), berth.portPos().immutable());
        b.setDirty();
    }

    /** Releases a docked craft. */
    public static void undock(Entity ship) {
        if (ship.level() instanceof ServerLevel server) {
            Berths b = get(server);
            if (b.docked.remove(ship.getUUID()) != null) {
                b.setDirty();
            }
        }
    }

    /** Whether a craft is docked at a berth. */
    public static boolean isDocked(Entity ship) {
        return ship.level() instanceof ServerLevel server && get(server).docked.containsKey(ship.getUUID());
    }
}
