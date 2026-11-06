package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.GlimmerBeetle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** Glimmer beetle with a softly pulsing, full-bright glow layer (dimmer while its shell regrows). */
public class GlimmerBeetleRenderer extends MobRenderer<GlimmerBeetle, GlimmerBeetleModel<GlimmerBeetle>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/glimmer_beetle.png");
    private static final ResourceLocation GLOW = Spacebreaker.id("textures/entity/moon/glimmer_beetle_glow.png");

    public GlimmerBeetleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GlimmerBeetleModel<>(ctx.bakeLayer(GlimmerBeetleModel.LAYER)), 0.25F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GlimmerBeetle beetle) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<GlimmerBeetle, GlimmerBeetleModel<GlimmerBeetle>> {
        Glow(RenderLayerParent<GlimmerBeetle, GlimmerBeetleModel<GlimmerBeetle>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, GlimmerBeetle beetle, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float pulse = 0.8F + 0.2F * Mth.sin((ageInTicks + beetle.getId() * 13) * 0.08F);
            int v = (int) (255 * Mth.clamp(beetle.glow() * pulse, 0F, 1F));
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255, v, v, v));
        }
    }
}
