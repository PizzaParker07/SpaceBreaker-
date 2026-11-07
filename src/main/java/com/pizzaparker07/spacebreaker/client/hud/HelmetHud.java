package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
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
 * Helmet HUD, drawn in the GUI layer so it stays on top of shader packs: a faint visor frame and a
 * compact vanilla-style gauge cluster beside the hotbar on the offhand side (oxygen, body temperature; suit, radiation,
 * surroundings and Maneuvering Unit gas only when they matter) with one blinking warning above the
 * hotbar. Visible in first person while an EVA helmet is worn; scale and opacity are configurable.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class HelmetHud {
    private static final int TEXT = 0x9FE3EA;
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
        if (!active(mc)) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (HudConfig.visorFrame()) {
            // the rounded visor rim with its glass reflections, stretched over the screen
            RenderSystem.enableBlend();
            g.setColor(1F, 1F, 1F, HudConfig.visorFrameOpacity());
            g.blit(FRAME, 0, 0, 0, 0, w, h, w, h);
            g.setColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
        }
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (SpaceSuitItem.isSunVisorDown(helmet)
                || (helmet.getItem() instanceof SpaceSuitItem s && s.tier() == com.pizzaparker07.spacebreaker.content.suit.SuitTier.THERMAL)) {
            g.fill(0, 0, w, h, 0x38C08616); // warm gold filter
        }
        // rime on the visor rim when the cold is beyond the suit's rating (a light touch in any cold trap)
        float frost = frost(delta.getGameTimeDeltaPartialTick(false));
        if (frost > 0.01F) {
            RenderSystem.enableBlend();
            g.setColor(1F, 1F, 1F, frost);
            g.blit(FROST, 0, 0, 0, 0, w, h, w, h);
            g.setColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
        }
    }

    private static final ResourceLocation FRAME = Spacebreaker.id("textures/gui/visor_frame.png");
    private static final ResourceLocation FROST = Spacebreaker.id("textures/gui/visor_frost.png");
    private static float frostLevel, frostPrev;

    /** Client tick: moves the frost towards its target. */
    static void tickFrost(SuitStatusPayload s) {
        frostPrev = frostLevel;
        float target = s == null ? 0F : s.visorFrost();
        frostLevel += target > frostLevel ? Math.min(0.005F, target - frostLevel) : Math.max(-0.008F, target - frostLevel);
    }

    private static float frost(float partialTick) {
        return Mth.lerp(partialTick, frostPrev, frostLevel);
    }

    // ------------------------------------------------------------------ gauges

    private static final ResourceLocation ICONS = Spacebreaker.id("textures/gui/hud_icons.png");
    private static final int ICON_OXYGEN = 0, ICON_SUIT = 1, ICON_TEMP = 2, ICON_RAD = 3, ICON_PRESSURE = 4, ICON_MMU = 5;
    private static final int OXYGEN = 0x36B1C0;
    private static final int GOOD = 0x7CD36A;
    private static final int BAR_W = 41;
    private static final int ROW_H = 11;

    /** One gauge row: icon, bar (fill 0..1, or a marker position when {@code marker}), short value. */
    private record Row(int icon, float fill, int color, String value, int valueColor, boolean marker) {
    }

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
        boolean breathable = s.has(SuitStatusPayload.BREATHABLE);
        boolean sealed = s.has(SuitStatusPayload.SEALED);

        List<Row> rows = new ArrayList<>();
        // oxygen: always
        float o2 = s.capacity() > 0 ? (float) s.oxygen() / s.capacity() : 0F;
        boolean o2Low = s.capacity() == 0 || o2 < 0.15F;
        rows.add(new Row(ICON_OXYGEN, o2, o2Low ? ALARM : (o2 < 0.3F ? WARN : OXYGEN),
                s.capacity() > 0 ? OxygenTankItem.time(s.oxygen()) : "--:--", o2Low && !breathable ? ALARM : 0xFFFFFF, false));
        // suit: only when it matters
        if (!sealed || s.integrity() < 50) {
            int c = !sealed ? WARN : (s.integrity() < 20 ? ALARM : WARN);
            rows.add(new Row(ICON_SUIT, s.integrity() / 100F, c,
                    sealed ? s.integrity() + "%" : Component.translatable("hud.spacebreaker.open").getString(), c, false));
        }
        // suit rating: only when the surroundings are beyond it
        boolean beyond = sealed && (s.ambientC() < s.ratingMinC() || s.ambientC() > s.ratingMaxC());
        if (beyond) {
            rows.add(new Row(ICON_SUIT, -1F, 0, String.format(Locale.ROOT, "%d..%d°", s.ratingMinC(), s.ratingMaxC()), WARN, false));
        }
        // body temperature: marker on a 30..44 °C scale, normal band in the middle
        float body = s.bodyTempC();
        int bodyCol = body < 32 || body > 41 ? ALARM : (body < 35 || body > 38.5F ? WARN : GOOD);
        rows.add(new Row(ICON_TEMP, Mth.clamp((body - 30F) / 14F, 0F, 1F), bodyCol,
                String.format(Locale.ROOT, "%.1f°", body), bodyCol == GOOD ? 0xFFFFFF : bodyCol, true));
        // radiation: only when there is some
        if (s.doseRate() > 0.05F || s.doseMsv() >= 1F) {
            int c = s.doseMsv() >= 1000 ? ALARM : (s.doseMsv() >= 250 || s.doseRate() > 50 ? WARN : 0xD8C040);
            float fill = (float) (Math.log10(1 + s.doseMsv()) / Math.log10(1 + 3000));
            rows.add(new Row(ICON_RAD, Mth.clamp(fill, 0F, 1F), c, fmt(s.doseRate()) + "/d" + (s.doseMsv() >= 1F ? " " + fmt(s.doseMsv()) + "mSv" : ""), c == ALARM ? ALARM : 0xFFFFFF, false));
        }
        // surroundings: only off Earth-normal conditions
        // magnetic boots gripping / tether clipped: short tags after the surroundings
        boolean mag = s.has(SuitStatusPayload.MAGNETISED), tethered = s.has(SuitStatusPayload.TETHERED);
        boolean pressurised = s.has(SuitStatusPayload.PRESSURISED);
        if (pressurised) {
            // inside a pressurised module: breathable air, the helmet may come off
            rows.add(new Row(ICON_PRESSURE, -1F, 0, Component.translatable("hud.spacebreaker.pressurised").getString(), GOOD, false));
        } else if (!breathable || s.gravity() < 0.95F || s.gravity() > 1.05F || s.ambientC() < -20 || s.ambientC() > 50 || mag || tethered) {
            String env = String.format(Locale.ROOT, "%s kPa %.2fg %.0f°", fmt(s.pressureKpa()), s.gravity(), s.ambientC());
            if (mag) {
                env += " " + Component.translatable("hud.spacebreaker.mag").getString();
            }
            if (tethered) {
                env += " " + Component.translatable("hud.spacebreaker.tether").getString();
            }
            if (s.has(SuitStatusPayload.COLD_TRAP)) {
                env += " " + Component.translatable("hud.spacebreaker.cold_trap").getString();
            }
            if (s.has(SuitStatusPayload.MICRO_G) && !mag && !tethered) {
                env += " " + Component.translatable("hud.spacebreaker.micro_g").getString();
            }
            rows.add(new Row(ICON_PRESSURE, -1F, 0, env, TEXT, false));
        }
        if (s.has(SuitStatusPayload.MMU)) {
            float gas = (float) s.mmuFuel() / com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.CAPACITY;
            rows.add(new Row(ICON_MMU, gas, gas < 0.15F ? WARN : 0xE0B040, Math.round(gas * 100) + "%", 0xFFFFFF, false));
        }

        int width = 0;
        for (Row r : rows) {
            width = Math.max(width, 11 + (r.fill() >= 0 ? BAR_W + 3 : 0) + font.width(r.value()));
        }
        int height = rows.size() * ROW_H - 2;
        // anchor: beside the hotbar on the offhand side (past the offhand slot), away from the held item;
        // mirrored for left-handed play; top-left corner if the screen is too narrow
        int pad = 3;
        int pw = Math.round((width + pad * 2) * scale);
        int ph = Math.round((height + pad * 2) * scale);
        boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        int ax = rightHanded ? g.guiWidth() / 2 - 91 - 29 - 6 - pw : g.guiWidth() / 2 + 91 + 29 + 6;
        int ay = g.guiHeight() - 2 - ph;
        if (ax < 2 || ax + pw > g.guiWidth() - 2) {
            ax = 4;
            ay = 4;
        }

        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(ax, ay, 0);
        g.pose().scale(scale, scale, 1F);
        // translucent backing like vanilla's chat/tooltip background, for bright terrain
        g.fill(0, 0, width + pad * 2, height + pad * 2, argb(0x000000, 0.45F * alpha));
        g.pose().translate(pad, pad, 0);
        int y = 0;
        for (Row r : rows) {
            g.setColor(1F, 1F, 1F, alpha);
            g.blit(ICONS, 0, y, r.icon() * 9, 0, 9, 9, 9 * 6, 9);
            g.setColor(1F, 1F, 1F, 1F);
            int x = 11;
            if (r.fill() >= 0) {
                drawBar(g, x, y + 2, r, alpha, blink);
                x += BAR_W + 3;
            }
            g.drawString(font, r.value(), x, y + 1, argb(r.valueColor(), alpha), true);
            y += ROW_H;
        }
        g.pose().popPose();
        RenderSystem.disableBlend();

        // warnings: centred above the hotbar, like vanilla's action bar
        Component warning = null;
        if (!breathable && !sealed) {
            warning = Component.translatable("hud.spacebreaker.warn.unsealed");
        } else if (!breathable && s.oxygen() == 0) {
            warning = Component.translatable("hud.spacebreaker.warn.no_oxygen");
        } else if (!breathable && o2 < 0.15F) {
            warning = Component.translatable("hud.spacebreaker.warn.low_oxygen");
        } else if (beyond) {
            warning = Component.translatable(s.has(SuitStatusPayload.COLD_TRAP) && s.ambientC() < s.ratingMinC()
                    ? "hud.spacebreaker.warn.cold_trap" : "hud.spacebreaker.warn.rating");
        } else if (body < 35) {
            warning = Component.translatable("hud.spacebreaker.warn.cold");
        } else if (body > 38.5F) {
            warning = Component.translatable("hud.spacebreaker.warn.hot");
        } else if (s.doseRate() > 50 || s.doseMsv() >= 500) {
            warning = Component.translatable("hud.spacebreaker.warn.radiation");
        } else if (s.integrity() < 20) {
            warning = Component.translatable("hud.spacebreaker.warn.damaged");
        } else if (sealed && s.pressureKpa() >= com.pizzaparker07.spacebreaker.content.suit.LifeSupport.STRAIN_KPA) {
            warning = Component.translatable("hud.spacebreaker.warn.strain", Math.round(s.pressureKpa() / 100F));
        } else if (s.has(SuitStatusPayload.MMU) && s.mmuFuel() * 10 < com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem.CAPACITY) {
            warning = Component.translatable("hud.spacebreaker.warn.mmu_low");
        }
        if (warning != null && blink) {
            int ww = font.width(warning);
            int wx = (g.guiWidth() - ww) / 2;
            int wy = g.guiHeight() - 59 - (player.isCreative() ? 0 : 10);
            g.drawString(font, warning, wx, wy, argb(ALARM, alpha), true);
        }
    }

    /** Vanilla-looking bar: dark frame, coloured fill with a lighter top line; marker style for temperatures. */
    private static void drawBar(GuiGraphics g, int x, int y, Row r, float alpha, boolean blink) {
        int h = 5;
        g.fill(x, y, x + BAR_W, y + h, argb(0x101014, 0.75F * alpha));
        g.fill(x + 1, y + 1, x + BAR_W - 1, y + h - 1, argb(0x2A2C33, 0.75F * alpha));
        int inner = BAR_W - 2;
        if (r.marker()) {
            // normal band (35..38.5 °C) and the marker
            int b0 = x + 1 + Math.round(inner * (5F / 14F));
            int b1 = x + 1 + Math.round(inner * (8.5F / 14F));
            g.fill(b0, y + 1, b1, y + h - 1, argb(0x3E6B3A, alpha));
            int mx = x + 1 + Math.round((inner - 2) * r.fill());
            g.fill(mx, y, mx + 2, y + h, argb(r.color(), alpha));
            return;
        }
        int w = Math.round(inner * Mth.clamp(r.fill(), 0F, 1F));
        if (w > 0) {
            g.fill(x + 1, y + 1, x + 1 + w, y + h - 1, argb(r.color(), alpha));
            g.fill(x + 1, y + 1, x + 1 + w, y + 2, argb(lighten(r.color()), alpha));
        }
        if (r.color() == ALARM && !blink) {
            g.fill(x, y, x + BAR_W, y + h, argb(ALARM, 0.25F * alpha));
        }
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 50);
        int gr = Math.min(255, ((rgb >> 8) & 0xFF) + 50);
        int b = Math.min(255, (rgb & 0xFF) + 50);
        return (r << 16) | (gr << 8) | b;
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

}
