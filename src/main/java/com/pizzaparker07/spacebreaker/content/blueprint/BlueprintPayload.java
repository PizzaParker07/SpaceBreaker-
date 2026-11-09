package com.pizzaparker07.spacebreaker.content.blueprint;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: the holder of a projector chose a structure ({@code action = SELECT}) or turned the
 * projection ({@code ROTATE_CW / ROTATE_CCW}) or asked to build the placed projection ({@code BUILD}). The server
 * changes the item and does the building, so everything stays authoritative.
 */
public record BlueprintPayload(int action, ResourceLocation blueprint) implements CustomPacketPayload {
    public static final int SELECT = 0, ROTATE_CW = 1, ROTATE_CCW = 2, BUILD = 3;
    public static final Type<BlueprintPayload> TYPE = new Type<>(Spacebreaker.id("blueprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BlueprintPayload::action, ResourceLocation.STREAM_CODEC, BlueprintPayload::blueprint, BlueprintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
