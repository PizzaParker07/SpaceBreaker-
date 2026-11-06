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
    public static final float ORBIT_TICKS = 12000F;

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

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection,
                             boolean isFoggy, Runnable setupFog) {
        ResourceLocation body = orbitedBody(level);
        float t = level.getGameTime() + partialTick;
        float orbit = t / ORBIT_TICKS;
        // the sun swings around once per orbit; the planet turns underneath us
        float sunAngle = orbit * Mth.TWO_PI;
        Vector3f sun = new Vector3f(Mth.cos(sunAngle), 0.9F, Mth.sin(sunAngle)).normalize();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        SkyDraw.blackout(modelView, 0xFF000000);
        SkyDraw.stars(modelView, projection, 1F);
        SkyDraw.billboard(modelView, SUN, sun, 100F, sunSize(body), 1F);
        // the body below, seen from a high orbit (angular radius ~50 degrees) so the whole curve reads
        Vector3f center = new Vector3f(0, -100F, 0);
        float radius = 76F;
        if (Location.EARTH.equals(body)) {
            SkyDraw.billboard(modelView, MOON_DISC, new Vector3f(0.7F, 0.45F, -0.55F), 100F, 2.4F, 1F);
            SkyDraw.planet(modelView, EARTH, center, radius, orbit, sun, 0.05F, 1F, false);
            SkyDraw.planet(modelView, EARTH_CLOUDS, center, radius * 1.006F, orbit * 1.15F, sun, 0.03F, 0.6F, false);
            // thin blue atmosphere halo around the limb (drawn last, additive)
            SkyDraw.planet(modelView, EARTH_LIMB, center, radius * 1.035F, 0, sun, 0.25F, 1F, true, true);
        } else {
            // the planet a moon belongs to hangs in the sky (the Earth seen from the Moon)
            Locations.parentPlanet(body).ifPresent(parent -> SkyDraw.billboard(modelView, disc(parent),
                    new Vector3f(-0.6F, 0.35F, 0.7F), 100F, Location.EARTH.equals(parent) ? 3.2F : 6F, 1F));
            SkyDraw.planet(modelView, Location.MOON.equals(body) ? MOON : surface(body), center, radius, orbit * 0.5F, sun, 0.04F, 1F, false);
            Locations.body(body).filter(b -> b.atmosphere().pressureKpa() >= 0.1).ifPresent(b ->
                    SkyDraw.planet(modelView, limb(body), center, radius * (b.atmosphere().pressureKpa() > 1000 ? 1.05F : 1.03F), 0, sun, 0.2F, 1F, true, true));
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        return true;
    }
}
