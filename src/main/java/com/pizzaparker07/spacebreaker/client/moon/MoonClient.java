package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registrations of the Moon: creature models and renderers. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class MoonClient {
    private MoonClient() {
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RegolithTickModel.LAYER, RegolithTickModel::createLayer);
        event.registerLayerDefinition(ElderRegolithTickModel.LAYER, ElderRegolithTickModel::createLayer);
        event.registerLayerDefinition(DustSwimmerModel.LAYER, DustSwimmerModel::createLayer);
        event.registerLayerDefinition(SurveyDroneModel.LAYER, SurveyDroneModel::createLayer);
        event.registerLayerDefinition(GlimmerBeetleModel.LAYER, GlimmerBeetleModel::createLayer);
        event.registerLayerDefinition(HeatSiphonModel.LAYER, HeatSiphonModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MoonEntities.REGOLITH_TICK.get(), ctx -> new RegolithTickRenderer(ctx, 1F));
        event.registerEntityRenderer(MoonEntities.ELDER_REGOLITH_TICK.get(), ElderRegolithTickRenderer::new);
        event.registerEntityRenderer(MoonEntities.DUST_SWIMMER.get(), DustSwimmerRenderer::new);
        event.registerEntityRenderer(MoonEntities.SURVEY_DRONE.get(), SurveyDroneRenderer::new);
        event.registerEntityRenderer(MoonEntities.STUN_BOLT.get(), StunBoltRenderer::new);
        event.registerEntityRenderer(MoonEntities.GLIMMER_BEETLE.get(), GlimmerBeetleRenderer::new);
        event.registerEntityRenderer(MoonEntities.HEAT_SIPHON.get(), HeatSiphonRenderer::new);
    }
}
