package com.pizzaparker07.spacebreaker.client.pluto;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.client.jupiter.GiantPlanetSkyEffects;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import org.joml.Matrix4f;

/**
 * Pluto's sky: black and starry like an airless moon's, with Charon hanging still above the near hemisphere - and low
 * over the horizon the thin nitrogen haze, a few separate blue layers that glow when the sun is up (lit from the side,
 * the way they showed against the black sky in the flyby pictures).
 */
public class PlutoSkyEffects extends GiantPlanetSkyEffects {
    /** Haze layers: elevation (degrees) and strength. */
    private static final double[][] LAYERS = {{0.6, 1.0}, {2.2, 0.7}, {4.0, 0.5}, {6.3, 0.35}, {9.0, 0.22}};
    private static final int SEGMENTS = 48;

    public PlutoSkyEffects(ResourceKey<CelestialBody> body, Parent look, ResourceKey<CelestialBody> companion) {
        super(body, look, companion);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        super.renderSky(level, ticks, partialTick, modelViewMatrix, camera, projectionMatrix, isFoggy, setupFog);
        float sun = (float) Math.max(0, SkyMath.sunDirection(level.getTimeOfDay(partialTick), 0).y + 0.15);
        float glow = Math.min(1F, sun * 2.5F);
        if (glow <= 0.01F) {
            return true;
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (double[] layer : LAYERS) {
            double el = Math.toRadians(layer[0]);
            double w = Math.toRadians(0.55);
            float a = (float) (0.32 * layer[1] * glow);
            for (int k = 0; k < SEGMENTS; k++) {
                double a0 = Math.PI * 2 * k / SEGMENTS;
                double a1 = Math.PI * 2 * (k + 1) / SEGMENTS;
                // each layer a soft band: transparent edges, bright middle
                band(bb, m, a0, a1, el - w, el, 0F, a);
                band(bb, m, a0, a1, el, el + w, a, 0F);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void band(BufferBuilder bb, Matrix4f m, double a0, double a1, double e0, double e1, float alpha0, float alpha1) {
        vertex(bb, m, a0, e0, alpha0);
        vertex(bb, m, a1, e0, alpha0);
        vertex(bb, m, a1, e1, alpha1);
        vertex(bb, m, a0, e1, alpha1);
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, double az, double el, float alpha) {
        float x = (float) (Math.cos(el) * Math.cos(az) * 100);
        float y = (float) (Math.sin(el) * 100);
        float z = (float) (Math.cos(el) * Math.sin(az) * 100);
        bb.addVertex(m, x, y, z).setColor(0.45F, 0.65F, 1.0F, alpha);
    }
}
