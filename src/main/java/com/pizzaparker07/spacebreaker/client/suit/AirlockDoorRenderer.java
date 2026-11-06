package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Slides the two panels of an airlock door half apart (into the frame) as the door opens. */
public class AirlockDoorRenderer implements BlockEntityRenderer<AirlockDoorBlockEntity> {
    public static final ModelResourceLocation LEFT_LOWER = model("airlock_door_panel_left_lower");
    public static final ModelResourceLocation RIGHT_LOWER = model("airlock_door_panel_right_lower");
    public static final ModelResourceLocation LEFT_UPPER = model("airlock_door_panel_left_upper");
    public static final ModelResourceLocation RIGHT_UPPER = model("airlock_door_panel_right_upper");
    /** Panel travel when fully open, in blocks (each panel is 6 px wide). */
    private static final float STROKE = 5.5F / 16F;

    public AirlockDoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(Spacebreaker.id("block/" + name));
    }

    @Override
    public void render(AirlockDoorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof AirlockDoorBlock)) {
            return;
        }
        boolean upper = state.getValue(AirlockDoorBlock.HALF) == DoubleBlockHalf.UPPER;
        float slide = be.getOpenness(partialTick) * STROKE;
        var models = Minecraft.getInstance().getModelManager();
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(AirlockDoorBlock.FACING).toYRot()));
        pose.translate(-0.5F, 0F, -0.5F);
        for (int side = -1; side <= 1; side += 2) {
            BakedModel model = models.getModel(side < 0 ? (upper ? LEFT_UPPER : LEFT_LOWER) : (upper ? RIGHT_UPPER : RIGHT_LOWER));
            pose.pushPose();
            pose.translate(side * slide, 0F, 0F);
            renderer.renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), state, model, 1F, 1F, 1F, light, overlay,
                    ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AirlockDoorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.5);
    }
}
