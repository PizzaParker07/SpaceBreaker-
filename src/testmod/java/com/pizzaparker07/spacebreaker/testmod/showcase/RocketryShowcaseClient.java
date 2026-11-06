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
    /** Destination the star map opens with (body, orbit?) in the showcase. */
    static volatile net.minecraft.resources.ResourceLocation preselect;
    static volatile boolean preselectOrbit;
    /** Keep the cockpit (first-person) view while riding, overriding the rocket camera. */
    static volatile boolean cockpit;

    private RocketryShowcaseClient() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (openMapWhenRiding && mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket) {
            if (!(mc.screen instanceof StarMapScreen)) {
                StarMapScreen map = new StarMapScreen(rocket);
                mc.setScreen(map);
                if (preselect != null) {
                    map.preselect(preselect, preselectOrbit);
                }
            }
        } else if (!openMapWhenRiding && mc.screen instanceof StarMapScreen) {
            mc.setScreen(null);
        }
        if (cockpit && mc.player != null && mc.player.getVehicle() instanceof RocketEntity) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }
    }

    /** Switches the game to Turkish for the screenshots that follow. */
    static void turkish() {
        Minecraft mc = Minecraft.getInstance();
        if (!"tr_tr".equals(mc.options.languageCode)) {
            mc.options.languageCode = "tr_tr";
            mc.getLanguageManager().setSelected("tr_tr");
            mc.reloadResourcePacks();
        }
    }
}
