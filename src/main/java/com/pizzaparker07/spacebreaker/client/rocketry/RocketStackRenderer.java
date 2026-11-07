package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a rocket (or part of one) from its parts' 3D item models, stacked as {@link RocketGeometry} says.
 * Used by the rocket entity, spent stages and the assembler preview. Origin = centre of the bottom.
 */
public final class RocketStackRenderer {
    public static final ResourceLocation PLUME = Spacebreaker.id("textures/entity/rocket_plume.png");
    public static final ResourceLocation PLUME_NUCLEAR = Spacebreaker.id("textures/entity/rocket_plume_nuclear.png");
    private static final Map<ResourceLocation, ItemStack> STACKS = new HashMap<>();

    private RocketStackRenderer() {
    }

    private static ItemStack stackFor(ResourceLocation model) {
        return STACKS.computeIfAbsent(model, id -> new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    /**
     * @param legs 0 = stowed, 1 = deployed landing legs
     */
    public static void render(RocketLayout layout, RocketGeometry geometry, PoseStack pose, MultiBufferSource buffers,
                              int light, float legs) {
        render(layout, geometry, pose, buffers, light, legs, Integer.MAX_VALUE);
    }

    /** Draws the parts below index {@code skipFrom} (the cockpit view leaves out the capsule around the camera). */
    public static void render(RocketLayout layout, RocketGeometry geometry, PoseStack pose, MultiBufferSource buffers,
                              int light, float legs, int skipFrom) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        for (RocketGeometry.Placement p : geometry.placements()) {
            if (p.part() >= skipFrom) {
                continue;
            }
            RocketPart part = layout.parts().get(p.part());
            ItemStack stack = stackFor(part.model());
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(p.yaw()));
            pose.translate(0, p.y() / 16.0, 0);
            if (part.type() == PartType.LANDING_LEGS && legs > 0) {
                // swing out around the hinge at the top of the leg and slide the inner strut down
                double hingeY = 20 / 16.0, hingeZ = -9 / 16.0;
                pose.translate(0, -legs * 10 / 16.0, 0);
                pose.translate(0, hingeY, hingeZ);
                pose.mulPose(Axis.XP.rotationDegrees(-28F * legs));
                pose.translate(0, -hingeY, -hingeZ);
            }
            // item models are drawn around (0.5, 0.5, 0.5): shift so model (8, 0, 8) sits on our origin
            pose.translate(0, 0.5, 0);
            BakedModel model = items.getModel(stack, null, null, 0);
            items.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
        }
    }

    /** Exhaust plume under every engine of the lowest stage (vacuum plumes are wider and paler). */
    public static void renderPlume(RocketLayout layout, RocketGeometry geometry, int activeStage, PoseStack pose,
                                   MultiBufferSource buffers, float time, float strength, boolean vacuum) {
        if (activeStage >= layout.stages().size() || strength <= 0) {
            return;
        }
        RocketLayout.Stage stage = layout.stages().get(activeStage);
        for (RocketGeometry.Placement p : geometry.placements()) {
            if (p.part() < stage.firstPart() || p.part() > stage.lastPart()) {
                continue;
            }
            RocketPart part = layout.parts().get(p.part());
            if (!part.isEngine()) {
                continue;
            }
            float r = part.nozzle() / 16F;
            float flicker = 0.9F + 0.1F * Mth.sin(time * 2.3F) + 0.05F * Mth.sin(time * 7.1F);
            boolean nuclear = part.propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR;
            float length = (vacuum ? 3.2F : 4.5F) * strength * flicker;
            float width = r * (vacuum ? 2.2F : 1.05F);
            pose.pushPose();
            pose.translate(0, p.y() / 16.0, 0);
            if (nuclear) {
                // hot hydrogen is nearly invisible: a thin, faint blue-violet glow, then a dull red glow at the nozzle lip
                // (one buffer at a time: asking for the next one ends the previous)
                VertexConsumer hydrogen = buffers.getBuffer(RenderType.entityTranslucentEmissive(PLUME_NUCLEAR));
                for (int k = 0; k < 4; k++) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(k * 45F + time * 3F));
                    plane(hydrogen, pose.last().pose(), pose.last(), r * 0.85F, r * 1.25F, length, 0.8F, 255, 255, 255);
                    pose.popPose();
                }
                VertexConsumer lip = buffers.getBuffer(RenderType.entityTranslucentEmissive(PLUME));
                for (int k = 0; k < 4; k++) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(k * 45F));
                    plane(lip, pose.last().pose(), pose.last(), r * 0.98F, r * 0.92F, 0.2F, 0.5F, 255, 110, 80);
                    pose.popPose();
                }
                pose.popPose();
                continue;
            }
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(PLUME));
            for (int k = 0; k < 4; k++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(k * 45F + time * 3F));
                plane(vc, pose.last().pose(), pose.last(), r * 0.95F, width, length, vacuum ? 0.75F : 1F, 255, 255, 255);
                pose.popPose();
            }
            if (!vacuum) {
                // the white-hot core of a sea-level flame: short, narrow and added on top, so it glows even at night
                VertexConsumer core = buffers.getBuffer(RenderType.eyes(PLUME));
                for (int k = 0; k < 3; k++) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(k * 60F + time * 5F));
                    plane(core, pose.last().pose(), pose.last(), r * 0.8F, r * 0.45F, length * 0.55F, strength, 255, 235, 200);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
    }

    private static void plane(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, float top, float bottom, float length, float alpha,
                              int r, int g, int b) {
        int a = (int) (alpha * 255);
        // a tapered quad: nozzle width at the top, wider (or narrower) at the bottom; the colour tints the texture
        vertex(vc, m, last, -top, 0, 0, 0, a, r, g, b);
        vertex(vc, m, last, top, 0, 1, 0, a, r, g, b);
        vertex(vc, m, last, bottom, -length, 1, 1, a, r, g, b);
        vertex(vc, m, last, -bottom, -length, 0, 1, a, r, g, b);
        // back face
        vertex(vc, m, last, -bottom, -length, 0, 1, a, r, g, b);
        vertex(vc, m, last, bottom, -length, 1, 1, a, r, g, b);
        vertex(vc, m, last, top, 0, 1, 0, a, r, g, b);
        vertex(vc, m, last, -top, 0, 0, 0, a, r, g, b);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, float x, float y, float u, float v, int a, int r, int g, int b) {
        vc.addVertex(m, x, y, 0).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(last, 0, 0, 1);
    }
}
