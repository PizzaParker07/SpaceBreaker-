package com.pizzaparker07.spacebreaker.client.saturn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.jupiter.GiantPlanetSkyEffects;
import com.pizzaparker07.spacebreaker.client.mars.SimpleMobRenderer;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import com.pizzaparker07.spacebreaker.content.saturn.TitanWeather;
import com.pizzaparker07.spacebreaker.content.saturn.entity.GeyserDrifter;
import com.pizzaparker07.spacebreaker.content.saturn.entity.SaturnEntities;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client side of Saturn's moons: Titan's hazy orange sky and its methane rain, Enceladus' black sky with Saturn 28°
 * wide and its rings seen almost edge-on, and the creatures.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SaturnClient {
    public static final ResourceLocation SATURN_DISC = Spacebreaker.id("textures/environment/saturn_disc.png");
    public static final ResourceLocation SATURN_RINGS = Spacebreaker.id("textures/environment/saturn_rings.png");

    private SaturnClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Spacebreaker.id("titan"), new TitanSkyEffects());
        event.register(Spacebreaker.id("enceladus"), new GiantPlanetSkyEffects(Bodies.key("enceladus"),
                new GiantPlanetSkyEffects.Parent(SATURN_DISC, SATURN_RINGS, 1.24, 2.27, 4.0, 1F)));
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MethaneSkaterModel.LAYER, MethaneSkaterModel::createLayer);
        event.registerLayerDefinition(GeyserDrifterModel.LAYER, GeyserDrifterModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SaturnEntities.METHANE_SKATER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new MethaneSkaterModel(ctx.bakeLayer(MethaneSkaterModel.LAYER)), Spacebreaker.id("textures/entity/saturn/methane_skater.png"), 1.0F, 0.5F, false));
        event.registerEntityRenderer(SaturnEntities.GEYSER_DRIFTER.get(), ctx -> {
            var r = new SimpleMobRenderer<GeyserDrifter, GeyserDrifterModel>(ctx, new GeyserDrifterModel(ctx.bakeLayer(GeyserDrifterModel.LAYER)),
                    Spacebreaker.id("textures/entity/saturn/geyser_drifter.png"), 1.0F, 0.3F, true);
            RenderType glow = RenderType.eyes(Spacebreaker.id("textures/entity/saturn/geyser_drifter_glow.png"));
            r.addLayer(new EyesLayer<>(r) {
                @Override
                public RenderType renderType() {
                    return glow;
                }

                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, GeyserDrifter d, float a, float b, float c, float e, float f,
                                   float g) {
                    super.render(pose, buffers, light, d, a, b, c, e, f, g);
                }
            });
            return r;
        });
    }

    /** Methane rain on Titan: big, slow drops falling around the player while a shower passes. */
    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.dimension() != SaturnModule.TITAN) {
            return;
        }
        float rain = TitanWeather.rain(mc.level.getGameTime());
        if (rain <= 0) {
            return;
        }
        RandomSource r = mc.level.random;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int drops = (int) (30 * rain);
        for (int i = 0; i < drops; i++) {
            double x = mc.player.getX() + (r.nextDouble() - 0.5) * 28;
            double z = mc.player.getZ() + (r.nextDouble() - 0.5) * 28;
            int ground = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            double top = Math.max(ground, mc.player.getY()) + 6 + r.nextDouble() * 10;
            if (r.nextInt(4) == 0) {
                mc.level.addParticle(ParticleTypes.RAIN, x, ground + 0.05, z, 0, 0, 0);
            } else {
                mc.level.addParticle(ParticleTypes.FALLING_WATER, x, top, z, 0, -0.05, 0);
            }
        }
    }
}
