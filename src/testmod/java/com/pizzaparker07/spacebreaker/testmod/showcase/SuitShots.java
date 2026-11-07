package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Shared bits of the suit area's showcase scenarios. */
@EventBusSubscriber(modid = com.pizzaparker07.spacebreaker.Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class SuitShots {
    /** Set by the first suit shot: from then on toasts are cleared every tick, also those a shot's setup unlocks. */
    private static boolean noToasts;

    /** Client setup for every suit shot: no advancement or recipe toasts over the picture. */
    static final Runnable CLEAR_TOASTS = () -> {
        noToasts = true;
        Minecraft.getInstance().getToasts().clear();
    };

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (noToasts) {
            Minecraft.getInstance().getToasts().clear();
        }
    }

    /** Clears the toasts, then runs the shot's own client setup. */
    static Runnable clearToastsThen(Runnable setup) {
        return () -> {
            CLEAR_TOASTS.run();
            setup.run();
        };
    }

    private SuitShots() {
    }
}
