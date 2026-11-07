package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.jupiter.GiantPlanetSkyEffects;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of Miranda, Ariel and Titania: Uranus, pale cyan with its thin dark rings, hanging still in the sky (22° wide from
 * Miranda, 15° from Ariel, 7° from Titania); the cliff clinger, the frost moth and the scree roller.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class UranusClient {
    public static final ResourceLocation URANUS_DISC = Spacebreaker.id("textures/environment/uranus_disc.png");
    public static final ResourceLocation URANUS_RINGS = Spacebreaker.id("textures/environment/uranus_rings.png");

    private UranusClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("miranda"), new GiantPlanetSkyEffects(Bodies.key("miranda"),
                new GiantPlanetSkyEffects.Parent(URANUS_DISC, URANUS_RINGS, 1.5, 2.02, 6.0, 1F)));
        event.register(Spacebreaker.id("ariel"), new GiantPlanetSkyEffects(Bodies.key("ariel"),
                new GiantPlanetSkyEffects.Parent(URANUS_DISC, URANUS_RINGS, 1.5, 2.02, 4.0, 1F)));
        event.register(Spacebreaker.id("titania"), new GiantPlanetSkyEffects(Bodies.key("titania"),
                new GiantPlanetSkyEffects.Parent(URANUS_DISC, URANUS_RINGS, 1.5, 2.02, 3.0, 1F)));
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CliffClingerModel.LAYER, CliffClingerModel::createLayer);
        event.registerLayerDefinition(FrostMothModel.LAYER, FrostMothModel::createLayer);
        event.registerLayerDefinition(ScreeRollerModel.LAYER, ScreeRollerModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UranusEntities.CLIFF_CLINGER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new CliffClingerModel(ctx.bakeLayer(CliffClingerModel.LAYER)), Spacebreaker.id("textures/entity/uranus/cliff_clinger.png"), 1.0F, 0.4F,
                false));
        event.registerEntityRenderer(UranusEntities.FROST_MOTH.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new FrostMothModel(ctx.bakeLayer(FrostMothModel.LAYER)), Spacebreaker.id("textures/entity/uranus/frost_moth.png"), 1.0F, 0.2F, false));
        event.registerEntityRenderer(UranusEntities.SCREE_ROLLER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new ScreeRollerModel(ctx.bakeLayer(ScreeRollerModel.LAYER)), Spacebreaker.id("textures/entity/uranus/scree_roller.png"), 1.0F, 0.45F,
                false));
    }
}
