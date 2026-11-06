package com.pizzaparker07.spacebreaker.client.mars;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.sky.PlanetSkyEffects;
import com.pizzaparker07.spacebreaker.client.sky.SkyConfig;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Sky of Phobos and Deimos: airless and black like the Moon's (fog, lighting and weather come from
 * {@link PlanetSkyEffects}), but the parent is Mars, hanging huge and still above the near side: about 40° across
 * from Phobos, 16° from Deimos. Its disc is shaded per vertex as a lit sphere, so it shows the right phase.
 */
public class MarsMoonSkyEffects extends PlanetSkyEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    private static final ResourceLocation MARS = Spacebreaker.id("textures/environment/mars_disc.png");
    private static final int GRID = 20;
    private final String bodyName;
    private final double marsSize;
    private ClientLevel cachedLevel;
    private double poleDistance = 900;
    private VertexBuffer stars;

    /**
     * @param body      the moon
     * @param marsSize  angular diameter of Mars from its surface, degrees
     */
    public MarsMoonSkyEffects(ResourceKey<CelestialBody> body, double marsSize) {
        super(body);
        this.bodyName = body.location().getPath();
        this.marsSize = marsSize;
    }

    private void refresh(ClientLevel level) {
        if (level != cachedLevel) {
            cachedLevel = level;
            poleDistance = level.registryAccess().registry(TerrainProfile.REGISTRY)
                    .flatMap(r -> r.stream().filter(t -> t.body().equals(Spacebreaker.id(bodyName))).findFirst())
                    .map(TerrainProfile::poleDistance).orElse(900.0);
        }
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        refresh(level);
        Vec3 cam = camera.getPosition();
        double lat = PlanetTerrain.latitude(cam.z, poleDistance);
        double lon = 90 * cam.x / poleDistance;
        float timeOfDay = level.getTimeOfDay(partialTick);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);

        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees((float) lat));
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
        ps.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360F));
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        float day = (float) Math.max(0, SkyMath.sunDirection(timeOfDay, lat).y);
        float sb = 1F - 0.35F * Math.min(1F, day * 3F);
        RenderSystem.setShaderColor(sb, sb, sb, 1F);
        if (stars == null) {
            stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
            stars.bind();
            stars.upload(StarMesh.build(0x9B05L, 3000));
            VertexBuffer.unbind();
        }
        stars.bind();
        stars.drawWithShader(ps.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        float half = (float) (100 * Math.tan(Math.toRadians(0.35 * SkyConfig.bodyScale() / 2)) / (12F / 32F));
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -half, 100F, -half).setUv(0F, 0F);
        bb.addVertex(m, half, 100F, -half).setUv(1F, 0F);
        bb.addVertex(m, half, 100F, half).setUv(1F, 1F);
        bb.addVertex(m, -half, 100F, half).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        ps.popPose();

        mars(ps.last().pose(), level, lat, lon, timeOfDay);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    /** Mars as a sphere-shaded disc: each grid vertex gets the lambert light of the sphere point behind it. */
    private void mars(Matrix4f m, ClientLevel level, double lat, double lon, float timeOfDay) {
        Vector3d e = SkyMath.parentDirection(lat, lon, PlanetTime.dayFraction(level));
        if (e.y < -0.5) {
            return;
        }
        Vector3d s = SkyMath.sunDirection(timeOfDay, lat);
        Vector3d worldUp = Math.abs(e.y) > 0.98 ? new Vector3d(0, 0, -1) : new Vector3d(0, 1, 0);
        Vector3d up = new Vector3d(worldUp).sub(new Vector3d(e).mul(worldUp.dot(e))).normalize();
        Vector3d right = new Vector3d(e).cross(up).normalize();
        Vector3d toViewer = new Vector3d(e).negate();
        // the sun's direction in the disc frame: x right, y up, z toward the viewer
        Vector3d sun = new Vector3d(s.dot(right), s.dot(up), s.dot(toViewer));
        float half = (float) (100 * Math.tan(Math.toRadians(Math.min(70, marsSize * SkyConfig.bodyScale()) / 2)));
        Vector3d c = new Vector3d(e).mul(100);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, MARS);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                vertex(bb, m, c, right, up, half, sun, i, j);
                vertex(bb, m, c, right, up, half, sun, i, j + 1);
                vertex(bb, m, c, right, up, half, sun, i + 1, j + 1);
                vertex(bb, m, c, right, up, half, sun, i + 1, j);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float half, Vector3d sun, int i, int j) {
        float u = (float) i / GRID;
        float v = (float) j / GRID;
        double dx = u * 2 - 1;
        double dy = 1 - v * 2;
        double r2 = dx * dx + dy * dy;
        double nz = Math.sqrt(Math.max(0, 1 - r2));
        double lambert = Math.max(0, dx * sun.x + dy * sun.y + nz * sun.z);
        // a faint glow on the night side from the thin, dusty atmosphere at the limb
        float b = (float) Math.min(1, 0.04 + lambert * 1.05);
        float px = (float) (c.x + right.x * dx * half + up.x * dy * half);
        float py = (float) (c.y + right.y * dx * half + up.y * dy * half);
        float pz = (float) (c.z + right.z * dx * half + up.z * dy * half);
        // outside the disc the texture is transparent anyway
        bb.addVertex(m, px, py, pz).setUv(u, v).setColor(b, b * 0.97F, b * 0.95F, 1F);
    }
}
