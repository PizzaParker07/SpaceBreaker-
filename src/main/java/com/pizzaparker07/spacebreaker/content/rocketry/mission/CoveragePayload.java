package com.pizzaparker07.spacebreaker.content.rocketry.mission;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;

/** Server → client: planet systems with a relay and worlds that have been mapped (see {@link Coverage}). */
public record CoveragePayload(List<ResourceLocation> relays, List<ResourceLocation> observed) implements CustomPacketPayload {
    public static final Type<CoveragePayload> TYPE = new Type<>(Spacebreaker.id("satellite_coverage"));
    public static final StreamCodec<ByteBuf, CoveragePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256)), CoveragePayload::relays,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256)), CoveragePayload::observed,
            CoveragePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoveragePayload payload, IPayloadContext context) {
        Coverage.set(new HashSet<>(payload.relays()), new HashSet<>(payload.observed()));
    }

    public static CoveragePayload of(MinecraftServer server) {
        SatelliteData d = SatelliteData.get(server);
        return new CoveragePayload(List.copyOf(d.relaySystems()), List.copyOf(d.observed()));
    }

    public static void sendToAll(MinecraftServer server) {
        CoveragePayload p = of(server);
        for (var player : server.getPlayerList().getPlayers()) {
            send(player, p);
        }
    }

    /** Sends to one player if their client knows the payload (fake and vanilla clients do not). */
    public static void send(net.minecraft.server.level.ServerPlayer player, CoveragePayload p) {
        if (player.connection != null && player.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(player, p);
        }
    }
}
