package com.pizzaparker07.spacebreaker.content.research.net;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server → client: the full set of technologies the player knows, plus those learned just now (for the toast).
 * Sent on login, respawn, dimension change and whenever the set changes.
 */
public record KnownTechPayload(Set<ResourceLocation> known, List<ResourceLocation> justLearned) implements CustomPacketPayload {
    public static final Type<KnownTechPayload> TYPE = new Type<>(Spacebreaker.id("known_tech"));

    public static final StreamCodec<ByteBuf, KnownTechPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), KnownTechPayload::known,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), KnownTechPayload::justLearned,
            KnownTechPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
