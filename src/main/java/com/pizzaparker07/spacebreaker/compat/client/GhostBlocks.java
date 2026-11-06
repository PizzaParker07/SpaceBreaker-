package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Shader-safe "ghost" blocks for previews and holograms (structure projectors, placement previews).
 *
 * <p>Draws a block's normal baked model, tinted and semi-transparent, through {@link Sheets#translucentCullBlockSheet()}
 * (the render type vanilla uses for translucent blocks held as items). That type goes through the normal buffer
 * source, so Iris batches it with translucent entities and every shader pack shades it with its translucent entity
 * program: alpha, depth sorting against water and glass, and the pack's fog all work, unlike immediate-mode
 * position/colour drawing, which packs may discard or overwrite. Full-bright light so a projection reads as glowing.
 *
 * <p>Use from {@code RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS} with the event's pose stack (translated to
 * the camera) and {@code Minecraft.getInstance().renderBuffers().bufferSource()}; call {@link #flush} once after all
 * ghosts of the frame. Checked in vanilla, Sodium, and Iris with Complementary (docs/COMPAT.md).
 */
public final class GhostBlocks {
    private GhostBlocks() {
    }

    /** The render type used for ghosts (for callers that manage their own buffer source). */
    public static RenderType renderType() {
        return Sheets.translucentCullBlockSheet();
    }

    /**
     * Draws one ghost block at the pose stack's current origin (the block's minimum corner).
     *
     * @param alpha 0..1 opacity (0.35-0.5 reads well as a hologram)
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, BlockState state, float red, float green, float blue, float alpha) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var model = dispatcher.getBlockModel(state);
        RenderType type = renderType();
        VertexConsumer consumer = new Tinted(buffers.getBuffer(type), alpha);
        dispatcher.getModelRenderer().renderModel(poseStack.last(), consumer, state, model, red, green, blue,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
    }

    /** Draws the batched ghosts now (call once per frame after the last {@link #render}). */
    public static void flush(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(renderType());
    }

    /** Scales every vertex's alpha; block models are drawn with alpha 1. */
    private record Tinted(VertexConsumer inner, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            inner.setColor(red, green, blue, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }
}
