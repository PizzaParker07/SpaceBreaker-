package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.hud.ClientSuitState;
import com.pizzaparker07.spacebreaker.content.suit.Gravity;
import com.pizzaparker07.spacebreaker.content.suit.LowGravity;
import com.pizzaparker07.spacebreaker.content.suit.ZeroG;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Less steering in the air on low-gravity worlds (see {@link LowGravity#airControl}): the walking keys push less
 * while airborne and part of the air drag is given back ({@link LowGravity#airDrag}), so a long hop on the Moon keeps
 * its take-off speed and mostly the direction it was launched in. On the ground nothing
 * changes; microgravity has its own controls ({@link ZeroGControls}) and a fuelled Maneuvering Unit steers itself.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class LowGravityControls {
    private LowGravityControls() {
    }

    /** Whether the local player was airborne when this tick's movement started (vanilla picks the drag then). */
    private static boolean airborneAtStart;

    /** Gravity factor when the local player hops in low gravity on their own, or -1 when these rules do not apply. */
    private static double lowGravityHop(LocalPlayer p) {
        if (p.onGround() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger() || p.isInWater() || p.isInLava()
                || p.onClimbable() || p.isSpectator()) {
            return -1;
        }
        double f = Gravity.factorFor(p);
        if (f >= 1.0 || ZeroG.microGravity(f)) {
            return -1;
        }
        SuitStatusPayload status = ClientSuitState.get(p.level().getGameTime());
        if (status != null && status.has(SuitStatusPayload.MMU) && status.mmuFuel() > 0) {
            return -1;
        }
        return f;
    }

    @SubscribeEvent
    static void beforeTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer p) {
            airborneAtStart = lowGravityHop(p) >= 0;
        }
    }

    /** Gives back part of the vanilla air drag, so the take-off speed lasts (see {@link LowGravity#airDrag}). */
    @SubscribeEvent
    static void afterTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer p) || !airborneAtStart) {
            return;
        }
        airborneAtStart = false;
        double f = lowGravityHop(p);
        if (f < 0) {
            return;
        }
        double scale = LowGravity.airDrag(f) / LowGravity.VANILLA_AIR_DRAG;
        var v = p.getDeltaMovement();
        p.setDeltaMovement(v.x * scale, v.y, v.z * scale);
    }

    @SubscribeEvent
    static void onInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer p)) {
            return;
        }
        double f = lowGravityHop(p);
        if (f < 0) {
            return;
        }
        float k = (float) LowGravity.airControl(f);
        var input = event.getInput();
        input.forwardImpulse *= k;
        input.leftImpulse *= k;
    }
}
