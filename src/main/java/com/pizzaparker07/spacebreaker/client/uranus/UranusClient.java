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

/** Client side of Miranda: Uranus, pale cyan and 22° wide, with its thin dark rings, hanging still in the sky; the clinger. */
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
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CliffClingerModel.LAYER, CliffClingerModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UranusEntities.CLIFF_CLINGER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new CliffClingerModel(ctx.bakeLayer(CliffClingerModel.LAYER)), Spacebreaker.id("textures/entity/uranus/cliff_clinger.png"), 1.0F, 0.4F,
                false));
    }
}
