package com.pizzaparker07.spacebreaker.client.saturn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnData;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Titan's sky: a thick orange photochemical haze with no stars and no blue, darker brown-orange toward the zenith, the
 * sun only a soft pale smudge through it, and Saturn no more than a very faint pale glow where it hangs behind the haze
 * (no disc). The haze is drawn as a smooth colour dome (no texture), fog is thick (the horizon fades into orange a few
 * chunks away), and daylight is dim and warm.
 */
public class TitanSkyEffects extends DimensionSpecialEffects {
    private static final float[] HORIZON = rgb(SaturnData.TITAN_FOG);
    private static final float[] ZENITH = rgb(0x6E4422);
    private static final int BANDS = 12;
    private static final int SEGMENTS = 32;
    /** Saturn's glow through the haze: pale gold, radius on the sky sphere (100 units out) and peak opacity. */
    private static final float[] SATURN = rgb(0xE6D3A3);
    private static final float SATURN_GLOW_RADIUS = 9F;
    private static final float SATURN_GLOW_ALPHA = 0.09F;
    private ClientLevel cachedLevel;
    private double poleDistance = 6000;

    public TitanSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    private static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F};
    }

    /** Daylight 0..1 from the sun's height (smoothed: the haze scatters light well before sunrise and after sunset). */
    public static float daylight(float timeOfDay) {
        double h = SkyMath.sunDirection(timeOfDay, 0).y;
        double t = Mth.clamp((h + 0.12) / 0.4, 0, 1);
        return (float) (t * t * (3 - 2 * t));
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        float d = 0.12F + 0.88F * brightness;
        return new Vec3(HORIZON[0] * d, HORIZON[1] * d, HORIZON[2] * d);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, org.joml.Vector3f colors) {
        float day = daylight(level.getTimeOfDay(partialTicks));
        float sky = LightTexture.getBrightness(level.dimensionType(), pixelY) * (0.05F + 0.6F * day);
        float bl = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        // warm, dim haze light
        colors.set(bl + sky, bg + sky * 0.8F, bb + sky * 0.58F);
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
        float timeOfDay = level.getTimeOfDay(partialTick);
        float day = 0.1F + 0.9F * daylight(timeOfDay);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);
        Matrix4f m = ps.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < BANDS; i++) {
            double e0 = Math.toRadians(-20 + 110.0 * i / BANDS);
            double e1 = Math.toRadians(-20 + 110.0 * (i + 1) / BANDS);
            for (int k = 0; k < SEGMENTS; k++) {
                double a0 = Math.PI * 2 * k / SEGMENTS;
                double a1 = Math.PI * 2 * (k + 1) / SEGMENTS;
                dome(bb, m, a0, e0, day);
                dome(bb, m, a1, e0, day);
                dome(bb, m, a1, e1, day);
                dome(bb, m, a0, e1, day);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        saturnGlow(level, camera, m, day);
        // the sun: a soft pale glow, no disc
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
        ps.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360F));
        Matrix4f sm = ps.last().pose();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        BufferBuilder sun = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        sun.addVertex(sm, 0, 100, 0).setColor(1F, 0.88F, 0.66F, 0.32F * day);
        for (int k = 0; k <= 24; k++) {
            double a = Math.PI * 2 * k / 24;
            sun.addVertex(sm, (float) (Math.cos(a) * 14), 100, (float) (Math.sin(a) * 14)).setColor(1F, 0.8F, 0.5F, 0F);
        }
        BufferUploader.drawWithShader(sun.buildOrThrow());
        ps.popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    /**
     * Saturn behind the haze: where it hangs (still, over the near side), a soft pale-gold glow that fades out to nothing,
     * a little stronger in daylight when the haze is lit. Not a disc: the haze hides the planet itself.
     */
    private void saturnGlow(ClientLevel level, Camera camera, Matrix4f m, float day) {
        if (level != cachedLevel) {
            cachedLevel = level;
            poleDistance = level.registryAccess().registry(com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile.REGISTRY)
                    .flatMap(r -> r.stream().filter(t -> t.body().equals(com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.TITAN.location())).findFirst())
                    .map(com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile::poleDistance).orElse(6000.0);
        }
        Vec3 cam = camera.getPosition();
        double lat = com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain.latitude(cam.z, poleDistance);
        double lon = 90 * cam.x / poleDistance;
        org.joml.Vector3d e = SkyMath.parentDirection(lat, lon, com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime.dayFraction(level));
        if (e.y < -0.1) {
            return;
        }
        org.joml.Vector3d worldUp = Math.abs(e.y) > 0.98 ? new org.joml.Vector3d(0, 0, -1) : new org.joml.Vector3d(0, 1, 0);
        org.joml.Vector3d up = new org.joml.Vector3d(worldUp).sub(new org.joml.Vector3d(e).mul(worldUp.dot(e))).normalize();
        org.joml.Vector3d right = new org.joml.Vector3d(e).cross(up).normalize();
        float a = SATURN_GLOW_ALPHA * (0.35F + 0.65F * day);
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        BufferBuilder glow = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        glow.addVertex(m, (float) (e.x * 100), (float) (e.y * 100), (float) (e.z * 100)).setColor(SATURN[0], SATURN[1], SATURN[2], a);
        for (int k = 0; k <= 32; k++) {
            double t = Math.PI * 2 * k / 32;
            double rx = (right.x * Math.cos(t) + up.x * Math.sin(t)) * SATURN_GLOW_RADIUS;
            double ry = (right.y * Math.cos(t) + up.y * Math.sin(t)) * SATURN_GLOW_RADIUS;
            double rz = (right.z * Math.cos(t) + up.z * Math.sin(t)) * SATURN_GLOW_RADIUS;
            glow.addVertex(m, (float) (e.x * 100 + rx), (float) (e.y * 100 + ry), (float) (e.z * 100 + rz)).setColor(SATURN[0], SATURN[1], SATURN[2], 0F);
        }
        BufferUploader.drawWithShader(glow.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    private static void dome(BufferBuilder bb, Matrix4f m, double az, double el, float day) {
        float t = (float) Mth.clamp(Math.sin(el), 0, 1);
        t = (float) Math.pow(t, 0.7);
        float r = Mth.lerp(t, HORIZON[0], ZENITH[0]) * day;
        float g = Mth.lerp(t, HORIZON[1], ZENITH[1]) * day;
        float b = Mth.lerp(t, HORIZON[2], ZENITH[2]) * day;
        float x = (float) (Math.cos(el) * Math.cos(az) * 100);
        float y = (float) (Math.sin(el) * 100);
        float z = (float) (Math.cos(el) * Math.sin(az) * 100);
        bb.addVertex(m, x, y, z).setColor(r, g, b, 1F);
    }
}
