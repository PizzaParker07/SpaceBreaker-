package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SurvivalData;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helmet HUD, drawn in the GUI layer so it stays on top of shader packs: a faint visor frame, a compact
 * read-out panel (oxygen, suit, body temperature, outside temperature, radiation, pressure, gravity)
 * and blinking warnings. Visible in first person while an EVA helmet is worn.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class HelmetHud {
    private static final int TEXT = 0x9FE3EA;
    private static final int DIM = 0x78B4BC;
    private static final int WARN = 0xFFB040;
    private static final int ALARM = 0xFF5A4A;

    private HelmetHud() {
    }

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, Spacebreaker.id("visor_frame"), HelmetHud::renderFrame);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, Spacebreaker.id("helmet_hud"), HelmetHud::renderPanel);
    }

    private static boolean active(Minecraft mc) {
        return mc.player != null && !mc.options.hideGui && mc.options.getCameraType().isFirstPerson()
                && SpaceSuitItem.wearsHelmet(mc.player) && HudConfig.enabled();
    }

    private static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ visor frame

    static void renderFrame(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc) || !HudConfig.visorFrame()) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        int band = Math.max(8, h / 14);
        // soft dark edges, a little heavier at the top where the helmet's brow is
        g.fillGradient(0, 0, w, band + 4, 0x70000000, 0x00000000);
        g.fillGradient(0, h - band, w, h, 0x00000000, 0x50000000);
        int side = Math.max(6, w / 40);
        for (int i = 0; i < side; i++) {
            int a = (int) (0x48 * (1.0 - (double) i / side));
            g.fill(i, 0, i + 1, h, a << 24);
            g.fill(w - 1 - i, 0, w - i, h, a << 24);
        }
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (SpaceSuitItem.isSunVisorDown(helmet)) {
            g.fill(0, 0, w, h, 0x38C08616); // warm gold filter
        }
    }

    // ------------------------------------------------------------------ read-out panel

    static void renderPanel(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) {
            return;
        }
        Player player = mc.player;
        SuitStatusPayload s = ClientSuitState.get(mc.level.getGameTime());
        if (s == null) {
            return;
        }
        float alpha = HudConfig.opacity();
        float scale = (float) (SbConfig.CLIENT_SPEC.isLoaded() ? SbConfig.HUD_SCALE.get() : 1.0);
        Font font = mc.font;
        boolean blink = (player.tickCount / 8) % 2 == 0;

        List<Line> lines = new ArrayList<>();
        // oxygen
        float o2 = s.capacity() > 0 ? (float) s.oxygen() / s.capacity() : 0F;
        int o2Col = s.capacity() == 0 || o2 < 0.15F ? ALARM : (o2 < 0.3F ? WARN : TEXT);
        lines.add(new Line(Component.translatable("hud.spacebreaker.oxygen"), OxygenTankItem.time(s.oxygen()), o2Col, o2));
        // suit
        boolean sealed = s.has(SuitStatusPayload.SEALED);
        lines.add(new Line(Component.translatable("hud.spacebreaker.suit"),
                Component.translatable(sealed ? "hud.spacebreaker.sealed" : "hud.spacebreaker.open").getString() + "  " + s.integrity() + "%",
                sealed ? TEXT : WARN, -1));
        // temperatures
        float body = s.bodyTempC();
        int bodyCol = body < 32 || body > 41 ? ALARM : (body < 35 || body > 38.5F ? WARN : TEXT);
        lines.add(new Line(Component.translatable("hud.spacebreaker.body"), String.format(Locale.ROOT, "%.1f °C", body), bodyCol, -1));
        lines.add(new Line(Component.translatable("hud.spacebreaker.outside"), String.format(Locale.ROOT, "%.0f °C", s.ambientC()), DIM, -1));
        // radiation
        int radCol = s.doseMsv() >= 1000 ? ALARM : (s.doseMsv() >= 250 || s.doseRate() > 50 ? WARN : TEXT);
        lines.add(new Line(Component.translatable("hud.spacebreaker.radiation"),
                String.format(Locale.ROOT, "%s mSv/d · %s mSv", fmt(s.doseRate()), fmt(s.doseMsv())), radCol, -1));
        // pressure and gravity
        lines.add(new Line(Component.translatable("hud.spacebreaker.pressure"),
                String.format(Locale.ROOT, "%s kPa · %.2f g", fmt(s.pressureKpa()), s.gravity()), DIM, -1));
        if (s.has(SuitStatusPayload.MMU)) {
            lines.add(new Line(Component.translatable("hud.spacebreaker.mmu"),
                    Math.round(100F * s.mmuFuel() / com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.CAPACITY) + "%", TEXT, -1));
        }

        int labelW = 0;
        int valueW = 0;
        for (Line l : lines) {
            labelW = Math.max(labelW, font.width(l.label));
            valueW = Math.max(valueW, font.width(l.value));
        }
        int pad = 4;
        int barW = 34;
        int width = pad * 3 + labelW + valueW + barW + 4;
        int lineH = 10;
        int height = pad * 2 + lines.size() * lineH - 1;

        g.pose().pushPose();
        g.pose().translate(6, 6, 0);
        g.pose().scale(scale, scale, 1F);
        g.fill(0, 0, width, height, argb(0x081018, 0.45F * alpha));
        g.fill(0, 0, width, 1, argb(TEXT, 0.5F * alpha));
        g.fill(0, 0, 1, 6, argb(TEXT, 0.8F * alpha));
        g.fill(width - 1, height - 6, width, height, argb(TEXT, 0.8F * alpha));
        int y = pad;
        for (Line l : lines) {
            g.drawString(font, l.label, pad, y, argb(DIM, alpha), false);
            int vx = pad * 2 + labelW;
            if (l.bar >= 0) {
                g.fill(vx, y + 1, vx + barW, y + 7, argb(0x000000, 0.5F * alpha));
                g.fill(vx + 1, y + 2, vx + 1 + Math.round((barW - 2) * Mth.clamp(l.bar, 0F, 1F)), y + 6, argb(l.color, alpha));
            }
            g.drawString(font, l.value, vx + barW + 4, y, argb(l.color, alpha), false);
            y += lineH;
        }
        g.pose().popPose();

        // warnings, centred under the crosshair area
        List<Component> warnings = new ArrayList<>();
        boolean breathable = s.has(SuitStatusPayload.BREATHABLE);
        if (!breathable && !sealed) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.unsealed"));
        } else if (!breathable && s.oxygen() == 0) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.no_oxygen"));
        } else if (!breathable && o2 < 0.15F) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.low_oxygen"));
        }
        if (s.breath() < SurvivalData.MAX_BREATH / 2 && !breathable) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.hypoxia"));
        }
        if (body < 35) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.cold"));
        } else if (body > 38.5F) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.hot"));
        }
        if (s.doseRate() > 50 || s.doseMsv() >= 500) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.radiation"));
        }
        if (s.integrity() < 20) {
            warnings.add(Component.translatable("hud.spacebreaker.warn.damaged"));
        }
        int wy = g.guiHeight() / 2 + 18;
        for (Component w : warnings) {
            if (blink) {
                int ww = font.width(w);
                int x = (g.guiWidth() - ww) / 2;
                g.fill(x - 3, wy - 2, x + ww + 3, wy + 9, argb(0x200000, 0.5F * alpha));
                g.drawString(font, w, x, wy, argb(ALARM, alpha), false);
            }
            wy += 12;
        }
    }

    private static String fmt(float v) {
        if (v >= 100) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        if (v >= 1) {
            return String.format(Locale.ROOT, "%.1f", v);
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private record Line(Component label, String value, int color, float bar) {
    }
}
