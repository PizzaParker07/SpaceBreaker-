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

/**
 * Less steering in the air on low-gravity worlds (see {@link LowGravity#airControl}): the walking keys push less
 * while airborne, so a long hop on the Moon mostly keeps the direction it was launched in. On the ground nothing
 * changes; microgravity has its own controls ({@link ZeroGControls}) and a fuelled Maneuvering Unit steers itself.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class LowGravityControls {
    private LowGravityControls() {
    }

    @SubscribeEvent
    static void onInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer p) || p.onGround() || p.getAbilities().flying || p.isFallFlying()
                || p.isPassenger() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isSpectator()) {
            return;
        }
        double f = Gravity.factorFor(p);
        if (f >= 1.0 || ZeroG.microGravity(f)) {
            return;
        }
        SuitStatusPayload status = ClientSuitState.get(p.level().getGameTime());
        if (status != null && status.has(SuitStatusPayload.MMU) && status.mmuFuel() > 0) {
            return;
        }
        float k = (float) LowGravity.airControl(f);
        var input = event.getInput();
        input.forwardImpulse *= k;
        input.leftImpulse *= k;
    }
}
