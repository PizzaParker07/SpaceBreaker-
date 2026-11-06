package com.pizzaparker07.spacebreaker.client.mars;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Sky of a world with a thin, dusty atmosphere (Mars). Fine dust scatters red light: the daytime sky is butterscotch,
 * paler toward the horizon, while around a low sun the sky turns blue (forward scattering on the dust grains).
 * A small sun, faint stars at night, and the two small moons racing across the sky: Phobos rises in the west and
 * sets in the east twice a sol; Deimos drifts slowly westward. Dust storms dim and redden everything.
 * <p>
 * Shader packs draw their own sky from the biome sky/fog colours, which are set to the same butterscotch tones.
 */
public class MarsSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    private static final ResourceLocation PHOBOS = Spacebreaker.id("textures/environment/phobos.png");
    private static final ResourceLocation DEIMOS = Spacebreaker.id("textures/environment/deimos.png");
    private static final float SUN_DISC_FRACTION = 12F / 32F;
    /** Sun as seen from Mars (1.52 AU), degrees. */
    private static final double SUN_SIZE = 0.35;
    /** Phobos (~0.2°) and Deimos (~0.04°, a bright star) from the surface, exaggerated a little to read at all. */
    private static final double PHOBOS_SIZE = 0.6;
    private static final double DEIMOS_SIZE = 0.22;
    /** Synodic periods relative to the ground, in sols: Phobos moves east (negative), Deimos west. */
    private static final double PHOBOS_PERIOD = -0.45;
    private static final double DEIMOS_PERIOD = 5.4;

    static final float[] ZENITH = rgb(0xA27A5A);
    static final float[] HORIZON = rgb(0xD9B48C);
    static final float[] SUNSET_BLUE = rgb(0x6C8FB3);
    static final float[] DUSK = rgb(0x4A3A34);
    static final float[] STORM = rgb(0x9A6A48);

    private static final int RINGS = 12;
    private static final int SEGMENTS = 32;
    private VertexBuffer stars;
    private double poleDistance = 8000;
    private ClientLevel cachedLevel;

    public MarsSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F};
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
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

    private void refresh(ClientLevel level) {
        if (level == cachedLevel) {
            return;
        }
        cachedLevel = level;
        poleDistance = level.registryAccess().registry(TerrainProfile.REGISTRY)
                .flatMap(r -> r.stream().filter(t -> t.body().equals(Spacebreaker.id("mars"))).findFirst())
                .map(TerrainProfile::poleDistance).orElse(8000.0);
    }

    /** Daylight 0..1 from the sun elevation (with a long twilight: dust scatters light past sunset). */
    static float daylight(double sunY) {
        return (float) smooth((sunY + 0.18) / 0.4);
    }

    static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Colour of the sky in direction d (unit), for the sun in direction s. */
    /** Horizon colour in the camera's horizontal look direction: the fog colour, so distant ground melts into the sky. */
    public static float[] horizonFog(net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.client.Camera camera, float partialTick) {
        double pd = level.registryAccess().registry(TerrainProfile.REGISTRY)
                .flatMap(r -> r.stream().filter(t -> t.body().equals(Spacebreaker.id("mars"))).findFirst())
                .map(TerrainProfile::poleDistance).orElse(8000.0);
        double lat = PlanetTerrain.latitude(camera.getPosition().z, pd);
        Vector3d sun = SkyMath.sunDirection(level.getTimeOfDay(partialTick), lat);
        org.joml.Vector3f look = camera.getLookVector();
        Vector3d d = new Vector3d(look.x(), 0, look.z());
        if (d.lengthSquared() < 1e-6) {
            d.set(1, 0, 0);
        }
        d.normalize();
        return skyColour(d, sun, MarsWeather.stormStrength(level));
    }

    static float[] skyColour(Vector3d d, Vector3d s, float storm) {
        float day = daylight(s.y);
        double up = Math.max(0, d.y);
        float[] c = new float[3];
        double t = Math.pow(up, 0.45);
        for (int i = 0; i < 3; i++) {
            c[i] = (float) Mth.lerp(t, HORIZON[i], ZENITH[i]);
        }
        // blue aureole around a low sun
        // blue aureole: forward scattering on the fine dust turns the sky around a low sun blue, ~30 degrees wide,
        // while the rest of the sky stays butterscotch
        double low = 1 - smooth((s.y - 0.08) / 0.45);
        double toward = Math.max(0, d.dot(s));
        double glow = (0.75 * Math.pow(toward, 5) + 0.35 * Math.pow(toward, 18)) * low * smooth((s.y + 0.22) / 0.22);
        for (int i = 0; i < 3; i++) {
            c[i] = (float) Mth.lerp(Math.min(1, glow * 1.6), c[i], SUNSET_BLUE[i]);
        }
        // night: deep brown-black, a faint glow lingers on the sun side
        double twilight = Math.pow(toward, 3) * (1 - day) * smooth((s.y + 0.3) / 0.3) * 0.5;
        for (int i = 0; i < 3; i++) {
            double lit = c[i] * day + DUSK[i] * 0.12 * (1 - day) + SUNSET_BLUE[i] * twilight * 0.35;
            c[i] = (float) Mth.lerp(storm * 0.75, lit, STORM[i] * (0.15 + 0.6 * day));
        }
        return c;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        refresh(level);
        Vec3 cam = camera.getPosition();
        double lat = PlanetTerrain.latitude(cam.z, poleDistance);
        float timeOfDay = level.getTimeOfDay(partialTick);
        Vector3d sun = SkyMath.sunDirection(timeOfDay, lat);
        float storm = MarsWeather.stormStrength(level);
        float day = daylight(sun.y);

        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);

        // ---- the dome, coloured per vertex
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int r = 0; r < RINGS; r++) {
            double e0 = -0.35 + (Math.PI / 2 + 0.35) * Math.pow((double) r / RINGS, 1.6);
            double e1 = -0.35 + (Math.PI / 2 + 0.35) * Math.pow((double) (r + 1) / RINGS, 1.6);
            for (int sgm = 0; sgm < SEGMENTS; sgm++) {
                double a0 = sgm * Mth.TWO_PI / SEGMENTS;
                double a1 = (sgm + 1) * Mth.TWO_PI / SEGMENTS;
                domeVertex(bb, m, e0, a0, sun, storm);
                domeVertex(bb, m, e0, a1, sun, storm);
                domeVertex(bb, m, e1, a1, sun, storm);
                domeVertex(bb, m, e1, a0, sun, storm);
            }
        }
        // below the horizon: the ground colour of the far haze, so the void never shows
        for (int sgm = 0; sgm < SEGMENTS; sgm++) {
            double a0 = sgm * Mth.TWO_PI / SEGMENTS;
            double a1 = (sgm + 1) * Mth.TWO_PI / SEGMENTS;
            float[] h0 = skyColour(new Vector3d(Math.cos(a0), 0, Math.sin(a0)), sun, storm);
            float[] h1 = skyColour(new Vector3d(Math.cos(a1), 0, Math.sin(a1)), sun, storm);
            bb.addVertex(m, (float) Math.cos(a0) * 100, -35, (float) Math.sin(a0) * 100).setColor(h0[0], h0[1], h0[2], 1F);
            bb.addVertex(m, (float) Math.cos(a1) * 100, -35, (float) Math.sin(a1) * 100).setColor(h1[0], h1[1], h1[2], 1F);
            bb.addVertex(m, 0, -100, 0).setColor(h1[0], h1[1], h1[2], 1F);
            bb.addVertex(m, 0, -100, 0).setColor(h0[0], h0[1], h0[2], 1F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        // ---- stars and the sun turn with the sky (same frame as the vacuum sky renderer)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees((float) lat));
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
        ps.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360F));
        float starBrightness = (1F - day) * (1F - storm) * 0.75F;
        if (starBrightness > 0.01F) {
            RenderSystem.setShaderColor(starBrightness, starBrightness * 0.95F, starBrightness * 0.9F, 1F);
            VertexBuffer sb = stars();
            sb.bind();
            sb.drawWithShader(ps.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
        }
        float sunVis = 1F - 0.85F * storm;
        RenderSystem.setShaderColor(sunVis, sunVis * 0.97F, sunVis * 0.92F, 1F);
        float sunHalf = (float) (100 * Math.tan(Math.toRadians(SUN_SIZE * 2.2 / 2)) / SUN_DISC_FRACTION);
        quad(ps.last().pose(), SUN, sunHalf);
        ps.popPose();

        // pale glow around the sun (dust scattering), additive
        if (sun.y > -0.2) {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            glow(m, sun, 9F * (1 + storm), (0.22F + 0.2F * storm) * sunVis * Math.max(0.2F, day));
            if (sun.y < 0.45 && storm < 0.5F) {
                glowBlue(m, sun, 16F, 0.35F * (float) (1 - sun.y / 0.45) * (1 - 2 * storm) * Math.max(0.3F, day));
            }
        }

        // ---- the moons: their own hour angles (equatorial orbits), lit by the sun
        double sols = PlanetTime.clock(level).map(c -> (level.getDayTime() + partialTick + c.offset()) / (double) c.dayTicks())
                .orElse(level.getDayTime() / 24000.0);
        // additive: the dark side of a moon never shows against the bright dusty sky, as in reality
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        if (Math.abs(lat) < 69) {
            moon(ps, PHOBOS, sols, PHOBOS_PERIOD, 0.31, PHOBOS_SIZE, lat, sun, day, storm);
        }
        if (Math.abs(lat) < 82) {
            moon(ps, DEIMOS, sols, DEIMOS_PERIOD, 0.77, DEIMOS_SIZE, lat, sun, day, storm);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    private static void domeVertex(BufferBuilder bb, Matrix4f m, double elev, double az, Vector3d sun, float storm) {
        double ce = Math.cos(elev);
        Vector3d d = new Vector3d(ce * Math.cos(az), Math.sin(elev), ce * Math.sin(az));
        float[] c = skyColour(d, sun, storm);
        bb.addVertex(m, (float) d.x * 100, (float) d.y * 100, (float) d.z * 100).setColor(c[0], c[1], c[2], 1F);
    }

    private static void quad(Matrix4f m, ResourceLocation tex, float half) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -half, 100F, -half).setUv(0F, 0F);
        bb.addVertex(m, half, 100F, -half).setUv(1F, 0F);
        bb.addVertex(m, half, 100F, half).setUv(1F, 1F);
        bb.addVertex(m, -half, 100F, half).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    /** Additive soft disc (triangle fan) of angular radius {@code deg} around direction d. */
    private static void glow(Matrix4f m, Vector3d d, float deg, float strength) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Vector3d up = Math.abs(d.y) > 0.95 ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(d).cross(up).normalize();
        Vector3d up2 = new Vector3d(right).cross(d).normalize();
        float rad = (float) (100 * Math.tan(Math.toRadians(deg)));
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Vector3d c = new Vector3d(d).mul(100);
        bb.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(1F, 0.93F, 0.82F, strength);
        for (int i = 0; i <= 24; i++) {
            double a = i * Mth.TWO_PI / 24;
            Vector3d p = new Vector3d(c).add(new Vector3d(right).mul(Math.cos(a) * rad)).add(new Vector3d(up2).mul(Math.sin(a) * rad));
            bb.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(0.85F, 0.9F, 1F, 0F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    /** Soft additive blue halo around a low sun (adds to the aureole of the dome; kept in the shader-pack fog colour too). */
    private static void glowBlue(Matrix4f m, Vector3d d, float deg, float strength) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Vector3d up = Math.abs(d.y) > 0.95 ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(d).cross(up).normalize();
        Vector3d up2 = new Vector3d(right).cross(d).normalize();
        float rad = (float) (100 * Math.tan(Math.toRadians(deg)));
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Vector3d c = new Vector3d(d).mul(100);
        bb.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(SUNSET_BLUE[0], SUNSET_BLUE[1], SUNSET_BLUE[2] * 1.1F, strength);
        for (int i = 0; i <= 32; i++) {
            double a = i * Mth.TWO_PI / 32;
            Vector3d p = new Vector3d(c).add(new Vector3d(right).mul(Math.cos(a) * rad)).add(new Vector3d(up2).mul(Math.sin(a) * rad));
            bb.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(SUNSET_BLUE[0], SUNSET_BLUE[1], SUNSET_BLUE[2], 0F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    private void moon(PoseStack ps, ResourceLocation tex, double sols, double period, double phase0, double size, double lat,
                      Vector3d sun, float day, float storm) {
        double hour = Mth.frac(sols / period + phase0);
        Vector3d dir = SkyMath.sunDirection(hour, lat);
        if (dir.y < -0.05) {
            return;
        }
        double lit = SkyMath.illuminatedFraction(sun, dir);
        float b = (float) ((0.08 + 0.92 * lit) * (1 - 0.8 * storm) * (1 - 0.6 * day));
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees((float) lat));
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
        ps.mulPose(Axis.XP.rotationDegrees((float) (hour * 360)));
        RenderSystem.setShaderColor(b, b * 0.97F, b * 0.94F, 1F);
        float half = (float) (100 * Math.tan(Math.toRadians(size * com.pizzaparker07.spacebreaker.client.sky.SkyConfig.bodyScale() / 2)));
        Matrix4f m = ps.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -half, 100F, -half).setUv(0F, 0F);
        bb.addVertex(m, half, 100F, -half).setUv(1F, 0F);
        bb.addVertex(m, half, 100F, half).setUv(1F, 1F);
        bb.addVertex(m, -half, 100F, half).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        ps.popPose();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private VertexBuffer stars() {
        if (stars == null) {
            stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
            stars.bind();
            stars.upload(buildStars());
            VertexBuffer.unbind();
        }
        return stars;
    }

    private static MeshData buildStars() {
        return StarMesh.build(0x4A45L, 1600);
    }
}
