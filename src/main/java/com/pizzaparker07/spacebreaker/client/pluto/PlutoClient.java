package com.pizzaparker07.spacebreaker.client.pluto;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.jupiter.GiantPlanetSkyEffects;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.pluto.entity.PlutoEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of Pluto and Charon: each in the other's sky, never moving (the pair is locked face to face) - Charon a
 * grey disc with its red cap 3.5° wide over Pluto, Pluto 7° wide over Charon with the bright heart turned toward it -
 * Pluto's blue haze layers, and the creatures.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class PlutoClient {
    public static final ResourceLocation PLUTO_DISC = Spacebreaker.id("textures/environment/pluto_disc.png");
    public static final ResourceLocation CHARON_DISC = Spacebreaker.id("textures/environment/charon_disc.png");

    private PlutoClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("pluto"), new PlutoSkyEffects(Bodies.key("pluto"), GiantPlanetSkyEffects.Parent.plain(CHARON_DISC),
                Bodies.key("charon")));
        event.register(Spacebreaker.id("charon"), new GiantPlanetSkyEffects(Bodies.key("charon"), GiantPlanetSkyEffects.Parent.plain(PLUTO_DISC)));
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NitrogenSliderModel.LAYER, NitrogenSliderModel::createLayer);
        event.registerLayerDefinition(FrostTortoiseModel.LAYER, FrostTortoiseModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PlutoEntities.NITROGEN_SLIDER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new NitrogenSliderModel(ctx.bakeLayer(NitrogenSliderModel.LAYER)), Spacebreaker.id("textures/entity/pluto/nitrogen_slider.png"), 1.0F, 0.5F,
                false));
        event.registerEntityRenderer(PlutoEntities.FROST_TORTOISE.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new FrostTortoiseModel(ctx.bakeLayer(FrostTortoiseModel.LAYER)), Spacebreaker.id("textures/entity/pluto/frost_tortoise.png"), 1.0F, 0.6F,
                false));
    }
}
