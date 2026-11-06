package com.pizzaparker07.spacebreaker.client.sky;

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
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.Optional;

/**
 * Sky of an airless world: always black, with a dense star field, a sharp white sun disc sized by the real
 * distance to the sun, and — for tidally locked moons — the parent planet hanging still in the sky with its
 * correct phase. No clouds, rain, sunrise glow or blue haze. Everything follows the body's own clock and the
 * observer's latitude (low polar sun) and longitude (the parent sinks toward the far side).
 */
public class PlanetSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation SUN = Spacebreaker.id("textures/environment/sun_vacuum.png");
    /** Share of the sun texture's width covered by the bright disc. */
    private static final float SUN_DISC_FRACTION = 12F / 32F;

    private final ResourceKey<CelestialBody> bodyKey;
    private ClientLevel cachedLevel;
    private Optional<CelestialBody> body = Optional.empty();
    private Optional<CelestialBody> parent = Optional.empty();
    private double poleDistance = 6000;
    private double sunSize = 0.53;
    private double parentSize = 1.9;

    public PlanetSkyEffects(ResourceKey<CelestialBody> body) {
        super(Float.NaN, true, SkyType.NONE, false, false);
        this.bodyKey = body;
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

    /**
     * Airless light: no blue twilight. The ground is either in hard, neutral sunlight or in darkness, softened at
     * night by earthshine when the parent planet is up and lit.
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, org.joml.Vector3f colors) {
        refresh(level);
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        double lat = PlanetTerrain.latitude(cam.getPosition().z, poleDistance);
        double lon = 90 * cam.getPosition().x / poleDistance;
        float tod = level.getTimeOfDay(partialTicks);
        Vector3d sun = SkyMath.sunDirection(tod, 0);
        double day = smooth((sun.y + 0.04) / 0.16);
        double night = 0.05;
        if (parent.isPresent()) {
            Vector3d e = SkyMath.parentDirection(lat, lon, PlanetTime.dayFraction(level));
            if (e.y > 0) {
                night += 0.11 * SkyMath.illuminatedFraction(SkyMath.sunDirection(tod, lat), e) * Math.min(1, e.y * 3);
            }
        }
        float lunar = (float) (night + (1 - night) * day);
        float sky = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelY) * lunar;
        float bl = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        // earthshine is faintly blue, sunlight neutral white
        float nightTint = (float) (1 - day);
        colors.set(bl + sky, bg + sky, bb + sky * (1 + 0.15F * nightTint));
        colors.lerp(new org.joml.Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
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
        body = Bodies.get(level.registryAccess(), bodyKey);
        parent = body.flatMap(CelestialBody::parent).flatMap(p -> Bodies.get(level.registryAccess(), ResourceKey.create(Bodies.REGISTRY, p)));
        poleDistance = level.registryAccess().registry(TerrainProfile.REGISTRY)
                .flatMap(r -> r.stream().filter(t -> t.body().equals(bodyKey.location())).findFirst())
                .map(TerrainProfile::poleDistance).orElse(6000.0);
        // distance to the sun: the orbit of the first ancestor that circles a star
        double sunDistance = 1.496e8;
        Optional<CelestialBody> b = body;
        while (b.isPresent()) {
            Optional<CelestialBody> p = b.get().parent().flatMap(id -> Bodies.get(level.registryAccess(), ResourceKey.create(Bodies.REGISTRY, id)));
            if (p.isPresent() && p.get().type() == BodyType.STAR) {
                sunDistance = b.get().orbit().semiMajorAxisKm();
                double sunRadius = p.get().physical().radiusKm();
                sunSize = SkyMath.angularDiameter(sunRadius, Math.max(sunDistance, sunRadius * 2));
                break;
            }
            b = p;
        }
        parentSize = body.map(bb -> parent.map(pp -> SkyMath.angularDiameter(pp.physical().radiusKm(), Math.max(1, bb.orbit().semiMajorAxisKm())))
                .orElse(0.0)).orElse(0.0);
        EarthDisc.reset();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        refresh(level);
        Vec3 cam = camera.getPosition();
        double lat = PlanetTerrain.latitude(cam.z, poleDistance);
        double lon = 90 * cam.x / poleDistance;
        float timeOfDay = level.getTimeOfDay(partialTick);
        double scale = SkyConfig.bodyScale();

        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelViewMatrix);

        // ---- stars and sun turn with the sky
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees((float) lat));
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
        ps.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360F));
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        float day = (float) Math.max(0, SkyMath.sunDirection(timeOfDay, lat).y);
        float starBrightness = 1F - 0.35F * Math.min(1F, day * 3F);
        RenderSystem.setShaderColor(starBrightness, starBrightness, starBrightness, 1F);
        VertexBuffer stars = StarField.buffer();
        stars.bind();
        stars.drawWithShader(ps.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        float sunHalf = (float) (100 * Math.tan(Math.toRadians(sunSize * scale / 2)) / SUN_DISC_FRACTION);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -sunHalf, 100F, -sunHalf).setUv(0F, 0F);
        bb.addVertex(m, sunHalf, 100F, -sunHalf).setUv(1F, 0F);
        bb.addVertex(m, sunHalf, 100F, sunHalf).setUv(1F, 1F);
        bb.addVertex(m, -sunHalf, 100F, sunHalf).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        ps.popPose();

        // ---- the parent planet, fixed above a tidally locked moon
        if (parent.isPresent() && body.map(b -> b.rotation().tidallyLocked()).orElse(false)) {
            renderParent(level, ps, partialTick, lat, lon, timeOfDay, scale);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    private void renderParent(ClientLevel level, PoseStack ps, float partialTick, double lat, double lon, float timeOfDay, double scale) {
        double fraction = PlanetTime.dayFraction(level);
        Vector3d e = SkyMath.parentDirection(lat, lon, fraction);
        if (e.y < -0.35) {
            return; // far side: below the horizon
        }
        Vector3d s = SkyMath.sunDirection(timeOfDay, lat);
        Vector3d worldUp = new Vector3d(0, 1, 0);
        if (Math.abs(e.y) > 0.98) {
            worldUp.set(0, 0, -1);
        }
        Vector3d up = new Vector3d(worldUp).sub(new Vector3d(e).mul(worldUp.dot(e))).normalize();
        Vector3d right = new Vector3d(e).cross(up).normalize();
        Vector3d toViewer = new Vector3d(e).negate();
        Vector3d sunInDisc = new Vector3d(s.dot(right), s.dot(up), s.dot(toViewer));
        double dayTicks = level.getDayTime() + partialTick;
        double spin = dayTicks / 24000.0 * Math.PI * 2;
        double cloudSpin = spin * 0.985 + level.getGameTime() / 90000.0;
        ResourceLocation tex = EarthDisc.update(sunInDisc, spin, cloudSpin);
        if (tex == null) {
            return;
        }
        float half = (float) (100 * Math.tan(Math.toRadians(parentSize * scale / 2)));
        Vector3d c = new Vector3d(e).mul(100);
        Matrix4f m = ps.last().pose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertex(bb, m, c, right, up, -half, half, 0, 0);
        vertex(bb, m, c, right, up, -half, -half, 0, 1);
        vertex(bb, m, c, right, up, half, -half, 1, 1);
        vertex(bb, m, c, right, up, half, half, 1, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float dx, float dy, float u, float v) {
        bb.addVertex(m, (float) (c.x + right.x * dx + up.x * dy), (float) (c.y + right.y * dx + up.y * dy), (float) (c.z + right.z * dx + up.z * dy))
                .setUv(u, v);
    }
}
