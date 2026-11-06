package com.pizzaparker07.spacebreaker.content.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * What a Hologram Projector shows, stored on the item (so only its holder sees it, and it is saved with it).
 *
 * @param blueprint multiblock id
 * @param anchor    controller position of the projection, empty = not placed yet
 * @param facing    which way the structure's north side points
 * @param layer     shown layer, -1 = all layers
 */
public record Hologram(ResourceLocation blueprint, Optional<BlockPos> anchor, Direction facing, int layer) {
    public static final Codec<Hologram> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("blueprint").forGetter(Hologram::blueprint),
            BlockPos.CODEC.optionalFieldOf("anchor").forGetter(Hologram::anchor),
            Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(Hologram::facing),
            Codec.INT.optionalFieldOf("layer", -1).forGetter(Hologram::layer)
    ).apply(i, Hologram::new));
    public static final StreamCodec<ByteBuf, Hologram> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Hologram::blueprint,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Hologram::anchor,
            Direction.STREAM_CODEC, Hologram::facing,
            ByteBufCodecs.VAR_INT, h -> h.layer + 1,
            (b, a, f, l) -> new Hologram(b, a, f, l - 1));

    public static Hologram of(ResourceLocation blueprint) {
        return new Hologram(blueprint, Optional.empty(), Direction.NORTH, -1);
    }

    public Hologram withAnchor(BlockPos pos, Direction facing) {
        return new Hologram(blueprint, Optional.of(pos.immutable()), facing, layer);
    }

    public Hologram cleared() {
        return new Hologram(blueprint, Optional.empty(), facing, -1);
    }

    public Hologram rotated(int steps) {
        Direction d = facing;
        for (int i = 0; i < Math.floorMod(steps, 4); i++) {
            d = d.getClockWise();
        }
        return new Hologram(blueprint, anchor, d, layer);
    }

    /** Next layer: all -> 0 -> 1 -> ... -> height-1 -> all. */
    public Hologram nextLayer(int height) {
        int next = layer + 1 >= height ? -1 : layer + 1;
        return new Hologram(blueprint, anchor, facing, next);
    }
}
