package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The space stations the star map shows: the docking ports of every orbit, grouped into stations (ports within
 * {@link #CLUSTER} blocks of each other are one station). Worked out on the server, sent to every client when a
 * port is built or broken and when a player joins.
 */
public final class Stations {
    /** Ports this close belong to the same station. */
    public static final int CLUSTER = 48;

    /**
     * One station in orbit (or, with {@code surface}, one base on the ground): the body, who built it (first known
     * builder) and how many docking ports (landing beacons for a base) it has.
     */
    public record Station(ResourceLocation body, String owner, int ports, boolean surface) {
        public Station(ResourceLocation body, String owner, int ports) {
            this(body, owner, ports, false);
        }

        public static final StreamCodec<ByteBuf, Station> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Station::body,
                ByteBufCodecs.STRING_UTF8, Station::owner,
                ByteBufCodecs.VAR_INT, Station::ports,
                ByteBufCodecs.BOOL, Station::surface,
                Station::new);
    }

    private static volatile boolean dirty;
    private static volatile List<Station> received = List.of();

    private Stations() {
    }

    static void markDirty() {
        dirty = true;
    }

    /** Every station in every orbit of this server. */
    public static List<Station> of(MinecraftServer server) {
        List<Station> out = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            LandingBeacons beacons = LandingBeacons.get(level);
            if (beacons.size() > 0) {
                out.addAll(bases(beacons.all(), pos -> Locations.at(level.dimension(), null)));
            }
            DockingPorts ports = DockingPorts.get(level);
            if (ports.size() == 0) {
                continue;
            }
            out.addAll(group(ports.all(), pos -> {
                ResourceLocation shared = Location.Orbits.SHARED.equals(level.dimension()) ? Locations.bodyForSharedX(pos.getX()).orElse(null) : null;
                return Locations.at(level.dimension(), shared);
            }));
        }
        return out;
    }

    /** Groups landing beacons into surface bases (pure, for tests): only surfaces count. */
    public static List<Station> bases(java.util.Collection<DockingPorts.Port> beacons, java.util.function.Function<BlockPos, java.util.Optional<Location>> where) {
        List<Station> out = new ArrayList<>();
        for (List<DockingPorts.Port> g : clusters(beacons)) {
            Location loc = where.apply(g.get(0).pos()).orElse(null);
            if (loc == null || loc.orbit()) {
                continue;
            }
            String owner = g.stream().map(DockingPorts.Port::owner).filter(o -> !o.isEmpty()).findFirst().orElse("");
            out.add(new Station(loc.body(), owner, g.size(), true));
        }
        return out;
    }

    private static List<List<DockingPorts.Port>> clusters(java.util.Collection<DockingPorts.Port> ports) {
        List<List<DockingPorts.Port>> groups = new ArrayList<>();
        for (DockingPorts.Port p : ports) {
            List<DockingPorts.Port> home = null;
            for (List<DockingPorts.Port> g : groups) {
                if (g.stream().anyMatch(q -> q.pos().closerThan(p.pos(), CLUSTER))) {
                    home = g;
                    break;
                }
            }
            if (home == null) {
                home = new ArrayList<>();
                groups.add(home);
            }
            home.add(p);
        }
        return groups;
    }

    /** Groups ports into stations (pure, for tests): {@code where} tells the place of a port; only orbits count. */
    public static List<Station> group(java.util.Collection<DockingPorts.Port> ports, java.util.function.Function<BlockPos, java.util.Optional<Location>> where) {
        List<List<DockingPorts.Port>> groups = clusters(ports);
        List<Station> out = new ArrayList<>();
        for (List<DockingPorts.Port> g : groups) {
            Location loc = where.apply(g.get(0).pos()).orElse(null);
            if (loc == null || !loc.orbit()) {
                continue;
            }
            String owner = g.stream().map(DockingPorts.Port::owner).filter(o -> !o.isEmpty()).findFirst().orElse("");
            out.add(new Station(loc.body(), owner, g.size()));
        }
        return out;
    }

    /** Client: the stations last received. */
    public static List<Station> received() {
        return received;
    }

    /** Client: the stations around this body. */
    public static List<Station> around(ResourceLocation body) {
        return received.stream().filter(s -> s.body().equals(body) && !s.surface()).toList();
    }

    /** Client: the bases on this body. */
    public static List<Station> basesOn(ResourceLocation body) {
        return received.stream().filter(s -> s.body().equals(body) && s.surface()).toList();
    }

    /** Server tick: tell everyone when the stations changed. */
    public static void tick(MinecraftServer server) {
        if (dirty) {
            dirty = false;
            Payload p = new Payload(of(server));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, p);
            }
        }
    }

    public static void send(ServerPlayer player, Payload p) {
        if (player.connection != null && player.connection.hasChannel(Payload.TYPE)) {
            PacketDistributor.sendToPlayer(player, p);
        }
    }

    /** Server → client: the list of stations. */
    public record Payload(List<Station> stations) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Spacebreaker.id("stations"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = Station.STREAM_CODEC.apply(ByteBufCodecs.list(1024))
                .map(Payload::new, Payload::stations);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            received = List.copyOf(payload.stations());
        }
    }

    /**
     * A station's orbit, for the space scenes (docs/ASTRO.md): circular, in the equator plane, at this altitude above
     * its body; {@code phase} says where on the circle it is at time zero (radians).
     */
    public record Orbit(double altitudeKm, double inclinationDeg, double phase) {
    }

    /** Default altitude of a station's orbit, km. */
    public static final double ORBIT_ALTITUDE_KM = 400;

    /** The orbit of a station (fixed per station, its phase from the station's id). */
    public static Orbit orbit(java.util.UUID stationId) {
        double phase = (stationId.getLeastSignificantBits() & 0xFFFFL) / 65536.0 * Math.PI * 2;
        return new Orbit(ORBIT_ALTITUDE_KM, 0, phase);
    }
}
