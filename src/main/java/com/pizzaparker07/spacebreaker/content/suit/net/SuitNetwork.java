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
                (payload, ctx) -> {
                    var data = LifeSupport.data(ctx.player());
                    data.thrusting = payload.thrusting();
                    data.thrustDir = new net.minecraft.world.phys.Vec3(payload.x(), payload.y(), payload.z());
                    if (data.thrustDir.lengthSqr() > 1.01) {
                        data.thrustDir = data.thrustDir.normalize(); // never trust a client vector
                    }
                });
        registrar.playToClient(TetherPayload.TYPE, TetherPayload.CODEC,
                (payload, ctx) -> com.pizzaparker07.spacebreaker.client.suit.TetherClient.accept(payload));
    }
}
