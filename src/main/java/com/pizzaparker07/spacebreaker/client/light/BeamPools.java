package com.pizzaparker07.spacebreaker.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fallback light of {@link LightBeams} when no dynamic lights mod is installed: where a beam lands, the lit faces
 * of the surface blocks are drawn once more with an additive emissive type (neutral under shader packs) and a smooth
 * falloff per vertex, so the pool has a soft round edge instead of rings; a faint glow lies around the lamp itself.
 * The landing point is eased from frame to frame, so the pool glides instead of jumping block to block.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class BeamPools {
    private static final RenderType POOL = RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
    /** Brightest the pool gets on top of a surface's own colours. */
    private static final float MAX_ADD = 0.7F;
    /** Eased landing points, by beam key. */
    private static final Map<Object, Vec3> EASED = new HashMap<>();

    private BeamPools() {
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || LightBeams.dynamicLights()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<LightBeams.Beam> beams = LightBeams.collect(mc, pt);
        if (beams.isEmpty()) {
            EASED.clear();
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Map<Object, Vec3> seen = new HashMap<>();
        for (LightBeams.Beam b : beams) {
            if (b.nearGlow() > 0) {
                pool(event.getPoseStack(), buffers, mc, BlockPos.containing(b.from()).below(2), Direction.UP,
                        b.from().subtract(0, 1.6, 0), 2.5, b.nearGlow(), cam);
            }
            if (!b.hit() || b.strength() <= 0) {
                continue;
            }
            Vec3 prev = EASED.get(b.key());
            // ease towards the new landing point; jump if it moved far (a wall swung into view)
            Vec3 at = prev == null || prev.distanceToSqr(b.to()) > 16 ? b.to() : prev.lerp(b.to(), 0.35);
            seen.put(b.key(), at);
            pool(event.getPoseStack(), buffers, mc, BlockPos.containing(at.relative(b.face(), -0.01)), b.face(), at,
                    b.poolRadius(), b.strength(), cam);
        }
        EASED.clear();
        EASED.putAll(seen);
        buffers.endBatch(POOL);
    }

    /**
     * Draws the lit faces ({@code face}) of the surface blocks around {@code center} (one per column, looked for one
     * block either side of the surface), each vertex faded by its distance to {@code at}.
     */
    private static void pool(PoseStack pose, MultiBufferSource buffers, Minecraft mc, BlockPos center, Direction face, Vec3 at,
                             double radius, double strength, Vec3 cam) {
        int r = (int) Math.ceil(radius);
        var renderer = mc.getBlockRenderer();
        VertexConsumer out = buffers.getBuffer(POOL);
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                for (int depth = 1; depth >= -1; depth--) {
                    BlockPos p = offset(center, face.getAxis(), i, j).relative(face, depth);
                    BlockState state = mc.level.getBlockState(p);
                    if (state.isAir() || state.getRenderShape() != RenderShape.MODEL
                            || !Block.shouldRenderFace(state, mc.level, p, face, p.relative(face))) {
                        continue;
                    }
                    if (Vec3.atCenterOf(p).relative(face, 0.5).distanceTo(at) < radius + 0.75) {
                        pose.pushPose();
                        // only the lit face, lifted a hair along its normal: no z-fighting, no seams between blocks
                        pose.translate(p.getX() - cam.x + face.getStepX() * 0.003, p.getY() - cam.y + face.getStepY() * 0.003,
                                p.getZ() - cam.z + face.getStepZ() * 0.003);
                        VertexConsumer vc = new Falloff(out, at.subtract(cam), radius, (float) strength);
                        var model = renderer.getBlockModel(state);
                        for (var quad : model.getQuads(state, face, RandomSource.create(state.getSeed(p)), ModelData.EMPTY, null)) {
                            vc.putBulkData(pose.last(), quad, 1F, 1F, 1F, 1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        }
                        pose.popPose();
                    }
                    break;
                }
            }
        }
    }

    private static BlockPos offset(BlockPos p, Direction.Axis normal, int i, int j) {
        return switch (normal) {
            case Y -> p.offset(i, 0, j);
            case X -> p.offset(0, i, j);
            case Z -> p.offset(i, j, 0);
        };
    }

    /**
     * Adds the block's own colours scaled by a smooth falloff from the pool centre, computed per vertex (camera
     * relative), so the edge is a soft gradient. Additive, so the alpha stays opaque.
     */
    private static final class Falloff implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Vec3 center;
        private final double radius;
        private final float strength;
        private float k;

        Falloff(VertexConsumer delegate, Vec3 center, double radius, float strength) {
            this.delegate = delegate;
            this.center = center;
            this.radius = radius;
            this.strength = strength;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            double d = Math.sqrt(center.distanceToSqr(x, y, z)) / radius;
            // smoothstep down to 0 at the rim, squared for a soft centre-weighted glow
            float t = (float) Mth.clamp(1 - d, 0, 1);
            float s = t * t * (3 - 2 * t);
            k = s * s * strength * MAX_ADD;
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            delegate.setColor(Math.round(red * k), Math.round(green * k), Math.round(blue * k), 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
