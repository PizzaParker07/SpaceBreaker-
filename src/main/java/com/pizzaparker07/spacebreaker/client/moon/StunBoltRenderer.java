package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.StunBolt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** A small full-bright sprite facing the camera. */
public class StunBoltRenderer extends EntityRenderer<StunBolt> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/stun_bolt.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    public StunBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected int getBlockLightLevel(StunBolt bolt, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(StunBolt bolt, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0F, 0.15F, 0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = 0.45F + 0.05F * (float) Math.sin((bolt.tickCount + partialTick) * 1.7F);
        pose.scale(s, s, s);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(TYPE);
        vertex(vc, m, pose, light, -0.5F, -0.5F, 0, 1);
        vertex(vc, m, pose, light, 0.5F, -0.5F, 1, 1);
        vertex(vc, m, pose, light, 0.5F, 0.5F, 1, 0);
        vertex(vc, m, pose, light, -0.5F, 0.5F, 0, 0);
        pose.popPose();
        super.render(bolt, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack pose, int light, float x, float y, float u, float v) {
        vc.addVertex(m, x, y, 0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose.last(), 0F, 1F, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(StunBolt bolt) {
        return TEXTURE;
    }
}
