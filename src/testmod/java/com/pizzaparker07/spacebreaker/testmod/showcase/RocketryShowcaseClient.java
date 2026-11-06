package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.rocketry.StarMapScreen;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Showcase helper (dev only): opens the star map once the player sits in the scene's rocket. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class RocketryShowcaseClient {
    static volatile boolean openMapWhenRiding;

    private RocketryShowcaseClient() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (openMapWhenRiding && mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket) {
            if (!(mc.screen instanceof StarMapScreen)) {
                mc.setScreen(new StarMapScreen(rocket));
            }
        } else if (!openMapWhenRiding && mc.screen instanceof StarMapScreen) {
            mc.setScreen(null);
        }
    }
}
