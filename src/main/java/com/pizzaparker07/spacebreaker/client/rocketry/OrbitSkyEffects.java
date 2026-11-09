package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sky of the orbit dimensions: black space full of stars, the sun, and the body you orbit filling the
 * lower sky as a slowly turning sphere with a day/night terminator (Earth also gets a thin blue limb and
 * its own cloud layer). The other body of the pair hangs small in the distance.
 */
public class OrbitSkyEffects extends DimensionSpecialEffects {
    static final ResourceLocation EARTH = Spacebreaker.id("textures/environment/orbit_earth.png");
    static final ResourceLocation EARTH_CLOUDS = Spacebreaker.id("textures/environment/orbit_earth_clouds.png");
    static final ResourceLocation EARTH_LIMB = Spacebreaker.id("textures/environment/orbit_earth_limb.png");
    static final ResourceLocation MOON = Spacebreaker.id("textures/environment/orbit_moon.png");
    static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    static final ResourceLocation EARTH_DISC = Spacebreaker.id("textures/environment/orbit_earth_disc.png");
    static final ResourceLocation MOON_DISC = Spacebreaker.id("textures/environment/orbit_moon_disc.png");

    /** One orbit (and one day/night cycle of the view) in ticks. */
    public static final float ORBIT_TICKS = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.ORBIT_TICKS;

    private static ResourceLocation lastBody = Location.EARTH;

    /** Which body this orbit view is around: the orbit dimension's own body, or the one our rocket circles. */
    static ResourceLocation orbitedBody(ClientLevel level) {
        if (Location.Orbits.EARTH_ORBIT.equals(level.dimension())) {
            return Location.EARTH;
        }
        if (Location.Orbits.MOON_ORBIT.equals(level.dimension())) {
            return Location.MOON;
        }
        var rocket = RocketHud.ridden();
        if (rocket != null && rocket.location().orbit()) {
            lastBody = rocket.location().body();
        } else {
            var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Locations.bodyForSharedX(cam.x).ifPresent(b -> lastBody = b);
        }
        return Locations.at(level.dimension(), lastBody).map(Location::body).orElse(lastBody);
    }

    private static float sunSize(ResourceLocation body) {
        // smaller sun further out (Mercury large, outer planets small)
        double au = Locations.body(Locations.parentPlanet(body).orElse(body)).map(b -> b.orbit().semiMajorAxisKm() / 1.496e8).orElse(1.0);
        return (float) Mth.clamp(12.0 / Math.max(0.3, au), 2.0, 30.0);
    }

    private static final java.util.Map<ResourceLocation, Boolean> EXISTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean exists(ResourceLocation tex) {
        return EXISTS.computeIfAbsent(tex, t -> net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
    }

    private static ResourceLocation typed(ResourceLocation body, String suffix, String genericRock) {
        ResourceLocation own = Spacebreaker.id("textures/environment/orbit_" + body.getPath() + suffix + ".png");
        if (exists(own)) {
            return own;
        }
        String kind = Locations.body(body).map(b -> switch (b.type()) {
            case GAS_GIANT -> "gas";
            case ICE_GIANT -> "ice";
            default -> genericRock;
        }).orElse(genericRock);
        return Spacebreaker.id("textures/environment/orbit_generic_" + kind + suffix + ".png");
    }

    static ResourceLocation surface(ResourceLocation body) {
        return typed(body, "", "rock");
    }

    static ResourceLocation disc(ResourceLocation body) {
        if (Location.EARTH.equals(body)) {
            return EARTH_DISC;
        }
        if (Location.MOON.equals(body)) {
            return MOON_DISC;
        }
        // planets drawn by their own area ship "<name>_disc.png"
        ResourceLocation theirs = Spacebreaker.id("textures/environment/" + body.getPath() + "_disc.png");
        ResourceLocation own = Spacebreaker.id("textures/environment/orbit_" + body.getPath() + "_disc.png");
        return exists(own) || !exists(theirs) ? typed(body, "_disc", "rock") : theirs;
    }

    private static ResourceLocation limb(ResourceLocation body) {
        ResourceLocation own = Spacebreaker.id("textures/environment/orbit_" + body.getPath() + "_limb.png");
        return exists(own) ? own : Spacebreaker.id("textures/environment/orbit_generic_limb.png");
    }

    public OrbitSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
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
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, double camX,
                                double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    /** Where the sun is in the orbit sky at this time: overhead at phase 0, straight below (behind the body) at 0.5. */
    static Vector3f sunDirection(float time) {
        float a = (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.phase(time) * Mth.TWO_PI;
        return new Vector3f(Mth.sin(a), Mth.cos(a), 0.3F).normalize();
    }

    /** Share of sunlight now (1 day, 0 in the shadow), smoothed over the penumbra. */
    static float sunlit(ClientLevel level, float partialTick) {
        return (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(
                com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(level.registryAccess()),
                orbitedBody(level), level.getGameTime() + partialTick);
    }

    /**
     * Light in orbit: hard white sunlight by day; in the planet's shadow only starlight and the faint glow of the
     * night side, so lamps matter.
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, org.joml.Vector3f colors) {
        float lit = sunlit(level, partialTicks);
        float daylight = 0.06F + 0.94F * lit;
        float sky = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelY) * daylight;
        float bl = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float bg = bl * ((bl * 0.6F + 0.4F) * 0.6F + 0.4F);
        float bb = bl * (bl * bl * 0.6F + 0.4F);
        // the shadow side is faintly blue
        colors.set(bl + sky, bg + sky, bb + sky * (1 + 0.25F * (1 - lit)));
        colors.lerp(new org.joml.Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
    }

    /**
     * The sun, the body below at this angular radius and (round the Earth) the Moon, exactly as the orbit sky draws
     * them at game time {@code t}: shared by the orbit sky and the high-altitude sky of a surface world, so the two
     * frames either side of a change of dimension match.
     *
     * @param lit share of sunlight (orbit shadow), {@code alpha} of the whole
     */
    static void drawBodyBelow(Matrix4f modelView, ResourceLocation body, float angularDeg, float t, float lit, float alpha) {
        drawBodyBelow(modelView, body, angularDeg, t, lit, alpha, com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH);
    }

    /** As above at a fixed ball resolution (see {@link BodyLook#drawBelow}): for a ball whose size is changing. */
    static void drawBodyBelow(Matrix4f modelView, ResourceLocation body, float angularDeg, float t, float lit, float alpha, int n) {
        drawBodyBelow(modelView, body, angularDeg, t, lit, alpha, n, new Vector3f(0, -1, 0));
    }

    /** As above with the body's centre in direction {@code dir} (unit) instead of straight down. */
    static void drawBodyBelow(Matrix4f modelView, ResourceLocation body, float angularDeg, float t, float lit, float alpha, int n, Vector3f dir) {
        float orbit = t / ORBIT_TICKS;
        Vector3f sun = sunDirection(t);
        if (lit * alpha > 0.01F) {
            SkyDraw.billboard(modelView, SUN, sun, 100F, sunSize(body), lit * alpha);
        }
        Vector3f center = new Vector3f(dir).mul(100F);
        float radius = 100F * Mth.sin(angularDeg * Mth.DEG_TO_RAD);
        if (Location.EARTH.equals(body)) {
            SkyDraw.billboard(modelView, MOON_DISC, new Vector3f(0.7F, 0.45F, -0.55F), 100F, 2.4F, alpha);
            if (!BodyLook.drawBelow(modelView, Location.EARTH, center, radius, orbit, sun, alpha, true, n)) {
                SkyDraw.planet(modelView, EARTH, center, radius, orbit, sun, 0.05F, alpha, false);
                SkyDraw.planet(modelView, EARTH_CLOUDS, center, radius * 1.006F, orbit * 1.15F, sun, 0.03F, 0.6F * alpha, false);
                // thin blue atmosphere halo around the limb (drawn last, additive)
                SkyDraw.planet(modelView, EARTH_LIMB, center, radius * 1.035F, 0, sun, 0.25F, alpha, true, true);
            }
        } else {
            // the planet a moon belongs to hangs in the sky (the Earth seen from the Moon)
            Locations.parentPlanet(body).ifPresent(parent -> SkyDraw.billboard(modelView, disc(parent),
                    new Vector3f(-0.6F, 0.35F, 0.7F), 100F, Location.EARTH.equals(parent) ? 3.2F : 6F, alpha));
            boolean air = Locations.body(body).filter(b -> b.atmosphere().pressureKpa() >= 0.1).isPresent();
            if (!BodyLook.drawBelow(modelView, body, center, radius, orbit * 0.5F, sun, alpha, air, n)) {
                SkyDraw.planet(modelView, Location.MOON.equals(body) ? MOON : surface(body), center, radius, orbit * 0.5F, sun, 0.04F, alpha, false);
                Locations.body(body).filter(b -> b.atmosphere().pressureKpa() >= 0.1).ifPresent(b ->
                        SkyDraw.planet(modelView, limb(body), center, radius * (b.atmosphere().pressureKpa() > 1000 ? 1.05F : 1.03F), 0, sun, 0.2F, alpha, true, true));
            }
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /** Angular radius of the body below as seen from orbit (degrees); the ascent sky grows towards it. */
    static final float ORBIT_ANGULAR_RADIUS = 68F;

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection,
                             boolean isFoggy, Runnable setupFog) {
        ResourceLocation body = orbitedBody(level);
        float t = level.getGameTime() + partialTick;
        float orbit = t / ORBIT_TICKS;
        // the sun swings around once per orbit: overhead at orbital noon, then down behind the planet (night)
        Vector3f sun = sunDirection(t);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        SkyDraw.blackout(modelView, 0xFF000000);
        SkyDraw.stars(modelView, projection, 1F);
        // the sun fades through the penumbra as it slips behind the planet's edge; the body below fills the lower view
        float lit = sunlit(level, partialTick);
        var rocket = RocketHud.ridden();
        Location dest = rocket != null && rocket.state() == com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState.TRANSFER
                ? rocket.destination() : null;
        if (dest != null && !dest.body().equals(body)) {
            // the coast between worlds, sped up: the world left behind shrinks away below, the next one grows to fill
            // the view as the orbit sky there will show it (so the hand-over matches)
            float p = Mth.clamp((rocket.timer() + partialTick) / com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity.TRANSFER_TICKS, 0, 1);
            // both balls at a fixed resolution while their size runs; the close-up patch again for the last stretch,
            // so the hand-over meets the destination's own orbit sky exactly
            int fixed = 64;
            if (p < 0.5F) {
                float e = smooth(p / 0.45F);
                drawBodyBelow(modelView, body, Mth.lerp(e, ORBIT_ANGULAR_RADIUS, 0.8F), t, lit, 1F - smooth((p - 0.4F) / 0.1F),
                        p < 0.04F ? com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH : fixed);
            }
            if (p > 0.3F) {
                float e = smooth((p - 0.3F) / 0.7F);
                float size = (float) Math.toDegrees(Math.asin(Mth.lerp(e * e * e, Mth.sin(0.8F * Mth.DEG_TO_RAD), Mth.sin(ORBIT_ANGULAR_RADIUS * Mth.DEG_TO_RAD))));
                float destLit = (float) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(
                        com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(level.registryAccess()), dest.body(), t);
                // it comes up ahead, a little below the line of sight, and swings round under the ship as it grows
                var look = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
                float hl = Math.max(1e-3F, (float) Math.sqrt(look.x * look.x + look.z * look.z));
                Vector3f ahead = new Vector3f(look.x / hl * 0.9F, -0.44F, look.z / hl * 0.9F).normalize();
                float swing = smooth((p - 0.55F) / 0.4F);
                Vector3f dir = ahead.lerp(new Vector3f(0, -1, 0), swing).normalize();
                drawBodyBelow(modelView, dest.body(), size, t, destLit, smooth((p - 0.3F) / 0.08F),
                        p > 0.96F ? com.pizzaparker07.spacebreaker.client.sky.BlockyBodyRenderer.APPROACH : fixed, dir);
            }
        } else {
            drawBodyBelow(modelView, body, ORBIT_ANGULAR_RADIUS, t, lit, 1F);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        return true;
    }
}
