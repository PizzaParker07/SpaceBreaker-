package com.pizzaparker07.spacebreaker.content.rocketry.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** The nearby stars, sent to a client that opened the star map's star view. */
public record NearbyStarsPayload(List<NearbyStars.Star> stars) implements CustomPacketPayload {
    public static final Type<NearbyStarsPayload> TYPE = new Type<>(Spacebreaker.id("nearby_stars"));
    public static final StreamCodec<ByteBuf, NearbyStarsPayload> STREAM_CODEC = NearbyStars.Star.STREAM_CODEC
            .apply(ByteBufCodecs.list(NearbyStars.MAX_STARS)).map(NearbyStarsPayload::new, NearbyStarsPayload::stars);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NearbyStarsPayload payload, IPayloadContext context) {
        NearbyStars.receive(payload.stars());
    }

    /** Client → server: "send me the nearby stars". */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Spacebreaker.id("nearby_stars_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request request, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                context.reply(new NearbyStarsPayload(NearbyStars.forServer(player.server)));
            }
        }
    }
}
