package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
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
    static final ResourceLocation EARTH = Spacebreaker.id("textures/environment/earth.png");
    static final ResourceLocation EARTH_CLOUDS = Spacebreaker.id("textures/environment/orbit_earth_clouds.png");
    static final ResourceLocation EARTH_LIMB = Spacebreaker.id("textures/environment/earth_limb.png");
    static final ResourceLocation MOON = Spacebreaker.id("textures/environment/moon.png");
    static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    static final ResourceLocation EARTH_DISC = Spacebreaker.id("textures/environment/earth_disc.png");
    static final ResourceLocation MOON_DISC = Spacebreaker.id("textures/environment/moon_disc.png");

    /** One orbit (and one day/night cycle of the view) in ticks. */
    public static final float ORBIT_TICKS = 12000F;

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
        boolean moonOrbit = Location.of(level.dimension()).orElse(Location.EARTH_ORBIT) == Location.MOON_ORBIT;
        float t = level.getGameTime() + partialTick;
        float orbit = t / ORBIT_TICKS;
        // the sun swings around once per orbit; the planet turns underneath us
        float sunAngle = orbit * Mth.TWO_PI;
        Vector3f sun = new Vector3f(Mth.cos(sunAngle), 0.35F, Mth.sin(sunAngle)).normalize();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        SkyDraw.blackout(modelView, 0xFF000000);
        SkyDraw.stars(modelView, projection, 1F);
        SkyDraw.billboard(modelView, SUN, sun, 100F, 12F, 1F);
        // the body below: angular radius ~70 degrees from a low orbit
        Vector3f center = new Vector3f(0, -100F, 0);
        float radius = 94F;
        if (moonOrbit) {
            SkyDraw.billboard(modelView, EARTH_DISC, new Vector3f(-0.6F, 0.35F, 0.7F), 100F, 3.2F, 1F);
            SkyDraw.planet(modelView, MOON, center, radius, orbit * 0.5F, sun, 0.04F, 1F, false);
        } else {
            SkyDraw.billboard(modelView, MOON_DISC, new Vector3f(0.7F, 0.45F, -0.55F), 100F, 2.4F, 1F);
            SkyDraw.planet(modelView, EARTH_LIMB, center, radius * 1.025F, 0, sun, 0.0F, 0.85F, true);
            SkyDraw.planet(modelView, EARTH, center, radius, orbit, sun, 0.05F, 1F, false);
            SkyDraw.planet(modelView, EARTH_CLOUDS, center, radius * 1.006F, orbit * 1.15F, sun, 0.03F, 0.9F, false);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        return true;
    }
}
