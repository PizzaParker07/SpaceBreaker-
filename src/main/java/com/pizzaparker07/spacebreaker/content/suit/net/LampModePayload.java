package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: switch the worn helmet's lamps between wide and long beam. */
public record LampModePayload() implements CustomPacketPayload {
    public static final LampModePayload INSTANCE = new LampModePayload();
    public static final Type<LampModePayload> TYPE = new Type<>(Spacebreaker.id("lamp_mode"));
    public static final StreamCodec<ByteBuf, LampModePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
