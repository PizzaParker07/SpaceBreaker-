package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetPoints;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Planets as bright star-like points at their real positions, from the astro library's sky ephemeris.
 * Each planet keeps its true angle from the sun along the ecliptic, so it rides the sun's daily arc in the
 * game sky: evening stars set after the sun, morning stars rise before it. Size and brightness follow the
 * apparent magnitude, colour the body's own tint; square pixel points like vanilla's stars.
 * <p>
 * Callers draw with a pose already turned into the sun's frame (sun at local +y, daily path in the y-z plane).
 */
public final class PlanetDots {


    private static RegistryAccess cachedAccess;
    private static BodySystem system;
    private static String cachedObserver;
    private static long cachedTick = Long.MIN_VALUE;
    private static List<PlanetPoints.Dot> dots = List.of();

    private PlanetDots() {
    }

    /** Renders the planets visible from the level's body. {@code visibility} 0..1 fades them by daylight. */
    public static void render(ClientLevel level, PoseStack sunFrame, Matrix4f projection, float visibility) {
        if (visibility <= 0.01F) {
            return;
        }
        String observer = Bodies.forLevel(level).flatMap(b -> Bodies.registry(level.registryAccess())
                .map(r -> r.getKey(b))).map(Object::toString).orElse(null);
        if (observer == null || Bodies.isOrbit(level)) {
            return;
        }
        refresh(level, observer);
        if (dots.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = sunFrame.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (PlanetPoints.Dot d : dots) {
            Vector3f center = new Vector3f(d.local()).mul(100F);
            Quaternionf q = new Quaternionf().rotateTo(new Vector3f(0, 0, -1), d.local());
            float a = d.alpha() * visibility;
            float s = d.size();
            for (float[] c : new float[][]{{s, -s}, {s, s}, {-s, s}, {-s, -s}}) {
                Vector3f p = new Vector3f(c[0], c[1], 0).rotate(q).add(center);
                bb.addVertex(m, p.x, p.y, p.z).setColor(d.r() * a, d.g() * a, d.b() * a, 1F);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    /** Turns a pose stack into vanilla's sun frame for this level's time of day. */
    public static void vanillaSunFrame(PoseStack pose, ClientLevel level, float partialTick) {
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360F));
    }

    private static void refresh(ClientLevel level, String observer) {
        long tick = level.getGameTime() / 40;
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
        dots = PlanetPoints.compute(system, observer, BodyAdapter.simTime(level));
    }

}
