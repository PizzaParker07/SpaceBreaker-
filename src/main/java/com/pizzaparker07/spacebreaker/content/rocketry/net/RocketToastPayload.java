package com.pizzaparker07.spacebreaker.content.rocketry.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/** Server to client: a short toast (a title and one line), e.g. when a rocket reaches orbit. */
public record RocketToastPayload(Component title, Component line) implements CustomPacketPayload {
    public static final Type<RocketToastPayload> TYPE = new Type<>(Spacebreaker.id("rocket_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketToastPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, RocketToastPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, RocketToastPayload::line,
            RocketToastPayload::new);

    /** Set by the client: shows the toast. */
    public static volatile Consumer<RocketToastPayload> onClient = p -> {
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RocketToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> onClient.accept(payload));
    }

    /** Toast to this player, or a chat line when their client cannot show it. */
    public static void send(ServerPlayer player, Component title, Component line) {
        if (player.connection != null && player.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(player, new RocketToastPayload(title, line));
        } else {
            player.displayClientMessage(title.copy().append(": ").append(line), false);
        }
    }
}
