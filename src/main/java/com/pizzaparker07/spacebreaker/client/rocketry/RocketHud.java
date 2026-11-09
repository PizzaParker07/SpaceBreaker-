package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Rocket HUD and full-screen effects. Everything here is drawn in the GUI layer, i.e. on top of the final
 * frame of any shader pack, so it stays visible with Iris.
 */
public final class RocketHud {
    private static long arrivalTick = -1000;

    private RocketHud() {
    }

    @Nullable
    static RocketEntity ridden() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof RocketEntity r ? r : null;
    }

    /** Called when the player's level changes while riding a rocket: fade in from black. */
    static void markArrival() {
        Minecraft mc = Minecraft.getInstance();
        arrivalTick = mc.level == null ? 0 : mc.level.getGameTime();
    }

    // ------------------------------------------------------------------ full-screen overlays

    public static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        RocketEntity rocket = ridden();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        float transit = TransitView.alpha();
        if (transit > 0) {
            TransitView.render(g, w, h, transit); // seen through the window in the cockpit view
        }
        if (rocket != null && RocketCamera.inCockpit()) {
            cockpitFrame(g, w, h);
        }
        if (rocket != null) {
            FlightState s = rocket.state();
            float t = rocket.timer() + pt;
            if (s == FlightState.REENTRY) {
                plasma(g, w, h, reentryHeat(t), rocket.tickCount + pt);
            }
            if (s == FlightState.TRANSFER && t > 1) {
                // the orbit stays in view: only a thin progress rule at the top (the panel names the destination)
                float a = Mth.clamp(t / 20F, 0, 1);
                float p = Mth.clamp(t / RocketEntity.TRANSFER_TICKS, 0, 1);
                int bw = 100;
                g.fill(w / 2 - bw / 2, 14, w / 2 + bw / 2, 15, (int) (a * 0x40) << 24 | 0xFFFFFF);
                g.fill(w / 2 - bw / 2, 14, w / 2 - bw / 2 + (int) (bw * p), 15, (int) (a * 0xC0) << 24 | 0xFFFFFF);
                // the coast takes days: say how fast time runs while it passes in a few seconds
                Location dest = rocket.destination();
                if (dest != null && !dest.body().equals(rocket.location().body())) {
                    double warp = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.coastSeconds(rocket.location(), dest)
                            / (RocketEntity.TRANSFER_TICKS / 20.0);
                    if (warp > 1.5) {
                        Component label = Component.translatable("hud.spacebreaker.time_warp", com.pizzaparker07.spacebreaker.content.rocketry.item.Fmt.num(warp));
                        g.drawCenteredString(mc.font, label, w / 2, 18, (int) (a * 0xD0) << 24 | 0xBFD8FF);
                    }
                }
            }
        }
        if (mc.level != null) {
            float since = mc.level.getGameTime() - arrivalTick + pt;
            if (since < 20) {
                float a = 1 - Mth.clamp(since / 20F, 0, 1);
                g.fill(0, 0, w, h, (int) (a * 255) << 24);
            }
        }
    }

    /** Capsule window seen from the inside: a padded frame with rounded corners and one centre strut. */
    private static void cockpitFrame(GuiGraphics g, int w, int h) {
        int side = Math.max(18, w / 9);
        int top = Math.max(14, h / 8);
        int bottom = Math.max(20, h / 6);
        int wall = 0xF0181A1E;
        g.fill(0, 0, w, top, wall);
        g.fill(0, h - bottom, w, h, wall);
        g.fill(0, top, side, h - bottom, wall);
        g.fill(w - side, top, w, h - bottom, wall);
        // rounded inner corners, one column at a time
        int r = Math.min(side, top) + 6;
        for (int i = 0; i < r; i++) {
            int cut = r - (int) Math.round(Math.sqrt(r * r - (double) (r - i) * (r - i)));
            g.fill(side + i, top, side + i + 1, top + cut, wall);
            g.fill(w - side - i - 1, top, w - side - i, top + cut, wall);
            g.fill(side + i, h - bottom - cut, side + i + 1, h - bottom, wall);
            g.fill(w - side - i - 1, h - bottom - cut, w - side - i, h - bottom, wall);
        }
        // soft shading on the inside edge of the frame and a thin rim highlight
        g.fillGradient(side, top, w - side, top + 10, 0x60000000, 0x00000000);
        g.fill(side, h - bottom - 1, w - side, h - bottom, 0x40B8C0CC);
        // centre strut of the window
        int sw = Math.max(3, w / 160);
        g.fill(w / 2 - sw, top, w / 2 + sw, h - bottom, wall);
        g.fill(w / 2 - sw, top, w / 2 - sw + 1, h - bottom, 0x30B8C0CC);
    }

    /** Orange-red glow over the whole window, white-hot from below (the shield), with streaks rushing up past. */
    /**
     * How hot the entry is, 0..1, from ticks since it began: up in about a second, a short peak, then it dies away
     * over a few seconds as the capsule slows (shared by the screen glow and the glow round the capsule).
     */
    public static float reentryHeat(float t) {
        float up = Mth.clamp(t / 22F, 0, 1);
        float down = 1F - Mth.clamp((t - 45F) / 60F, 0, 1);
        return up * up * (3 - 2 * up) * down * down;
    }

    /** The entry seen from inside: only a soft orange light at the screen's edges; the planet stays in view. */
    static void plasma(GuiGraphics g, int w, int h, float heat, float time) {
        if (heat <= 0.01F) {
            return;
        }
        float flicker = 0.92F + 0.08F * Mth.sin(time * 1.7F) * Mth.sin(time * 0.63F + 1F);
        int bottom = (int) (h * 0.28F);
        g.fillGradient(0, h - bottom, w, h, 0x00FF8A40, ((int) (heat * flicker * 110) << 24) | 0xFF8A40);
        int side = (int) (w * 0.14F);
        for (int i = 0; i < side; i++) {
            float f = 1 - i / (float) side;
            int a = (int) (heat * flicker * 85 * f * f * f);
            int col = (a << 24) | 0xFF7A3A;
            g.fill(i, 0, i + 1, h, col);
            g.fill(w - i - 1, 0, w - i, h, col);
        }
        g.fillGradient(0, 0, w, (int) (h * 0.12F), ((int) (heat * flicker * 50) << 24) | 0xFF6A50, 0x00FF6A50);
    }

    // ------------------------------------------------------------------ instrument panel

    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        RocketEntity rocket = ridden();
        if (rocket == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        FlightState s = rocket.state();
        int w = g.guiWidth();
        // countdown
        if (s == FlightState.COUNTDOWN) {
            int secs = (int) Math.ceil(rocket.timer() / 20.0);
            String text = "T-" + secs;
            g.pose().pushPose();
            g.pose().translate(w / 2F, 30, 0);
            g.pose().scale(3, 3, 1);
            int col = rocket.timer() <= RocketEntity.IGNITION_TICKS ? 0xFFB347 : 0xFFFFFF;
            g.drawCenteredString(font, text, 0, 0, col);
            g.pose().popPose();
            if (rocket.timer() <= RocketEntity.IGNITION_TICKS) {
                g.drawCenteredString(font, Component.translatable("hud.spacebreaker.ignition"), w / 2, 62, 0xFFB347);
            }
        }
        if (s == FlightState.ASCENT && rocket.timer() < 60) {
            g.drawCenteredString(font, Component.translatable("hud.spacebreaker.liftoff"), w / 2, 40, 0xFFFFFF);
        }
        Location here = rocket.location();
        if (details) {
            panel(g, font, mc, rocket, s, here);
        } else {
            brief(g, font, rocket, s, here);
        }
        // key hints
        Component hint = switch (s) {
            case IDLE -> RocketEntity.launchTarget(here) != null
                    ? Component.translatable("hud.spacebreaker.hint.launch", RocketryClient.LAUNCH.getTranslatedKeyMessage())
                    : null;
            case COUNTDOWN -> Component.translatable("hud.spacebreaker.hint.abort", RocketryClient.LAUNCH.getTranslatedKeyMessage());
            case ORBIT -> Component.translatable("hud.spacebreaker.hint.map", RocketryClient.STAR_MAP.getTranslatedKeyMessage());
            default -> null;
        };
        if (!com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.hasSignal(here.body())) {
            g.drawString(font, Component.translatable("hud.spacebreaker.no_signal"), 6, details ? 82 : 32, 0xB0A070, true);
        }
        if (hint != null) {
            int hy = g.guiHeight() - 86;
            g.drawCenteredString(font, hint, w / 2, hy, 0xFFFFFF);
        }
        if (s == FlightState.IDLE && !Location.EARTH_SURFACE.equals(here)) {
            g.drawCenteredString(font, Component.translatable("hud.spacebreaker.hint.map", RocketryClient.STAR_MAP.getTranslatedKeyMessage()),
                    w / 2, g.guiHeight() - 98, 0xC0C8D0);
        }
    }

    private static boolean details;

    static void toggleDetails() {
        details = !details;
    }

    /** The default: where the rocket is going and how long it takes, and the fuel. */
    private static void brief(GuiGraphics g, Font font, RocketEntity rocket, FlightState s, Location here) {
        int x = 6, y = 6;
        Location to = s == FlightState.ASCENT || s == FlightState.TRANSFER || s == FlightState.BURN ? rocket.destination() : null;
        Component top;
        if (rocket.docking()) {
            top = Component.translatable("hud.spacebreaker.docking_line", Location.bodyName(here.body()));
        } else if (to != null) {
            String time;
            if (s == FlightState.ASCENT) {
                // remaining burn, from the share of the climb's Δv already delivered
                float p = rocket.ascentProgress();
                float secs = p > 0.03F ? rocket.timer() / 20F * (1 - p) / p : Float.NaN;
                time = Float.isNaN(secs) ? "…" : String.format(Locale.ROOT, "%d:%02d", (int) secs / 60, (int) secs % 60);
            } else {
                double days = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.coastSeconds(here, to) / 86400.0;
                time = Component.translatable("hud.spacebreaker.days", String.format(Locale.ROOT, days < 10 ? "%.1f" : "%.0f", days)).getString();
            }
            top = Component.translatable("hud.spacebreaker.brief", to.name(), time);
        } else {
            top = here.name();
        }
        int width = Math.max(100, font.width(top));
        g.fill(x - 3, y - 3, x + width + 3, y + 21, 0x70000000);
        g.drawString(font, top, x, y, 0xE8EEF4, false);
        float fuel = (float) rocket.fuelFraction();
        g.fill(x, y + 12, x + 100, y + 16, 0xFF202326);
        g.fill(x, y + 12, x + (int) (100 * fuel), y + 16, fuel > 0.2F ? 0xFF7FC27A : 0xFFD9673A);
        g.drawString(font, Component.translatable("hud.spacebreaker.details_hint", RocketryClient.FLIGHT_DETAILS.getTranslatedKeyMessage()),
                x, y + 24, 0x60C0C8D0, false);
    }

    /** The full instrument panel (toggled with the flight details key). */
    private static void panel(GuiGraphics g, Font font, Minecraft mc, RocketEntity rocket, FlightState s, Location here) {
        int x = 6, y = 6;
        int lines = 7;
        g.fill(x - 3, y - 3, x + 142, y + lines * 10 + 2, 0x90000000);
        line(g, font, x, y, Component.translatable("hud.spacebreaker.state." + s.name().toLowerCase(Locale.ROOT)), 0xFFE08A);
        line(g, font, x, y + 10, Component.translatable("hud.spacebreaker.location", here.name()), 0xDDE6F0);
        double alt = here.orbit() ? Double.NaN : rocket.getY() - (mc.level != null ? mc.level.getSeaLevel() : 63);
        String altText = Double.isNaN(alt) ? "—" : String.format(Locale.ROOT, "%,.0f m", Math.max(0, alt));
        line(g, font, x, y + 20, Component.translatable("hud.spacebreaker.altitude", altText), 0xDDE6F0);
        double speed = rocket.getDeltaMovement().length() * 20;
        line(g, font, x, y + 30, Component.translatable("hud.spacebreaker.speed", String.format(Locale.ROOT, "%.0f m/s", speed)), 0xDDE6F0);
        int stages = rocket.layout().stages().size();
        int stage = Math.min(stages, rocket.activeStage() + 1);
        line(g, font, x, y + 40, Component.translatable(rocket.isCapsuleOnly() ? "hud.spacebreaker.capsule" : "hud.spacebreaker.stage", stage, stages), 0xDDE6F0);
        line(g, font, x, y + 50, Component.translatable("hud.spacebreaker.delta_v", Fmt.ms(rocket.deltaV())), 0x9FE0A0);
        float fuel = (float) rocket.fuelFraction();
        int by = y + 61;
        g.drawString(font, Component.translatable("hud.spacebreaker.fuel"), x, by, 0xDDE6F0, false);
        int bx = x + 40;
        g.fill(bx, by + 1, bx + 96, by + 7, 0xFF202326);
        g.fill(bx, by + 1, bx + (int) (96 * fuel), by + 7, fuel > 0.2F ? 0xFF7FC27A : 0xFFD9673A);
    }

    private static void line(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        g.drawString(font, text, x, y, color, false);
    }
}
