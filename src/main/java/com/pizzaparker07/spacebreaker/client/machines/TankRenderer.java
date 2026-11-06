package com.pizzaparker07.spacebreaker.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Draws the fluid behind each tank's window strip, up to the fill level. */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {
    private static final float WIN_LO = 6F / 16F, WIN_HI = 10F / 16F, Y_LO = 2F / 16F, Y_HI = 14F / 16F;
    private static final float OUT = 1F / 16F - 0.002F; // just in front of the shell (shell spans 1..15)

    public TankRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = be.tank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        float level = Y_LO + (Y_HI - Y_LO) * be.tank().fraction();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        int a = Math.max(200, (tint >>> 24) & 0xFF), r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        float u0 = sprite.getU(6 / 16F), u1 = sprite.getU(10 / 16F);
        float v0 = sprite.getV(1 - level), v1 = sprite.getV(1 - Y_LO);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            float nx = d.getStepX(), nz = d.getStepZ();
            // quad in the plane just outside the shell face, between window edges
            float[][] c = switch (d) {
                case NORTH -> new float[][]{{WIN_HI, OUT}, {WIN_LO, OUT}};
                case SOUTH -> new float[][]{{WIN_LO, 1 - OUT}, {WIN_HI, 1 - OUT}};
                case WEST -> new float[][]{{OUT, WIN_LO}, {OUT, WIN_HI}};
                default -> new float[][]{{1 - OUT, WIN_HI}, {1 - OUT, WIN_LO}};
            };
            vertex(vc, m, c[0][0], Y_LO, c[0][1], u0, v1, r, g, b, a, light, nx, nz);
            vertex(vc, m, c[1][0], Y_LO, c[1][1], u1, v1, r, g, b, a, light, nx, nz);
            vertex(vc, m, c[1][0], level, c[1][1], u1, v0, r, g, b, a, light, nx, nz);
            vertex(vc, m, c[0][0], level, c[0][1], u0, v0, r, g, b, a, light, nx, nz);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int r, int g, int b, int a,
                               int light, float nx, float nz) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(nx, 0, nz);
    }
}
