package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The jump between dimensions, kept out of sight: the world itself carries the flight (the sky darkens as the rocket
 * climbs, the planet curves away below in orbit), and only around the actual change of dimension does the screen
 * fade softly to black, hold the last frame while the next dimension loads ({@link TransitScreen}, never a
 * "loading" screen) and fade back into the new world. Timing comes from {@link Transit}. GUI layer, so it also
 * covers the world under a shader pack.
 */
public final class TransitView {
    @Nullable
    private static Transit.Kind kind;
    @Nullable
    private static Location to;
    private static long arrived = -1;
    private static float lead;
    private static boolean holding;
    private static boolean pinned;
    private static float pinnedAlpha;

    private TransitView() {
    }

    /** Called every client tick while the player rides a rocket. */
    static void update(RocketEntity rocket) {
        if (pinned) {
            return;
        }
        FlightState s = rocket.state();
        float l = Transit.leadIn(s, rocket.timer(), rocket.ascentProgress());
        if (arrived >= 0) {
            return; // fading back into the new world
        }
        if (l > 0 && kind == null) {
            Location here = rocket.location();
            Location dest = s == FlightState.ASCENT ? RocketEntity.launchTarget(here) : rocket.destination();
            if (dest != null) {
                begin(Transit.kind(here, dest), here, dest);
            }
        }
        lead = l;
        if (l <= 0 && !holding) {
            reset(); // aborted (failed ascent, cancelled travel)
        }
    }

    static void begin(Transit.Kind k, @Nullable Location here, @Nullable Location dest) {
        kind = k;
        to = dest;
        arrived = -1;
    }

    /** The dimension is changing ({@link TransitScreen} is up): stay fully dark. */
    static void hold() {
        holding = true;
    }

    /** The new dimension is there: start fading back in. */
    static void arrived() {
        if (kind != null && arrived < 0) {
            arrived = System.nanoTime();
            holding = false;
        }
    }

    static boolean holding() {
        return holding;
    }

    static boolean active() {
        return kind != null;
    }

    /**
     * For screenshots: shows (or with a null kind, clears) the fade at a fixed strength until cleared.
     *
     * @param alpha how dark (0 = clear, 1 = black)
     */
    public static void showcase(@Nullable Transit.Kind k, @Nullable Location here, @Nullable Location dest, float alpha) {
        reset();
        if (k != null) {
            begin(k, here, dest);
            pinned = true;
            pinnedAlpha = alpha;
        }
    }

    private static void reset() {
        pinned = false;
        kind = null;
        to = null;
        arrived = -1;
        lead = 0;
        holding = false;
    }

    private static float seconds(long since) {
        return (System.nanoTime() - since) / 1e9F;
    }

    /** How dark the screen is right now (0 when the fade is not shown). */
    static float alpha() {
        if (kind == null) {
            return 0;
        }
        if (arrived >= 0) {
            float a = Transit.fadeOut(seconds(arrived));
            if (a <= 0) {
                reset();
            }
            return a;
        }
        if (pinned) {
            return pinnedAlpha;
        }
        return holding ? 1F : lead;
    }

    @Nullable
    static Location destination() {
        return to;
    }

    /** The fade: plain black, eased so it starts and ends gently. */
    static void render(GuiGraphics g, int w, int h, float alpha) {
        if (kind == null || alpha <= 0.003F) {
            return;
        }
        float a = Mth.clamp(alpha, 0, 1);
        float eased = a * a * (3 - 2 * a);
        g.fill(0, 0, w, h, (int) (eased * 255) << 24);
    }
}
