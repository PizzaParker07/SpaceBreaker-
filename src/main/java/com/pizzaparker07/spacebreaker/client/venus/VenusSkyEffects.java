package com.pizzaparker07.spacebreaker.client.venus;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sky of Venus, by altitude. Under the cloud deck the whole sky is a dim, uniform orange glow (no sun, no stars);
 * inside the clouds everything is a pale yellow-white blur; above them the sky clears to a pale blue with a bright
 * sun, and the cloud tops spread out below like a slowly drifting sea (the 4-day super-rotation winds). Venus spins
 * backwards, so the sun rises in the west. Lighting and fog follow the same bands; with shader packs the biome
 * sky/fog colours carry the same yellow haze.
 */
public class VenusSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    private static final ResourceLocation SEA = Spacebreaker.id("textures/environment/venus_cloud_sea.png");
    static final float[] SURFACE = {0.48F, 0.33F, 0.16F};
    static final float[] CLOUD = {0.86F, 0.80F, 0.60F};
    static final float[] ZENITH = {0.62F, 0.72F, 0.84F};
    static final float[] HORIZON = {0.95F, 0.90F, 0.76F};

    public VenusSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    /** 0 below the clouds, 1 above them. */
    static float above(double y) {
        return (float) Mth.clamp((y - VenusClimate.CLOUD_TOP + 6) / 12.0, 0, 1);
    }

    /** 1 deep inside the cloud band. */
    static float inside(double y) {
        double lo = Mth.clamp((y - VenusClimate.CLOUD_BASE + 8) / 16.0, 0, 1);
        double hi = Mth.clamp((VenusClimate.CLOUD_TOP + 6 - y) / 12.0, 0, 1);
        return (float) Math.min(lo, hi);
    }

    /** Fog / horizon colour at height y for daylight d. */
    public static float[] haze(double y, float day) {
        float a = above(y);
        float in = inside(y);
        float[] c = new float[3];
        for (int i = 0; i < 3; i++) {
            float below = Mth.lerp((float) Mth.clamp((y - VenusClimate.SURFACE) / (VenusClimate.CLOUD_BASE - VenusClimate.SURFACE), 0, 1),
                    SURFACE[i], CLOUD[i] * 0.75F);
            float v = Mth.lerp(a, below, HORIZON[i]);
            v = Mth.lerp(in, v, CLOUD[i]);
            c[i] = v * (0.12F + 0.88F * day);
        }
        return c;
    }

    static float daylight(ClientLevel level, float partialTick) {
        float tod = level.getTimeOfDay(partialTick);
        float sunY = Mth.cos(tod * Mth.TWO_PI);
        return Mth.clamp((sunY + 0.15F) / 0.35F, 0F, 1F);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    /** Dim, orange daylight under the clouds; full sunlight above them. */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, Vector3f colors) {
        Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        double y = cam.getPosition().y;
        float sunlight = (float) VenusClimate.sunlight((int) y);
        float day = daylight(level, partialTicks);
        float s = LightTexture.getBrightness(level.dimensionType(), pixelY) * (0.06F + 0.94F * day) * (0.35F + 0.65F * sunlight);
        float bl = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        float warm = 1 - above(y);
        colors.set(bl + s, bg + s * (1 - 0.18F * warm), bb + s * (1 - 0.45F * warm));
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        double y = camera.getPosition().y;
        float day = daylight(level, partialTick);
        float a = above(y);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        Matrix4f m = ps.last().pose();

        // dome: haze colour at the horizon, blue zenith only above the clouds
        float[] hz = haze(y, day);
        float[] zen = new float[3];
        for (int i = 0; i < 3; i++) {
            zen[i] = Mth.lerp(a, hz[i] * 0.92F, ZENITH[i] * (0.1F + 0.9F * day));
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            // upper cap
            bb.addVertex(m, 0, 100, 0).setColor(zen[0], zen[1], zen[2], 1F);
            bb.addVertex(m, Mth.cos(a0) * 100, 8, Mth.sin(a0) * 100).setColor(hz[0], hz[1], hz[2], 1F);
            bb.addVertex(m, Mth.cos(a1) * 100, 8, Mth.sin(a1) * 100).setColor(hz[0], hz[1], hz[2], 1F);
            // lower cap
            bb.addVertex(m, 0, -100, 0).setColor(hz[0], hz[1], hz[2], 1F);
            bb.addVertex(m, Mth.cos(a0) * 100, 8, Mth.sin(a0) * 100).setColor(hz[0], hz[1], hz[2], 1F);
            bb.addVertex(m, Mth.cos(a1) * 100, 8, Mth.sin(a1) * 100).setColor(hz[0], hz[1], hz[2], 1F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        if (a > 0.01F) {
            // the sun: visible only above the clouds; retrograde rotation mirrors its path (rises in the west)
            float tod = level.getTimeOfDay(partialTick);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(90F));
            ps.mulPose(Axis.XP.rotationDegrees(tod * 360F));
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 0.98F, 0.92F, a);
            float half = (float) (100 * Math.tan(Math.toRadians(0.73 * 2.2 / 2)) / (12F / 32F));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, SUN);
            Matrix4f sm = ps.last().pose();
            BufferBuilder sb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            sb.addVertex(sm, -half, 100F, -half).setUv(0F, 0F);
            sb.addVertex(sm, half, 100F, -half).setUv(1F, 0F);
            sb.addVertex(sm, half, 100F, half).setUv(1F, 1F);
            sb.addVertex(sm, -half, 100F, half).setUv(0F, 1F);
            BufferUploader.drawWithShader(sb.buildOrThrow());
            ps.popPose();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            cloudSea(m, level, partialTick, camera, y, day * a);
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    /** The cloud tops below: a large drifting textured plane at the deck height, fading into the horizon haze. */
    private static void cloudSea(Matrix4f m, ClientLevel level, float partialTick, Camera camera, double camY, float bright) {
        float h = (float) (VenusClimate.CLOUD_TOP - 1 - camY);
        if (h > -1) {
            return;
        }
        Vec3 cam = camera.getPosition();
        double drift = (level.getGameTime() + partialTick) * 0.05;
        float scale = 1F / 48F;
        float u0 = (float) ((cam.x + drift) * scale);
        float v0 = (float) (cam.z * scale);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SEA);
        float b = 0.25F + 0.75F * bright;
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int n = 8;
        float r = 384;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float x0 = -r + 2 * r * i / n;
                float x1 = -r + 2 * r * (i + 1) / n;
                float z0 = -r + 2 * r * j / n;
                float z1 = -r + 2 * r * (j + 1) / n;
                quadVertex(bb, m, x0, h, z0, u0, v0, scale, r, b);
                quadVertex(bb, m, x0, h, z1, u0, v0, scale, r, b);
                quadVertex(bb, m, x1, h, z1, u0, v0, scale, r, b);
                quadVertex(bb, m, x1, h, z0, u0, v0, scale, r, b);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void quadVertex(BufferBuilder bb, Matrix4f m, float x, float y, float z, float u0, float v0, float scale, float r, float b) {
        float d = Math.min(1F, (float) Math.sqrt(x * x + z * z) / r);
        float alpha = 1F - d * d;
        bb.addVertex(m, x, y, z).setUv(u0 + x * scale, v0 + z * scale).setColor(b, b, b * 0.97F, alpha);
    }
}
