package com.pizzaparker07.spacebreaker.content.research.net;

import com.pizzaparker07.spacebreaker.content.research.ResearchClientData;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.function.Consumer;

public final class ResearchNet {
    /** Client-side hook for newly learned techs (toasts); set by client/research. */
    public static Consumer<List<ResourceLocation>> onLearnedClient = l -> {
    };

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(KnownTechPayload.TYPE, KnownTechPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            ResearchClientData.setKnown(payload.known());
            if (ctx.player() != null) {
                Tech.setKnownSilently(ctx.player(), payload.known());
            }
            if (!payload.justLearned().isEmpty()) {
                onLearnedClient.accept(payload.justLearned());
            }
        }));
    }

    public static void sync(ServerPlayer player, List<ResourceLocation> justLearned) {
        if (player.connection == null || !player.connection.hasChannel(KnownTechPayload.TYPE)) {
            return; // vanilla client or test player without the channel
        }
        PacketDistributor.sendToPlayer(player, new KnownTechPayload(Tech.known(player), justLearned));
    }

    private ResearchNet() {
    }
}
