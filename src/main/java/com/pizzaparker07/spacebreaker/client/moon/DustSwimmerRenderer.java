package com.pizzaparker07.spacebreaker.client.moon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.DustSwimmer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the swimmer sunk into loose dust (only the back and the spine ridge break the surface), whole on hard ground. */
public class DustSwimmerRenderer extends MobRenderer<DustSwimmer, DustSwimmerModel<DustSwimmer>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/dust_swimmer.png");
    /** Depth below the surface in blocks (model space is upside down). */
    static final float SUNK = 0.27F;

    public DustSwimmerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DustSwimmerModel<>(ctx.bakeLayer(DustSwimmerModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(DustSwimmer swimmer, PoseStack pose, float partialTick) {
        // glides through the dust, porpoising: rises to show its back now and then, then dips again
        float t = (swimmer.tickCount + partialTick) * 0.045F + swimmer.getId() * 1.7F;
        float wave = 0.5F + 0.5F * (float) Math.sin(t);
        float sunk = swimmer.getDeltaMovement().y < -0.3 ? 0.05F : SUNK - 0.16F * wave * wave;
        // only loose dust takes it in: on rock, concrete or a base floor it lies on top, whole
        if (!swimmer.level().getBlockState(swimmer.blockPosition().below()).is(com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_SOILS)
                && !swimmer.level().getBlockState(swimmer.blockPosition()).is(com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_SOILS)) {
            sunk = 0F;
        }
        pose.translate(0F, sunk, 0F);
    }

    @Override
    protected float getShadowRadius(DustSwimmer swimmer) {
        // a shadow only when it lies out of the dust
        return swimmer.level().getBlockState(swimmer.blockPosition().below()).is(com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_SOILS)
                ? 0F : super.getShadowRadius(swimmer);
    }

    @Override
    public ResourceLocation getTextureLocation(DustSwimmer swimmer) {
        return TEXTURE;
    }
}
