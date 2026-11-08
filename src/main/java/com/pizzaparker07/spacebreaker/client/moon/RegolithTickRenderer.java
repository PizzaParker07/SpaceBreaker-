package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.RegolithTick;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws ticks, sunk into the dust while burrowed (only the shield and the glinting eyes show). Elders have their own model. */
public class RegolithTickRenderer extends MobRenderer<RegolithTick, RegolithTickModel<RegolithTick>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/regolith_tick.png");
    private static final RenderType EYES = RenderType.eyes(Spacebreaker.id("textures/entity/moon/regolith_tick_eyes.png"));
    private final float scale;

    public RegolithTickRenderer(EntityRendererProvider.Context ctx, float scale) {
        super(ctx, new RegolithTickModel<>(ctx.bakeLayer(RegolithTickModel.LAYER)), 0.4F * scale);
        this.scale = scale;
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(RegolithTick tick, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
        if (tick.isBurrowed()) {
            pose.translate(0F, 0.28F, 0F); // model space is upside down: +y sinks into the ground
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

    @Override
    public void render(RegolithTick tick, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(tick, yaw, partialTick, pose, buffers, light);
    }
}
