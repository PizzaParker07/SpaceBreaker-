package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * The capsule's parachutes, drawn from quads: a small drogue first, then the main canopy that comes out reefed and
 * fills over a second and a half, hung from the capsule on suspension lines, swinging gently like a pendulum; after
 * touchdown the canopy sags, slides off to one side and lies flat on the ground. Sizes follow a real capsule's
 * (the main canopy is about four times as wide as the capsule).
 *
 * <p>Times are ticks since the parachute phase began ({@code t}) or since touchdown ({@code landed}).
 */
public final class ParachuteRenderer {
    /** Drogue out until this tick, then the main canopy comes out reefed. */
    public static final float DROGUE_END = 30F;
    /** The main canopy is full from here. */
    public static final float MAIN_FULL = 66F;
    /** After touchdown the canopy has collapsed by this many ticks. */
    public static final float COLLAPSE = 50F;

    private static final int GORES = 16, RINGS = 6;
    private static final int ORANGE = 0xE8692A, WHITE = 0xF0EEE8, LINE = 0xC8C4BC;

    private ParachuteRenderer() {
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** Pendulum swing of capsule and canopy about the canopy, degrees about X and Z. */
    public static float[] swing(float t, float time) {
        float settle = Mth.clamp((t - MAIN_FULL + 20) / 30F, 0F, 1F);
        float amp = 7F * settle * (0.6F + 0.4F * Mth.sin(time * 0.013F));
        return new float[]{amp * Mth.sin(time * 0.071F), amp * 0.6F * Mth.sin(time * 0.053F + 1.3F)};
    }

    /** Height of the canopy's skirt above the capsule top, blocks (the suspension lines' length). */
    public static float lineLength(float capsuleWidth) {
        return 2.6F * Math.max(1.2F, capsuleWidth);
    }

    /**
     * Draws the chutes in flight. The pose is at the capsule's top centre. {@code capsuleWidth} in blocks.
     */
    public static void flying(PoseStack pose, MultiBufferSource buffers, float t, float time, float capsuleWidth, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite sprite = sprite();
        float attach = capsuleWidth * 0.3F;
        if (t < DROGUE_END + 8) {
            // drogue: out fast, small, high on a long riser; cut away when the main comes out
            float open = ease(t / 10F);
            float r = 0.55F * capsuleWidth * open;
            float h = 2.2F + 2.8F * open;
            float fade = t > DROGUE_END ? 1F - (t - DROGUE_END) / 8F : 1F;
            if (fade > 0) {
                float drift = t > DROGUE_END ? (t - DROGUE_END) * 0.35F : 0F;
                canopy(pose, vc, sprite, 0, h + drift, drift * 0.4F, r, r * 0.7F, time, light, fade);
                lines(pose, vc, sprite, attach * 0.4F, h + drift, drift * 0.4F, r, light, 4);
            }
        }
        if (t >= DROGUE_END) {
            // main: a slim reefed bundle first, then it fills
            float pull = ease((t - DROGUE_END) / 8F);
            float fill = ease((t - DROGUE_END - 8) / (MAIN_FULL - DROGUE_END - 8));
            float full = 2.1F * capsuleWidth;
            float breathe = 1F + 0.025F * Mth.sin(time * 0.31F) * fill;
            float r = Mth.lerp(fill, 0.22F * full, full) * breathe;
            float depth = Mth.lerp(fill, 1.1F * full, 0.55F * full);
            float h = lineLength(capsuleWidth) * pull;
            canopy(pose, vc, sprite, 0, h, 0, r, depth, time, light, 1F);
            lines(pose, vc, sprite, attach, h, 0, r * 0.98F, light, 12);
        }
    }

    /**
     * Draws the main canopy after touchdown: it sags and slides off to one side, then lies flat. The pose is at the
     * capsule's base centre; {@code top} is the capsule's height in blocks.
     */
    public static void landed(PoseStack pose, MultiBufferSource buffers, float landed, float time, float top, float capsuleWidth, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite sprite = sprite();
        float k = ease(landed / COLLAPSE);
        float full = 2.1F * capsuleWidth;
        float lineLen = lineLength(capsuleWidth);
        // the canopy's centre travels from above the capsule to the ground beside it, tipping onto its side
        float side = Mth.lerp(k, 0F, lineLen * 0.9F + full * 0.6F);
        float height = Mth.lerp(k, top + lineLen, 0.15F);
        float r = full * Mth.lerp(k, 1F, 1.15F);
        float depth = full * Mth.lerp(k, 0.55F, 0.06F);
        pose.pushPose();
        pose.translate(side, height, 0);
        // it leans downwind as it goes, then settles flat on the ground
        float lean = k < 0.6F ? Mth.lerp(k / 0.6F, 0F, 35F) : Mth.lerp((k - 0.6F) / 0.4F, 35F, 4F);
        pose.mulPose(Axis.ZP.rotationDegrees(-lean));
        canopy(pose, vc, sprite, 0, 0, 0, r, depth, time * (1F - k), light, 1F);
        pose.popPose();
        // slack lines from the capsule to the skirt's near edge
        float skirtX = side - r * 0.2F * k, skirtY = Mth.lerp(k, top + lineLen, 0.1F);
        for (int i = 0; i < 6; i++) {
            float a = (float) (i * Math.PI / 3);
            float sag = 0.6F * k;
            line(pose, vc, sprite, Mth.cos(a) * capsuleWidth * 0.3F, top, Mth.sin(a) * capsuleWidth * 0.3F,
                    (skirtX + Mth.cos(a) * r * 0.25F * (1 - k)) * 0.5F, (top + skirtY) * 0.5F - sag, Mth.sin(a) * r * 0.3F * 0.5F, light);
            line(pose, vc, sprite, (skirtX + Mth.cos(a) * r * 0.25F * (1 - k)) * 0.5F, (top + skirtY) * 0.5F - sag, Mth.sin(a) * r * 0.3F * 0.5F,
                    skirtX, skirtY, Mth.sin(a) * r * 0.3F, light);
        }
    }

    private static TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState());
    }

    /** A dome of gores, apex up, skirt at height {@code y}; alternate gores orange and white. */
    private static void canopy(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, float cx, float y, float cz, float r, float depth,
                               float time, int light, float alpha) {
        if (r <= 0.01F || alpha <= 0) {
            return;
        }
        PoseStack.Pose last = pose.last();
        for (int g = 0; g < GORES; g++) {
            int colour = g % 2 == 0 ? ORANGE : WHITE;
            float a0 = (float) (g * Math.PI * 2 / GORES), a1 = (float) ((g + 1) * Math.PI * 2 / GORES);
            for (int ring = 0; ring < RINGS; ring++) {
                // ring 0 is the skirt, RINGS the apex; the skirt flutters a little
                float p0 = ring / (float) RINGS, p1 = (ring + 1) / (float) RINGS;
                float flutter = ring == 0 ? 0.04F * Mth.sin(time * 0.5F + g * 1.7F) * r : 0F;
                float[] v00 = dome(a0, p0, r, depth, flutter), v01 = dome(a1, p0, r, depth, flutter);
                float[] v10 = dome(a0, p1, r, depth, 0), v11 = dome(a1, p1, r, depth, 0);
                int shade = shade(colour, 0.78F + 0.22F * p0);
                // wound so the normal faces outward (lit from outside, as the sun lights a canopy)
                quad(last, vc, s, shade, light,
                        cx + v00[0], y + v00[1], cz + v00[2], cx + v10[0], y + v10[1], cz + v10[2],
                        cx + v11[0], y + v11[1], cz + v11[2], cx + v01[0], y + v01[1], cz + v01[2]);
            }
        }
    }

    /** A point of the dome: angle round, p from skirt (0) to apex (1). */
    private static float[] dome(float a, float p, float r, float depth, float flutter) {
        float rr = (r + flutter) * Mth.cos(p * Mth.HALF_PI) ;
        float yy = depth * Mth.sin(p * Mth.HALF_PI);
        // gores bulge between the lines a little
        return new float[]{Mth.cos(a) * rr, yy, Mth.sin(a) * rr};
    }

    /** Suspension lines from a ring on the capsule top to the skirt. */
    private static void lines(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, float attach, float y, float cz, float r, int light, int n) {
        for (int i = 0; i < n; i++) {
            float a = (float) (i * Math.PI * 2 / n);
            line(pose, vc, s, Mth.cos(a) * attach, 0, Mth.sin(a) * attach, Mth.cos(a) * r, y, cz + Mth.sin(a) * r, light);
        }
    }

    /** A thin line drawn as two crossed strips. */
    private static void line(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        PoseStack.Pose last = pose.last();
        float w = 0.025F;
        quad(last, vc, s, LINE, light, x0 - w, y0, z0, x0 + w, y0, z0, x1 + w, y1, z1, x1 - w, y1, z1);
        quad(last, vc, s, LINE, light, x0, y0, z0 - w, x0, y0, z0 + w, x1, y1, z1 + w, x1, y1, z1 - w);
    }

    private static int shade(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return (r << 16) | (g << 8) | b;
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite s, int rgb, int light,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        Vector3f n = new Vector3f(x1 - x0, y1 - y0, z1 - z0).cross(new Vector3f(x3 - x0, y3 - y0, z3 - z0));
        if (n.lengthSquared() < 1e-12F) {
            n.set(0, 1, 0);
        } else {
            n.normalize();
        }
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        vertex(pose, vc, x0, y0, z0, r, g, b, u0, v0, light, n);
        vertex(pose, vc, x1, y1, z1, r, g, b, u1, v0, light, n);
        vertex(pose, vc, x2, y2, z2, r, g, b, u1, v1, light, n);
        vertex(pose, vc, x3, y3, z3, r, g, b, u0, v1, light, n);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, int r, int g, int b, float u, float v, int light, Vector3f n) {
        vc.addVertex(pose, x, y, z).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, n.x, n.y, n.z);
    }
}
