package com.pizzaparker07.spacebreaker.client.vehicles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import org.joml.Matrix4f;

/** Draws the rover, its headlights after dark (emissive layer plus two soft light cones) and the hit wobble. */
public class RoverRenderer extends EntityRenderer<RoverEntity> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/rover.png");
    private static final ResourceLocation LIGHTS = Spacebreaker.id("textures/entity/rover_lights.png");
    private static final ResourceLocation BEAM = Spacebreaker.id("textures/entity/rover_beam.png");
    private final RoverModel model;

    public RoverRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new RoverModel(ctx.bakeLayer(RoverModel.LAYER));
        this.shadowRadius = 1.1F;
    }

    /**
     * Headlights come on by themselves when the rover has power and it is dark: the sun of this world (its own
     * clock) near or below the horizon, or no sky and little lamp light around it.
     */
    public static boolean lightsOn(RoverEntity rover) {
        if (rover.energy() <= 0) {
            return false;
        }
        BlockPos p = BlockPos.containing(rover.getX(), rover.getY() + 1, rover.getZ());
        boolean sunUp = Math.abs(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.sunAngle(rover.level(), 1F)) < Math.toRadians(80);
        if (rover.level().canSeeSky(p) && rover.level().dimensionType().hasSkyLight()) {
            return !sunUp;
        }
        return rover.level().getBrightness(LightLayer.BLOCK, p) < 9;
    }

    @Override
    public void render(RoverEntity rover, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0F, 1.5F, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        float hurt = rover.hurtTime() - partialTick;
        if (hurt > 0) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(hurt) * hurt * 0.4F));
        }
        pose.scale(-1F, -1F, 1F);
        model.setupAnim(rover, 0, 0, rover.tickCount + partialTick, 0, 0);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        float dust = rover.hasSolar() ? rover.dust() : 0F;
        if (dust > 0.05F) {
            roofDust(pose, buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),
                    dust, model.bodyY(), light);
        }
        boolean on = lightsOn(rover);
        if (on) {
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(LIGHTS)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            beams(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM)));
        }
        pose.popPose();
        super.render(rover, yaw, partialTick, pose, buffers, light);
    }

    /** The same dust film as on a solar tracker, laid over the deck panel (model space, y down: its top is y = 5). */
    private static void roofDust(PoseStack pose, VertexConsumer vc, float dust, float bodyY, int light) {
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .apply(com.pizzaparker07.spacebreaker.Spacebreaker.id("block/solar_dust"));
        // the roof cells are small and dark: a thinner film keeps them readable at full cover
        int alpha = Math.round(com.pizzaparker07.spacebreaker.client.machines.SolarPanelRenderer.dustAlpha(dust) * 0.6F * 255);
        PoseStack.Pose last = pose.last();
        float y = (bodyY + 4.95F) / 16F;
        float x0 = -12F / 16F;
        float x1 = 12F / 16F;
        // the 24 x 16 panel takes the 16 px dust tile one and a half times across so the grain keeps its size
        float zs = 15F / 16F;
        float ze = 31F / 16F;
        for (int i = 0; i < 2; i++) {
            float a = i == 0 ? x0 : 0F;
            float b = i == 0 ? 0F : x1;
            float u0 = sprite.getU0();
            float u1 = sprite.getU(0.75F);
            dustVertex(vc, last, a, y, zs, u0, sprite.getV0(), alpha, light);
            dustVertex(vc, last, a, y, ze, u0, sprite.getV1(), alpha, light);
            dustVertex(vc, last, b, y, ze, u1, sprite.getV1(), alpha, light);
            dustVertex(vc, last, b, y, zs, u1, sprite.getV0(), alpha, light);
            dustVertex(vc, last, b, y, zs, u1, sprite.getV0(), alpha, light);
            dustVertex(vc, last, b, y, ze, u1, sprite.getV1(), alpha, light);
            dustVertex(vc, last, a, y, ze, u0, sprite.getV1(), alpha, light);
            dustVertex(vc, last, a, y, zs, u0, sprite.getV0(), alpha, light);
        }
    }

    private static void dustVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int alpha, int light) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0F, -1F, 0F);
    }

    /** Two cones of light ahead of the lamps (model space: front is -Z, y grows downward). */
    private static void beams(PoseStack pose, VertexConsumer vc) {
        Matrix4f m = pose.last().pose();
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 6F / 16F;
            float y = 13.5F / 16F;
            float z = -19.2F / 16F;
            float len = 5F;
            float r0 = 0.08F;
            float r1 = 1.1F;
            // a cross of two quads makes the cone read from any angle
            quad(vc, m, x - r0, y, z, x + r0, y, z, x + r1, y + 0.25F, z - len, x - r1, y + 0.25F, z - len);
            quad(vc, m, x, y - r0, z, x, y + r0, z, x, y + r1 * 0.6F, z - len, x, y - r1 * 0.4F, z - len);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        int a0 = 90;
        vertex(vc, m, x0, y0, z0, 0F, 0F, a0);
        vertex(vc, m, x1, y1, z1, 1F, 0F, a0);
        vertex(vc, m, x2, y2, z2, 1F, 1F, 0);
        vertex(vc, m, x3, y3, z3, 0F, 1F, 0);
        vertex(vc, m, x3, y3, z3, 0F, 1F, 0);
        vertex(vc, m, x2, y2, z2, 1F, 1F, 0);
        vertex(vc, m, x1, y1, z1, 1F, 0F, a0);
        vertex(vc, m, x0, y0, z0, 0F, 0F, a0);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int alpha) {
        vc.addVertex(m, x, y, z).setColor(255, 244, 214, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0F, 1F, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(RoverEntity rover) {
        return TEXTURE;
    }
}
