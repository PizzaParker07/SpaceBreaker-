package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the wearer started or stopped firing the Maneuvering Unit (server burns the gas). */
public record ThrustPayload(boolean thrusting) implements CustomPacketPayload {
    public static final Type<ThrustPayload> TYPE = new Type<>(Spacebreaker.id("mmu_thrust"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThrustPayload> CODEC =
            ByteBufCodecs.BOOL.map(ThrustPayload::new, ThrustPayload::thrusting).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
