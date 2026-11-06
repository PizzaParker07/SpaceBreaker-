package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The press ram strokes down and up while the press works. */
public class PressRenderer implements BlockEntityRenderer<ProcessingMachineBlockEntity> {
    public static final ModelResourceLocation RAM = ModelResourceLocation.standalone(Spacebreaker.id("block/press_ram"));

    public PressRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ProcessingMachineBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float drop = 0F;
        if (be.getBlockState().getValue(MachineBlock.LIT) && be.getLevel() != null) {
            float t = (be.getLevel().getGameTime() + partialTick) / 30F * Mth.TWO_PI;
            drop = (float) Math.pow(Math.max(0F, Mth.sin(t)), 3) * 3.5F / 16F;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(RAM);
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(MachineBlock.FACING).toYRot() + 180F));
        pose.translate(-0.5F, -drop, -0.5F);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(), model, 1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
