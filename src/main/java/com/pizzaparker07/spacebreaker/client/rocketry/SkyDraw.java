package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Sky drawing helpers for space: star field, black-out, sun, and textured planets lit by the sun. */
final class SkyDraw {
    private static VertexBuffer stars;

    private SkyDraw() {
    }

    /** A fixed field of a few thousand stars of different sizes and brightness. */
    static void stars(Matrix4f modelView, Matrix4f projection, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        if (stars == null) {
            stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
            stars.bind();
            stars.upload(buildStars());
            VertexBuffer.unbind();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        RenderSystem.disableCull();
        stars.bind();
        stars.drawWithShader(modelView, projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static MeshData buildStars() {
        RandomSource rng = RandomSource.create(20261106L);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 3200; i++) {
            float x = rng.nextFloat() * 2 - 1, y = rng.nextFloat() * 2 - 1, z = rng.nextFloat() * 2 - 1;
            float d = x * x + y * y + z * z;
            if (d < 0.01F || d >= 1F) {
                continue;
            }
            float inv = Mth.invSqrt(d);
            Vector3f dir = new Vector3f(x * inv, y * inv, z * inv).mul(100F);
            float size = 0.17F + rng.nextFloat() * rng.nextFloat() * 0.38F;
            int shade = 150 + rng.nextInt(106);
            // a few warm and a few cool stars
            float tint = rng.nextFloat();
            int r = tint < 0.1F ? 255 : tint > 0.9F ? shade - 30 : shade;
            int g = tint < 0.1F ? shade - 20 : shade;
            int bl = tint < 0.1F ? shade - 60 : tint > 0.9F ? 255 : shade;
            quad(b, dir, size, r, g, bl, 255);
        }
        return b.buildOrThrow();
    }

    private static void quad(BufferBuilder b, Vector3f c, float size, int r, int g, int bl, int a) {
        Vector3f n = new Vector3f(c).normalize();
        Vector3f up = Math.abs(n.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(n).cross(up).normalize().mul(size);
        Vector3f v = new Vector3f(n).cross(u).normalize().mul(size);
        b.addVertex(c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).setColor(r, g, bl, a);
        b.addVertex(c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).setColor(r, g, bl, a);
        b.addVertex(c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).setColor(r, g, bl, a);
        b.addVertex(c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).setColor(r, g, bl, a);
    }

    /** Covers the whole view with a colour (used to blacken the sky with altitude). */
    static void blackout(Matrix4f modelView, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = 60F;
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, bl = argb & 255;
        float[][] faces = {
                {-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s},
                {-s, -s, s, -s, s, s, s, s, s, s, -s, s},
                {-s, -s, -s, -s, s, -s, -s, s, s, -s, -s, s},
                {s, -s, -s, s, -s, s, s, s, s, s, s, -s},
                {-s, s, -s, s, s, -s, s, s, s, -s, s, s},
                {-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s}};
        for (float[] f : faces) {
            for (int i = 0; i < 4; i++) {
                b.addVertex(modelView, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).setColor(r, g, bl, a);
            }
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    /**
     * Like {@link #blackout} but shaded from the zenith to the horizon: {@code over} straight up, {@code horizon} at
     * eye level and below.
     */
    static void gradient(Matrix4f modelView, int over, int horizon) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = 60F;
        float[][] ring = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
        for (int i = 0; i < 4; i++) {
            float[] p = ring[i], q = ring[(i + 1) % 4];
            // upper half of each wall: horizon colour at eye level up to the zenith colour
            vertex(b, modelView, p[0], 0, p[1], horizon);
            vertex(b, modelView, q[0], 0, q[1], horizon);
            vertex(b, modelView, q[0], s, q[1], over);
            vertex(b, modelView, p[0], s, p[1], over);
            // lower half: horizon colour all the way down
            vertex(b, modelView, p[0], -s, p[1], horizon);
            vertex(b, modelView, q[0], -s, q[1], horizon);
            vertex(b, modelView, q[0], 0, q[1], horizon);
            vertex(b, modelView, p[0], 0, p[1], horizon);
        }
        for (float[] p : ring) {
            vertex(b, modelView, p[0], s, p[1], over);
        }
        for (float[] p : ring) {
            vertex(b, modelView, p[0], -s, p[1], horizon);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int argb) {
        b.addVertex(m, x, y, z).setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    /** A textured billboard in the given direction (sun, distant bodies). */
    static void billboard(Matrix4f modelView, ResourceLocation texture, Vector3f dir, float distance, float size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        Vector3f c = new Vector3f(dir).normalize().mul(distance);
        Vector3f n = new Vector3f(dir).normalize();
        Vector3f up = Math.abs(n.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(n).cross(up).normalize().mul(size);
        Vector3f v = new Vector3f(n).cross(u).normalize().mul(size);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(modelView, c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).setUv(0, 0);
        b.addVertex(modelView, c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).setUv(0, 1);
        b.addVertex(modelView, c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).setUv(1, 1);
        b.addVertex(modelView, c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).setUv(1, 0);
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * A planet: lat/long sphere with an equirectangular texture, centred at {@code center} (camera space),
     * spun by {@code spin} (fraction of a turn), lit by {@code sun} with a soft terminator.
     *
     * @param night brightness of the dark side (0..1)
     */
    static void planet(Matrix4f modelView, ResourceLocation texture, Vector3f center, float radius, float spin, Vector3f sun,
                       float night, float alpha, boolean additive) {
        planet(modelView, texture, center, radius, spin, sun, night, alpha, additive, false);
    }

    /** @param rim fade the sphere towards its centre so only a thin glowing limb remains (atmosphere halo) */
    static void planet(Matrix4f modelView, ResourceLocation texture, Vector3f center, float radius, float spin, Vector3f sun,
                       float night, float alpha, boolean additive, boolean rim) {
        RenderSystem.enableBlend();
        if (additive) {
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        Vector3f s = new Vector3f(sun).normalize();
        int lon = 64, lat = 32;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int j = 0; j < lat; j++) {
            for (int i = 0; i < lon; i++) {
                // counter-clockwise seen from outside, so back-face culling hides the far side
                vert(b, modelView, center, radius, i, j, lon, lat, spin, s, night, alpha, rim);
                vert(b, modelView, center, radius, i + 1, j, lon, lat, spin, s, night, alpha, rim);
                vert(b, modelView, center, radius, i + 1, j + 1, lon, lat, spin, s, night, alpha, rim);
                vert(b, modelView, center, radius, i, j + 1, lon, lat, spin, s, night, alpha, rim);
            }
        }
        RenderSystem.enableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    private static void vert(BufferBuilder b, Matrix4f m, Vector3f c, float r, int i, int j, int lon, int lat, float spin, Vector3f sun,
                             float night, float alpha, boolean rim) {
        float theta = (float) Math.PI * j / lat;               // 0 = north pole
        float phi = (float) (Math.PI * 2) * i / lon;
        float sy = Mth.cos(theta);
        float sr = Mth.sin(theta);
        // body-local normal, then rotated 90 degrees about x so the poles lie on the horizon and we look down on the tropics
        float lx = sr * Mth.cos(phi), ly = sy, lz = sr * Mth.sin(phi);
        float nx = lx, nyw = -lz, nz = ly;
        float light = nx * sun.x + nyw * sun.y + nz * sun.z;
        float shade = night + (1 - night) * Mth.clamp(light * 1.6F + 0.15F, 0F, 1F);
        int col = (int) (Mth.clamp(shade, 0, 1) * 255);
        float u = (float) i / lon + spin;
        float v = (float) j / lat;
        float a = alpha;
        if (rim) {
            // camera at the origin: fresnel term from the angle between normal and line of sight
            float px = c.x + nx * r, py = c.y + nyw * r, pz = c.z + nz * r;
            float inv = Mth.invSqrt(px * px + py * py + pz * pz);
            float facing = Math.abs(nx * px * inv + nyw * py * inv + nz * pz * inv);
            float edge = 1 - facing;
            a *= edge * edge * edge * Mth.clamp(0.35F + light, 0.15F, 1F);
        }
        b.addVertex(m, c.x + nx * r, c.y + nyw * r, c.z + nz * r).setUv(u, v).setColor(col, col, col, (int) (Mth.clamp(a, 0, 1) * 255));
    }
}
