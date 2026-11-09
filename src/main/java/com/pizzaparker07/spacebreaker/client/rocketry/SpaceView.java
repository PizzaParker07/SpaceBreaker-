package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * The sky of a surface world seen from high up, built so that the top of an ascent and the start of a descent look
 * exactly like the orbit above (the "matched cut"): with altitude the sky darkens, the stars come out, the ground
 * fades under the planet's own ball, whose horizon curves away until it is the ball of the orbit sky
 * ({@link OrbitSkyEffects#drawBodyBelow}, same sun, same spin, same moon). At that moment the dimension changes and
 * nothing on screen moves.
 *
 * <p>The space factor {@code k} (0 on the ground, 1 = the orbit view) follows the camera's height, and while the
 * player rides a rocket it follows the flight: the ascent's progress on the way up, the height below the arrival
 * point on the way down (the descent starts at the orbit view and the ground comes back in over a few seconds).
 */
public final class SpaceView {
    /** Height where the air starts to thin for a camera that is not in a rocket (overworld). */
    private static final double THIN_FROM = 260, THIN_SPAN = 640;
    /** Ascent progress at which the view starts to leave the ground, and where it is the orbit view. */
    static final float ASCENT_FROM = 0.25F, ASCENT_FULL = 0.97F;
    /** Angular radius of the ball when it first appears (a nearly flat horizon), degrees. */
    static final float LOW_ANGLE = 72F;

    private static double arrivalTop = Double.NaN;
    @Nullable
    private static ResourceKey<Level> arrivalDim;
    private static int arrivalRocket = -1;

    private SpaceView() {
    }

    /** The surface place this dimension is, or null (orbit, unknown). */
    @Nullable
    static Location surface(@Nullable ClientLevel level) {
        if (level == null) {
            return null;
        }
        return Locations.at(level.dimension(), null).filter(l -> !l.orbit()).orElse(null);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /** The space factor now: 0 on the ground, 1 when the view is the orbit's. */
    public static float factor() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Location here = surface(level);
        if (here == null) {
            return 0F;
        }
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        float byHeight = level.dimension() == Level.OVERWORLD ? (float) Mth.clamp((y - THIN_FROM) / THIN_SPAN, 0, 1) : 0F;
        RocketEntity rocket = RocketHud.ridden();
        if (rocket == null || rocket.level() != level) {
            return byHeight;
        }
        FlightState s = rocket.state();
        if (s == FlightState.ASCENT || s == FlightState.COUNTDOWN) {
            arrivalTop = Double.NaN;
            return Math.max(byHeight, smooth((rocket.ascentProgress() - ASCENT_FROM) / (ASCENT_FULL - ASCENT_FROM)));
        }
        if (s == FlightState.REENTRY || s == FlightState.DESCENT || s == FlightState.PARACHUTE) {
            // the descent starts where the orbit view left off: remember the height we arrived at
            if (Double.isNaN(arrivalTop) || arrivalDim != level.dimension() || arrivalRocket != rocket.getId()) {
                if (rocket.timer() > 40 && s != FlightState.REENTRY) {
                    return byHeight; // a descent that did not come from orbit (a hop on the same world)
                }
                arrivalTop = y;
                readySince = -1;
                arrivalDim = level.dimension();
                arrivalRocket = rocket.getId();
            }
            // the ground takes over once it is well inside the view distance (and the fog), never before it is drawn
            double low = ground(level, mc.gameRenderer.getMainCamera().getPosition()) + Math.min(here.atmosphere() ? 150 : 60, reach(mc));
            float k = (float) Mth.clamp((y - low) / Math.max(1, arrivalTop - low), 0, 1);
            return Math.max(byHeight, smooth(k));
        }
        arrivalTop = Double.NaN;
        return byHeight;
    }

    /**
     * How much of the ground is hidden under the ball (0..1): once the ground is far below and small, and in any case
     * once the ground has dropped out of the view distance (so there is never a moment with neither ground nor ball).
     */
    static float hide(float k) {
        // (only in a rocket: someone flying high on their own keeps the real ground for as long as it is drawn)
        return k <= 0F ? 0F : Math.max(smooth((k - 0.35F) / 0.4F), RocketHud.ridden() != null ? groundOutOfReach() : 0F);
    }

    /** How strongly the ball itself is drawn: it comes in once it is a ball, and whenever it has to cover for the ground. */
    static float ball(float k) {
        return Math.max(smooth((k - 0.25F) / 0.2F), hide(k));
    }

    private static float readySince = -1;
    private static float fogEnd = Float.NaN;
    private static boolean fogSphere;

    /** The world's own terrain fog, as it comes in (before this mod widens it with altitude). */
    static void terrainFog(float end, boolean sphere) {
        fogEnd = end;
        fogSphere = sphere;
    }

    /**
     * 0 while the ground below is drawn, 1 while it is not: before the client has the terrain under the camera built
     * (just after arriving in a world), or when it is beyond the far plane. Once the terrain is there, the cover lifts
     * over a second and a half rather than at once.
     */
    static float groundOutOfReach() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0F;
        }
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        var below = net.minecraft.core.BlockPos.containing(cam.x, mc.level.getMinBuildHeight(), cam.z);
        // (waiting for the terrain to be built only matters on the way down into a world just arrived in)
        boolean built = mc.level.hasChunkAt(below) && mc.levelRenderer.hasRenderedAllSections();
        // counted in game time, so the fade lasts the same share of the descent at any frame rate
        float now = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        double groundY = mc.level.hasChunkAt(below)
                ? mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, below.getX(), below.getZ())
                : mc.level.getSeaLevel();
        // terrain sections are drawn up to the view distance in every direction, so the ground straight below is lost
        // once it is that far down (and sooner in a round fog)
        double far = mc.options.getEffectiveRenderDistance() * 16.0;
        if (fogSphere && !Float.isNaN(fogEnd)) {
            far = Math.min(far, fogEnd);
        }
        // a wide window, so the ground hands over to the ball over a few seconds of the climb or the descent
        float tooFar = smooth((float) ((cam.y - groundY - far * 0.45) / (far * 0.5)));
        if (Double.isNaN(arrivalTop)) {
            return tooFar; // on the way up the ground is there from the start
        }
        // once the ground has been there, it stays (sections built later as the camera drops do not bring the ball back)
        if (readySince < 0) {
            if (!built) {
                return 1F;
            }
            readySince = now;
        }
        float lifting = 1F - smooth((now - readySince) / 30F);
        return Math.max(lifting, tooFar);
    }

    /** Height of the ground under the camera (sea level while it is not loaded). */
    private static double ground(ClientLevel level, net.minecraft.world.phys.Vec3 cam) {
        var at = net.minecraft.core.BlockPos.containing(cam);
        return level.hasChunkAt(at) ? level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ())
                : level.getSeaLevel();
    }

    /** Distance below which the ground is clearly drawn (well inside the view distance). */
    private static double reach(Minecraft mc) {
        return mc.options.getEffectiveRenderDistance() * 16 * 0.55;
    }

    /**
     * Angular radius of the ball below for this space factor, degrees: from a wide ball (close to, but not touching,
     * the camera, where its stepped surface would rise above the horizon) to the orbit sky's.
     */
    static float angle(float k) {
        return Mth.lerp(smooth(k), LOW_ANGLE, OrbitSkyEffects.ORBIT_ANGULAR_RADIUS);
    }

    private static float lit(ClientLevel level, Location here, float t) {
        return (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(
                com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(level.registryAccess()), here.body(), t);
    }

    /** After the vanilla sky: thin air turning to black, stars, and the ball below (behind the terrain). */
    static void sky(Matrix4f modelView, Matrix4f projection, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Location here = surface(level);
        float k = factor();
        if (here == null || k <= 0) {
            return;
        }
        // deep blue overhead first, then black; the horizon stays a thin band of light blue the longest
        float top = Mth.clamp(k * 3F, 0, 1);
        // without air the sky is black from the ground up: only the stars and the ball change
        float night = here.atmosphere() ? Mth.clamp((k - 0.3F) / 0.6F, 0, 1) : 1F;
        int r = (int) Mth.lerp(night, 8, 0), g = (int) Mth.lerp(night, 16, 0), b = (int) Mth.lerp(night, 52, 0);
        int over = (int) (top * 255) << 24 | r << 16 | g << 8 | b;
        int horizon = (int) (Mth.clamp((k - 0.35F) / 0.6F, 0, 1) * 255) << 24 | r << 16 | g << 8 | b;
        SkyDraw.gradient(modelView, over, horizon);
        SkyDraw.stars(modelView, projection, k);
        float t = level.getGameTime() + partialTick;
        if (k < 1F) {
            // the world's own sun, handing over to the orbit sky's as the view climbs
            float theta = level.getTimeOfDay(partialTick) * Mth.TWO_PI;
            Vector3f day = new Vector3f(-Mth.sin(theta), Mth.cos(theta), 0);
            SkyDraw.billboard(modelView, OrbitSkyEffects.SUN, day, 100F, 15F, k * (1F - smooth(k)));
        }
        float planet = ball(k);
        RenderSystem.depthMask(false);
        OrbitSkyEffects.drawBodyBelow(modelView, here.body(), angle(k), t, lit(level, here, t) * smooth(k), planet);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Nullable
    private static TextureTarget saved;

    /**
     * After the solid terrain: the ball again, over the ground, as it fades out of sight below; once it is gone the ball
     * also takes the depth of a far sky object, so water and clouds drawn later stay under it while the rocket (close
     * to the camera) is still drawn over it.
     */
    static void overTerrain(Matrix4f modelView, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Location here = surface(level);
        float k = factor();
        float hide = hide(k);
        if (here == null || hide <= 0.003F) {
            return;
        }
        float t = level.getGameTime() + partialTick;
        var main = mc.getMainRenderTarget();
        // the body renderer clears the depth buffer for its own use: keep the terrain's depth and put it back after
        if (saved == null || saved.width != main.width || saved.height != main.height) {
            if (saved != null) {
                saved.destroyBuffers();
            }
            saved = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        }
        saved.copyDepthFrom(main);
        main.bindWrite(false);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        // a black disc under the ball, so the ground does not show through its stepped edge
        disc(modelView, angle(k), hide, false);
        OrbitSkyEffects.drawBodyBelow(modelView, here.body(), angle(k), t, lit(level, here, t) * smooth(k), hide);
        main.copyDepthFrom(saved);
        main.bindWrite(false);
        if (hide > 0.98F) {
            disc(modelView, angle(k), 1F, true);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /**
     * A disc facing the camera that covers the ball's outline (the ball's centre 100 below the camera): in black, or
     * only into the depth buffer at the ball's distance.
     */
    private static void disc(Matrix4f modelView, float angleDeg, float alpha, boolean depth) {
        float a = angleDeg * Mth.DEG_TO_RAD;
        // the cone from the eye that just touches the ball: a disc through the centre, square to the view of it
        // (the black one close in, so its far edge is never cut by the far plane; the depth one at the ball's distance)
        float dist = depth ? 100F : 30F, r = dist * Mth.sin(a) / Mth.cos(a) * 1.02F;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (depth) {
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.depthMask(true);
        }
        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int alphaByte = (int) (Mth.clamp(alpha, 0, 1) * 255);
        buf.addVertex(modelView, 0, -dist, 0).setColor(0, 0, 0, alphaByte);
        int n = 48;
        for (int i = 0; i <= n; i++) {
            float th = i * Mth.TWO_PI / n;
            buf.addVertex(modelView, Mth.cos(th) * r, -dist, Mth.sin(th) * r).setColor(0, 0, 0, alphaByte);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        if (depth) {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
