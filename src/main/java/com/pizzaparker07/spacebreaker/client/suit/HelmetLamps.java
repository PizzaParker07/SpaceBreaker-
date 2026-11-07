package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.light.LightBeams;
import com.pizzaparker07.spacebreaker.content.suit.HelmetLamp;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Helmet lamps: in the dark the lamps of every EVA helmet in view shine where the wearer looks. They are beams of
 * {@link LightBeams}: real moving light along the beam with a dynamic lights mod, otherwise a soft pool where the beam
 * lands and a faint glow around the wearer. Seen from outside, each lamp also shows a soft glow cone (see
 * {@link SuitOverlayLayer}).
 */
public final class HelmetLamps {
    public static final ResourceLocation BEAM = Spacebreaker.id("textures/misc/helmet_lamp_beam.png");
    /** Light level along the beam (dynamic lights): the wide lamps, and the brighter long beam. */
    static final int WIDE_LUMINANCE = 11, LONG_LUMINANCE = 13;

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

    /** The lamp beam of every lit helmet within 64 blocks of the camera. */
    static void collect(Minecraft mc, float pt, java.util.function.Consumer<LightBeams.Beam> out) {
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (Player player : mc.level.players()) {
            if (player.distanceToSqr(cam) > 64 * 64 || player.isSpectator() || !lampsOn(player)) {
                continue;
            }
            boolean longBeam = HelmetLamp.longBeam(player);
            Vec3 eye = player.getEyePosition(pt);
            Vec3 look = player.getViewVector(pt);
            Vec3 end = eye.add(look.scale(HelmetLamp.range(longBeam)));
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            boolean onSurface = hit.getType() == HitResult.Type.BLOCK;
            Vec3 to = onSurface ? hit.getLocation() : end;
            double d = to.distanceTo(eye);
            out.accept(new LightBeams.Beam(player.getUUID(), eye.add(look.scale(0.4)), to, onSurface, hit.getDirection(),
                    longBeam ? LONG_LUMINANCE : WIDE_LUMINANCE, HelmetLamp.poolRadius(d, longBeam), HelmetLamp.strength(d, longBeam), 0.18));
        }
    }

    /**
     * The short glow cone of one lamp, in head model space (pixels / 16): two crossed quads from the lamp face
     * forward along -z, widening as they go.
     */
    static void beam(PoseStack pose, VertexConsumer vc, float lampX, float lampY, float lampZ, boolean longBeam) {
        PoseStack.Pose p = pose.last();
        // shader packs add bloom to emissive glows: keep the cone fainter there
        int color = com.pizzaparker07.spacebreaker.compat.client.ShaderState.shaderPackInUse() ? 0x38FFFFFF : 0x70FFFFFF;
        // long beam: a narrow cone twice as long
        float len = (longBeam ? 24F : 12F) / 16F, r0 = 0.6F / 16F, r1 = (longBeam ? 1.4F : 3.0F) / 16F;
        float x = lampX / 16F, y = lampY / 16F, z = lampZ / 16F;
        for (int plane = 0; plane < 2; plane++) {
            float ax = plane == 0 ? 1 : 0, ay = plane == 0 ? 0 : 1;
            float[][] v = {
                    {x - ax * r0, y - ay * r0, z, 0, 0}, {x + ax * r0, y + ay * r0, z, 1, 0},
                    {x + ax * r1, y + ay * r1, z - len, 1, 1}, {x - ax * r1, y - ay * r1, z - len, 0, 1}};
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 4; i++) {
                    float[] k = v[side == 0 ? i : 3 - i];
                    vc.addVertex(p, k[0], k[1], k[2]).setColor(color).setUv(k[3], k[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
                }
            }
        }
    }
}
