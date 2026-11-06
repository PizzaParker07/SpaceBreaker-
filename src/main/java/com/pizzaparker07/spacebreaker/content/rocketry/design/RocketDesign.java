package com.pizzaparker07.spacebreaker.content.rocketry.design;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A rocket as built in the assembler: rocket part ids from the bottom of the first stage to the top of
 * the payload. Decouplers and interstages close a stage; radial parts (fins, legs, RCS) mount on the axial
 * part listed just before them. Stored on the rocket item and on the rocket entity.
 */
public record RocketDesign(List<ResourceLocation> parts) {
    public static final RocketDesign EMPTY = new RocketDesign(List.of());
    public static final Codec<RocketDesign> CODEC = ResourceLocation.CODEC.listOf().xmap(RocketDesign::new, RocketDesign::parts);
    public static final StreamCodec<ByteBuf, RocketDesign> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(64)).map(RocketDesign::new, RocketDesign::parts);

    public RocketDesign {
        parts = List.copyOf(parts);
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public RocketDesign subList(int from, int to) {
        return new RocketDesign(parts.subList(from, to));
    }
}
