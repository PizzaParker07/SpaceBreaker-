package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The jump between dimensions, kept out of sight: the world itself carries the flight (the sky darkens as the rocket
 * climbs, the planet curves away below in orbit). Just before the jump the view dims slightly ({@link Transit#MAX_DIP});
 * the last frame is then taken and held on screen while the next dimension loads ({@link TransitScreen}, never a
 * "loading" screen and never a black screen) and until the crew is back in the seat, and finally cross-fades into the
 * new world in {@link Transit#FADE_OUT_SECONDS}. GUI layer, so it also covers the world under a shader pack.
 */
public final class TransitView {
    static final ResourceLocation SNAPSHOT = Spacebreaker.id("transit_snapshot");
    @Nullable
    private static Transit.Kind kind;
    @Nullable
    private static Location to;
    private static long arrived = -1;
    private static float lead;
    private static boolean holding;
    private static boolean pinned;
    private static float pinnedAlpha;
    private static boolean snapshot;

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
            return; // cross-fading into the new world
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

    /** The dimension is changing ({@link TransitScreen} is up): take the last frame and hold it. */
    static void hold() {
        holding = true;
        releaseSnapshot();
        try {
            var mc = Minecraft.getInstance();
            var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget());
            mc.getTextureManager().register(SNAPSHOT, new net.minecraft.client.renderer.texture.DynamicTexture(image));
            snapshot = true;
        } catch (RuntimeException e) {
            snapshot = false;
        }
    }

    /** The new dimension is there and the crew seated: start the cross-fade. */
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
     * For screenshots: shows (or with a null kind, clears) the dip at a fixed strength until cleared.
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
        releaseSnapshot();
    }

    private static void releaseSnapshot() {
        if (snapshot) {
            snapshot = false;
            Minecraft.getInstance().getTextureManager().release(SNAPSHOT);
        }
    }

    private static float seconds(long since) {
        return (System.nanoTime() - since) / 1e9F;
    }

    /** How strongly the transit view covers the world right now (0 when it is not shown). */
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

    /**
     * Before the jump: a slight dip. While held and while cross-fading: the last frame (dimmed as the dip left it) over
     * the world; black only if the frame could not be taken.
     */
    static void render(GuiGraphics g, int w, int h, float alpha) {
        if (kind == null || alpha <= 0.003F) {
            return;
        }
        float a = Mth.clamp(alpha, 0, 1);
        float eased = a * a * (3 - 2 * a);
        if (holding || arrived >= 0) {
            if (snapshot) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                g.setColor(1F, 1F, 1F, eased);
                g.blit(SNAPSHOT, 0, 0, 0, 0F, 0F, w, h, w, h);
                g.setColor(1F, 1F, 1F, 1F); // the frame already carries the dip
            } else {
                g.fill(0, 0, w, h, (int) (eased * 255) << 24);
            }
            return;
        }
        g.fill(0, 0, w, h, (int) (eased * (pinned ? 1F : Transit.MAX_DIP) * 255) << 24);
    }
}
