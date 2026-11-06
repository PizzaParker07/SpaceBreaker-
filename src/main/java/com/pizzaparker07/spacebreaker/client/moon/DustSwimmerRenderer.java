package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.DustSwimmer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the swimmer sunk into the dust: only the back and dorsal fin break the surface. */
public class DustSwimmerRenderer extends MobRenderer<DustSwimmer, DustSwimmerModel<DustSwimmer>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/dust_swimmer.png");
    /** Depth below the surface in blocks (model space is upside down). */
    static final float SUNK = 0.27F;

    public DustSwimmerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DustSwimmerModel<>(ctx.bakeLayer(DustSwimmerModel.LAYER)), 0F);
    }

    @Override
    protected void scale(DustSwimmer swimmer, PoseStack pose, float partialTick) {
        // glides through the dust, porpoising: rises to show its back now and then, then dips again
        float t = (swimmer.tickCount + partialTick) * 0.045F + swimmer.getId() * 1.7F;
        float wave = 0.5F + 0.5F * (float) Math.sin(t);
        float sunk = swimmer.getDeltaMovement().y < -0.3 ? 0.05F : SUNK - 0.16F * wave * wave;
        pose.translate(0F, sunk, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(DustSwimmer swimmer) {
        return TEXTURE;
    }
}
