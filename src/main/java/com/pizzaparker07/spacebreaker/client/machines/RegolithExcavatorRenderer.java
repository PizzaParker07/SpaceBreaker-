package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The excavator's bucket wheel, in front of the housing; it turns while the excavator digs. */
public class RegolithExcavatorRenderer<T extends MachineBlockEntity> implements BlockEntityRenderer<T> {
    public static final ModelResourceLocation WHEEL = ModelResourceLocation.standalone(Spacebreaker.id("block/regolith_excavator_wheel"));
    /** Degrees per tick while digging: one turn in six seconds. */
    private static final float SPEED = 3F;
    /** Fixed resting angle so a stopped wheel looks the same on every client. */
    private static final float REST = 11.25F;

    public RegolithExcavatorRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float angle = REST;
        if (be.getBlockState().getValue(MachineBlock.LIT) && be.getLevel() != null) {
            angle += ((be.getLevel().getGameTime() % 120L) + partialTick) * SPEED;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(WHEEL);
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(MachineBlock.FACING).toYRot() + 180F));
        pose.translate(-0.5F, 0F, -0.5F);
        // spin about the hub (8, 7, 1); the wheel digs downwards at the front
        pose.translate(0.5F, 7F / 16F, 1F / 16F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5F, -7F / 16F, -1F / 16F);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(), model, 1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
