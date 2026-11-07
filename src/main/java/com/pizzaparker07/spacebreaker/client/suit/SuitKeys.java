package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.platform.InputConstants;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.net.LampModePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Suit keys: H switches the helmet lamps between wide and long beam (sneak + use stays the sun visor). */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SuitKeys {
    public static final KeyMapping HELMET_LAMP = new KeyMapping("key.spacebreaker.helmet_lamp", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.spacebreaker");

    private SuitKeys() {
    }

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(HELMET_LAMP);
        NeoForge.EVENT_BUS.addListener(SuitKeys::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (HELMET_LAMP.consumeClick()) {
            if (mc.player != null && SpaceSuitItem.wearsHelmet(mc.player)) {
                PacketDistributor.sendToServer(LampModePayload.INSTANCE);
            }
        }
    }
}
