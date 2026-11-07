package com.pizzaparker07.spacebreaker.content.rocketry.nav;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import net.minecraft.util.Mth;

/**
 * Timing of the transit view: the animation that carries the crew from one place to the next instead of a
 * loading screen. It starts on the client shortly before the server moves the rocket to another dimension,
 * keeps playing (as the level-loading screen) while the new dimension arrives and then fades into it.
 * Pure logic, shared by the client view and the tests.
 */
public final class Transit {
    /** What the crew sees. */
    public enum Kind {
        /** Climbing out of the sky to orbit. */
        ASCENT,
        /** Between worlds. */
        TRANSFER,
        /** Coming down from orbit. */
        LANDING
    }

    /** Share of the ascent burn after which the screen starts to fade (the last second or two of the climb). */
    public static final float ASCENT_LEAD_IN = 0.93F;
    /** Ticks at the end of a transfer over which the screen fades to black (one second). */
    public static final int TRANSFER_FADE_TICKS = 20;
    /** Seconds over which the held last frame cross-fades into the new dimension. */
    public static final float FADE_OUT_SECONDS = 0.3F;
    /**
     * How dark the view gets before the jump (0..1): only a slight dip. The screen never goes black; the last frame
     * is held while the next dimension loads and then cross-fades into it.
     */
    public static final float MAX_DIP = 0.25F;

    private Transit() {
    }

    public static Kind kind(Location from, Location to) {
        if (!to.orbit()) {
            return Kind.LANDING;
        }
        return from.orbit() ? Kind.TRANSFER : Kind.ASCENT;
    }

    /**
     * How dark the screen is before the jump (0 = not at all, 1 = black), from the rocket's state, its timer and its
     * ascent progress: only the last moments before a change of dimension. Zero in every other state.
     */
    public static float leadIn(FlightState state, int timer, float ascentProgress) {
        return switch (state) {
            case ASCENT -> Mth.clamp((ascentProgress - ASCENT_LEAD_IN) / (0.99F - ASCENT_LEAD_IN), 0F, 1F);
            case TRANSFER -> Mth.clamp((timer - (com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity.TRANSFER_TICKS
                    - TRANSFER_FADE_TICKS)) / (float) TRANSFER_FADE_TICKS, 0F, 1F);
            default -> 0F;
        };
    }

    /** Strength of the view this many seconds after the new dimension arrived. */
    public static float fadeOut(float secondsSinceArrival) {
        return 1F - Mth.clamp(secondsSinceArrival / FADE_OUT_SECONDS, 0F, 1F);
    }

    /**
     * Animation progress (0..1, then held) for a view that has run this many seconds; a long load keeps the
     * last part moving slowly instead of freezing.
     */
    public static float progress(float seconds, float expectedSeconds) {
        float p = seconds / Math.max(0.1F, expectedSeconds);
        return p <= 0.9F ? p : 0.9F + 0.1F * (1F - (float) Math.exp(-(p - 0.9F) * 3F));
    }
}
