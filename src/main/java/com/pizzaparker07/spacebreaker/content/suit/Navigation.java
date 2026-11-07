package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * "Way home" for the helmet HUD: the nearest of the wearer's own rocket, their own landing beacon or a
 * pressurised base, and whether the oxygen left lasts the walk back. Only the wearer's own rocket and beacons
 * count (multiplayer); pressurised rooms are shelters for anyone.
 */
public final class Navigation {
    /** What the HUD points at. AUTO = the nearest of the others. */
    public enum Kind {
        AUTO, ROCKET, BEACON, BASE;

        public Kind next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String key() {
            return "nav.spacebreaker." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Return status: enough oxygen, turn back soon (yellow), or not enough to make it (red). */
    public enum Status { OK, TURN_BACK, CRITICAL }

    /** A suited walk in blocks per second: careful on uneven ground, slower still when every step floats. */
    public static final double WALK_SPEED = 3.0, MICRO_G_WALK_SPEED = 1.5;
    /** Turn back while the oxygen is still this many times the walk home. */
    public static final double MARGIN = 1.5;
    /** Rockets further than this are not searched for (loaded entities only). */
    public static final double ROCKET_RANGE = 512;

    public record Target(Kind kind, BlockPos pos) {
    }

    private Navigation() {
    }

    /** Ticks a suited walk of {@code distance} blocks takes at this gravity. */
    public static double walkTicks(double distance, double gravity) {
        double speed = ZeroG.microGravity(gravity) ? MICRO_G_WALK_SPEED : WALK_SPEED;
        return distance / speed * 20.0;
    }

    /** Does the oxygen left ({@code oxygenTicks} of breathing) last the walk home? */
    public static Status status(double distance, double gravity, int oxygenTicks) {
        double need = walkTicks(distance, gravity);
        if (need > oxygenTicks) {
            return Status.CRITICAL;
        }
        return need * MARGIN > oxygenTicks ? Status.TURN_BACK : Status.OK;
    }

    /** Direction to the target relative to where the wearer looks, in degrees (0 = straight ahead, + = right). */
    public static float relativeYaw(Vec3 from, float yaw, BlockPos to) {
        double dx = to.getX() + 0.5 - from.x;
        double dz = to.getZ() + 0.5 - from.z;
        float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F; // Minecraft yaw: 0 = +z (south)
        return Mth.wrapDegrees(bearing - yaw);
    }

    /** All candidates of one kind in this level. */
    static List<Target> candidates(ServerLevel level, Player player, Kind kind) {
        List<Target> out = new ArrayList<>();
        if (kind == Kind.AUTO || kind == Kind.ROCKET) {
            for (RocketEntity r : level.getEntitiesOfClass(RocketEntity.class, new AABB(player.blockPosition()).inflate(ROCKET_RANGE),
                    r -> player.getUUID().equals(r.owner()))) {
                out.add(new Target(Kind.ROCKET, r.blockPosition()));
            }
        }
        if (kind == Kind.AUTO || kind == Kind.BEACON) {
            String name = player.getGameProfile().getName();
            for (var port : LandingBeacons.get(level).all()) {
                if (name.equals(port.owner())) {
                    out.add(new Target(Kind.BEACON, port.pos()));
                }
            }
        }
        if (kind == Kind.AUTO || kind == Kind.BASE) {
            for (BlockPos p : SealedRooms.activeDistributors(level)) {
                out.add(new Target(Kind.BASE, p));
            }
        }
        return out;
    }

    /** The nearest target of the chosen kind, if any. */
    public static Optional<Target> find(ServerLevel level, Player player, Kind kind) {
        Vec3 at = player.position();
        return candidates(level, player, kind).stream().min(Comparator.comparingDouble(t -> Vec3.atCenterOf(t.pos()).distanceToSqr(at)));
    }
}
