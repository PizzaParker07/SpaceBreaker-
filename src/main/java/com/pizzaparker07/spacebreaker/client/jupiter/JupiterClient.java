package com.pizzaparker07.spacebreaker.client.jupiter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.GlowSerpent;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of Jupiter's moons: each sky holds Jupiter at its real size and phase (19° from Io, 12° from Europa, 7.5°
 * from Ganymede, 4° from Callisto), and the creatures - the glow serpent's spots are drawn full-bright.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class JupiterClient {
    public static final ResourceLocation JUPITER_DISC = Spacebreaker.id("textures/environment/jupiter_disc.png");

    private JupiterClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        for (String moon : new String[]{"io", "europa", "ganymede", "callisto"}) {
            event.register(Spacebreaker.id(moon), new GiantPlanetSkyEffects(Bodies.key(moon), GiantPlanetSkyEffects.Parent.plain(JUPITER_DISC)));
        }
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GlowSerpentModel.LAYER, GlowSerpentModel::createLayer);
        event.registerLayerDefinition(SulfurCrabModel.LAYER, SulfurCrabModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(JupiterEntities.GLOW_SERPENT.get(), ctx -> {
            var r = new SimpleMobRenderer<GlowSerpent, GlowSerpentModel>(ctx, new GlowSerpentModel(ctx.bakeLayer(GlowSerpentModel.LAYER)),
                    Spacebreaker.id("textures/entity/jupiter/glow_serpent.png"), 1.0F, 0.2F, false);
            RenderType glow = RenderType.eyes(Spacebreaker.id("textures/entity/jupiter/glow_serpent_glow.png"));
            r.addLayer(new EyesLayer<>(r) {
                @Override
                public RenderType renderType() {
                    return glow;
                }

                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, GlowSerpent s, float a, float b, float c, float d, float e, float f) {
                    super.render(pose, buffers, light, s, a, b, c, d, e, f);
                }
            });
            return r;
        });
        event.registerEntityRenderer(JupiterEntities.SULFUR_CRAB.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new SulfurCrabModel(ctx.bakeLayer(SulfurCrabModel.LAYER)), Spacebreaker.id("textures/entity/jupiter/sulfur_crab.png"), 1.0F, 0.5F, false));
    }
}
