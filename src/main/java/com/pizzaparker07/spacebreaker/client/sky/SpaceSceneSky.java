package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh;
import com.pizzaparker07.spacebreaker.content.worldgen.SpaceSceneLayout;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.astro.flight.SceneScale;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The sky of a travel scene (the Earth–Moon space dimension): black, the star field fixed to the ecliptic, the Sun with
 * its glare, and every body of the scene at its real place ({@link SpaceSceneLayout}) as a blocky body seen from the
 * camera: night sides dark, Earth in a thin shell of air, bodies drawn finer as the camera closes in. The camera's
 * block position is the scene position.
 * <p>
 * Each body is drawn scaled about the camera so it sits 100 blocks away: the picture is the same (angles are kept)
 * but nothing reaches past the sky's far plane, however far the Moon is.
 */
public class SpaceSceneSky extends DimensionSpecialEffects {
    /** The Earth–Moon scene: central body and the bodies drawn in it. */
    public static final String CENTRAL = "spacebreaker:earth";
    public static final List<String> BODIES = List.of("spacebreaker:earth", "spacebreaker:moon");
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    private static final float SUN_DISC_FRACTION = 12F / 32F;
    private static final BlockyBodyRenderer.ColorMap AIR = BlockyBodyRenderer.flat(0x6FA8FF);

    private final SceneScale scale;

    public SpaceSceneSky(SceneScale scale) {
        super(Float.NaN, false, SkyType.NONE, false, false);
        this.scale = scale;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
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
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        Vec3 cam = camera.getPosition();
        render(level, modelViewMatrix, projectionMatrix, new Vector3d(cam.x, cam.y, cam.z), scale);
        setupFog.run();
        return true;
    }

    /**
     * Draws the whole scene from {@code cameraBlocks} (scene block position). Usable from other skies or a development
     * camera; the colour buffer should already be black.
     */
    public static void render(ClientLevel level, Matrix4f modelView, Matrix4f projection, Vector3d cameraBlocks, SceneScale scale) {
        render(level, modelView, projection, cameraBlocks, null, scale);
    }

    /**
     * As {@link #render(ClientLevel, Matrix4f, Matrix4f, Vector3d, SceneScale)}, but with the camera at an offset from a
     * body that moves with it ({@code follow} its id), as a ship holding station would.
     */
    public static void render(ClientLevel level, Matrix4f modelView, Matrix4f projection, Vector3d cameraBlocks, String follow, SceneScale scale) {
        var sysOpt = BodyAdapter.system(level.registryAccess());
        var reg = Bodies.registry(level.registryAccess());
        if (sysOpt.isEmpty() || reg.isEmpty()) {
            return;
        }
        BodySystem sys = sysOpt.get();
        double t = BodyAdapter.simTime(level);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        // ---- stars, fixed to the ecliptic
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        VertexBuffer stars = StarField.buffer();
        stars.bind();
        stars.drawWithShader(modelView, projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();

        // ---- the Sun and its glare
        Vector3d sunDir = SpaceSceneLayout.sunDirection(sys, CENTRAL, t);
        PoseStack ps = new PoseStack();
        ps.mulPose(modelView);
        ps.pushPose();
        ps.mulPose(new Quaternionf().rotateTo(new Vector3f(0, 1, 0), new Vector3f((float) sunDir.x, (float) sunDir.y, (float) sunDir.z)));
        float sunHalf = (float) (100 * Math.tan(Math.toRadians(0.53 * SkyConfig.bodyScale() / 2)) / SUN_DISC_FRACTION);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -sunHalf, 100F, -sunHalf).setUv(0F, 0F);
        bb.addVertex(m, sunHalf, 100F, -sunHalf).setUv(1F, 0F);
        bb.addVertex(m, sunHalf, 100F, sunHalf).setUv(1F, 1F);
        bb.addVertex(m, -sunHalf, 100F, sunHalf).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        CometTrails.skyGlow(ps, new Vector3f(0, 1, 0), 0.6F, 0.5F, 1F, 0.97F, 0.9F);
        ps.popPose();
        RenderSystem.defaultBlendFunc();

        // ---- the bodies, farthest first
        List<SpaceSceneLayout.Body> bodies;
        try {
            bodies = SpaceSceneLayout.layout(sys, scale, CENTRAL, BODIES, t,
                    id -> BodyAdapter.look(sys, ResourceLocation.parse(id), reg.get().get(ResourceLocation.parse(id)),
                            sys.position(sys.require(CENTRAL), t), t));
        } catch (RuntimeException e) {
            return;
        }
        if (follow != null) {
            for (SpaceSceneLayout.Body b : bodies) {
                if (b.id().equals(follow)) {
                    cameraBlocks = new Vector3d(cameraBlocks).add(b.center());
                }
            }
        }
        Vector3d camera = cameraBlocks;
        List<SpaceSceneLayout.Body> order = new ArrayList<>(bodies);
        order.sort(Comparator.comparingDouble((SpaceSceneLayout.Body b) -> b.center().distanceSquared(camera)).reversed());
        for (SpaceSceneLayout.Body b : order) {
            drawBody(modelView, b, camera);
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private static void drawBody(Matrix4f modelView, SpaceSceneLayout.Body b, Vector3d camera) {
        Vector3d rel = new Vector3d(b.center()).sub(camera);
        double d = rel.length();
        if (d <= b.radius() * 1.001) {
            return; // inside it: the terrain of the scene takes over
        }
        // scale about the camera so the body's centre is 100 blocks away: same picture, inside the far plane
        float s = (float) (100 / d);
        Vector3f centre = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z).mul(s);
        float radius = (float) (b.radius() * s);
        Vector3f pole = new Vector3f((float) b.pole().x, (float) b.pole().y, (float) b.pole().z);
        Vector3f meridian = new Vector3f((float) b.meridian().x, (float) b.meridian().y, (float) b.meridian().z);
        Vector3f sun = new Vector3f((float) b.sun().x, (float) b.sun().y, (float) b.sun().z);
        BlockyBodyMesh.Style style = SkyConfig.blockyStyle() == null ? BlockyBodyMesh.Style.VOXEL : SkyConfig.blockyStyle();
        // close in (within a few radii): allow the finest blocks
        int n = d < b.radius() * 4 ? BlockyBodyRenderer.APPROACH : 0;
        String path = ResourceLocation.parse(b.id()).getPath();
        BlockyBodyRenderer.ColorMap map = BlockyBodyRenderer.surface(path);
        if (map == null) {
            map = BlockyBodyRenderer.flat(0x9A9A9A);
        }
        BlockyBodyRenderer.draw(modelView, style, n, map, new BlockyBodyRenderer.Pose(centre, radius, pole, meridian, 0F), sun, 0.05F, 1F, 1F,
                null);
        if (path.equals("earth")) {
            // a thin shell of air: one layer of pale blue, see-through
            BlockyBodyRenderer.draw(modelView, style, 32, AIR,
                    new BlockyBodyRenderer.Pose(centre, radius * 1.03F, pole, meridian, 0F), sun, 0.15F, 1F, 0.22F, null);
        }
    }
}
