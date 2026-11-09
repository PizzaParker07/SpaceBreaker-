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

    private static double arrivalTop = Double.NaN;
    @Nullable
    private static ResourceKey<Level> arrivalDim;
    private static int arrivalRocket = -1;

    /**
     * Switches to take parts of this view out one at a time, for tracking a clash with another mod's renderer:
     * -Dspacebreaker.spaceView.off=fog,fogColour,gradient,stars,ball,overTerrain (any of them, comma separated), or the
     * environment variable SB_SPACEVIEW_OFF.
     */
    private static final java.util.Set<String> OFF = java.util.Set.of(System.getProperty("spacebreaker.spaceView.off",
            System.getenv().getOrDefault("SB_SPACEVIEW_OFF", "")).split(","));

    static boolean off(String part) {
        return OFF.contains(part);
    }

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
     * How much of the ground is hidden under the ball drawn over it (0..1): only in the last stretch before the view is
     * the orbit's (and the first stretch of a descent), where the ball is small enough on screen to cover the ground
     * as one smooth body. Below that the ball stays behind the terrain (see {@link #ball}).
     */
    static float hide(float k) {
        return smooth((k - 0.75F) / 0.2F);
    }

    /**
     * How strongly the ball is drawn in the sky, behind the terrain: wherever the ground is drawn it covers the ball,
     * so the ball only shows beyond the view distance, as the world's far ground and horizon. In a rocket it comes in
     * with height above the ground (from a quarter of the view distance up); for anyone else with the space factor.
     */
    static float ball(float k) {
        float byK = smooth((k - 0.05F) / 0.25F);
        // a ball of blocks seen from close above its surface breaks up into slabs: it is only drawn once it is well
        // rounded on screen (lower, the ground and the world's own sky make the horizon)
        float rounded = smooth((MAX_BALL_ANGLE + 2.5F - angle(k)) / 2.5F);
        if (RocketHud.ridden() == null) {
            return byK * rounded;
        }
        // the far ground is drawn by the game to the view distance, and by an LOD mod (Distant Horizons) much further
        double reach = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        // (with the LOD mod running and its terrain shown, the far ground is its business: no ball until the LODs
        // are switched off for the last stretch before the orbit view)
        boolean lods = com.pizzaparker07.spacebreaker.compat.client.DhCompat.active() && !com.pizzaparker07.spacebreaker.compat.client.DhCompat.hidingLods();
        if (lods) {
            reach = Math.max(reach, com.pizzaparker07.spacebreaker.compat.client.DhCompat.lodReachBlocks());
        }
        float far = Math.max(lods ? 0F : byK, smooth((float) ((altitude() - 0.25 * reach) / (0.35 * reach))));
        // coming down into a world just arrived in, the ball stays until the ground under the camera is really drawn
        if (!Double.isNaN(arrivalTop) && !groundDrawn()) {
            far = 1F;
        }
        return far * rounded;
    }

    private static boolean lodsHidden;

    /** Whether the terrain straight below the camera has been built for drawing (or an LOD mod draws it). */
    static boolean groundDrawn() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        var column = net.minecraft.core.BlockPos.containing(cam.x, mc.level.getMinBuildHeight(), cam.z);
        if (!mc.level.hasChunkAt(column)) {
            return false;
        }
        int y = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) - 1;
        return mc.levelRenderer.isSectionCompiled(new net.minecraft.core.BlockPos(column.getX(), Math.max(mc.level.getMinBuildHeight(), y), column.getZ()));
    }

    /**
     * Each client tick: while the ball covers the ground (the last stretch before the orbit view), an LOD mod's far
     * terrain is switched off, or it would show past the ball's edge; it comes back as soon as the ball lifts.
     */
    private static int dhLogTicks;

    static void tickLods() {
        if (RocketHud.ridden() != null && com.pizzaparker07.spacebreaker.compat.Compat.isInstalled("distanthorizons") && --dhLogTicks <= 0) {
            // what this view sees of Distant Horizons, every five seconds of a flight (for checking a DH install)
            dhLogTicks = 100;
            float k = factor();
            com.pizzaparker07.spacebreaker.Spacebreaker.LOGGER.info(
                    "Space view with Distant Horizons: active {}, LOD reach {}, curvature {}, LODs hidden {}, k {}, hide {}, ball {}, altitude {}",
                    com.pizzaparker07.spacebreaker.compat.client.DhCompat.active(),
                    com.pizzaparker07.spacebreaker.compat.client.DhCompat.lodReachBlocks(),
                    com.pizzaparker07.spacebreaker.compat.client.DhCompat.curvatureRatio(),
                    com.pizzaparker07.spacebreaker.compat.client.DhCompat.hidingLods(),
                    String.format(java.util.Locale.ROOT, "%.2f", k), String.format(java.util.Locale.ROOT, "%.2f", hide(k)),
                    String.format(java.util.Locale.ROOT, "%.2f", ball(k)), (int) altitude());
        }
        // (once hidden, DH may report itself off: keep going so the LODs are always given back)
        if (!lodsHidden && !com.pizzaparker07.spacebreaker.compat.client.DhCompat.active()) {
            return;
        }
        boolean want = RocketHud.ridden() != null && hide(factor()) > 0.5F;
        if (want != lodsHidden) {
            lodsHidden = want;
            com.pizzaparker07.spacebreaker.compat.client.DhCompat.hideLods(want);
        }
        // DH's own fog is tuned for standing on the ground: from the first rise of the space view it would wash the
        // LOD terrain and the sky behind it, so it stays off for the climb, the orbit and the descent
        boolean noFog = RocketHud.ridden() != null && factor() > 0.02F;
        if (noFog != dhFogHidden) {
            dhFogHidden = noFog;
            hideDhFog(noFog);
        }
    }

    private static boolean dhFogHidden;
    private static java.lang.reflect.Method hideFog;
    private static boolean hideFogLooked;

    /** {@code DhCompat.hideFog}, where this build has it (a no-op where it does not). */
    private static void hideDhFog(boolean hide) {
        if (!hideFogLooked) {
            hideFogLooked = true;
            try {
                hideFog = com.pizzaparker07.spacebreaker.compat.client.DhCompat.class.getMethod("hideFog", boolean.class);
            } catch (NoSuchMethodException e) {
                hideFog = null;
            }
        }
        if (hideFog != null) {
            try {
                hideFog.invoke(null, hide);
            } catch (ReflectiveOperationException e) {
                hideFog = null;
            }
        }
    }

    /** The widest the ball below is drawn (angular radius, degrees). */
    static final float MAX_BALL_ANGLE = 76F;

    private static float fogEnd = Float.NaN;
    private static boolean fogSphere;

    /** The world's own terrain fog, as it comes in (before this mod widens it with altitude). */
    static void terrainFog(float end, boolean sphere) {
        fogEnd = end;
        fogSphere = sphere;
    }

    /** Height of the camera above the ground below it (above sea level while that ground is not loaded). */
    static double altitude() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0;
        }
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        return cam.y - ground(mc.level, cam);
    }

    /** How much the fog is cleared away with height (0..1): as soon as the far ground (the ball) is to be seen. */
    static float clearAir(float k) {
        return Math.max(Mth.clamp(k / 0.3F, 0, 1), surface(Minecraft.getInstance().level) == null ? 0F : ball(k));
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
     * Angular radius of the ball below, degrees: the real one for this height over a world of the body's size (a
     * nearly flat horizon a few hundred blocks up), turning into the orbit sky's angle over the last stretch before
     * the cut so the two views meet.
     */
    static float angle(float k) {
        Location here = surface(Minecraft.getInstance().level);
        double radius = virtualRadius(here);
        double h = Math.max(1, altitude());
        float real = (float) Math.toDegrees(Math.asin(radius / (radius + h)));
        return Math.min(89.5F, Mth.lerp(smooth((k - 0.55F) / 0.45F), real, OrbitSkyEffects.ORBIT_ANGULAR_RADIUS));
    }

    /**
     * Radius in blocks of the world the terrain stands for, for the curve of its horizon (the Earth's 6,000: the
     * ball is rounded enough to draw from about 190 blocks up, just past where the ground is drawn to).
     */
    static double virtualRadius(@Nullable Location here) {
        double km = here == null ? 6371 : Locations.body(here.body()).map(b -> b.physical().radiusKm()).orElse(6371.0);
        return Math.max(2000, 6000 * km / 6371.0);
    }

    /**
     * The sun that lights the ball: the orbit sky's at the cut (so the two views match), turning into this world's own
     * sun as the view comes down (by k = 0.7), so the ball below is lit like the ground it hands over to.
     */
    static Vector3f sun(ClientLevel level, float k, float t, float partialTick) {
        float theta = level.getTimeOfDay(partialTick) * Mth.TWO_PI;
        Vector3f local = new Vector3f(-Mth.sin(theta), Mth.cos(theta), 0);
        Vector3f orbit = OrbitSkyEffects.sunDirection(t);
        float w = smooth((k - 0.7F) / 0.3F);
        Vector3f s = local.lerp(orbit, w);
        return s.lengthSquared() < 1e-6F ? orbit : s.normalize();
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
        float planet = here == null ? 0F : ball(k);
        if (here == null || k <= 0 && planet <= 0.003F) {
            return;
        }
        // deep blue overhead first, then black; the horizon stays a thin band of light blue the longest
        float top = Mth.clamp(k * 3F, 0, 1);
        // without air the sky is black from the ground up: only the stars and the ball change
        float night = here.atmosphere() ? Mth.clamp((k - 0.3F) / 0.6F, 0, 1) : 1F;
        int r = (int) Mth.lerp(night, 8, 0), g = (int) Mth.lerp(night, 16, 0), b = (int) Mth.lerp(night, 52, 0);
        int over = (int) (top * 255) << 24 | r << 16 | g << 8 | b;
        int horizon = (int) (Mth.clamp((k - 0.35F) / 0.6F, 0, 1) * 255) << 24 | r << 16 | g << 8 | b;
        if (com.pizzaparker07.spacebreaker.compat.client.ShaderState.shaderPackInUse()) {
            // a shader pack draws its own horizon: the light band left open at eye level would show it as a thin stepped
            // line across the screen, so the darkening comes down evenly instead
            horizon = over;
        }
        if (!off("gradient")) {
            SkyDraw.gradient(modelView, over, horizon);
        }
        if (!off("stars")) {
            SkyDraw.stars(modelView, projection, k);
        }
        float t = level.getGameTime() + partialTick;
        if (k < 1F) {
            // the world's own sun, handing over to the orbit sky's as the view climbs
            float theta = level.getTimeOfDay(partialTick) * Mth.TWO_PI;
            Vector3f day = new Vector3f(-Mth.sin(theta), Mth.cos(theta), 0);
            SkyDraw.billboard(modelView, OrbitSkyEffects.SUN, day, 100F, 15F, k * (1F - smooth(k)));
        }
        RenderSystem.depthMask(false);
        if (!off("ball")) OrbitSkyEffects.drawBodyBelow(modelView, here.body(), angle(k), t, lit(level, here, t) * smooth(k), planet,
                com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH, new Vector3f(0, -1, 0), sun(level, k, t, partialTick));
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
        if (here == null || hide <= 0.003F || off("overTerrain")) {
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
        OrbitSkyEffects.drawBodyBelow(modelView, here.body(), angle(k), t, lit(level, here, t) * smooth(k), hide,
                com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH, new Vector3f(0, -1, 0), sun(level, k, t, partialTick));
        main.copyDepthFrom(saved);
        main.bindWrite(false);
        // the ball takes a sky object's depth once it covers the ground, so water and clouds stay under it. Not with a
        // shader pack: there the ball's pixels are left at the far depth, where the space sky is laid over the pack's
        // picture untouched by its fog (a sky object's depth beyond the view distance would be fogged out)
        boolean pack = com.pizzaparker07.spacebreaker.compat.client.ShaderState.shaderPackInUse();
        if (hide > 0.98F && !pack) {
            disc(modelView, angle(k), 1F, true);
        } else if (pack && hide > 0.5F) {
            // with a pack: where the ball covers the ground, set the depth back to the far value
            GL11.glDepthRange(1.0, 1.0);
            disc(modelView, angle(k), 1F, true);
            GL11.glDepthRange(0.0, 1.0);
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
