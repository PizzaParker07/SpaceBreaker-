package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Shipyard berths: where a spacecraft is built, parks and docks at a station (see docs/ASTRO.md, "Spaceflight
 * layout"). The shipyard module comes in station stage 3; until then {@link #find} finds nothing, and docking only
 * snaps the craft to the berth it is given. The API is fixed now so the spacecraft side can be written against it.
 * <p>
 * Craft limits: hull at most 13 × 13 × 29 blocks (one block of clearance inside the 15 × 15 × 31 shipyard), one
 * docking port at the nose.
 */
public final class Berths {
    /** The largest hull a berth takes, blocks (width, height, length). */
    public static final int MAX_WIDTH = 13, MAX_HEIGHT = 13, MAX_LENGTH = 29;

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

    private Berths() {
    }

    /** The berth at or nearest to a position (within the station it belongs to), if any. */
    public static Optional<Berth> find(Level level, BlockPos pos) {
        return Optional.empty(); // shipyards arrive with station stage 3
    }

    /**
     * Docks a craft: server side, its nose port is placed against the berth's port, pointing back along
     * {@code portFacing}, and it stops moving. Fuel depots and power reach a docked craft (stage 3).
     */
    public static void dock(Entity ship, Berth berth) {
        if (ship.level().isClientSide) {
            return;
        }
        Vec3 front = Vec3.atCenterOf(berth.portPos()).add(Vec3.atLowerCornerOf(berth.portFacing().getNormal()).scale(1.0));
        ship.setYRot(berth.portFacing().getOpposite().toYRot());
        ship.setDeltaMovement(Vec3.ZERO);
        ship.moveTo(front.x, front.y, front.z, ship.getYRot(), ship.getXRot());
    }

    /** Releases a docked craft (no-op until berths keep docking state, stage 3). */
    public static void undock(Entity ship) {
    }
}
