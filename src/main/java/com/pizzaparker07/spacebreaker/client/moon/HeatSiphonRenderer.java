package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.HeatSiphon;
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

/** Heat siphon with a full-bright core glow that grows with the energy it has drunk. */
public class HeatSiphonRenderer extends MobRenderer<HeatSiphon, HeatSiphonModel<HeatSiphon>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/heat_siphon.png");
    private static final ResourceLocation GLOW = Spacebreaker.id("textures/entity/moon/heat_siphon_glow.png");

    public HeatSiphonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HeatSiphonModel<>(ctx.bakeLayer(HeatSiphonModel.LAYER)), 0.45F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HeatSiphon mob) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<HeatSiphon, HeatSiphonModel<HeatSiphon>> {
        Glow(RenderLayerParent<HeatSiphon, HeatSiphonModel<HeatSiphon>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, HeatSiphon mob, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float pulse = 0.85F + 0.15F * Mth.sin((ageInTicks + mob.getId() * 7) * 0.12F);
            int v = (int) (255 * Mth.clamp(mob.glow() * pulse, 0F, 1F));
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255, v, v, v));
        }
    }
}
