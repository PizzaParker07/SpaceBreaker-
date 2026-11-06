package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Server → client: the clock of one planet dimension. */
public record PlanetTimePayload(ResourceKey<Level> dimension, long dayTicks, long offset) implements CustomPacketPayload {
    public static final Type<PlanetTimePayload> TYPE = new Type<>(Spacebreaker.id("planet_time"));
    public static final StreamCodec<ByteBuf, PlanetTimePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), PlanetTimePayload::dimension,
            ByteBufCodecs.VAR_LONG, PlanetTimePayload::dayTicks,
            ByteBufCodecs.VAR_LONG, PlanetTimePayload::offset,
            PlanetTimePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
