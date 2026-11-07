package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Renders the moving part of the tracker (panel, rails and torque tube) rotated towards the sun. */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity> {
    public static final ModelResourceLocation HEAD_MODEL = ModelResourceLocation.standalone(Spacebreaker.id("block/solar_panel_head"));
    public static final ModelResourceLocation ARRAY_HEAD_MODEL = ModelResourceLocation.standalone(Spacebreaker.id("block/solar_array_head"));
    private static final float PIVOT_Y = 12.5F / 16F; // must match the torque tube in solar_panel_head.json
    /** Dust below this cover is not drawn. */
    private static final float DUST_SHOWN = 0.05F;
    private static final net.minecraft.resources.ResourceLocation DUST_TEXTURE = net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/red_sand");
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
        float dust = be.dust();
        if (dust > DUST_SHOWN) {
            renderDust(pose, buffers.getBuffer(RenderType.translucent()), dust, light);
        }
        pose.popPose();
    }

    /** A rust-coloured film of settled dust over the cells; thicker cover is more opaque. */
    private void renderDust(PoseStack pose, VertexConsumer out, float dust, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(DUST_TEXTURE);
        float alpha = Math.min(1F, dust * 0.45F); // the cells still show through: full cover keeps 60% output
        float y = 15.05F / 16F;
        float z0 = (float) (0.5 - reach);
        float z1 = (float) (0.5 + reach);
        PoseStack.Pose last = pose.last();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        // one texture tile per block along the array so the grains keep their size
        int tiles = Math.max(1, Math.round(z1 - z0));
        for (int i = 0; i < tiles; i++) {
            float a = z0 + i;
            float b = Math.min(z1, a + 1);
            vertex(out, last, 0, y, a, u0, sprite.getV0(), alpha, light);
            vertex(out, last, 0, y, b, u0, sprite.getV1(), alpha, light);
            vertex(out, last, 1, y, b, u1, sprite.getV1(), alpha, light);
            vertex(out, last, 1, y, a, u1, sprite.getV0(), alpha, light);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, float alpha, int light) {
        out.addVertex(pose, x, y, z).setColor(0.86F, 0.56F, 0.36F, alpha).setUv(u, v).setLight(light).setNormal(pose, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(SolarPanelBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
