package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.machine.PumpjackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Pumpjack motion: the crank turns, the pitman arms push the walking beam, the horsehead nods and the bridle
 * strokes the polished rod in the wellhead. All coordinates are pixels in the controller's block, facing north.
 */
public class PumpjackRenderer implements BlockEntityRenderer<PumpjackBlockEntity> {
    public static final ModelResourceLocation BEAM = ModelResourceLocation.standalone(Spacebreaker.id("block/pumpjack_beam"));
    public static final ModelResourceLocation CRANK = ModelResourceLocation.standalone(Spacebreaker.id("block/pumpjack_crank"));
    public static final ModelResourceLocation ROD = ModelResourceLocation.standalone(Spacebreaker.id("block/pumpjack_rod"));

    private static final Vec3 PIVOT = new Vec3(8, 34, 8);
    private static final Vec3 AXLE = new Vec3(8, 12, 24);
    private static final double CRANK_R = 6;
    private static final float SPEED = 0.06F; // rad per tick
    private static final float SWING = 0.2F;

    public PumpjackRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PumpjackBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long now = System.nanoTime();
        if (be.animLastNanos != 0 && be.getBlockState().getValue(MachineBlock.LIT)) {
            float ticks = Math.min(5F, (now - be.animLastNanos) / 50_000_000F);
            be.animPhase = (be.animPhase + ticks * SPEED) % Mth.TWO_PI;
        }
        be.animLastNanos = now;
        float phi = be.animPhase;
        float beamAngle = -SWING * Mth.cos(phi);

        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        var buffer = buffers.getBuffer(RenderType.cutout());
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(MachineBlock.FACING).toYRot() + 180F));
        pose.translate(-0.5F, 0F, -0.5F);

        // walking beam with horsehead
        pose.pushPose();
        pose.translate(PIVOT.x / 16, PIVOT.y / 16, PIVOT.z / 16);
        pose.mulPose(Axis.XP.rotation(beamAngle));
        pose.translate(-0.5F, -0.5F, -0.5F);
        draw(renderer, pose, buffer, be, mc.getModelManager().getModel(BEAM), light, overlay);
        pose.popPose();

        // crank and counterweights
        pose.pushPose();
        pose.translate(AXLE.x / 16, AXLE.y / 16, AXLE.z / 16);
        pose.mulPose(Axis.XP.rotation(phi));
        pose.translate(-0.5F, -0.5F, -0.5F);
        draw(renderer, pose, buffer, be, mc.getModelManager().getModel(CRANK), light, overlay);
        pose.popPose();

        // pitman arms: crank pin -> equalizer at the beam's rear end
        Vec3 pin = AXLE.add(0, CRANK_R * Mth.cos(phi), CRANK_R * Mth.sin(phi));
        Vec3 eq = rotateX(new Vec3(0, -4, 22), beamAngle).add(PIVOT);
        BakedModel rod = mc.getModelManager().getModel(ROD);
        for (double x : new double[]{3, 13}) {
            rod(renderer, pose, buffer, be, rod, new Vec3(x, pin.y, pin.z), new Vec3(x, eq.y, eq.z), light, overlay);
        }
        // bridle and polished rod: horsehead bottom -> wellhead
        Vec3 head = rotateX(new Vec3(0, -14, -16), beamAngle).add(PIVOT);
        rod(renderer, pose, buffer, be, rod, new Vec3(8, 9, -8), new Vec3(8, head.y, head.z), light, overlay);
        pose.popPose();
    }

    private static Vec3 rotateX(Vec3 v, float a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
    }

    private static void rod(ModelBlockRenderer renderer, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, PumpjackBlockEntity be,
                            BakedModel model, Vec3 from, Vec3 to, int light, int overlay) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) {
            return;
        }
        pose.pushPose();
        pose.translate(from.x / 16, from.y / 16, from.z / 16);
        pose.mulPose(Axis.XP.rotation((float) Math.atan2(d.z, d.y)));
        pose.scale(1F, (float) (len / 16), 1F);
        pose.translate(-0.5F, 0F, -0.5F);
        draw(renderer, pose, buffer, be, model, light, overlay);
        pose.popPose();
    }

    private static void draw(ModelBlockRenderer renderer, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, PumpjackBlockEntity be,
                             BakedModel model, int light, int overlay) {
        renderer.renderModel(pose.last(), buffer, be.getBlockState(), model, 1F, 1F, 1F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    @Override
    public AABB getRenderBoundingBox(PumpjackBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 3, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(PumpjackBlockEntity be) {
        return true;
    }
}
