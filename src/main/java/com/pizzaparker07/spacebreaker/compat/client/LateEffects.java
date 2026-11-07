package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Glows that write no depth (emissive translucent render types such as {@code entityTranslucentEmissive} and
 * {@code eyes}: rocket plumes, beams) in front of a space sky under a shader pack. The sky pass copies our sky onto
 * every pixel whose depth is still far after the pack's frame, which includes those glows, so they vanished. While the
 * sky pass is active such effects are drawn later, right after our sky and haze, with the scene's depth (so the rocket
 * still hides the part of the plume behind it). Without a pack, or in worlds without our sky, they are drawn at once.
 *
 * <p>Use: {@code LateEffects.drawOrDefer(pose, buffers, (p, b) -> ...draw with p and b...)}. The callback must not keep
 * the arguments; it may run once now or once at the end of the frame.
 */
public final class LateEffects {
    private record Entry(Matrix4f pose, Matrix3f normal, BiConsumer<PoseStack, MultiBufferSource> draw) {
    }

    private static final List<Entry> QUEUE = new ArrayList<>();
    private static boolean warned;

    private LateEffects() {
    }

    /** Draws now, or defers to the end of the frame while a shader pack's sky is replaced (skipped in its shadow pass). */
    public static void drawOrDefer(PoseStack pose, MultiBufferSource buffers, BiConsumer<PoseStack, MultiBufferSource> draw) {
        if (!ShaderSkyPass.active()) {
            draw.accept(pose, buffers);
            return;
        }
        if (ShaderState.renderingShadowPass()) {
            return; // glows cast no shadow
        }
        PoseStack.Pose last = pose.last();
        QUEUE.add(new Entry(new Matrix4f(last.pose()), new Matrix3f(last.normal()), draw));
    }

    /** Called at AFTER_LEVEL after the sky and haze passes. */
    static void replay(Matrix4f modelView, Matrix4f projection) {
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(modelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(projection), VertexSorting.DISTANCE_TO_ORIGIN);
        mc.getMainRenderTarget().bindWrite(false);
        try {
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            for (Entry e : QUEUE) {
                PoseStack ps = new PoseStack();
                ps.last().pose().set(e.pose());
                ps.last().normal().set(e.normal());
                e.draw().accept(ps, buffers);
            }
            buffers.endBatch();
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                Spacebreaker.LOGGER.error("Deferred glow effects over the shader pack failed", ex);
            }
        } finally {
            QUEUE.clear();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Drops anything queued (a frame without the replay, world unload). */
    static void clear() {
        QUEUE.clear();
    }
}
