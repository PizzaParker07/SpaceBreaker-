package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the wearer started, stopped or turned the thrust of the Maneuvering Unit. The server burns
 * the gas and shows the thruster puffs to everyone; (x, y, z) is the world direction of the push.
 */
public record ThrustPayload(boolean thrusting, float x, float y, float z) implements CustomPacketPayload {
    public static final Type<ThrustPayload> TYPE = new Type<>(Spacebreaker.id("mmu_thrust"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThrustPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ThrustPayload::thrusting,
            ByteBufCodecs.FLOAT, ThrustPayload::x,
            ByteBufCodecs.FLOAT, ThrustPayload::y,
            ByteBufCodecs.FLOAT, ThrustPayload::z,
            ThrustPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
