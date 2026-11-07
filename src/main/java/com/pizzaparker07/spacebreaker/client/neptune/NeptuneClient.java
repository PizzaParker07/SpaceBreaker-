package com.pizzaparker07.spacebreaker.client.neptune;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.jupiter.GiantPlanetSkyEffects;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.neptune.entity.NeptuneEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/** Client side of Triton: Neptune, deep blue and 8° wide, hanging still in the black sky, and the plume beetle. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class NeptuneClient {
    public static final ResourceLocation NEPTUNE_DISC = Spacebreaker.id("textures/environment/neptune_disc.png");

    private NeptuneClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("triton"), new GiantPlanetSkyEffects(Bodies.key("triton"), GiantPlanetSkyEffects.Parent.plain(NEPTUNE_DISC)));
        event.register(Spacebreaker.id("proteus"), new GiantPlanetSkyEffects(Bodies.key("proteus"), GiantPlanetSkyEffects.Parent.plain(NEPTUNE_DISC)));
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlumeBeetleModel.LAYER, PlumeBeetleModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NeptuneEntities.PLUME_BEETLE.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new PlumeBeetleModel(ctx.bakeLayer(PlumeBeetleModel.LAYER)), Spacebreaker.id("textures/entity/neptune/plume_beetle.png"), 1.0F, 0.45F,
                false));
    }
}
