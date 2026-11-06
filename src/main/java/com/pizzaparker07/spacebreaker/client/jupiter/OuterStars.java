package com.pizzaparker07.spacebreaker.client.jupiter;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The star field of the outer-system skies: a seeded set of small quads at radius 100, most of them faint, with a denser
 * band along a tilted great circle (the Milky Way). Built once into a static vertex buffer.
 */
final class OuterStars {
    private static VertexBuffer buffer;

    private OuterStars() {
    }

    static VertexBuffer buffer() {
        if (buffer == null) {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(build(0x0E7E5L, 3600));
            VertexBuffer.unbind();
        }
        return buffer;
    }

    private static com.mojang.blaze3d.vertex.MeshData build(long seed, int count) {
        RandomSource rnd = RandomSource.create(seed);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f band = new Vector3f(0.35F, 0.88F, 0.32F).normalize();
        for (int i = 0; i < count; i++) {
            Vector3f dir = new Vector3f((float) rnd.nextGaussian(), (float) rnd.nextGaussian(), (float) rnd.nextGaussian()).normalize();
            if (i % 3 == 0) {
                // pull a third of the stars toward the galactic plane
                dir.sub(new Vector3f(band).mul(dir.dot(band) * 0.85F)).normalize();
            }
            float u = rnd.nextFloat();
            float bright = 0.15F + 0.85F * u * u * u * u;
            Vector3f center = new Vector3f(dir).mul(100F);
            float size = 0.12F + bright * 0.2F;
            float warm = rnd.nextFloat() < 0.25F ? 0.85F : 1F;
            Quaternionf q = new Quaternionf().rotateTo(new Vector3f(0, 0, -1), dir).rotateZ(rnd.nextFloat() * Mth.TWO_PI);
            for (Vector3f c : new Vector3f[]{new Vector3f(size, -size, 0), new Vector3f(size, size, 0), new Vector3f(-size, size, 0),
                    new Vector3f(-size, -size, 0)}) {
                Vector3f p = c.rotate(q).add(center);
                b.addVertex(p.x, p.y, p.z).setColor(bright, bright * (0.5F + 0.5F * warm), bright * warm, 1F);
            }
        }
        return b.buildOrThrow();
    }
}
