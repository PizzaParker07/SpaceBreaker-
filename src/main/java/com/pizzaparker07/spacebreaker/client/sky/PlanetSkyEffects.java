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
import org.joml.Vector3f;
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
        double day = smooth((sun.y + 0.04) / 0.16)
                * (1 - 0.93 * com.pizzaparker07.spacebreaker.content.worldgen.EclipseState.smoothed(level));
        double shine = parent.isPresent()
                ? com.pizzaparker07.spacebreaker.content.worldgen.Earthshine.brightness(lat, lon, tod, PlanetTime.dayFraction(level)) : 0;
        double night = 0.05 + shine;
        float lunar = (float) (night + (1 - night) * day);
        float sky = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelY) * lunar;
        float bl = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        // earthshine is faintly blue, sunlight neutral white
        float nightTint = (float) (1 - day);
        float blue = nightTint * (0.15F + com.pizzaparker07.spacebreaker.content.worldgen.Earthshine.BLUE
                * (float) (shine / com.pizzaparker07.spacebreaker.content.worldgen.Earthshine.MAX));
        colors.set(bl + sky * (1 - 0.3F * blue), bg + sky * (1 - 0.1F * blue), bb + sky * (1 + blue));
        colors.lerp(new org.joml.Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
    }

    /**
     * The rare horizon glow after sunset or before sunrise: a thin warm band hugging the horizon above the hidden Sun.
     * Additive quads in the camera-rotated world frame ({@code pose}), so it shows with or without shader packs.
     */
    public void renderHorizonGlow(ClientLevel level, PoseStack pose) {
        refresh(level);
        var clock = PlanetTime.clock(level);
        if (clock.isEmpty()) {
            return;
        }
        Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double lat = PlanetTerrain.latitude(cam.z, poleDistance);
        float glow = (float) com.pizzaparker07.spacebreaker.content.worldgen.HorizonGlow.at(clock.get(), level.getDayTime(), lat,
                level.dimension().location().hashCode());
        if (glow <= 0.002F) {
            return;
        }
        Vector3d sun = SkyMath.sunDirection(clock.get().timeOfDay(level.getDayTime()), lat);
        double az = Math.atan2(sun.z, sun.x);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = pose.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n = 32;
        double half = Math.toRadians(40);
        double r = 100;
        for (int i = 0; i < n; i++) {
            double u0 = -1 + 2.0 * i / n;
            double u1 = -1 + 2.0 * (i + 1) / n;
            float b0 = glow * 0.42F * (float) (1 - u0 * u0) * (float) (1 - u0 * u0);
            float b1 = glow * 0.42F * (float) (1 - u1 * u1) * (float) (1 - u1 * u1);
            double top0 = Math.toRadians(0.4 + 1.6 * (1 - u0 * u0));
            double top1 = Math.toRadians(0.4 + 1.6 * (1 - u1 * u1));
            double a0 = az + u0 * half;
            double a1 = az + u1 * half;
            double low = Math.toRadians(-4);
            // bright from below the horizon (hidden by any terrain in front) fading to nothing a degree or two up
            vertex(bb, m, r, a0, low, b0);
            vertex(bb, m, r, a1, low, b1);
            vertex(bb, m, r, a1, top1, 0);
            vertex(bb, m, r, a0, top0, 0);
        }
        var mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, double r, double az, double el, float b) {
        float x = (float) (r * Math.cos(el) * Math.cos(az));
        float y = (float) (r * Math.sin(el));
        float z = (float) (r * Math.cos(el) * Math.sin(az));
        bb.addVertex(m, x, y, z).setColor(b, 0.93F * b, 0.8F * b, 1F);
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
                sunRadiusKm = sunRadius;
                sunSize = SkyMath.angularDiameter(sunRadius, Math.max(sunDistance, sunRadius * 2));
                break;
            }
            b = p;
        }
        parentSize = body.map(bb -> parent.map(pp -> SkyMath.angularDiameter(pp.physical().radiusKm(), Math.max(1, bb.orbit().semiMajorAxisKm())))
                .orElse(0.0)).orElse(0.0);
        EarthDisc.reset();
    }

    private long comaTick = Long.MIN_VALUE;
    private float coma;
    private double sunRadiusKm = 695700;

    /** Size of the sun disc relative to its mean size (eccentric orbits: larger near perihelion). */
    protected double sunScale(ClientLevel level) {
        return 1.0;
    }

    /** Extra glow around the sun (0 = none), for worlds that pass close to it. */
    protected float sunGlow(ClientLevel level) {
        return 0F;
    }

    /** Coma glow of the comet this sky belongs to (0 if it is not a comet or dormant), recomputed every 100 ticks. */
    private float comaGlow(ClientLevel level) {
        long tick = level.getGameTime() / 100;
        if (tick != comaTick) {
            comaTick = tick;
            var comet = com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.byId(bodyKey.location().getPath());
            coma = 0F;
            if (comet.isPresent()) {
                var activity = com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.at(comet.get(),
                        com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.simTime(level));
                coma = com.pizzaparker07.spacebreaker.content.worldgen.CometPoints.comaGlow(activity);
                // on an eccentric comet orbit the sun swells and shrinks: use the true distance, not the mean
                sunSize = SkyMath.angularDiameter(sunRadiusKm, Math.max(activity.distanceAu() * 1.495978707e8, sunRadiusKm * 2));
            }
        }
        return coma;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        refresh(level);
        float coma = comaGlow(level);
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

        float sunHalf = (float) (100 * Math.tan(Math.toRadians(sunSize * scale * sunScale(level) / 2)) / SUN_DISC_FRACTION);
        // eclipse: the planet slides over the sun's disc
        float eclipse = (float) com.pizzaparker07.spacebreaker.content.worldgen.EclipseState.coverage(level);
        RenderSystem.setShaderColor(1F - eclipse, 1F - eclipse, 1F - eclipse, 1F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(m, -sunHalf, 100F, -sunHalf).setUv(0F, 0F);
        bb.addVertex(m, sunHalf, 100F, -sunHalf).setUv(1F, 0F);
        bb.addVertex(m, sunHalf, 100F, sunHalf).setUv(1F, 1F);
        bb.addVertex(m, -sunHalf, 100F, sunHalf).setUv(0F, 1F);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        float glow = sunGlow(level);
        if (glow > 0) {
            CometTrails.skyGlow(ps, new Vector3f(0, 1, 0), 0.55F, glow, 1F, 0.97F, 0.9F);
        }
        // standing on an active comet: its own coma glows around the sun and, fainter, along the tail away from it
        if (coma > 0) {
            CometTrails.skyGlow(ps, new Vector3f(0, 1, 0), 1.2F, coma, 0.75F, 0.85F, 1F);
            CometTrails.skyGlow(ps, new Vector3f(0, -1, 0), 0.7F, coma * 0.45F, 0.7F, 0.8F, 1F);
        }
        // planets at their real positions along the sun's arc (no air: visible by day too)
        PlanetDots.render(level, ps, projectionMatrix, starBrightness);
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
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
        float eclipseNow = (float) com.pizzaparker07.spacebreaker.content.worldgen.EclipseState.coverage(level);
        var blocky = SkyConfig.blockyStyle();
        if (blocky != null) {
            // Minecraft-style planet: a ball of blocks or a pixel cube, its pole up, turning with its own day
            float radius = (float) (100 * Math.tan(Math.toRadians(parentSize * scale / 2)));
            Vector3f centre = new Vector3f((float) e.x, (float) e.y, (float) e.z).mul(100);
            var pose = new BlockyBodyRenderer.Pose(centre, radius, new Vector3f((float) up.x, (float) up.y, (float) up.z),
                    new Vector3f((float) toViewer.x, (float) toViewer.y, (float) toViewer.z), (float) spin);
            BlockyBodyRenderer.draw(ps.last().pose(), blocky, 0, earthMap(), pose,
                    new Vector3f((float) s.x, (float) s.y, (float) s.z), 0.07F, 1F - 0.9F * eclipseNow, 1F, null);
            if (eclipseNow > 0.01F) {
                sunsetRing(ps.last().pose(), new Vector3d(e).mul(100), right, up, radius * 0.985F, eclipseNow);
            }
            return;
        }
        ResourceLocation tex = EarthDisc.update(sunInDisc, spin, cloudSpin);
        if (tex == null) {
            return;
        }
        float half = (float) (100 * Math.tan(Math.toRadians(parentSize * scale / 2)));
        Vector3d c = new Vector3d(e).mul(100);
        Matrix4f m = ps.last().pose();
        float eclipse = (float) com.pizzaparker07.spacebreaker.content.worldgen.EclipseState.coverage(level);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        // during an eclipse the planet shows us its night side, lit only by its own atmosphere
        float lit = 1F - 0.9F * eclipse;
        RenderSystem.setShaderColor(lit, lit, lit, 1F);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertex(bb, m, c, right, up, -half, half, 0, 0);
        vertex(bb, m, c, right, up, -half, -half, 0, 1);
        vertex(bb, m, c, right, up, half, -half, 1, 1);
        vertex(bb, m, c, right, up, half, half, 1, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        if (eclipse > 0.01F) {
            sunsetRing(m, c, right, up, half * 0.985F, eclipse);
        }
    }

    private static BlockyBodyRenderer.ColorMap earthMap;

    /** Earth's surface with its clouds, for the blocky planet. */
    private static BlockyBodyRenderer.ColorMap earthMap() {
        if (earthMap == null) {
            earthMap = BlockyBodyRenderer.withClouds(BlockyBodyRenderer.texture(Spacebreaker.id("textures/environment/earth_map.png")),
                    BlockyBodyRenderer.texture(Spacebreaker.id("textures/environment/earth_clouds.png")));
        }
        return earthMap;
    }

    /**
     * The ring of every sunrise and sunset on the planet at once: sunlight bent through its atmosphere, a thin red-orange
     * band hugging the dark disc, brighter on the side toward the hidden sun. Additive, no texture.
     */
    private static void sunsetRing(Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float radius, float strength) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int sectors = 48;
        float[][] bands = {{0.97F, 0F}, {1.0F, 1F}, {1.035F, 0.55F}, {1.09F, 0F}};
        for (int b = 0; b + 1 < bands.length; b++) {
            for (int j = 0; j < sectors; j++) {
                double a0 = j * Math.PI * 2 / sectors;
                double a1 = (j + 1) * Math.PI * 2 / sectors;
                float k0 = bands[b][1] * strength;
                float k1 = bands[b + 1][1] * strength;
                ringVertex(bb, m, c, right, up, radius * bands[b][0], a0, k0);
                ringVertex(bb, m, c, right, up, radius * bands[b][0], a1, k0);
                ringVertex(bb, m, c, right, up, radius * bands[b + 1][0], a1, k1);
                ringVertex(bb, m, c, right, up, radius * bands[b + 1][0], a0, k1);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    private static void ringVertex(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float r, double a, float k) {
        float dx = (float) (Math.cos(a) * r);
        float dy = (float) (Math.sin(a) * r);
        bb.addVertex(m, (float) (c.x + right.x * dx + up.x * dy), (float) (c.y + right.y * dx + up.y * dy), (float) (c.z + right.z * dx + up.z * dy))
                .setColor(1F * k, 0.42F * k, 0.16F * k, 1F);
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, Vector3d c, Vector3d right, Vector3d up, float dx, float dy, float u, float v) {
        bb.addVertex(m, (float) (c.x + right.x * dx + up.x * dy), (float) (c.y + right.y * dx + up.y * dy), (float) (c.z + right.z * dx + up.z * dy))
                .setUv(u, v);
    }
}
