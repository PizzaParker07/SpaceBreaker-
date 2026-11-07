package com.pizzaparker07.spacebreaker.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pizzaparker07.spacebreaker.content.worldgen.ShowerClientState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Bright streaks of micrometeorites across an airless sky during a shower: short, fast, fading trails drawn in the
 * sky pass (additive quads, no fog), so they show with or without shader packs.
 */
public final class MeteorStreaks {
    private static final List<Streak> STREAKS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static long lastTick = Long.MIN_VALUE;

    private record Streak(Vector3f from, Vector3f dir, long born, int life, float length, float brightness) {
    }

    private MeteorStreaks() {
    }

    /** Spawns new streaks once per game tick while a shower falls, drops old ones. */
    private static void update(ClientLevel level) {
        long tick = level.getGameTime();
        if (tick == lastTick) {
            return;
        }
        lastTick = tick;
        STREAKS.removeIf(s -> tick - s.born() > s.life());
        if (!ShowerClientState.active(level.dimension())) {
            return;
        }
        int n = 1 + RANDOM.nextInt(4);
        for (int i = 0; i < n; i++) {
            // a start point high in the sky, moving roughly downward at a slant
            float az = RANDOM.nextFloat() * (float) Math.PI * 2;
            float el = 0.25F + RANDOM.nextFloat() * 1.1F;
            Vector3f from = new Vector3f((float) (Math.cos(el) * Math.cos(az)), (float) Math.sin(el), (float) (Math.cos(el) * Math.sin(az)));
            float daz = az + (RANDOM.nextFloat() - 0.5F) * 1.2F;
            Vector3f dir = new Vector3f((float) Math.cos(daz), -0.6F - RANDOM.nextFloat() * 0.8F, (float) Math.sin(daz)).normalize();
            STREAKS.add(new Streak(from, dir, tick, 10 + RANDOM.nextInt(12), 0.12F + RANDOM.nextFloat() * 0.25F, 0.7F + RANDOM.nextFloat() * 0.3F));
        }
    }

    /** Draws the streaks; {@code pose} is the camera-rotated sky pose (no time-of-day turn). */
    public static void render(ClientLevel level, PoseStack pose, float partialTick) {
        update(level);
        if (STREAKS.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = pose.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float now = level.getGameTime() + partialTick;
        for (Streak s : STREAKS) {
            float age = (now - s.born()) / s.life();
            if (age < 0 || age > 1) {
                continue;
            }
            float fade = (1 - age) * s.brightness();
            Vector3f head = new Vector3f(s.dir()).mul(age * s.length() * 2).add(s.from()).normalize().mul(100F);
            Vector3f tail = new Vector3f(s.dir()).mul(Math.max(0, age * s.length() * 2 - s.length())).add(s.from()).normalize().mul(100F);
            Vector3f side = new Vector3f(head).sub(tail).cross(head).normalize().mul(0.45F);
            // additive, colour already scaled by the fade: dark tail, bright head
            bb.addVertex(m, tail.x - side.x, tail.y - side.y, tail.z - side.z).setColor(0F, 0F, 0F, 1F);
            bb.addVertex(m, tail.x + side.x, tail.y + side.y, tail.z + side.z).setColor(0F, 0F, 0F, 1F);
            bb.addVertex(m, head.x + side.x, head.y + side.y, head.z + side.z).setColor(fade, 0.97F * fade, 0.88F * fade, 1F);
            bb.addVertex(m, head.x - side.x, head.y - side.y, head.z - side.z).setColor(fade, 0.97F * fade, 0.88F * fade, 1F);
        }
        var mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        STREAKS.clear();
    }
}
