package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.content.worldgen.CometPoints;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Active comets in the night sky: a soft glowing coma, a broad curved yellowish dust tail and a straight blue ion tail
 * pointing away from the sun (additive quads in
 * the sky pass, no textures, so they show the same with shader packs). Positions come from {@link CometPoints}, cached
 * for a few seconds since comets crawl across the sky.
 */
public final class CometTrails {
    private static RegistryAccess cachedAccess;
    private static BodySystem system;
    private static String cachedObserver;
    private static long cachedTick = Long.MIN_VALUE;
    private static List<CometPoints.Sighting> sightings = List.of();

    private CometTrails() {
    }

    /** Draws the comets seen from {@code observer}; the pose is already in the sun frame. */
    public static void render(ClientLevel level, PoseStack sunFrame, String observer, float visibility) {
        refresh(level, observer);
        if (sightings.isEmpty() || visibility <= 0.01F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = sunFrame.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (CometPoints.Sighting s : sightings) {
            float a = s.alpha() * visibility;
            // dust tail: broad, fanning out, curved back along the orbit, yellow-white
            strip(bb, m, s.head(), s.dustMid(), s.dustTip(), s.headSize() * 0.9F, s.headSize() * 8F, a * 0.6F, 1F, 0.9F, 0.68F, 14);
            // ion tail: narrow, straight, bluish, exactly away from the sun
            strip(bb, m, s.head(), null, s.tip(), s.headSize() * 0.3F, s.headSize() * 0.7F, a * 0.5F, 0.5F, 0.72F, 1F, 10);
            coma(bb, m, s.head(), s.headSize(), a);
        }
        var mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    /** The coma: a round glow, bright in the core and fading smoothly to nothing (rings of quads, 20 sectors). */
    private static void coma(BufferBuilder bb, Matrix4f m, Vector3f headDir, float size, float a) {
        Vector3f c = new Vector3f(headDir).mul(100F);
        Quaternionf q = new Quaternionf().rotateTo(new Vector3f(0, 0, -1), headDir);
        // radius (in coma sizes) and brightness at each ring: a hot core, then a long soft falloff
        float[][] rings = {{0F, 1.0F}, {0.35F, 0.85F}, {0.9F, 0.35F}, {2.0F, 0.12F}, {3.6F, 0.04F}, {5.5F, 0F}};
        int sectors = 20;
        for (int i = 0; i + 1 < rings.length; i++) {
            float r0 = rings[i][0] * size;
            float r1 = rings[i + 1][0] * size;
            float k0 = rings[i][1] * a;
            float k1 = rings[i + 1][1] * a;
            for (int j = 0; j < sectors; j++) {
                double t0 = j * Math.PI * 2 / sectors;
                double t1 = (j + 1) * Math.PI * 2 / sectors;
                Vector3f p00 = ring(c, q, r0, t0);
                Vector3f p01 = ring(c, q, r0, t1);
                Vector3f p11 = ring(c, q, r1, t1);
                Vector3f p10 = ring(c, q, r1, t0);
                bb.addVertex(m, p00.x, p00.y, p00.z).setColor(0.88F * k0, 0.96F * k0, k0, 1F);
                bb.addVertex(m, p01.x, p01.y, p01.z).setColor(0.88F * k0, 0.96F * k0, k0, 1F);
                bb.addVertex(m, p11.x, p11.y, p11.z).setColor(0.8F * k1, 0.92F * k1, k1, 1F);
                bb.addVertex(m, p10.x, p10.y, p10.z).setColor(0.8F * k1, 0.92F * k1, k1, 1F);
            }
        }
    }

    private static Vector3f ring(Vector3f centre, Quaternionf q, float r, double angle) {
        return new Vector3f((float) (Math.cos(angle) * r), (float) (Math.sin(angle) * r), 0).rotate(q).add(centre);
    }

    /**
     * A tail strip from the head along a quadratic curve (or a straight line when {@code mid} is null) to the tip,
     * widening from {@code w0} to {@code w1} and fading to nothing.
     */
    private static void strip(BufferBuilder bb, Matrix4f m, Vector3f headDir, Vector3f midDir, Vector3f tipDir, float w0, float w1,
                              float a, float red, float green, float blue, int steps) {
        Vector3f head = new Vector3f(headDir).mul(100F);
        Vector3f tip = new Vector3f(tipDir).mul(100F);
        if (head.distance(tip) < 0.5F) {
            return;
        }
        Vector3f mid = midDir == null ? new Vector3f(head).add(tip).mul(0.5F) : new Vector3f(midDir).mul(100F);
        Vector3f prevL = null;
        Vector3f prevR = null;
        float prevK = 0;
        Vector3f prevP = head;
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps;
            float u = 1 - f;
            Vector3f p = new Vector3f(head).mul(u * u).add(new Vector3f(mid).mul(2 * u * f)).add(new Vector3f(tip).mul(f * f)).normalize().mul(100F);
            Vector3f along = i == 0 ? new Vector3f(mid).sub(head) : new Vector3f(p).sub(prevP);
            Vector3f side = new Vector3f(along).cross(p).normalize().mul(w0 + (w1 - w0) * f);
            Vector3f l = new Vector3f(p).add(side);
            Vector3f r = new Vector3f(p).sub(side);
            float k = a * u * (float) Math.sqrt(u);
            if (prevL != null) {
                bb.addVertex(m, prevL.x, prevL.y, prevL.z).setColor(red * prevK, green * prevK, blue * prevK, 1F);
                bb.addVertex(m, prevR.x, prevR.y, prevR.z).setColor(red * prevK, green * prevK, blue * prevK, 1F);
                bb.addVertex(m, r.x, r.y, r.z).setColor(red * k, green * k, blue * k, 1F);
                bb.addVertex(m, l.x, l.y, l.z).setColor(red * k, green * k, blue * k, 1F);
            }
            prevL = l;
            prevR = r;
            prevK = k;
            prevP = p;
        }
    }

    private static void refresh(ClientLevel level, String observer) {
        long tick = level.getGameTime() / 100;
        if (level.registryAccess() != cachedAccess) {
            cachedAccess = level.registryAccess();
            system = BodyAdapter.system(cachedAccess).orElse(null);
            cachedTick = Long.MIN_VALUE;
        }
        if (system == null || (tick == cachedTick && observer.equals(cachedObserver))) {
            return;
        }
        cachedTick = tick;
        cachedObserver = observer;
        sightings = CometPoints.compute(system, observer, BodyAdapter.simTime(level));
    }

    /** The comets currently drawn (for notices). */
    public static List<CometPoints.Sighting> current() {
        return sightings;
    }
}
