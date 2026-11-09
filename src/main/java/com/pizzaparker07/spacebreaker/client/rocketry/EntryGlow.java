package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Atmospheric entry seen from outside: the shock in front of the heat shield glowing orange and pink, a wake of hot
 * gas streaming back past the capsule, and sparks shed into it. Additive light, no texture; strength follows
 * {@link RocketHud#reentryHeat}. The pose is at the capsule's base, the shield facing down the way it falls.
 */
final class EntryGlow {
    private static final int RINGS = 6, SEGMENTS = 20;

    private EntryGlow() {
    }

    static void render(PoseStack pose, MultiBufferSource buffers, float heat, float time, float capsuleWidth, float top) {
        if (heat <= 0.01F) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose last = pose.last();
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.3F) * Mth.sin(time * 0.9F + 2F);
        float r = capsuleWidth * (0.62F + 0.18F * heat);
        float depth = capsuleWidth * (0.35F + 0.25F * heat);
        // the shock cap: a shallow bowl in front of the shield, white-pink in the middle, orange at its rim
        for (int ring = 0; ring < RINGS; ring++) {
            float f0 = ring / (float) RINGS, f1 = (ring + 1) / (float) RINGS;
            for (int seg = 0; seg < SEGMENTS; seg++) {
                float a0 = seg * Mth.TWO_PI / SEGMENTS, a1 = (seg + 1) * Mth.TWO_PI / SEGMENTS;
                float wobble = 1F + 0.05F * Mth.sin(time * 1.3F + seg * 1.9F);
                quad(last, vc,
                        cap(a0, f0, r, depth, wobble), cap(a1, f0, r, depth, wobble), cap(a1, f1, r, depth, wobble), cap(a0, f1, r, depth, wobble),
                        colour(f0, heat * flicker), colour(f1, heat * flicker));
            }
        }
        // the wake: hot gas peeling off the rim and streaming back past the capsule, fading as it goes
        float length = top + capsuleWidth * (1.5F + 4F * heat);
        RandomSource rng = RandomSource.create(7);
        for (int i = 0; i < 14; i++) {
            float a = i * Mth.TWO_PI / 14 + rng.nextFloat() * 0.2F;
            float len = length * (0.6F + 0.4F * rng.nextFloat()) * (0.85F + 0.15F * Mth.sin(time * 0.8F + i));
            float w = capsuleWidth * 0.09F;
            float x0 = Mth.cos(a) * r * 0.95F, z0 = Mth.sin(a) * r * 0.95F;
            float x1 = Mth.cos(a) * r * 0.55F, z1 = Mth.sin(a) * r * 0.55F;
            float tx = -Mth.sin(a) * w, tz = Mth.cos(a) * w;
            int base = (int) (heat * flicker * 150), tip = 0;
            vertex(last, vc, x0 - tx, 0.05F, z0 - tz, 255, 140, 70, base);
            vertex(last, vc, x0 + tx, 0.05F, z0 + tz, 255, 140, 70, base);
            vertex(last, vc, x1 + tx * 0.3F, len, z1 + tz * 0.3F, 255, 120, 60, tip);
            vertex(last, vc, x1 - tx * 0.3F, len, z1 - tz * 0.3F, 255, 120, 60, tip);
        }
    }

    /** Sparks shed into the wake (once per client tick, from the rim of the shield). */
    static void sparks(RocketEntity rocket, float heat, float capsuleWidth) {
        if (heat < 0.15F || rocket.level() == null) {
            return;
        }
        RandomSource rng = rocket.getRandom();
        int n = (int) (heat * 5);
        for (int i = 0; i < n; i++) {
            float a = rng.nextFloat() * Mth.TWO_PI;
            double rr = capsuleWidth * 0.55 * (0.7 + 0.3 * rng.nextDouble());
            rocket.level().addParticle(rng.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    rocket.getX() + Mth.cos(a) * rr, rocket.getY() + 0.1, rocket.getZ() + Mth.sin(a) * rr,
                    Mth.cos(a) * 0.04, 0.02, Mth.sin(a) * 0.04);
        }
    }

    private static float[] cap(float a, float f, float r, float depth, float wobble) {
        float rr = r * f * wobble;
        return new float[]{Mth.cos(a) * rr, -depth * (1F - f * f), Mth.sin(a) * rr};
    }

    /** Colour and strength at a share {@code f} from the middle of the cap to its rim. */
    private static int[] colour(float f, float k) {
        int r = 255, g = (int) Mth.lerp(f, 205, 110), b = (int) Mth.lerp(f, 190, 50);
        int a = (int) (k * 255 * (1F - f * f * 0.85F));
        return new int[]{r, g, b, a};
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, float[] p0, float[] p1, float[] p2, float[] p3, int[] c01, int[] c23) {
        vertex(pose, vc, p0[0], p0[1], p0[2], c01[0], c01[1], c01[2], c01[3]);
        vertex(pose, vc, p1[0], p1[1], p1[2], c01[0], c01[1], c01[2], c01[3]);
        vertex(pose, vc, p2[0], p2[1], p2[2], c23[0], c23[1], c23[2], c23[3]);
        vertex(pose, vc, p3[0], p3[1], p3[2], c23[0], c23[1], c23[2], c23[3]);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, int r, int g, int b, int a) {
        vc.addVertex(pose, x, y, z).setColor(r, g, b, a);
    }
}
