package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.net.SensorReadingPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The Environment Sensor's reading card, top left of the screen for ten seconds (GUI layer, fades out). */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SensorCard {
    private static final int SHOW_TICKS = 200, FADE_TICKS = 30;
    private static SensorReadingPayload reading;
    private static long shownAt;

    private SensorCard() {
    }

    public static void accept(SensorReadingPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        reading = payload;
        shownAt = mc.level == null ? 0 : mc.level.getGameTime();
    }

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Spacebreaker.id("sensor_card"), SensorCard::render);
    }

    static List<Component> lines(SensorReadingPayload r) {
        List<Component> out = new ArrayList<>();
        out.add(r.bodyKey().isEmpty() ? Component.translatable("sensor.spacebreaker.title")
                : Component.translatable("sensor.spacebreaker.title_at", Component.translatable(r.bodyKey())));
        out.add(Component.translatable("sensor.spacebreaker.pressure", fmt(r.pressureKpa()), String.format(Locale.ROOT, "%.3f", r.pressureKpa() / 100F)));
        if (r.composition().isEmpty()) {
            out.add(Component.translatable("sensor.spacebreaker.no_air"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> e : r.composition().entrySet()) {
                if (!sb.isEmpty()) {
                    sb.append("  ");
                }
                sb.append(e.getKey()).append(' ').append(pct(e.getValue()));
            }
            out.add(Component.translatable("sensor.spacebreaker.air", sb.toString()));
        }
        out.add(Component.translatable("sensor.spacebreaker.temperature", String.format(Locale.ROOT, "%.0f", r.temperatureC()),
                Component.translatable(r.sunlit() ? "sensor.spacebreaker.sunlit" : "sensor.spacebreaker.shade")));
        out.add(Component.translatable("sensor.spacebreaker.radiation", fmt(r.doseRateMsvDay())));
        out.add(Component.translatable("sensor.spacebreaker.gravity", String.format(Locale.ROOT, r.gravity() < 0.01F ? "%.5f" : "%.2f", r.gravity())));
        out.add(Component.translatable("sensor.spacebreaker.ground", Component.translatable(r.ground())));
        if (r.dustRate() >= 0) {
            out.add(Component.translatable("sensor.spacebreaker.dust", String.format(Locale.ROOT, "%.1f", r.dustRate() * 100F)));
        }
        return out;
    }

    private static String fmt(float v) {
        if (v == 0) {
            return "0";
        }
        if (v < 0.01F) {
            return String.format(Locale.ROOT, "%.1e", v);
        }
        return v < 10 ? String.format(Locale.ROOT, "%.2f", v) : String.format(Locale.ROOT, "%.0f", v);
    }

    private static String pct(double fraction) {
        double p = fraction * 100;
        return p >= 1 ? String.format(Locale.ROOT, "%.0f%%", p) : String.format(Locale.ROOT, "%.2f%%", p);
    }

    /** Below this GUI width the card's text is drawn at 3/4 size. */
    private static final int NARROW = 520;

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (reading == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        long age = mc.level.getGameTime() - shownAt;
        if (age > SHOW_TICKS || age < 0) {
            reading = null;
            return;
        }
        float alpha = age > SHOW_TICKS - FADE_TICKS ? (SHOW_TICKS - age) / (float) FADE_TICKS : 1F;
        Font font = mc.font;
        // top left (the top right is where advancement toasts appear), no wider than the space left of a
        // block-info box at the top centre: long lines wrap; on narrow screens the text is drawn at 3/4 size, and the
        // card never runs down into the arrival title and subtitle (overflowing rows end in "…")
        float scale = g.guiWidth() < NARROW ? 0.75F : 1F;
        int x = 6;
        int y = HelmetRadio.bottom();
        int maxText = Math.round((HelmetRadio.maxPanelWidth(g.guiWidth()) - 10) / scale);
        int maxRows = Math.max(2, (int) ((g.guiHeight() - 80 - y - 6) / (10 * scale)));
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        List<Boolean> title = new ArrayList<>();
        List<Component> source = lines(reading);
        for (int i = 0; i < source.size(); i++) {
            for (var row : font.split(source.get(i), maxText)) {
                lines.add(row);
                title.add(i == 0);
            }
        }
        if (lines.size() > maxRows) {
            lines = new ArrayList<>(lines.subList(0, maxRows - 1));
            lines.add(Component.literal("…").getVisualOrderText());
            title = title.subList(0, maxRows);
        }
        int textW = 0;
        for (var c : lines) {
            textW = Math.max(textW, font.width(c));
        }
        int w = Math.round(textW * scale) + 10;
        int h = Math.round(lines.size() * 10 * scale) + 6;
        g.fill(x, y, x + w, y + h, argb(0x000000, 0.6F * alpha));
        g.fill(x, y, x + w, y + 1, argb(0x36B1C0, alpha));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 5, y + 4, 0);
        pose.scale(scale, scale, 1F);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, argb(title.get(i) ? 0x9FE3EA : 0xE6E6E6, alpha), true);
        }
        pose.popPose();
    }

    private static int argb(int rgb, float a) {
        return (Mth.clamp(Math.round(a * 255), 4, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
