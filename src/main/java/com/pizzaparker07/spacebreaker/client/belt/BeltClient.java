package com.pizzaparker07.spacebreaker.client.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.client.sky.PlanetSkyEffects;
import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of the asteroid belt: the airless skies of Ceres and Vesta (black, starry, the sun a small disc from
 * 2.4-2.8 AU, under half its size from Earth) and the rock mite.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class BeltClient {
    private BeltClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("ceres"), new PlanetSkyEffects(Bodies.key("ceres")));
        event.register(Spacebreaker.id("vesta"), new PlanetSkyEffects(Bodies.key("vesta")));
        for (String asteroid : new String[]{"bennu", "ryugu", "eros", "itokawa", "churyumov_gerasimenko", "encke", "halley"}) {
            event.register(Spacebreaker.id(asteroid), new PlanetSkyEffects(Bodies.key(asteroid)));
        }
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RockMiteModel.LAYER, RockMiteModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeltEntities.ROCK_MITE.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new RockMiteModel(ctx.bakeLayer(RockMiteModel.LAYER)), Spacebreaker.id("textures/entity/belt/rock_mite.png"), 1.15F, 0.45F, false));
    }
}
