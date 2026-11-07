package com.pizzaparker07.spacebreaker.client.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.client.sky.PlanetSkyEffects;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities;
import com.pizzaparker07.spacebreaker.content.mercury.TerminatorCrawler;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of Mercury: the airless planet sky (black, starry, with the sun drawn at its real size from 0.39 AU,
 * nearly three times wider than from Earth) and the crawler's renderer.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class MercuryClient {
    private MercuryClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("mercury"), new MercurySkyEffects());
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TerminatorCrawlerModel.LAYER, TerminatorCrawlerModel::createLayer);
        event.registerLayerDefinition(HollowSnailModel.LAYER, HollowSnailModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MercuryEntities.HOLLOW_SNAIL.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new HollowSnailModel(ctx.bakeLayer(HollowSnailModel.LAYER)), Spacebreaker.id("textures/entity/mercury/hollow_snail.png"), 1.2F, 0.4F, false));
        event.registerEntityRenderer(MercuryEntities.TERMINATOR_CRAWLER.get(), ctx -> {
            var r = new SimpleMobRenderer<>(ctx, new TerminatorCrawlerModel(ctx.bakeLayer(TerminatorCrawlerModel.LAYER)),
                    Spacebreaker.id("textures/entity/mercury/terminator_crawler.png"), 1.1F, 0.55F, false);
            RenderType eyes = RenderType.eyes(Spacebreaker.id("textures/entity/mercury/terminator_crawler_eyes.png"));
            r.addLayer(new EyesLayer<>(r) {
                @Override
                public RenderType renderType() {
                    return eyes;
                }

                @Override
                public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                                   TerminatorCrawler c, float a, float b, float cc, float d, float e, float f) {
                    if (!c.isBurrowed()) {
                        super.render(pose, buffers, light, c, a, b, cc, d, e, f);
                    }
                }
            });
            return r;
        });
    }
}
