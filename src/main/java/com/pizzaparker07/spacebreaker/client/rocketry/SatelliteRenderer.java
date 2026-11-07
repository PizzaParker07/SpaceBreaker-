package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws a released satellite with its part's 3D model, turning slowly. */
public class SatelliteRenderer extends EntityRenderer<SatelliteEntity> {
    public SatelliteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(SatelliteEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(e.part()));
        if (stack.isEmpty()) {
            return;
        }
        float time = e.tickCount + partialTick;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot() + time * 0.4F));
        pose.mulPose(Axis.XP.rotationDegrees(8F * (float) Math.sin(time * 0.01)));
        pose.translate(0, 0.5, 0);
        var items = Minecraft.getInstance().getItemRenderer();
        items.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, items.getModel(stack, null, null, 0));
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
