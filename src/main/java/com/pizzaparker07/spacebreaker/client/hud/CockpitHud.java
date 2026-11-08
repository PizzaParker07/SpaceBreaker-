package com.pizzaparker07.spacebreaker.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Spacecraft cockpit HUD in the helmet HUD's plain style: beside the hotbar on the main-hand side (the oxygen gauge
 * sits on the other side), small and out of the way. By default: the target, speed relative to it, distance or
 * altitude, a Δv bar and the flight-assist tag. The detailed HUD option adds periapsis/apoapsis, what ends the current
 * path (a moon's sphere, impact) and the next manoeuvre node. Nothing at the top centre; drawn above the hotbar layer.
 * With a target position the target is also marked on screen: corner brackets around it, or an arrow at the edge when
 * it is off screen or behind, with its distance and closing speed (never at the top centre).
 * The craft's client code supplies the numbers ({@link #setSource}); without a source nothing is drawn.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class CockpitHud {
    private static final int TEXT = 0x9FE3EA, LABEL = 0x6E9AA0, WARN = 0xFFB040, ALARM = 0xFF5A4A;
    private static final int DV = 0xE0B040;
    private static final int BAR_W = 41;
    private static final int ROW_H = 11;
    private static Supplier<CockpitReadout> source = () -> null;

    private CockpitHud() {
    }

    /** Set by the spacecraft's client code: the readout while the player flies a craft, null otherwise. */
    public static void setSource(Supplier<CockpitReadout> s) {
        source = s == null ? () -> null : s;
    }

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Spacebreaker.id("cockpit_hud"), CockpitHud::render);
    }

    private record Line(Component label, String value, int color, float bar) {
    }

    /** The rows to show for a readout (testable without drawing). */
    static List<Line> lines(CockpitReadout r, boolean detailed) {
        List<Line> out = new ArrayList<>();
        out.add(new Line(Component.empty(), r.target().getString(), TEXT, -1F));
        // closing in: negative speed; a fast approach close to the target turns orange, then red
        boolean closing = r.relSpeed() < 0;
        double secondsToReach = closing ? r.distance() / -r.relSpeed() : Double.POSITIVE_INFINITY;
        int speedColor = closing && secondsToReach < 10 ? ALARM : closing && secondsToReach < 30 ? WARN : 0xFFFFFF;
        out.add(new Line(Component.translatable("hud.spacebreaker.cockpit.speed"), FlightFormat.speed(r.relSpeed())
                + (r.assist() ? "  " + Component.translatable("hud.spacebreaker.cockpit.assist").getString() : ""), speedColor, -1F));
        out.add(new Line(Component.translatable(r.altitude() ? "hud.spacebreaker.cockpit.altitude" : "hud.spacebreaker.cockpit.distance"),
                FlightFormat.distance(r.distance()), 0xFFFFFF, -1F));
        float dv = r.dvFraction();
        out.add(new Line(Component.literal("Δv"), FlightFormat.speed(r.dv()), dv < 0.1F ? ALARM : dv < 0.25F ? WARN : 0xFFFFFF, dv));
        if (detailed) {
            if (!Double.isNaN(r.periapsis())) {
                out.add(new Line(Component.translatable("hud.spacebreaker.cockpit.periapsis"), FlightFormat.distance(r.periapsis()),
                        r.periapsis() < 0 ? ALARM : TEXT, -1F));
                out.add(new Line(Component.translatable("hud.spacebreaker.cockpit.apoapsis"), FlightFormat.distance(r.apoapsis()), TEXT, -1F));
            }
            if (r.event() != null) {
                out.add(new Line(r.event(), FlightFormat.time(r.eventIn()), TEXT, -1F));
            }
            if (r.hasNode()) {
                out.add(new Line(Component.translatable("hud.spacebreaker.cockpit.node"),
                        FlightFormat.speed(r.nodeDv()) + " · " + FlightFormat.time(r.nodeIn()), r.nodeIn() < 10 ? WARN : TEXT, -1F));
            }
        }
        return out;
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !HudConfig.enabled()) {
            return;
        }
        CockpitReadout r = source.get();
        if (r == null) {
            return;
        }
        float alpha = HudConfig.opacity();
        Font font = mc.font;
        List<Line> lines = lines(r, HudConfig.detailed());
        int labelW = 0, valueW = 0;
        for (Line l : lines) {
            labelW = Math.max(labelW, font.width(l.label()));
            valueW = Math.max(valueW, (l.bar() >= 0 ? BAR_W + 3 : 0) + font.width(l.value()));
        }
        int pad = 3;
        int w = labelW + (labelW > 0 ? 4 : 0) + valueW + pad * 2;
        int h = lines.size() * ROW_H - 2 + pad * 2;
        // the main-hand side of the hotbar, past the hand slot; the oxygen gauge is on the other side
        boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        // the vanilla attack indicator in "hotbar" mode sits on that side too: step past it
        int indicator = mc.options.attackIndicator().get() == net.minecraft.client.AttackIndicatorStatus.HOTBAR ? 20 : 0;
        int x = rightHanded ? g.guiWidth() / 2 + 91 + 6 + indicator : g.guiWidth() / 2 - 91 - 6 - indicator - w;
        int y = g.guiHeight() - 2 - h;
        if (x < 2 || x + w > g.guiWidth() - 2) {
            x = g.guiWidth() - w - 4;
            y = g.guiHeight() - 24 - h;
        }
        RenderSystem.enableBlend();
        drawMarker(g, font, r, alpha);
        g.fill(x, y, x + w, y + h, argb(0x000000, 0.45F * alpha));
        int ly = y + pad;
        for (Line l : lines) {
            int lx = x + pad;
            if (labelW > 0) {
                g.drawString(font, l.label(), lx, ly + 1, argb(LABEL, alpha), true);
                lx += labelW + 4;
            }
            if (l.bar() >= 0) {
                g.fill(lx, ly + 2, lx + BAR_W, ly + 7, argb(0x101014, 0.75F * alpha));
                int fill = Math.round((BAR_W - 2) * l.bar());
                if (fill > 0) {
                    g.fill(lx + 1, ly + 3, lx + 1 + fill, ly + 6, argb(l.color() == 0xFFFFFF ? DV : l.color(), alpha));
                }
                lx += BAR_W + 3;
            }
            g.drawString(font, l.value(), lx, ly + 1, argb(l.color(), alpha), true);
            ly += ROW_H;
        }
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------ target marker

    /** The last frame's camera matrices and position, for projecting the target onto the screen. */
    private static final org.joml.Matrix4f VIEW = new org.joml.Matrix4f(), PROJ = new org.joml.Matrix4f();
    private static net.minecraft.world.phys.Vec3 camPos = net.minecraft.world.phys.Vec3.ZERO;
    private static boolean haveMatrices;
    /** Keep the marker this far inside the screen edge, and clear of the top centre (block-info tooltips). */
    private static final int EDGE = 24, TOP_CENTRE_HALF_WIDTH = 110, TOP_CENTRE_HEIGHT = 44, TOP_BAND = 64;

    @SubscribeEvent
    static void captureMatrices(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) {
            VIEW.set(event.getModelViewMatrix());
            PROJ.set(event.getProjectionMatrix());
            camPos = event.getCamera().getPosition();
            haveMatrices = true;
        }
    }

    /**
     * Where a world point lands on a {@code w}×{@code h} GUI screen: {x, y, onScreen (1/0), radius in pixels for a
     * {@code size}-block sphere}. Behind the camera or outside the screen, x/y give the direction from the centre.
     */
    public static double[] project(org.joml.Matrix4f view, org.joml.Matrix4f proj, net.minecraft.world.phys.Vec3 cam,
                            net.minecraft.world.phys.Vec3 pos, double size, int w, int h) {
        org.joml.Vector4f v = new org.joml.Vector4f((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z), 1F);
        view.transform(v);
        float depth = -v.z; // distance in front of the camera
        float vx = v.x, vy = v.y; // camera space: right and up
        proj.transform(v);
        if (v.w > 1e-4F && depth > 0) {
            float nx = v.x / v.w, ny = v.y / v.w;
            double px = (nx + 1) / 2 * w, py = (1 - ny) / 2 * h;
            double r = size / depth * proj.m11() * h / 2;
            boolean on = Math.abs(nx) <= 1 && Math.abs(ny) <= 1;
            return new double[]{px, py, on ? 1 : 0, r};
        }
        // behind or beside: point the way it lies in camera space (right / up)
        double dx = vx, dy = vy;
        double len = Math.max(1e-6, Math.hypot(dx, dy));
        return new double[]{w / 2.0 + dx / len * w, h / 2.0 - dy / len * h, 0, 0};
    }

    /** Brackets around the target, or an arrow at the screen edge pointing to it, with distance and closing speed. */
    private static void drawMarker(GuiGraphics g, Font font, CockpitReadout r, float alpha) {
        if (r.targetPos() == null || !haveMatrices) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        double[] p = project(VIEW, PROJ, camPos, r.targetPos(), Math.max(0.5, r.targetSize()), w, h);
        String label = FlightFormat.distance(r.distance()) + "  " + FlightFormat.speed(r.relSpeed());
        int color = argb(TEXT, alpha);
        if (p[2] > 0.5) {
            int cx = (int) Math.round(p[0]), cy = (int) Math.round(p[1]);
            int half = (int) Math.max(6, Math.min(Math.min(w, h) / 3.0, p[3] + 3));
            int arm = Math.max(3, half / 3);
            // four corner brackets
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    int x0 = cx + sx * half, y0 = cy + sy * half;
                    g.fill(Math.min(x0, x0 - sx * arm), y0, Math.max(x0, x0 - sx * arm) + 1, y0 + 1, color);
                    g.fill(x0, Math.min(y0, y0 - sy * arm), x0 + 1, Math.max(y0, y0 - sy * arm) + 1, color);
                }
            }
            int lx = cx + half + 4, ly = cy - 4;
            if (lx + font.width(label) > w - 2) {
                lx = cx - half - 4 - font.width(label);
            }
            g.drawString(font, r.target(), lx, ly - 9, color, true);
            g.drawString(font, label, lx, ly, color, true);
            return;
        }
        // off screen: an arrow on a rectangle inset from the edges, kept clear of the top centre
        double dx = p[0] - w / 2.0, dy = p[1] - h / 2.0;
        double sxl = (w / 2.0 - EDGE) / Math.max(1e-6, Math.abs(dx)), syl = (h / 2.0 - EDGE) / Math.max(1e-6, Math.abs(dy));
        double k = Math.min(sxl, syl);
        int ax = (int) Math.round(w / 2.0 + dx * k), ay = (int) Math.round(h / 2.0 + dy * k);
        if (ay < TOP_CENTRE_HEIGHT && Math.abs(ax - w / 2) < TOP_CENTRE_HALF_WIDTH) {
            ax = w / 2 + (ax >= w / 2 ? TOP_CENTRE_HALF_WIDTH : -TOP_CENTRE_HALF_WIDTH);
        }
        // the top band holds other panels (flight status, sensor card, radio, block info): stay below it
        ay = Math.max(ay, TOP_BAND);
        float angle = (float) Math.toDegrees(Math.atan2(dx, -dy));
        g.pose().pushPose();
        g.pose().translate(ax, ay, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        for (int i = 0; i < 5; i++) {
            g.fill(-i, -6 + i, i + 1, -5 + i, color); // head
        }
        g.fill(-1, -1, 2, 5, color); // shaft
        g.pose().popPose();
        int lx = ax + (ax > w / 2 ? -font.width(label) - 10 : 10);
        int ly = Math.min(h - 40, Math.max(TOP_BAND, ay - 4));
        g.drawString(font, r.target(), lx, ly - 9, color, true);
        g.drawString(font, label, lx, ly, color, true);
    }

    private static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
