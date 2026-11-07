package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

/** Server → client: the helmet HUD's way-home target ({@code kind} = Navigation.Kind ordinal of what was found). */
public record NavPayload(int selected, int kind, Optional<BlockPos> target) implements CustomPacketPayload {
    public static final Type<NavPayload> TYPE = new Type<>(Spacebreaker.id("nav_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NavPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NavPayload::selected,
            ByteBufCodecs.VAR_INT, NavPayload::kind,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), NavPayload::target,
            NavPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
