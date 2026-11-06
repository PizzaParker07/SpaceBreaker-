package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The airless night sky: several thousand stars with a realistic brightness distribution
 * (many faint, few bright), subtle colours by temperature and a Milky Way band of faint stars
 * along a tilted great circle. Built once into a static vertex buffer.
 */
final class StarField {
    private static VertexBuffer buffer;

    private StarField() {
    }

    static VertexBuffer buffer() {
        if (buffer == null) {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(build());
            VertexBuffer.unbind();
        }
        return buffer;
    }

    static void close() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    private static MeshData build() {
        RandomSource rnd = RandomSource.create(0x5BL);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // field stars
        for (int i = 0; i < 4200; i++) {
            Vector3f dir = randomDirection(rnd);
            star(b, rnd, dir, magnitude(rnd));
        }
        // Milky Way: faint stars concentrated near a great circle tilted 60° to the ecliptic
        Quaternionf plane = new Quaternionf().rotateX(Mth.DEG_TO_RAD * 62F).rotateZ(Mth.DEG_TO_RAD * 25F);
        for (int i = 0; i < 3800; i++) {
            float lon = rnd.nextFloat() * Mth.TWO_PI;
            float lat = (float) (rnd.nextGaussian() * 0.11 * (1 + 0.6 * Math.cos(lon * 2)));
            Vector3f dir = new Vector3f(Mth.cos(lat) * Mth.cos(lon), Mth.sin(lat), Mth.cos(lat) * Mth.sin(lon)).rotate(plane);
            star(b, rnd, dir, 0.08F + rnd.nextFloat() * 0.22F);
        }
        return b.buildOrThrow();
    }

    /** Brightness 0..1: power law, most stars are faint. */
    private static float magnitude(RandomSource rnd) {
        float u = rnd.nextFloat();
        return 0.12F + 0.88F * u * u * u * u;
    }

    private static Vector3f randomDirection(RandomSource rnd) {
        while (true) {
            float x = rnd.nextFloat() * 2 - 1;
            float y = rnd.nextFloat() * 2 - 1;
            float z = rnd.nextFloat() * 2 - 1;
            float l = x * x + y * y + z * z;
            if (l > 0.01F && l < 1F) {
                return new Vector3f(x, y, z).normalize();
            }
        }
    }

    private static void star(BufferBuilder b, RandomSource rnd, Vector3f dir, float bright) {
        Vector3f center = new Vector3f(dir).mul(100F);
        float size = 0.1F + bright * 0.22F;
        // colour by spectral class: blue-white, white, yellow, orange
        float t = rnd.nextFloat();
        float r;
        float g;
        float bl;
        if (t < 0.18F) {
            r = 0.78F; g = 0.86F; bl = 1.0F;
        } else if (t < 0.7F) {
            r = 1.0F; g = 1.0F; bl = 1.0F;
        } else if (t < 0.9F) {
            r = 1.0F; g = 0.95F; bl = 0.8F;
        } else {
            r = 1.0F; g = 0.82F; bl = 0.66F;
        }
        float a = Mth.clamp(bright, 0F, 1F);
        Quaternionf q = new Quaternionf().rotateTo(new Vector3f(0, 0, -1), dir).rotateZ(rnd.nextFloat() * Mth.TWO_PI);
        Vector3f[] corners = {new Vector3f(size, -size, 0), new Vector3f(size, size, 0), new Vector3f(-size, size, 0), new Vector3f(-size, -size, 0)};
        for (Vector3f c : corners) {
            Vector3f p = c.rotate(q).add(center);
            b.addVertex(p.x, p.y, p.z).setColor(r * a, g * a, bl * a, 1F);
        }
    }
}
