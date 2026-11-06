package com.pizzaparker07.spacebreaker.client.venus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client side of Venus: altitude-banded sky, haze and lighting; the cloud creatures' renderers. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class VenusClient {
    private VenusClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("venus"), new VenusSkyEffects());
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AcidRayModel.LAYER, AcidRayModel::createLayer);
        event.registerLayerDefinition(CloudJellyModel.LAYER, CloudJellyModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VenusEntities.ACID_RAY.get(), ctx -> new SimpleMobRenderer<>(ctx, new AcidRayModel(ctx.bakeLayer(AcidRayModel.LAYER)),
                Spacebreaker.id("textures/entity/venus/acid_ray.png"), 1.6F, 0.6F, false));
        event.registerEntityRenderer(VenusEntities.CLOUD_JELLY.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new CloudJellyModel(ctx.bakeLayer(CloudJellyModel.LAYER)), Spacebreaker.id("textures/entity/venus/cloud_jelly.png"), 1.3F, 0F, true));
    }

    private static boolean venus() {
        var level = Minecraft.getInstance().level;
        return level != null && level.dimension() == VenusModule.VENUS;
    }

    /** Thick haze: ~60 blocks of visibility at the surface, a whiteout inside the clouds, clear air above. */
    @SubscribeEvent
    static void fog(ViewportEvent.RenderFog event) {
        if (!venus() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        double y = event.getCamera().getPosition().y;
        float far = event.getFarPlaneDistance();
        float in = VenusSkyEffects.inside(y);
        float above = VenusSkyEffects.above(y);
        float end = Mth.lerp(above, Math.min(far, 64F), far);
        end = Mth.lerp(in, end, 18F);
        float start = Mth.lerp(above, 0F, far * 0.4F);
        start = Mth.lerp(in, start, 0F);
        event.setNearPlaneDistance(start);
        event.setFarPlaneDistance(end);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void fogColour(ViewportEvent.ComputeFogColor event) {
        if (!venus() || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        var level = Minecraft.getInstance().level;
        float[] c = VenusSkyEffects.haze(event.getCamera().getPosition().y, VenusSkyEffects.daylight(level, (float) event.getPartialTick()));
        event.setRed(c[0]);
        event.setGreen(c[1]);
        event.setBlue(c[2]);
    }

    static boolean inClouds(double y) {
        return y >= VenusClimate.CLOUD_BASE && y <= VenusClimate.CLOUD_TOP;
    }
}
