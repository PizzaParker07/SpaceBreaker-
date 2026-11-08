package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.RegolithTick;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws the Elder Regolith Tick: its own armoured model, glowing eyes, half sunk into the dust while burrowed. */
public class ElderRegolithTickRenderer extends MobRenderer<RegolithTick, ElderRegolithTickModel<RegolithTick>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/elder_regolith_tick.png");
    private static final RenderType EYES = RenderType.eyes(Spacebreaker.id("textures/entity/moon/elder_regolith_tick_eyes.png"));
    private static final float SCALE = 1.3F;

    public ElderRegolithTickRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ElderRegolithTickModel<>(ctx.bakeLayer(ElderRegolithTickModel.LAYER)), 0.7F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(RegolithTick tick, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
        if (tick.isBurrowed()) {
            pose.translate(0F, 0.4F, 0F); // model space is upside down: +y sinks into the ground
        }
    }

    @Override
    protected float getShadowRadius(RegolithTick tick) {
        return tick.isBurrowed() ? 0F : super.getShadowRadius(tick);
    }

    @Override
    public ResourceLocation getTextureLocation(RegolithTick tick) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(RegolithTick tick) {
        return 180F;
    }
}
