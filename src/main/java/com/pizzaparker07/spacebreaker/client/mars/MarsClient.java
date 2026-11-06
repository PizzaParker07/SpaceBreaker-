package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsParticles;
import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client side of Mars: sky effects, dusty haze and storm fog, blowing dust, and a dust tint drawn in the GUI layer
 * (on top of any shader pack's final image, so storms stay visible with shaders).
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class MarsClient {
    private MarsClient() {
    }

    @SubscribeEvent
    static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("mars"), new MarsSkyEffects());
        // Mars seen from its moons: 2 atan(R_mars / a)
        event.register(Spacebreaker.id("phobos"), new MarsMoonSkyEffects(com.pizzaparker07.spacebreaker.core.body.Bodies.key("phobos"),
                Math.toDegrees(2 * Math.atan(3389.5 / 9376))));
        event.register(Spacebreaker.id("deimos"), new MarsMoonSkyEffects(com.pizzaparker07.spacebreaker.core.body.Bodies.key("deimos"),
                Math.toDegrees(2 * Math.atan(3389.5 / 23463))));
        for (String body : new String[]{"venus", "mercury", "mars"}) {
            event.register(Spacebreaker.id(body + "_orbit"), new InnerOrbitSkyEffects(body));
        }
    }

    @SubscribeEvent
    static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RustBeetleModel.LAYER, RustBeetleModel::createLayer);
        event.registerLayerDefinition(StonegrazerModel.LAYER, StonegrazerModel::createLayer);
        event.registerLayerDefinition(CanyonLurkerModel.LAYER, CanyonLurkerModel::createLayer);
        event.registerLayerDefinition(PolarWormModel.LAYER, PolarWormModel::createLayer);
        event.registerLayerDefinition(DustDevilModel.LAYER, DustDevilModel::createLayer);
    }

    private static net.minecraft.resources.ResourceLocation tex(String name) {
        return Spacebreaker.id("textures/entity/mars/" + name + ".png");
    }

    @SubscribeEvent
    static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MarsEntities.RUST_BEETLE.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new RustBeetleModel(ctx.bakeLayer(RustBeetleModel.LAYER)), tex("rust_beetle"), 1.3F, 0.45F, false));
        event.registerEntityRenderer(MarsEntities.STONEGRAZER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new StonegrazerModel(ctx.bakeLayer(StonegrazerModel.LAYER)), tex("stonegrazer"), 1F, 0.9F, false));
        event.registerEntityRenderer(MarsEntities.POLAR_WORM.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new PolarWormModel(ctx.bakeLayer(PolarWormModel.LAYER)), tex("polar_worm"), 1F, 0.35F, false));
        event.registerEntityRenderer(MarsEntities.DUST_DEVIL.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new DustDevilModel(ctx.bakeLayer(DustDevilModel.LAYER)), tex("dust_devil"), 1.75F, 0F, true));
        event.registerEntityRenderer(MarsEntities.CANYON_LURKER.get(), ctx -> {
            var r = new SimpleMobRenderer<>(ctx, new CanyonLurkerModel(ctx.bakeLayer(CanyonLurkerModel.LAYER)), tex("canyon_lurker"), 1.2F, 0.75F, false);
            r.addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(r) {
                private final net.minecraft.client.renderer.RenderType eyes = net.minecraft.client.renderer.RenderType.eyes(tex("canyon_lurker_eyes"));

                @Override
                public net.minecraft.client.renderer.RenderType renderType() {
                    return eyes;
                }

                @Override
                public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                                   com.pizzaparker07.spacebreaker.content.mars.entity.CanyonLurker lurker, float a, float b, float c, float d, float e, float f) {
                    if (!lurker.isHidden()) {
                        super.render(pose, buffers, light, lurker, a, b, c, d, e, f);
                    }
                }
            });
            return r;
        });
    }

    @SubscribeEvent
    static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MarsParticles.DUST_MOTE.get(), DustMoteParticle.Provider::new);
    }

    @SubscribeEvent
    static void gui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Spacebreaker.id("dust_storm"), (g, delta) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null || !MarsWeather.dusty(mc.level)) {
                return;
            }
            float s = MarsWeather.stormStrength(mc.level);
            if (s <= 0.01F) {
                return;
            }
            int a = (int) (s * 0.32F * 255);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0x8A5A38);
        });
    }

    private static boolean mars() {
        var level = Minecraft.getInstance().level;
        return level != null && MarsWeather.dusty(level);
    }

    /** A thin, permanent dust haze; storms close the view in to a few dozen blocks. */
    @SubscribeEvent
    static void fog(ViewportEvent.RenderFog event) {
        if (!mars() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float storm = MarsWeather.stormStrength(Minecraft.getInstance().level);
        float far = event.getFarPlaneDistance();
        float end = Mth.lerp(storm, far, Math.min(far, 22F));
        float start = Mth.lerp(storm, far * 0.35F, 0F);
        event.setFarPlaneDistance(end);
        event.setNearPlaneDistance(start);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void fogColour(ViewportEvent.ComputeFogColor event) {
        if (!mars()) {
            return;
        }
        var mc = Minecraft.getInstance();
        if (event.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) {
            return;
        }
        float[] c = MarsSkyEffects.horizonFog(mc.level, event.getCamera(), (float) event.getPartialTick());
        event.setRed(c[0]);
        event.setGreen(c[1]);
        event.setBlue(c[2]);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !MarsWeather.dusty(mc.level)) {
            return;
        }
        MarsWeather.clientTick(mc.level);
        float storm = MarsWeather.stormStrength(mc.level);
        if (storm <= 0.02F) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        int n = (int) (storm * 14);
        for (int i = 0; i < n; i++) {
            double x = cam.x + (mc.level.random.nextDouble() - 0.7) * 24;
            double y = cam.y + (mc.level.random.nextDouble() - 0.4) * 10;
            double z = cam.z + (mc.level.random.nextDouble() - 0.5) * 24;
            if (!mc.level.canSeeSky(net.minecraft.core.BlockPos.containing(x, Math.max(y, cam.y), z))) {
                continue;
            }
            double wind = 0.25 + 0.35 * storm;
            mc.level.addParticle(MarsParticles.DUST_MOTE.get(), x, y, z, wind, (mc.level.random.nextDouble() - 0.5) * 0.04, wind * 0.35);
        }
    }

    @SubscribeEvent
    static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MarsWeather.clearClient();
    }
}
