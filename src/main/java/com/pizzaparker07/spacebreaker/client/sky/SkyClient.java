package com.pizzaparker07.spacebreaker.client.sky;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client hooks of the planet sky: dimension effects, vacuum fog, clock reset on disconnect. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SkyClient {
    private SkyClient() {
    }

    /** Called from the mod constructor on the client (registers the client config file). */
    public static void init() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, SkyConfig.SPEC, "spacebreaker-worlds-client.toml");
    }

    @SubscribeEvent
    static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("moon"), new PlanetSkyEffects(Bodies.MOON));
    }

    private static boolean vacuumSky() {
        var level = Minecraft.getInstance().level;
        return level != null && level.effects() instanceof PlanetSkyEffects;
    }

    /** No air: no haze. Keep a thin fog band only at the render distance to hide chunk edges. */
    @SubscribeEvent
    static void fog(ViewportEvent.RenderFog event) {
        if (vacuumSky() && event.getMode() == net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) {
            event.setNearPlaneDistance(event.getFarPlaneDistance() * 0.9F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (vacuumSky()) {
            event.setRed(0F);
            event.setGreen(0F);
            event.setBlue(0F);
        }
    }

    @SubscribeEvent
    static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PlanetTime.clearClient();
        EarthDisc.reset();
    }
}
