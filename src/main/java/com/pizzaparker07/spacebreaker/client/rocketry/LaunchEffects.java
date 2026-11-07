package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * What a launch leaves in the air on Earth, drawn with the rocket (world-aligned, not tilted): the glow of the flame on
 * the pad, and one smoke column hanging from the engines that grows longer and wider below the climbing rocket. The
 * column's texture is fixed to world height, so the smoke stays where it was left while the rocket pulls away.
 * The ground cloud is made of particles ({@link SteamParticle}).
 */
final class LaunchEffects {
    static final ResourceLocation SMOKE = Spacebreaker.id("textures/entity/rocket_smoke_column.png");
    static final ResourceLocation GLOW = Spacebreaker.id("textures/entity/rocket_ground_glow.png");
    /** Blocks of column per texture tile. */
    private static final float TILE = 24F;
    private static final int SEGMENTS = 10;

    private LaunchEffects() {
    }

    static void render(RocketEntity rocket, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, Vec3 camera) {
        if (!rocket.burning() || !Location.EARTH_SURFACE.equals(rocket.location()) || NuclearExhaust.reactorHot(rocket)) {
            return;
        }
        FlightState state = rocket.state();
        double x = Mth.lerp(partialTick, rocket.xo, rocket.getX());
        double y = Mth.lerp(partialTick, rocket.yo, rocket.getY());
        double z = Mth.lerp(partialTick, rocket.zo, rocket.getZ());
        double ground = rocket.level().getHeight(Heightmap.Types.MOTION_BLOCKING, rocket.getBlockX(), rocket.getBlockZ());
        float h = (float) Math.max(0, y - ground);
        float strength = state == FlightState.COUNTDOWN
                ? Mth.clamp((RocketEntity.IGNITION_TICKS - rocket.timer() + partialTick) / 30F, 0.15F, 1F) : 1F;
        // the flame lights up the pad
        if (h < 30) {
            float a = (1 - h / 30F) * strength;
            float size = 3.5F + h * 0.35F;
            float dy = (float) (ground - y) + 0.06F;
            com.pizzaparker07.spacebreaker.compat.client.LateEffects.drawOrDefer(pose, buffers, (p, b) -> {
                VertexConsumer vc = b.getBuffer(RenderType.eyes(GLOW));
                Matrix4f m = p.last().pose();
                int al = (int) (a * 255);
                vc.addVertex(m, -size, dy, -size).setColor(255, 255, 255, al).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p.last(), 0, 1, 0);
                vc.addVertex(m, -size, dy, size).setColor(255, 255, 255, al).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p.last(), 0, 1, 0);
                vc.addVertex(m, size, dy, size).setColor(255, 255, 255, al).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p.last(), 0, 1, 0);
                vc.addVertex(m, size, dy, -size).setColor(255, 255, 255, al).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p.last(), 0, 1, 0);
            });
        }
        if (state != FlightState.ASCENT) {
            return;
        }
        // the smoke column: grows with the climb, thins out as the air does (gone by ~y 700)
        float t = rocket.timer() + partialTick;
        float thin = 1 - Mth.clamp((float) (y - 320) / 380F, 0, 1);
        if (thin <= 0) {
            return;
        }
        float top = 3.2F; // starts where the flame ends
        float length = Math.min(Math.min(6 + t * 0.9F, 110F), h - top + 1);
        if (length <= 1) {
            return;
        }
        pose.pushPose();
        // turn to face the camera around the vertical axis
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(camera.x - x, camera.z - z)));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(SMOKE));
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = i / (float) SEGMENTS, f1 = (i + 1) / (float) SEGMENTS;
            float y0 = -top - f0 * length, y1 = -top - f1 * length;
            float w0 = width(f0 * length), w1 = width(f1 * length);
            int a0 = (int) (alpha(f0, length) * thin * 255), a1 = (int) (alpha(f1, length) * thin * 255);
            float v0 = (float) ((y + y0) / TILE), v1 = (float) ((y + y1) / TILE);
            int shade0 = shade(f0), shade1 = shade(f1);
            quadVertex(vc, m, pose, -w0, y0, 0, -v0, shade0, a0, light);
            quadVertex(vc, m, pose, w0, y0, 1, -v0, shade0, a0, light);
            quadVertex(vc, m, pose, w1, y1, 1, -v1, shade1, a1, light);
            quadVertex(vc, m, pose, -w1, y1, 0, -v1, shade1, a1, light);
        }
        pose.popPose();
    }

    /** Half width of the column at a distance below its top: a plume a block wide, spreading to ~8 blocks. */
    private static float width(float d) {
        return 0.6F + d * 0.035F;
    }

    private static float alpha(float f, float length) {
        float in = Mth.clamp(f * length / 2.5F, 0, 1);    // soft where it leaves the flame
        float out = 1 - Mth.clamp((f - 0.7F) / 0.3F, 0, 1); // and where it trails off
        return 0.92F * in * out;
    }

    /** Slightly warmer and brighter right under the flame. */
    private static int shade(float f) {
        return (int) Mth.lerp(Mth.clamp(f * 4, 0, 1), 255, 225);
    }

    private static void quadVertex(VertexConsumer vc, Matrix4f m, PoseStack pose, float x, float y, float u, float v, int shade, int a, int light) {
        vc.addVertex(m, x, y, 0).setColor(shade, shade, shade - 4, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose.last(), 0, 0, 1);
    }
}
