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
import net.minecraft.util.RandomSource;
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
        if (rocket != null) {
            FlightState s = rocket.state();
            float t = rocket.timer() + pt;
            if (s == FlightState.REENTRY) {
                float heat = Mth.clamp(t / 30F, 0, 1) * Mth.clamp((170 - t) / 50F, 0, 1);
                plasma(g, w, h, heat, rocket.tickCount + pt);
            }
            if (s == FlightState.TRANSFER) {
                float a = Mth.clamp(t / 25F, 0, 1);
                g.fill(0, 0, w, h, (int) (a * 255) << 24);
                if (a > 0.6F) {
                    Component title = Component.translatable("hud.spacebreaker.coasting", Component.translatable(rocket.destination().translationKey()));
                    int alpha = (int) ((a - 0.6F) / 0.4F * 255) << 24;
                    g.drawCenteredString(mc.font, title, w / 2, h / 2 - 10, 0xDDE6F0 | alpha);
                    float p = Mth.clamp(t / RocketEntity.TRANSFER_TICKS, 0, 1);
                    int bw = 120;
                    g.fill(w / 2 - bw / 2, h / 2 + 6, w / 2 + bw / 2, h / 2 + 8, 0x40FFFFFF);
                    g.fill(w / 2 - bw / 2, h / 2 + 6, w / 2 - bw / 2 + (int) (bw * p), h / 2 + 8, 0xC0FFFFFF);
                }
            }
        }
        if (mc.level != null) {
            float since = mc.level.getGameTime() - arrivalTick + pt;
            if (since < 30) {
                float a = 1 - Mth.clamp(since / 30F, 0, 1);
                g.fill(0, 0, w, h, (int) (a * 255) << 24);
            }
        }
    }

    /** Orange-red glow creeping in from the edges, with streaks rushing up past the window. */
    private static void plasma(GuiGraphics g, int w, int h, float heat, float time) {
        if (heat <= 0.01F) {
            return;
        }
        int bands = 24;
        for (int i = 0; i < bands; i++) {
            float f = 1 - i / (float) bands;
            int a = (int) (heat * 150 * f * f);
            int col = (a << 24) | (i < bands / 3 ? 0xFFB040 : 0xFF5A1E);
            int inset = (int) (i * (Math.min(w, h) * 0.32F) / bands);
            // bottom (the shield is below us), sides, and a fainter top
            g.fill(0, h - inset - 3, w, h - inset, col);
            g.fill(inset, 0, inset + 2, h, col);
            g.fill(w - inset - 2, 0, w - inset, h, col);
            g.fill(0, inset, w, inset + 1, ((int) (a * 0.5F) << 24) | 0xFF7A30);
        }
        RandomSource rng = RandomSource.create(42);
        for (int i = 0; i < 40; i++) {
            float x = rng.nextFloat() * w;
            float speed = 6 + rng.nextFloat() * 10;
            float len = 10 + rng.nextFloat() * 40;
            float y = h - ((time * speed + rng.nextFloat() * h) % (h + len));
            int a = (int) (heat * (90 + rng.nextInt(120)));
            boolean edge = x < w * 0.25F || x > w * 0.75F;
            if (!edge) {
                a /= 3;
            }
            g.fill((int) x, (int) y, (int) x + 1, (int) (y + len), (a << 24) | 0xFFE0A0);
        }
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
        // panel
        int x = 6, y = 6;
        int lines = 7;
        g.fill(x - 3, y - 3, x + 142, y + lines * 10 + 2, 0x90000000);
        Location here = rocket.location();
        line(g, font, x, y, Component.translatable("hud.spacebreaker.state." + s.name().toLowerCase(Locale.ROOT)), 0xFFE08A);
        line(g, font, x, y + 10, Component.translatable("hud.spacebreaker.location", Component.translatable(here.translationKey())), 0xDDE6F0);
        double alt = here.orbit() ? Double.NaN : rocket.getY() - (mc.level != null ? mc.level.getSeaLevel() : 63);
        String altText = Double.isNaN(alt) ? "—" : String.format(Locale.ROOT, "%,.0f m", Math.max(0, alt));
        line(g, font, x, y + 20, Component.translatable("hud.spacebreaker.altitude", altText), 0xDDE6F0);
        double speed = rocket.getDeltaMovement().length() * 20;
        line(g, font, x, y + 30, Component.translatable("hud.spacebreaker.speed", String.format(Locale.ROOT, "%.0f m/s", speed)), 0xDDE6F0);
        int stages = rocket.layout().stages().size();
        int stage = Math.min(stages, rocket.activeStage() + 1);
        line(g, font, x, y + 40, Component.translatable(rocket.isCapsuleOnly() ? "hud.spacebreaker.capsule" : "hud.spacebreaker.stage", stage, stages), 0xDDE6F0);
        line(g, font, x, y + 50, Component.translatable("hud.spacebreaker.delta_v", Fmt.ms(rocket.deltaV())), 0x9FE0A0);
        // fuel bar
        float fuel = (float) rocket.fuelFraction();
        int by = y + 61;
        g.drawString(font, Component.translatable("hud.spacebreaker.fuel"), x, by, 0xDDE6F0, false);
        int bx = x + 40;
        g.fill(bx, by + 1, bx + 96, by + 7, 0xFF202326);
        g.fill(bx, by + 1, bx + (int) (96 * fuel), by + 7, fuel > 0.2F ? 0xFF7FC27A : 0xFFD9673A);
        // key hints
        Component hint = switch (s) {
            case IDLE -> RocketEntity.launchTarget(here) != null
                    ? Component.translatable("hud.spacebreaker.hint.launch", RocketryClient.LAUNCH.getTranslatedKeyMessage())
                    : null;
            case COUNTDOWN -> Component.translatable("hud.spacebreaker.hint.abort", RocketryClient.LAUNCH.getTranslatedKeyMessage());
            case ORBIT -> Component.translatable("hud.spacebreaker.hint.map", RocketryClient.STAR_MAP.getTranslatedKeyMessage());
            default -> null;
        };
        if (hint != null) {
            int hy = g.guiHeight() - 72;
            g.drawCenteredString(font, hint, w / 2, hy, 0xFFFFFF);
        }
        if (s == FlightState.IDLE && here != Location.EARTH_SURFACE) {
            g.drawCenteredString(font, Component.translatable("hud.spacebreaker.hint.map", RocketryClient.STAR_MAP.getTranslatedKeyMessage()),
                    w / 2, g.guiHeight() - 82, 0xC0C8D0);
        }
    }

    private static void line(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        g.drawString(font, text, x, y, color, false);
    }
}
