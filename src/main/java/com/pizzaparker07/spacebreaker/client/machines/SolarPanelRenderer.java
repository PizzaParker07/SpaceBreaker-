package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Renders the moving part of the tracker (panel, rails and torque tube) rotated towards the sun. */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity> {
    public static final ModelResourceLocation HEAD_MODEL = ModelResourceLocation.standalone(Spacebreaker.id("block/solar_panel_head"));
    public static final ModelResourceLocation ARRAY_HEAD_MODEL = ModelResourceLocation.standalone(Spacebreaker.id("block/solar_array_head"));
    private static final float PIVOT_Y = 12.5F / 16F; // must match the torque tube in solar_panel_head.json
    private final ModelResourceLocation head;
    private final double reach;

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
        this(HEAD_MODEL, 0.5);
    }

    private SolarPanelRenderer(ModelResourceLocation head, double reach) {
        this.head = head;
        this.reach = reach;
    }

    /** The three-module station array: same pivot, a longer head reaching a block out north and south. */
    public static SolarPanelRenderer array(BlockEntityRendererProvider.Context context) {
        return new SolarPanelRenderer(ARRAY_HEAD_MODEL, 1.5);
    }

    @Override
    public void render(SolarPanelBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(head);
        pose.pushPose();
        pose.translate(0.5F, PIVOT_Y, 0.5F);
        pose.mulPose(Axis.ZP.rotation(be.getTilt(partialTick)));
        pose.translate(-0.5F, -PIVOT_Y, -0.5F);
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        renderer.renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), be.getBlockState(), model,
                1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolarPanelBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
