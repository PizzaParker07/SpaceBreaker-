package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Audible suit alarms for the wearer (only they hear them): a chime when the tanks drop below a quarter,
 * a beep every few seconds below a tenth, and a fast alarm when the oxygen is gone or the suit is open in
 * airless surroundings. Matches the blinking HUD warnings.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SuitAlarms {
    private static boolean quarterWarned;

    private SuitAlarms() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !SpaceSuitItem.wearsHelmet(mc.player)) {
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
            if (t % 10 == 0) {
                mc.player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.7F, t % 20 == 0 ? 1.8F : 1.4F);
            }
        } else if (o2 < 0.1F) {
            if (t % 60 == 0) {
                mc.player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.6F, 1.6F);
            }
        } else if (o2 < 0.25F) {
            if (!quarterWarned) {
                mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7F, 1.2F);
                quarterWarned = true;
            }
        } else {
            quarterWarned = false;
        }
    }
}
