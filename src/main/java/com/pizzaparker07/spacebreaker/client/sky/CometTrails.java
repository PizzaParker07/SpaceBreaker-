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
 * Active comets in the night sky: a soft glowing head and a fading tail pointing away from the sun (additive quads in
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
            tail(bb, m, s, a);
            Vector3f c = new Vector3f(s.head()).mul(100F);
            Quaternionf q = new Quaternionf().rotateTo(new Vector3f(0, 0, -1), s.head());
            // glow: three nested squares, faint and wide to bright and small
            float[][] rings = {{3.2F, 0.10F}, {1.9F, 0.25F}, {1.0F, 0.7F}, {0.45F, 1.0F}};
            for (float[] ring : rings) {
                float r = s.headSize() * ring[0];
                float k = a * ring[1];
                for (float[] corner : new float[][]{{r, -r}, {r, r}, {-r, r}, {-r, -r}}) {
                    Vector3f p = new Vector3f(corner[0], corner[1], 0).rotate(q).add(c);
                    bb.addVertex(m, p.x, p.y, p.z).setColor(0.82F * k, 0.92F * k, 1.0F * k, 1F);
                }
            }
        }
        var mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    /** The tail: a strip from the head to the tip, widening and fading, a little warmer (dust) toward its end. */
    private static void tail(BufferBuilder bb, Matrix4f m, CometPoints.Sighting s, float a) {
        Vector3f head = new Vector3f(s.head()).mul(100F);
        Vector3f tip = new Vector3f(s.tip()).mul(100F);
        if (head.distance(tip) < 0.5F) {
            return;
        }
        int steps = 8;
        Vector3f prevL = null;
        Vector3f prevR = null;
        float prevK = 0;
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps;
            Vector3f p = new Vector3f(head).lerp(tip, f).normalize().mul(100F);
            Vector3f along = new Vector3f(tip).sub(head);
            Vector3f side = new Vector3f(along).cross(p).normalize().mul(s.headSize() * (0.8F + 3.5F * f));
            Vector3f l = new Vector3f(p).add(side);
            Vector3f r = new Vector3f(p).sub(side);
            float k = a * 0.7F * (1 - f) * (float) Math.sqrt(1 - f);
            if (prevL != null) {
                bb.addVertex(m, prevL.x, prevL.y, prevL.z).setColor(0.85F * prevK, 0.9F * prevK, 1F * prevK, 1F);
                bb.addVertex(m, prevR.x, prevR.y, prevR.z).setColor(0.85F * prevK, 0.9F * prevK, 1F * prevK, 1F);
                bb.addVertex(m, r.x, r.y, r.z).setColor(1F * k, 0.92F * k, 0.75F * k, 1F);
                bb.addVertex(m, l.x, l.y, l.z).setColor(1F * k, 0.92F * k, 0.75F * k, 1F);
            }
            prevL = l;
            prevR = r;
            prevK = k;
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
