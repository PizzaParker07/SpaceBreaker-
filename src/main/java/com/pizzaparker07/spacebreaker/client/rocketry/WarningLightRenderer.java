package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.WarningLightBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.WarningLightBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * The turning beam of a lit warning light: two amber light fans on opposite sides of the lens, sweeping round
 * once every {@link #TURN_TICKS} ticks. Drawn with the vanilla beacon beam render type (translucent, full bright),
 * so it stays correct under shader packs; the light on the surroundings is the block's own light level.
 */
public class WarningLightRenderer implements BlockEntityRenderer<WarningLightBlockEntity> {
    private static final ResourceLocation BEAM = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    public static final int TURN_TICKS = 40;
    private static final float LENGTH = 2.6F;
    private static final float LENS_Y = 5.5F / 16F;

    public WarningLightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WarningLightBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (be.getLevel() == null || !state.hasProperty(WarningLightBlock.LIT) || !state.getValue(WarningLightBlock.LIT)) {
            return;
        }
        float turn = ((be.getLevel().getGameTime() % TURN_TICKS) + partialTick) / TURN_TICKS * 360F;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(state.getValue(WarningLightBlock.FACING).getRotation());
        pose.translate(0, LENS_Y - 0.5, 0);
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        VertexConsumer vc = buffers.getBuffer(RenderType.beaconBeam(BEAM, true));
        Matrix4f m = pose.last().pose();
        for (int side = 0; side < 2; side++) {
            float dir = side == 0 ? 1F : -1F;
            // a flat fan and a standing fan, so the beam reads from every angle
            fan(vc, m, dir, true);
            fan(vc, m, dir, false);
        }
        pose.popPose();
    }

    /** One fan from the lens outwards along x, widening and fading. */
    private static void fan(VertexConsumer vc, Matrix4f m, float dir, boolean flat) {
        float x0 = 0.22F * dir, x1 = LENGTH * dir;
        float w0 = 0.08F, w1 = 0.55F;
        int r = 255, g = 168, b = 40;
        int a0 = 150, a1 = 0;
        float[][] quad = flat
                ? new float[][]{{x0, 0, -w0}, {x0, 0, w0}, {x1, 0, w1}, {x1, 0, -w1}}
                : new float[][]{{x0, -w0, 0}, {x0, w0, 0}, {x1, w1, 0}, {x1, -w1, 0}};
        int[] alpha = {a0, a0, a1, a1};
        float[] u = {0, 1, 1, 0}, v = {0, 0, 1, 1};
        // both windings: the beacon beam type culls back faces
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int i = pass == 0 ? k : 3 - k;
                vc.addVertex(m, quad[i][0], quad[i][1], quad[i][2]).setColor(r, g, b, alpha[i]).setUv(u[i], v[i])
                        .setOverlay(0).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(WarningLightBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WarningLightBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(LENGTH + 0.5);
    }
}
