package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Server → client: a micrometeorite shower over one dimension, {@code ticksLeft} more ticks (0 = over). */
public record MeteorShowerPayload(ResourceKey<Level> dimension, int ticksLeft) implements CustomPacketPayload {
    public static final Type<MeteorShowerPayload> TYPE = new Type<>(Spacebreaker.id("meteor_shower"));
    public static final StreamCodec<ByteBuf, MeteorShowerPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), MeteorShowerPayload::dimension,
            ByteBufCodecs.VAR_INT, MeteorShowerPayload::ticksLeft,
            MeteorShowerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
