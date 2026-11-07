package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.client.Minecraft;

/** Shared bits of the suit area's showcase scenarios. */
final class SuitShots {
    /** Client setup for every suit shot: no advancement or recipe toasts over the picture. */
    static final Runnable CLEAR_TOASTS = () -> Minecraft.getInstance().getToasts().clear();

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
