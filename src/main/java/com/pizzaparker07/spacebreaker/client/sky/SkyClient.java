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
        // the Earth–Moon travel scene (its dimension names this effects id)
        event.register(Spacebreaker.id("space_earth_moon"),
                new SpaceSceneSky(com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale.EARTH_MOON));
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

    /**
     * Planets in skies this package does not draw itself (Earth's vanilla sky, other worlds' own sky effects):
     * drawn right after the sky, in vanilla's sun frame, fading with the stars.
     */
    @SubscribeEvent
    static void afterSky(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if ("blockyperf".equals(System.getProperty("spacebreaker.shotPrefix")) && net.minecraft.Util.getMillis() - lastFpsLog > 10000) {
            // measurement runs: frame rate whatever draws the planets
            lastFpsLog = net.minecraft.Util.getMillis();
            Spacebreaker.LOGGER.info("Sky fps {} ({})", Minecraft.getInstance().getFps(), SkyConfig.planetStyle());
        }
        // development camera for the travel scene (showcase only): draws the scene from a given scene block position
        String devCamera = System.getProperty("spacebreaker.spaceCamera");
        if (devCamera != null && !devCamera.isEmpty()) {
            // "x,y,z" in scene blocks, or "body id|dx,dy,dz" to ride along with a body
            String follow = null;
            if (devCamera.contains("|")) {
                follow = devCamera.substring(0, devCamera.indexOf('|'));
                devCamera = devCamera.substring(devCamera.indexOf('|') + 1);
            }
            String[] xyz = devCamera.split(",");
            com.mojang.blaze3d.systems.RenderSystem.clearColor(0F, 0F, 0F, 1F);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            SpaceSceneSky.render(level, event.getModelViewMatrix(), event.getProjectionMatrix(),
                    new org.joml.Vector3d(Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2])), follow,
                    com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale.EARTH_MOON);
            return;
        }
        // micrometeorite streaks over any world with a shower
        if (com.pizzaparker07.spacebreaker.content.worldgen.ShowerClientState.active(level.dimension())) {
            com.mojang.blaze3d.vertex.PoseStack sky = new com.mojang.blaze3d.vertex.PoseStack();
            sky.mulPose(event.getModelViewMatrix());
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            net.minecraft.client.renderer.FogRenderer.setupNoFog();
            MeteorStreaks.render(level, sky, pt);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }
        if (level.effects() instanceof PlanetSkyEffects airless) {
            com.mojang.blaze3d.vertex.PoseStack sky = new com.mojang.blaze3d.vertex.PoseStack();
            sky.mulPose(event.getModelViewMatrix());
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            net.minecraft.client.renderer.FogRenderer.setupNoFog();
            airless.renderHorizonGlow(level, sky);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            return;
        }
        if (level.effects().skyType() == net.minecraft.client.renderer.DimensionSpecialEffects.SkyType.END) {
            return;
        }
        float visibility = Math.min(1F, level.getStarBrightness(pt) * 2F) * (1F - level.getRainLevel(pt));
        if (visibility <= 0.01F) {
            return;
        }
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        PlanetDots.vanillaSunFrame(pose, level, pt);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        net.minecraft.client.renderer.FogRenderer.setupNoFog();
        PlanetDots.render(level, pose, event.getProjectionMatrix(), visibility);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static long lastFpsLog;

    @SubscribeEvent
    static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PlanetTime.clearClient();
        EarthDisc.reset();
        BlockyBodyRenderer.reset();
        com.pizzaparker07.spacebreaker.content.worldgen.ShowerClientState.clear();
        MeteorStreaks.clear();
    }
}
