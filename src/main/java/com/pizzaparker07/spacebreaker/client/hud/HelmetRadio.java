package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The helmet radio: while mission control speaks (any {@code mission_control.en/tr.*} line, however it was
 * started), a small pulsing wave icon and the line's subtitle show at the top of the visor. Only with a helmet
 * on, in first person; drawn on the GUI layer like the rest of the helmet HUD.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class HelmetRadio {
    /** Keep the caption at least this long, and at most this long if the sound never reports as playing. */
    private static final int MIN_TICKS = 40, MAX_TICKS = 200;
    private static SoundInstance sound;
    private static SbSounds.Line line;
    private static long startTick;

    private HelmetRadio() {
    }

    @SubscribeEvent
    static void onSound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s == null) {
            return;
        }
        SbSounds.Line l = com.pizzaparker07.spacebreaker.content.suit.HelmetRadioLines.lineOf(s.getLocation());
        Minecraft mc = Minecraft.getInstance();
        if (l != null && mc.level != null) {
            sound = event.getSound();
            line = l;
            startTick = mc.level.getGameTime();
        }
    }

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, Spacebreaker.id("helmet_radio"), HelmetRadio::render);
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (line == null || mc.player == null || mc.level == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()
                || !SpaceSuitItem.wearsHelmet(mc.player) || !HudConfig.enabled()) {
            return;
        }
        long age = mc.level.getGameTime() - startTick;
        boolean playing = sound != null && mc.getSoundManager().isActive(sound);
        if (age > MAX_TICKS || (age > MIN_TICKS && !playing)) {
            line = null;
            sound = null;
            return;
        }
        float alpha = HudConfig.opacity();
        Component text = Component.translatable(line.subtitleKey());
        var font = mc.font;
        int textW = font.width(text);
        int w = textW + 22;
        int x = (g.guiWidth() - w) / 2;
        int y = 26;
        g.fill(x, y, x + w, y + 13, argb(0x000000, 0.45F * alpha));
        // pulsing wave: five bars like a radio level meter, lively while the line plays
        float t = (mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false)) * 0.6F;
        for (int i = 0; i < 5; i++) {
            float level = playing ? 0.35F + 0.65F * Math.abs(Mth.sin(t + i * 1.3F) * Mth.cos(t * 0.7F + i)) : 0.2F;
            int h = Math.max(1, Math.round(level * 9));
            int bx = x + 3 + i * 3;
            g.fill(bx, y + 11 - h, bx + 2, y + 11, argb(0x36B1C0, alpha));
        }
        g.drawString(font, text, x + 19, y + 3, argb(0xE6F2F5, alpha), true);
    }

    private static int argb(int rgb, float a) {
        return (Mth.clamp(Math.round(a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
