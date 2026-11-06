package com.pizzaparker07.spacebreaker.client.audio;

import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Plays mission control announcements to the local player, as if heard over the helmet radio
 * (not positional). Turkish lines when the game language is Turkish, English otherwise.
 * Client only: call it from client code when the synced rocket state changes.
 */
public final class MissionControlVoice {
    private static SoundInstance current;

    private MissionControlVoice() {
    }

    /** Speaks {@code line}; a new line cuts the previous one off so announcements never overlap. */
    public static void say(SbSounds.Line line) {
        Minecraft mc = Minecraft.getInstance();
        if (line == null || mc.player == null || !SbConfig.MISSION_CONTROL_VOICE.get()) {
            return;
        }
        if (current != null) {
            mc.getSoundManager().stop(current);
        }
        if (!line.countdown) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SbSounds.RADIO_IN.get(), 1.0F, 0.5F));
        }
        current = SimpleSoundInstance.forUI(line.event(isTurkish(mc)), 1.0F, 1.0F);
        mc.getSoundManager().play(current);
    }

    /** Speaks the countdown number for {@code seconds} left (10 to 1); other values are ignored. */
    public static void countdown(int seconds) {
        say(SbSounds.Line.countdown(seconds));
    }

    static boolean isTurkish(Minecraft mc) {
        return mc.getLanguageManager().getSelected().startsWith("tr");
    }
}
