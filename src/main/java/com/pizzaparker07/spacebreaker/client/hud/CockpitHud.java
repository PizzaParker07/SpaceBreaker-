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
        int x = rightHanded ? g.guiWidth() / 2 + 91 + 6 : g.guiWidth() / 2 - 91 - 6 - w;
        int y = g.guiHeight() - 2 - h;
        if (x < 2 || x + w > g.guiWidth() - 2) {
            x = g.guiWidth() - w - 4;
            y = g.guiHeight() - 24 - h;
        }
        RenderSystem.enableBlend();
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

    private static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
