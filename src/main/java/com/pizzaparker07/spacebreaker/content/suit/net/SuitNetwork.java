package com.pizzaparker07.spacebreaker.content.suit.net;

import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the suit's network payloads (mod bus). */
public final class SuitNetwork {
    private SuitNetwork() {
    }

    // Client-side handlers of the play-to-client payloads, set by client.suit.SuitClient at client setup. Common code
    // names no client class, so nothing client-only can be resolved on a dedicated server (there these stay no-ops).
    public static java.util.function.BiConsumer<SuitStatusPayload, Long> onSuitStatus = (payload, gameTime) -> { };
    public static java.util.function.Consumer<SensorReadingPayload> onSensorReading = payload -> { };
    public static java.util.function.Consumer<NavPayload> onNav = payload -> { };
    public static java.util.function.Consumer<TetherPayload> onTether = payload -> { };

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(SuitStatusPayload.TYPE, SuitStatusPayload.CODEC,
                (payload, ctx) -> onSuitStatus.accept(payload, ctx.player().level().getGameTime()));
        registrar.playToServer(ThrustPayload.TYPE, ThrustPayload.CODEC,
                (payload, ctx) -> {
                    var data = LifeSupport.data(ctx.player());
                    data.thrusting = payload.thrusting();
                    data.thrustDir = new net.minecraft.world.phys.Vec3(payload.x(), payload.y(), payload.z());
                    if (data.thrustDir.lengthSqr() > 1.01) {
                        data.thrustDir = data.thrustDir.normalize(); // never trust a client vector
                    }
                });
        registrar.playToServer(LampModePayload.TYPE, LampModePayload.CODEC,
                (payload, ctx) -> com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.toggle(ctx.player()));
        registrar.playToClient(SensorReadingPayload.TYPE, SensorReadingPayload.CODEC,
                (payload, ctx) -> onSensorReading.accept(payload));
        registrar.playToClient(NavPayload.TYPE, NavPayload.CODEC,
                (payload, ctx) -> onNav.accept(payload));
        registrar.playToClient(TetherPayload.TYPE, TetherPayload.CODEC,
                (payload, ctx) -> onTether.accept(payload));
    }
}
