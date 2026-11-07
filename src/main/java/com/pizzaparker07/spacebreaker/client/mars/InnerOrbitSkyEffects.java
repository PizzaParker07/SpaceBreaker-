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
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sky of the orbits of the inner planets: black space, stars, a sun sized for the distance from it, and the planet
 * below as a slowly turning sphere with a day/night terminator. Venus wears a thick, bright haze on its limb, Mars a
 * thin pale one, airless Mercury none. Neighbours hang small in the distance: Phobos and Deimos over Mars, the Earth
 * and Mercury seen from Venus, brilliant Venus seen from Mercury.
 */
public class InnerOrbitSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    /** One orbit (and one day/night cycle of the view) in ticks: the same clock as the Earth and Moon orbits. */
    public static final float ORBIT_TICKS = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.ORBIT_TICKS;
    private static final float DISTANCE = 100F;
    private static final float RADIUS = 76F;
    private static final int RING = 128;

    /**
     * @param texture   equirectangular map of the planet
     * @param sunDeg    angular diameter of the sun there, degrees
     * @param haze      limb haze colour (r, g, b, peak alpha) or null
     * @param thickness haze thickness as a fraction of the limb radius
     */
    private record Look(ResourceLocation texture, float sunDeg, float[] haze, float thickness, Far... far) {
    }

    /** A distant body: texture, direction and half size of its billboard, and its colours for the blocky style. */
    private record Far(ResourceLocation texture, Vector3f dir, float size, com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.ColorMap map) {
    }

    private static com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.ColorMap flat(int rgb) {
        return com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.flat(rgb);
    }

    private final Look look;
    private final net.minecraft.resources.ResourceLocation bodyId;
    private VertexBuffer stars;

    public InnerOrbitSkyEffects(String body) {
        super(Float.NaN, false, SkyType.NONE, false, false);
        this.bodyId = Spacebreaker.id(body);
        this.look = switch (body) {
            case "venus" -> new Look(env("orbit_venus"), 0.72F, new float[]{1F, 0.93F, 0.72F, 0.85F}, 0.085F,
                    new Far(env("orbit_earth_disc"), new Vector3f(-0.55F, 0.4F, 0.7F), 1.1F,
                            com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.texture(env("earth_map"))),
                    new Far(env("mercury_disc"), new Vector3f(0.75F, 0.5F, -0.3F), 0.7F,
                            com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.texture(env("orbit_mercury"))));
            case "mercury" -> new Look(env("orbit_mercury"), 1.4F, null, 0F,
                    new Far(env("venus_disc"), new Vector3f(-0.6F, 0.45F, 0.6F), 1.0F,
                            com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.texture(env("orbit_venus"))));
            default -> new Look(env("orbit_mars"), 0.35F, new float[]{0.78F, 0.82F, 0.95F, 0.5F}, 0.035F,
                    new Far(env("phobos"), new Vector3f(0.5F, 0.25F, -0.8F), 3.2F, flat(0x5E554D)),
                    new Far(env("deimos"), new Vector3f(-0.7F, 0.55F, -0.3F), 1.2F, flat(0x8C8072)));
        };
    }

    /** Share of sunlight now (1 day, 0 in the planet's shadow), from the same model as the rocketry orbits. */
    private float sunlit(ClientLevel level, float partialTick) {
        return (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(
                com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(level.registryAccess()), bodyId, level.getGameTime() + partialTick);
    }

    /** Hard sunlight by day; in the planet's shadow only starlight, so lamps matter. */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, Vector3f colors) {
        float lit = sunlit(level, partialTicks);
        float daylight = 0.06F + 0.94F * lit;
        float sky = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelY) * daylight;
        float bl = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        colors.set(bl + sky, bg + sky, bb + sky * (1 + 0.25F * (1 - lit)));
        colors.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
    }

    private static ResourceLocation env(String name) {
        return Spacebreaker.id("textures/environment/" + name + ".png");
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
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
                                Matrix4f modelView, Matrix4f projection) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection,
                             boolean isFoggy, Runnable setupFog) {
        float time = level.getGameTime() + partialTick;
        float orbit = time / ORBIT_TICKS;
        // the same orbital day and night as the rocketry orbits: the sun overhead at orbital noon, behind the planet at night
        float a = (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.phase(time) * Mth.TWO_PI;
        Vector3f sun = new Vector3f(Mth.sin(a), Mth.cos(a), 0.3F).normalize();
        float lit = sunlit(level, partialTick);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        if (stars == null) {
            stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
            stars.bind();
            stars.upload(StarMesh.build(0x1A0B17L, 3200));
            VertexBuffer.unbind();
        }
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        stars.bind();
        stars.drawWithShader(modelView, projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        // the sun texture's disc fills 12/32 of the quad
        float sunHalf = (float) (DISTANCE * Math.tan(Math.toRadians(look.sunDeg * com.pizzaparker07.spacebreaker.client.sky.SkyConfig.bodyScale() / 2))
                / (12F / 32F));
        if (lit > 0.01F) {
            RenderSystem.setShaderColor(lit, lit, lit, 1F);
            billboard(modelView, SUN, sun, sunHalf, true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
        var blocky = com.pizzaparker07.spacebreaker.client.sky.SkyConfig.blockyStyle();
        Vector3f center = new Vector3f(0, -DISTANCE, 0);
        if (blocky != null) {
            // the planet below and its neighbours as balls of blocks, all lit by the same sun
            for (Far f : look.far) {
                Vector3f c = new Vector3f(f.dir).normalize().mul(DISTANCE);
                var pose = new com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.Pose(c, f.size * 0.4F, new Vector3f(0, 1, 0),
                        new Vector3f(c).negate(), 0F);
                com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.draw(modelView, blocky, 0, f.map, pose, sun, 0.03F, 1F, 1F, null);
            }
            var pose = new com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.Pose(center, RADIUS, new Vector3f(0, 0, 1),
                    new Vector3f(0, 1, 0), orbit * 0.5F * Mth.TWO_PI);
            com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.draw(modelView, blocky, com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH,
                    com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.surface(bodyId.getPath()), pose, sun, 0.03F, 1F, 1F, null);
            if (look.haze != null) {
                // the air: a thin bright line along the limb, brightest where the sun shines along it
                int rgb = (int) (look.haze[0] * 255) << 16 | (int) (look.haze[1] * 255) << 8 | (int) (look.haze[2] * 255);
                com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.drawLimb(modelView, pose, sun, rgb, look.haze[3],
                        look.thickness * 0.5F);
            }
        } else {
            for (Far f : look.far) {
                billboard(modelView, f.texture, f.dir, f.size, false);
            }
            sphere(modelView, center, orbit * 0.5F, sun);
            if (look.haze != null) {
                haze(modelView, sun);
            }
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void billboard(Matrix4f m, ResourceLocation texture, Vector3f dir, float half, boolean additive) {
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        Vector3f n = new Vector3f(dir).normalize();
        Vector3f c = new Vector3f(n).mul(DISTANCE);
        Vector3f up = Math.abs(n.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(n).cross(up).normalize().mul(half);
        Vector3f v = new Vector3f(n).cross(u).normalize().mul(half);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).setUv(0, 0);
        b.addVertex(m, c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).setUv(0, 1);
        b.addVertex(m, c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).setUv(1, 1);
        b.addVertex(m, c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).setUv(1, 0);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** The planet: a lat/long sphere, poles on the horizon so we look down on the tropics, lit with a soft terminator. */
    private void sphere(Matrix4f m, Vector3f c, float spin, Vector3f sun) {
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, look.texture);
        int lon = 96;
        int lat = 48;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int j = 0; j < lat; j++) {
            for (int i = 0; i < lon; i++) {
                vert(b, m, c, i, j, lon, lat, spin, sun);
                vert(b, m, c, i + 1, j, lon, lat, spin, sun);
                vert(b, m, c, i + 1, j + 1, lon, lat, spin, sun);
                vert(b, m, c, i, j + 1, lon, lat, spin, sun);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.disableCull();
    }

    private static void vert(BufferBuilder b, Matrix4f m, Vector3f c, int i, int j, int lon, int lat, float spin, Vector3f sun) {
        float theta = Mth.PI * j / lat;
        float phi = Mth.TWO_PI * i / lon;
        float sr = Mth.sin(theta);
        float nx = sr * Mth.cos(phi);
        float ny = -sr * Mth.sin(phi);
        float nz = Mth.cos(theta);
        float light = nx * sun.x + ny * sun.y + nz * sun.z;
        float shade = 0.03F + 0.97F * Mth.clamp(light * 1.6F + 0.15F, 0F, 1F);
        b.addVertex(m, c.x + nx * RADIUS, c.y + ny * RADIUS, c.z + nz * RADIUS).setUv((float) i / lon + spin, (float) j / lat)
                .setColor(shade, shade, shade, 1F);
    }

    /** Limb haze: a smooth ring around the planet's outline, brightest where the sun shines along it. */
    private void haze(Matrix4f m, Vector3f sun) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // the limb seen from the camera: a circle on the cone tangent to the sphere
        double alpha = Math.asin(RADIUS / DISTANCE);
        double tangent = Math.sqrt(DISTANCE * DISTANCE - RADIUS * RADIUS);
        float along = (float) (tangent * Math.cos(alpha));
        float limb = (float) (tangent * Math.sin(alpha));
        float[] radii = {0.97F, 1.0F, 1F + look.thickness};
        float[] alphas = {0F, look.haze[3], 0F};
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k < RING; k++) {
            float a0 = Mth.TWO_PI * k / RING;
            float a1 = Mth.TWO_PI * (k + 1) / RING;
            for (int band = 0; band < 2; band++) {
                ring(b, m, sun, a0, along, limb * radii[band], alphas[band]);
                ring(b, m, sun, a0, along, limb * radii[band + 1], alphas[band + 1]);
                ring(b, m, sun, a1, along, limb * radii[band + 1], alphas[band + 1]);
                ring(b, m, sun, a1, along, limb * radii[band], alphas[band]);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private void ring(BufferBuilder b, Matrix4f m, Vector3f sun, float ang, float along, float r, float a) {
        float dx = Mth.cos(ang);
        float dz = Mth.sin(ang);
        float lit = Mth.clamp(0.25F + 0.9F * (dx * sun.x + dz * sun.z) + 0.3F * sun.y, 0.08F, 1F);
        b.addVertex(m, dx * r, -along, dz * r).setColor(look.haze[0], look.haze[1], look.haze[2], a * lit);
    }
}
