package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.SpentStageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class SpentStageRenderer extends EntityRenderer<SpentStageEntity> {
    public SpentStageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpentStageEntity stage, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        RocketGeometry geo = stage.geometry();
        if (geo == null) {
            return;
        }
        float age = stage.tickCount + partialTick;
        double mid = geo.height() / 32.0;
        pose.pushPose();
        pose.translate(0, mid, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-stage.startTilt() - stage.spin() * age));
        pose.mulPose(Axis.XP.rotationDegrees(stage.spin() * age * 0.4F));
        pose.translate(0, -mid, 0);
        RocketStackRenderer.render(stage.layout(), geo, pose, buffers, light, 0F);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpentStageEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
