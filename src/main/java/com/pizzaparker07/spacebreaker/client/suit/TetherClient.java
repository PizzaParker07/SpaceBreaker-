package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.Gravity;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.core.env.TetherAnchor;
import com.pizzaparker07.spacebreaker.content.suit.Tether;
import com.pizzaparker07.spacebreaker.content.suit.net.TetherPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side of the safety tether: keeps the local player within the length of the line (movement is
 * client-side in Minecraft) and draws every tether in view as a safety-orange line that sags while slack
 * (less so in low gravity) and runs straight when taut. Uses the vanilla leash render type, so it looks and
 * behaves like a lead with shader packs too.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class TetherClient {
    /** A line's anchor: a block, or an entity by network id (-1 for none). */
    private record Anchor(BlockPos block, int entity) {
    }

    private static final Map<Integer, Anchor> ANCHORS = new HashMap<>();
    private static final int SEGMENTS = 24;

    private TetherClient() {
    }

    public static void accept(TetherPayload payload) {
        if (payload.anchor().isPresent() || payload.anchorEntity().isPresent()) {
            ANCHORS.put(payload.entityId(), new Anchor(payload.anchor().orElse(null), payload.anchorEntity().orElse(-1)));
        } else {
            ANCHORS.remove(payload.entityId());
        }
    }

    @SubscribeEvent
    static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ANCHORS.clear();
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused() || p.isSpectator()) {
            return;
        }
        Anchor anchor = ANCHORS.get(p.getId());
        coMove(mc, p, anchor);
        Vec3 at = anchor == null ? null : point(mc, p, anchor);
        if (at == null) {
            return;
        }
        Vec3 next = Tether.waist(p).add(p.getDeltaMovement());
        Vec3 v = Tether.constrain(next, p.getDeltaMovement(), at, Tether.LENGTH);
        if (v != p.getDeltaMovement()) {
            p.setDeltaMovement(v);
            p.resetFallDistance();
        }
    }

    /** Where a line holds now on the client: the block's centre or the entity's clip ring; null if not in view. */
    private static Vec3 point(Minecraft mc, Player player, Anchor anchor) {
        if (anchor.block() != null) {
            return Tether.anchorPoint(anchor.block());
        }
        Entity e = mc.level.getEntity(anchor.entity());
        return e instanceof TetherAnchor a ? a.tetherPoint(player) : null;
    }

    /**
     * A craft moves through the world blocks: a suited player on its line, or drifting free within
     * {@link Tether#CO_MOVE_RANGE} of it, moves along with it, so an EVA beside a moving craft stays beside it.
     */
    private static void coMove(Minecraft mc, LocalPlayer p, Anchor anchor) {
        if (p.isPassenger() || p.onGround() || !SpaceSuitItem.wearsHelmet(p)) {
            return;
        }
        Vec3 frame = Vec3.ZERO;
        if (anchor != null && anchor.entity() >= 0 && mc.level.getEntity(anchor.entity()) instanceof TetherAnchor a) {
            frame = a.frameVelocity();
        } else {
            double best = Double.MAX_VALUE;
            for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(Tether.CO_MOVE_RANGE), e -> e instanceof TetherAnchor)) {
                Vec3 v = ((TetherAnchor) e).frameVelocity();
                double d = e.distanceToSqr(p);
                if (v.lengthSqr() > 0 && d < best) {
                    best = d;
                    frame = v;
                }
            }
        }
        if (frame.lengthSqr() > 0) {
            p.setPos(p.position().add(frame));
        }
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ANCHORS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        for (Map.Entry<Integer, Anchor> e : ANCHORS.entrySet()) {
            Entity entity = mc.level.getEntity(e.getKey());
            if (!(entity instanceof Player player) || player.distanceToSqr(cam) > 96 * 96) {
                continue;
            }
            boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
            Vec3 from = belt(player, pt, firstPerson);
            Vec3 to = point(mc, player, e.getValue());
            if (to == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(from.x - cam.x, from.y - cam.y, from.z - cam.z);
            rope(pose, buffers.getBuffer(RenderType.leash()), player, BlockPos.containing(to), to.subtract(from), Gravity.factorFor(player));
            pose.popPose();
            buffers.endBatch(RenderType.leash()); // one strip per line
        }
    }

    /** The belt clip, behind the hip on the right side; in first person a little lower so the line hangs into view. */
    private static Vec3 belt(Player player, float pt, boolean firstPerson) {
        double x = Mth.lerp(pt, player.xo, player.getX());
        double y = Mth.lerp(pt, player.yo, player.getY());
        double z = Mth.lerp(pt, player.zo, player.getZ());
        float yaw = Mth.lerp(pt, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double side = firstPerson ? 0.0 : -0.22;
        double back = firstPerson ? 0.35 : -0.12;
        double ox = -Mth.sin(yaw) * back + Mth.cos(yaw) * side;
        double oz = Mth.cos(yaw) * back + Mth.sin(yaw) * side;
        return new Vec3(x + ox, y + player.getBbHeight() * (firstPerson ? 0.35 : 0.48), z + oz);
    }

    private static void rope(PoseStack pose, VertexConsumer buffer, Player player, BlockPos anchor, Vec3 d, double gravity) {
        Matrix4f m = pose.last().pose();
        float dx = (float) d.x, dy = (float) d.y, dz = (float) d.z;
        double len = d.length();
        // slack line sags (a catenary is close to a parabola here); taut it runs straight
        float sag = (float) (Math.max(0.0, Tether.LENGTH - len) * 0.35 * Mth.clamp(gravity, 0.15, 1.0));
        sag = Math.min(sag, 3.5F);
        BlockPos playerPos = BlockPos.containing(player.getEyePosition());
        int bl0 = player.level().getBrightness(LightLayer.BLOCK, playerPos);
        int bl1 = player.level().getBrightness(LightLayer.BLOCK, anchor);
        int sl0 = player.level().getBrightness(LightLayer.SKY, playerPos);
        int sl1 = player.level().getBrightness(LightLayer.SKY, anchor);
        float w = 0.03F;
        float h = Mth.invSqrt(dx * dx + dz * dz + 1e-6F) * w / 2F;
        float nx = dz * h, nz = dx * h;
        for (int i = 0; i <= SEGMENTS; i++) {
            pair(buffer, m, dx, dy, dz, sag, i, w, w, nx, nz, bl0, bl1, sl0, sl1, false);
        }
        for (int i = SEGMENTS; i >= 0; i--) {
            pair(buffer, m, dx, dy, dz, sag, i, w, 0F, nx, nz, bl0, bl1, sl0, sl1, true);
        }
    }

    private static void pair(VertexConsumer b, Matrix4f m, float dx, float dy, float dz, float sag, int i, float yOff, float ddy,
                             float nx, float nz, int bl0, int bl1, int sl0, int sl1, boolean reverse) {
        float f = (float) i / SEGMENTS;
        int light = LightTexture.pack((int) Mth.lerp(f, bl0, bl1), (int) Mth.lerp(f, sl0, sl1));
        // safety orange with darker braid marks every other segment
        float shade = i % 2 == (reverse ? 1 : 0) ? 0.72F : 1.0F;
        float r = 0.95F * shade, g = 0.48F * shade, bl = 0.12F * shade;
        float x = dx * f;
        float y = dy * f - sag * 4F * f * (1F - f);
        float z = dz * f;
        b.addVertex(m, x - nx, y + ddy, z + nz).setColor(r, g, bl, 1F).setLight(light);
        b.addVertex(m, x + nx, y + yOff - ddy, z - nz).setColor(r, g, bl, 1F).setLight(light);
    }
}
