package com.pizzaparker07.spacebreaker.client.jupiter;

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
import com.pizzaparker07.spacebreaker.core.body.Bodies;
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

import java.util.Optional;

/**
 * Sky of a moon of a giant planet: black and starry (lighting, fog and weather come from {@link PlanetSkyEffects}), with
 * the parent hanging still above the near side, as big as its real angular size: Jupiter 19° wide from Io, 12° from
 * Europa; Saturn 28° from Enceladus. The disc is shaded per vertex as a lit sphere so it shows the right phase, its
 * cloud bands slightly darkened toward the limb; a ringed parent gets its rings as smooth geometry, seen almost edge-on
 * from a moon in the ring plane (opened a few degrees for readability), lit by the sun, the far half behind the planet
 * and the near half in front.
 */
public class GiantPlanetSkyEffects extends PlanetSkyEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    private static final int GRID = 48;
    private static final int RING_SEGMENTS = 160;

    /**
     * @param disc      the planet's disc texture
     * @param rings     ring texture (a radial strip, inner edge on the left) or null
     * @param ringInner inner ring radius in planet radii
     * @param ringOuter outer ring radius in planet radii
     * @param ringTilt  opening angle of the rings as seen from the moon, degrees
     * @param haze      how much of the sky glow comes through (1 = airless, &lt; 1 dims the disc behind a hazy atmosphere)
     */
    public record Parent(ResourceLocation disc, ResourceLocation rings, double ringInner, double ringOuter, double ringTilt, float haze) {
        public static Parent plain(ResourceLocation disc) {
            return new Parent(disc, null, 0, 0, 0, 1F);
        }
    }

    private final ResourceKey<CelestialBody> bodyKey;
    /** A moon shown instead of the parent (a double world: Charon in Pluto's sky), or null. */
    private final ResourceKey<CelestialBody> companionKey;
    private final Parent look;
    private ClientLevel cachedLevel;
    private double poleDistance = 6000;
    private double parentSize = 10;
    private double sunSize = 0.1;

    public GiantPlanetSkyEffects(ResourceKey<CelestialBody> body, Parent look) {
        this(body, look, null);
    }

    /**
     * @param companion a moon of {@code body} locked face to face with it (Charon for Pluto): drawn where the parent would
     *                  be, at its own size, while the sun stays at the body's own distance
     */
    public GiantPlanetSkyEffects(ResourceKey<CelestialBody> body, Parent look, ResourceKey<CelestialBody> companion) {
        super(body);
        this.bodyKey = body;
        this.companionKey = companion;
        this.look = look;
    }

    private void refresh(ClientLevel level) {
        if (level == cachedLevel) {
            return;
        }
        cachedLevel = level;
        poleDistance = level.registryAccess().registry(TerrainProfile.REGISTRY)
                .flatMap(r -> r.stream().filter(t -> t.body().equals(bodyKey.location())).findFirst())
                .map(TerrainProfile::poleDistance).orElse(6000.0);
        Optional<CelestialBody> body = Bodies.get(level.registryAccess(), bodyKey);
        Optional<CelestialBody> parent = body.flatMap(CelestialBody::parent).flatMap(p -> Bodies.get(level.registryAccess(), ResourceKey.create(Bodies.REGISTRY, p)));
        if (companionKey != null) {
            Optional<CelestialBody> companion = Bodies.get(level.registryAccess(), companionKey);
            if (body.isPresent() && companion.isPresent()) {
                parentSize = SkyMath.angularDiameter(companion.get().physical().radiusKm(), Math.max(1, companion.get().orbit().semiMajorAxisKm()));
                sunSize = SkyMath.angularDiameter(695700, Math.max(1, body.get().orbit().semiMajorAxisKm()));
            }
        } else if (body.isPresent() && parent.isPresent()) {
            parentSize = SkyMath.angularDiameter(parent.get().physical().radiusKm(), Math.max(1, body.get().orbit().semiMajorAxisKm()));
            sunSize = SkyMath.angularDiameter(695700, Math.max(1, parent.get().orbit().semiMajorAxisKm()));
        }
    }

    /** Angular diameter of the parent in degrees (before the client's body scale). */
    public double parentSize() {
        return parentSize;
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
        VertexBuffer stars = OuterStars.buffer();
        stars.bind();
        stars.drawWithShader(ps.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        // the sun: small and sharp out here (Jupiter 5.2 AU, Saturn 9.5 AU), never smaller than a few pixels
        float half = (float) (100 * Math.tan(Math.toRadians(Math.max(0.12, sunSize * SkyConfig.bodyScale()) / 2)) / (12F / 32F));
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

        parent(ps.last().pose(), level, lat, lon, timeOfDay);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    private void parent(Matrix4f m, ClientLevel level, double lat, double lon, float timeOfDay) {
        Vector3d e = SkyMath.parentDirection(lat, lon, PlanetTime.dayFraction(level));
        if (e.y < -0.6) {
            return;
        }
        Vector3d s = SkyMath.sunDirection(timeOfDay, lat);
        Vector3d worldUp = Math.abs(e.y) > 0.98 ? new Vector3d(0, 0, -1) : new Vector3d(0, 1, 0);
        Vector3d up = new Vector3d(worldUp).sub(new Vector3d(e).mul(worldUp.dot(e))).normalize();
        Vector3d right = new Vector3d(e).cross(up).normalize();
        Vector3d toViewer = new Vector3d(e).negate();
        Vector3d sun = new Vector3d(s.dot(right), s.dot(up), s.dot(toViewer));
        double size = Math.min(75, parentSize * SkyConfig.bodyScale());
        float half = (float) (100 * Math.tan(Math.toRadians(size / 2)));
        Vector3d c = new Vector3d(e).mul(100);
        if (look.rings() != null) {
            rings(m, c, right, up, half, sun, false);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, look.disc());
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                disc(bb, m, c, right, up, half, sun, i, j);
                disc(bb, m, c, right, up, half, sun, i, j + 1);
                disc(bb, m, c, right, up, half, sun, i + 1, j + 1);
                disc(bb, m, c, right, up, half, sun, i + 1, j);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        if (look.rings() != null) {
            rings(m, c, right, up, half, sun, true);
        }
        RenderSystem.defaultBlendFunc();
    }

    private void disc(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float half, Vector3d sun, int i, int j) {
        float u = (float) i / GRID;
        float v = (float) j / GRID;
        double dx = u * 2 - 1;
        double dy = 1 - v * 2;
        double r2 = dx * dx + dy * dy;
        double nz = Math.sqrt(Math.max(0, 1 - r2));
        double cosI = dx * sun.x + dy * sun.y + nz * sun.z;
        // deep atmosphere: a soft terminator and a little limb darkening on the day side
        double t = Math.max(0, Math.min(1, (cosI + 0.06) / 0.2));
        double lambert = Math.max(0, cosI) * 0.8 + 0.2 * t * t * (3 - 2 * t);
        double limb = 0.78 + 0.22 * Math.pow(nz, 0.5);
        // a ringed planet carries the rings' shadow as a dark band just off its equator on the side away from the sun
        double shade = 1;
        if (look.rings() != null) {
            double band = dy + 0.06 * Math.signum(sun.y == 0 ? 1 : sun.y);
            if (Math.abs(band) < 0.035 && Math.abs(dx) < 0.98) {
                shade = 0.55;
            }
        }
        float b = (float) Math.min(1, (0.02 + lambert * 1.05 * limb) * shade * look.haze());
        float px = (float) (c.x + right.x * dx * half + up.x * dy * half);
        float py = (float) (c.y + right.y * dx * half + up.y * dy * half);
        float pz = (float) (c.z + right.z * dx * half + up.z * dy * half);
        bb.addVertex(m, px, py, pz).setUv(u, v).setColor(b, b, b, 1F);
    }

    /** The near (front) or far (back) half of the rings as a flattened annulus. */
    private void rings(Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float half, Vector3d sun, boolean front) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, look.rings());
        double open = Math.sin(Math.toRadians(look.ringTilt()));
        // rings are lit on the face toward the sun; seen from the unlit side they glow faintly by forward scattering
        double lit = Math.min(1, 0.25 + 0.75 * Math.max(0, sun.z) + 0.6 * Math.abs(sun.y) * 0.5);
        float b = (float) (Math.max(0.12, lit) * look.haze());
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        double r0 = look.ringInner() * half;
        double r1 = look.ringOuter() * half;
        for (int k = 0; k < RING_SEGMENTS; k++) {
            double a0 = Math.PI * k / RING_SEGMENTS + (front ? Math.PI : 0);
            double a1 = Math.PI * (k + 1) / RING_SEGMENTS + (front ? Math.PI : 0);
            ring(bb, m, c, right, up, open, a0, r0, 0F, b);
            ring(bb, m, c, right, up, open, a0, r1, 1F, b);
            ring(bb, m, c, right, up, open, a1, r1, 1F, b);
            ring(bb, m, c, right, up, open, a1, r0, 0F, b);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void ring(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, double open, double ang, double r, float u, float b) {
        double dx = Math.cos(ang) * r;
        double dy = Math.sin(ang) * r * open;
        float px = (float) (c.x + right.x * dx + up.x * dy);
        float py = (float) (c.y + right.y * dx + up.y * dy);
        float pz = (float) (c.z + right.z * dx + up.z * dy);
        bb.addVertex(m, px, py, pz).setUv(u, 0.5F).setColor(b, b * 0.98F, b * 0.95F, 1F);
    }
}
