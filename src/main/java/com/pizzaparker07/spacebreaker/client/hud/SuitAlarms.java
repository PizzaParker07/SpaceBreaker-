package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.client.audio.MissionControlVoice;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Audible suit alarms for the wearer (only they hear them, not positional): mission control says
 * "oxygen low" over the radio the first time the tanks drop below a quarter, the suit alarm repeats every
 * few seconds below a tenth and back to back when the oxygen is gone or the suit is open in airless
 * surroundings. Matches the blinking HUD warnings.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SuitAlarms {
    /** The alarm clip is two seconds long: back to back when empty, a pause in between below a tenth. */
    private static final int EMPTY_PERIOD = 40;
    private static final int LOW_PERIOD = 80;
    private static boolean quarterWarned;
    private static SoundInstance alarm;

    private SuitAlarms() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean helmet = SpaceSuitItem.wearsHelmet(mc.player);
        HelmetHud.tickFrost(helmet ? ClientSuitState.get(mc.level.getGameTime()) : null);
        if (!helmet) {
            return;
        }
        SuitStatusPayload s = ClientSuitState.get(mc.level.getGameTime());
        if (s == null || s.has(SuitStatusPayload.BREATHABLE) || mc.player.isCreative() || mc.player.isSpectator()) {
            quarterWarned = false;
            return;
        }
        int t = mc.player.tickCount;
        float o2 = s.capacity() > 0 ? (float) s.oxygen() / s.capacity() : 0F;
        boolean empty = !s.has(SuitStatusPayload.SEALED) || s.oxygen() == 0;
        if (empty) {
            if (t % EMPTY_PERIOD == 0) {
                alarm(mc, 0.8F);
            }
        } else if (o2 <= 0.1F) {
            if (t % LOW_PERIOD == 0) {
                alarm(mc, 0.6F);
            }
        } else if (o2 < 0.25F) {
            if (!quarterWarned) {
                quarterWarned = true;
                if (SbConfig.MISSION_CONTROL_VOICE.get()) {
                    MissionControlVoice.say(SbSounds.Line.OXYGEN_LOW);
                } else {
                    alarm(mc, 0.5F);
                }
            }
        } else {
            quarterWarned = false;
        }
    }

    private static void alarm(Minecraft mc, float volume) {
        if (alarm != null) {
            mc.getSoundManager().stop(alarm);
        }
        alarm = SimpleSoundInstance.forUI(SbSounds.SUIT_OXYGEN_ALARM.get(), 1.0F, volume);
        mc.getSoundManager().play(alarm);
    }
}
