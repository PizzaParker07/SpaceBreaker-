package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.CrewArmBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.TowerAttachmentBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The walkway of the crew access arm: reaches out towards the pad, swings 90 degrees back against the tower. */
public class CrewArmRenderer implements BlockEntityRenderer<CrewArmBlockEntity> {
    public static final ModelResourceLocation WALKWAY = ModelResourceLocation.standalone(Spacebreaker.id("block/crew_access_arm_walkway"));

    public CrewArmRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrewArmBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = be.getBlockState().getValue(TowerAttachmentBlock.FACING);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(WALKWAY);
        float swing = be.swing(partialTick);
        float eased = swing * swing * (3 - 2 * swing);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        // hinge on the housing at the back of the block (model coordinates: x = 3, z = 12)
        pose.translate(-5 / 16.0, 0, 4 / 16.0);
        pose.mulPose(Axis.YP.rotationDegrees(-90F * eased));
        pose.translate(5 / 16.0, 0, -4 / 16.0);
        pose.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(), model, 1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrewArmBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(3);
    }
}
