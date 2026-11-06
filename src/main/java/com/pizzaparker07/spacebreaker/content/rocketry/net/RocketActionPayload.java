package com.pizzaparker07.spacebreaker.content.rocketry.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Crew command for the rocket they ride: launch/abort, travel to a destination, emergency return. */
public record RocketActionPayload(int entityId, int action, int arg) implements CustomPacketPayload {
    public static final int LAUNCH = 0;
    public static final int TRAVEL = 1;
    public static final int ABORT = 2;

    public static final Type<RocketActionPayload> TYPE = new Type<>(Spacebreaker.id("rocket_action"));
    public static final StreamCodec<ByteBuf, RocketActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RocketActionPayload::entityId,
            ByteBufCodecs.VAR_INT, RocketActionPayload::action,
            ByteBufCodecs.VAR_INT, RocketActionPayload::arg,
            RocketActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RocketActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().getEntity(payload.entityId()) instanceof RocketEntity rocket && player.getVehicle() == rocket) {
            rocket.handleCrewAction(player, payload.action(), payload.arg());
        }
    }
}
