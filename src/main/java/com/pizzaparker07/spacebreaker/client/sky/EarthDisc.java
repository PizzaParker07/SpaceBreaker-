package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

import java.io.InputStream;

/**
 * The parent planet as a small pixel-art globe: an equirectangular surface map (drawn by code) is projected
 * onto a disc every few frames, rotating with the planet's own day, with the clouds drifting on their own and
 * a sharp day/night terminator from the real sun direction, so phases are always right.
 */
final class EarthDisc {
    static final int SIZE = 64;
    static final ResourceLocation TEXTURE = Spacebreaker.id("dynamic/sky_planet");
    private static final ResourceLocation MAP = Spacebreaker.id("textures/environment/earth_map.png");
    private static final ResourceLocation CLOUDS = Spacebreaker.id("textures/environment/earth_clouds.png");

    private static DynamicTexture texture;
    private static int[] map;
    private static int[] clouds;
    private static int mapW;
    private static int mapH;
    private static int cloudW;
    private static int cloudH;
    private static double lastSx = 9;
    private static double lastSy;
    private static double lastSz;
    private static double lastSpin = -1;

    private EarthDisc() {
    }

    static void reset() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
        map = null;
        clouds = null;
        lastSx = 9;
    }

    /**
     * Updates the texture.
     *
     * @param sunInDisc sun direction in the disc frame: x right, y up, z toward the viewer
     * @param spin      planet rotation angle (radians)
     * @param cloudSpin cloud layer rotation angle (radians)
     */
    static ResourceLocation update(Vector3d sunInDisc, double spin, double cloudSpin) {
        if (texture == null) {
            if (!load()) {
                return null;
            }
            texture = new DynamicTexture(SIZE, SIZE, true);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        }
        double change = Math.abs(sunInDisc.x - lastSx) + Math.abs(sunInDisc.y - lastSy) + Math.abs(sunInDisc.z - lastSz)
                + Math.abs(spin - lastSpin);
        if (change < 0.01) {
            return TEXTURE;
        }
        lastSx = sunInDisc.x;
        lastSy = sunInDisc.y;
        lastSz = sunInDisc.z;
        lastSpin = spin;
        NativeImage img = texture.getPixels();
        double r = SIZE / 2.0 - 0.5;
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                double x = (px + 0.5 - SIZE / 2.0) / r;
                double y = -(py + 0.5 - SIZE / 2.0) / r;
                double rr = x * x + y * y;
                if (rr > 1) {
                    img.setPixelRGBA(px, py, 0);
                    continue;
                }
                double z = Math.sqrt(1 - rr);
                double lat = Math.asin(y);
                double lon = Math.atan2(x, z);
                int argb = sample(map, mapW, mapH, lon + spin, lat);
                int cloud = sample(clouds, cloudW, cloudH, lon + cloudSpin, lat);
                int cr = (argb >> 16) & 255;
                int cg = (argb >> 8) & 255;
                int cb = argb & 255;
                int ca = (cloud >>> 24);
                if (ca > 0) {
                    double k = ca / 255.0;
                    cr = (int) Mth.lerp(k, cr, (cloud >> 16) & 255);
                    cg = (int) Mth.lerp(k, cg, (cloud >> 8) & 255);
                    cb = (int) Mth.lerp(k, cb, cloud & 255);
                }
                double light = x * sunInDisc.x + y * sunInDisc.y + z * sunInDisc.z;
                double shade;
                if (light > 0.06) {
                    shade = 0.82 + 0.18 * Math.min(1, light * 2);
                } else if (light > 0) {
                    shade = 0.55; // one-pixel soft terminator
                } else {
                    shade = 0.07;
                }
                // limb: thin blue atmosphere on the lit side
                double limb = rr > 0.86 ? (rr - 0.86) / 0.14 : 0;
                int outR = (int) (cr * shade);
                int outG = (int) (cg * shade);
                int outB = (int) (cb * shade);
                if (limb > 0 && light > 0) {
                    outR = (int) Mth.lerp(limb * 0.5, outR, 120);
                    outG = (int) Mth.lerp(limb * 0.5, outG, 170);
                    outB = (int) Mth.lerp(limb * 0.5, outB, 255);
                } else if (light <= 0) {
                    outB = Math.max(outB, 9);
                    outG = Math.max(outG, 5);
                    outR = Math.max(outR, 4);
                }
                img.setPixelRGBA(px, py, 0xFF000000 | (Mth.clamp(outB, 0, 255) << 16) | (Mth.clamp(outG, 0, 255) << 8) | Mth.clamp(outR, 0, 255));
            }
        }
        texture.upload();
        return TEXTURE;
    }

    private static int sample(int[] px, int w, int h, double lon, double lat) {
        double u = Mth.positiveModulo(lon / Mth.TWO_PI + 0.5, 1.0);
        double v = 0.5 - lat / Math.PI;
        int ix = Mth.clamp((int) (u * w), 0, w - 1);
        int iy = Mth.clamp((int) (v * h), 0, h - 1);
        return px[iy * w + ix];
    }

    private static boolean load() {
        try {
            NativeImage m = read(MAP);
            NativeImage c = read(CLOUDS);
            mapW = m.getWidth();
            mapH = m.getHeight();
            map = toArgb(m);
            cloudW = c.getWidth();
            cloudH = c.getHeight();
            clouds = toArgb(c);
            m.close();
            c.close();
            return true;
        } catch (Exception e) {
            Spacebreaker.LOGGER.warn("Could not load the sky planet textures", e);
            return false;
        }
    }

    private static NativeImage read(ResourceLocation loc) throws java.io.IOException {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc)) {
            return NativeImage.read(in);
        }
    }

    private static int[] toArgb(NativeImage img) {
        int[] out = new int[img.getWidth() * img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int abgr = img.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 255;
                int b = (abgr >> 16) & 255;
                int g = (abgr >> 8) & 255;
                int r = abgr & 255;
                out[y * img.getWidth() + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return out;
    }
}
