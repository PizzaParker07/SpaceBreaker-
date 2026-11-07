package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Helmet lamp light without a dynamic-lights mod: in the dark the lamps of every EVA helmet in view throw a
 * round pool of light where the wearer looks. The lit faces of the surface blocks there are added again on top
 * (additive emissive type, checked with Iris + Complementary) and faded out block by block towards the edge,
 * like vanilla light levels. Seen from outside, each lamp also shows a short glow cone (see
 * {@link SuitOverlayLayer}).
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class HelmetLamps {
    public static final ResourceLocation BEAM = Spacebreaker.id("textures/misc/helmet_lamp_beam.png");
    /**
     * The pool is added on top of the lit faces (additive "eyes" type over the block atlas): shader packs draw it
     * with their emissive program, so it stays neutral white instead of being tinted like torch light.
     */
    private static final RenderType POOL = RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);

    private HelmetLamps() {
    }

    /** Lamps switch on by themselves when it is dark around the helmet. */
    public static boolean lampsOn(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof SpaceSuitItem suit) || suit.getType() != ArmorItem.Type.HELMET || entity.isInvisible()) {
            return false;
        }
        // the planet clocks drive the client sky brightness (0.2 at night .. 1 by day), so judge the sky light by it
        BlockPos eye = BlockPos.containing(entity.getEyePosition());
        Level level = entity.level();
        float daylight = level instanceof ClientLevel cl ? (cl.getSkyDarken(1F) - 0.2F) / 0.8F : 1F;
        float sky = level.getBrightness(LightLayer.SKY, eye) * daylight;
        return Math.max(level.getBrightness(LightLayer.BLOCK, eye), sky) < 8;
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean any = false;
        for (Player player : mc.level.players()) {
            if (player.distanceToSqr(cam) > 64 * 64 || player.isSpectator() || !lampsOn(player)) {
                continue;
            }
            Vec3 eye = player.getEyePosition(pt);
            boolean longBeam = com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.longBeam(player);
            Vec3 end = eye.add(player.getViewVector(pt).scale(com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.range(longBeam)));
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            double d = hit.getLocation().distanceTo(eye);
            pool(event.getPoseStack(), buffers, mc, hit, cam, com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.poolRadius(d, longBeam),
                    com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.strength(d, longBeam));
            any = true;
        }
        if (any) {
            buffers.endBatch(POOL);
        }
    }

    /** Draws the lit surface blocks of one pool; only faces open towards the lamp side are lit. */
    private static void pool(PoseStack pose, MultiBufferSource buffers, Minecraft mc, BlockHitResult hit, Vec3 cam, double radius, double strength) {
        Direction face = hit.getDirection();
        BlockPos center = hit.getBlockPos();
        Vec3 c = hit.getLocation();
        int r = (int) Math.ceil(radius);
        var renderer = mc.getBlockRenderer();
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                for (int depth = 1; depth >= -1; depth--) {
                    BlockPos p = offset(center, face.getAxis(), i, j).relative(face, depth);
                    BlockState state = mc.level.getBlockState(p);
                    if (state.isAir() || state.getRenderShape() != RenderShape.MODEL
                            || !Block.shouldRenderFace(state, mc.level, p, face, p.relative(face))) {
                        continue;
                    }
                    double dist = Vec3.atCenterOf(p).relative(face, 0.5).distanceTo(c);
                    double fall = 1 - dist / radius;
                    if (fall > 0) {
                        // a round pool fading block by block like vanilla light levels (15 steps)
                        float a = (float) (Math.ceil(fall * 15) / 15 * 0.9 * strength);
                        pose.pushPose();
                        // only the lit face, lifted a hair along its normal: no z-fighting, no seams between blocks
                        pose.translate(p.getX() - cam.x + face.getStepX() * 0.003, p.getY() - cam.y + face.getStepY() * 0.003,
                                p.getZ() - cam.z + face.getStepZ() * 0.003);
                        VertexConsumer vc = new LitConsumer(buffers.getBuffer(POOL), a);
                        var model = renderer.getBlockModel(state);
                        RandomSource rand = RandomSource.create(state.getSeed(p));
                        for (var quad : model.getQuads(state, face, rand, ModelData.EMPTY, null)) {
                            vc.putBulkData(pose.last(), quad, 1F, 1F, 1F, 1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        }
                        pose.popPose();
                    }
                    break; // one surface block per column
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
     * The short glow cone of one lamp, in head model space (pixels / 16): two crossed quads from the lamp face
     * forward along -z, widening as they go.
     */
    static void beam(PoseStack pose, VertexConsumer vc, float lampX, float lampY, float lampZ, boolean longBeam) {
        PoseStack.Pose p = pose.last();
        // long beam: a narrow cone twice as long
        float len = (longBeam ? 28F : 14F) / 16F, r0 = 0.5F / 16F, r1 = (longBeam ? 1.2F : 2.5F) / 16F;
        float x = lampX / 16F, y = lampY / 16F, z = lampZ / 16F;
        for (int plane = 0; plane < 2; plane++) {
            float ax = plane == 0 ? 1 : 0, ay = plane == 0 ? 0 : 1;
            float[][] v = {
                    {x - ax * r0, y - ay * r0, z, 0, 0}, {x + ax * r0, y + ay * r0, z, 1, 0},
                    {x + ax * r1, y + ay * r1, z - len, 1, 1}, {x - ax * r1, y - ay * r1, z - len, 0, 1}};
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 4; i++) {
                    float[] k = v[side == 0 ? i : 3 - i];
                    vc.addVertex(p, k[0], k[1], k[2]).setColor(0xFFFFFFFF).setUv(k[3], k[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
                }
            }
        }
    }

    /** Adds the block's own colours scaled by the pool's strength (additive, so alpha stays opaque). */
    private static final class LitConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int alpha;

        LitConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = Math.round(alpha * 255);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            float k = alpha / 255F * 0.55F;
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
