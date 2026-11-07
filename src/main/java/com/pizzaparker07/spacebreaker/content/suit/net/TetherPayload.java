package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

/**
 * Server → client: the safety tether of a player (entity id) is clipped to a block, to an entity (its network id), or
 * loose (both empty).
 */
public record TetherPayload(int entityId, Optional<BlockPos> anchor, Optional<Integer> anchorEntity) implements CustomPacketPayload {
    public static final Type<TetherPayload> TYPE = new Type<>(Spacebreaker.id("tether"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TetherPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TetherPayload::entityId,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), TetherPayload::anchor,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), TetherPayload::anchorEntity,
            TetherPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
