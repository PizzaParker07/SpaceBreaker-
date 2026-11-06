package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.client.hud.ClientSuitState;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the suit's network payloads (mod bus). */
public final class SuitNetwork {
    private SuitNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(SuitStatusPayload.TYPE, SuitStatusPayload.CODEC,
                (payload, ctx) -> ClientSuitState.accept(payload, ctx.player().level().getGameTime()));
        registrar.playToServer(ThrustPayload.TYPE, ThrustPayload.CODEC,
                (payload, ctx) -> LifeSupport.data(ctx.player()).thrusting = payload.thrusting());
    }
}
