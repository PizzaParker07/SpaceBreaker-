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
    static final float SUNK = 0.24F;

    public DustSwimmerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DustSwimmerModel<>(ctx.bakeLayer(DustSwimmerModel.LAYER)), 0F);
    }

    @Override
    protected void scale(DustSwimmer swimmer, PoseStack pose, float partialTick) {
        float sunk = swimmer.getDeltaMovement().y < -0.3 ? 0.05F : SUNK; // surfaces only when falling
        pose.translate(0F, sunk, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(DustSwimmer swimmer) {
        return TEXTURE;
    }
}
