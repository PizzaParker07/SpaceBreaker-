package com.pizzaparker07.spacebreaker.client.mars;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** A plain mob renderer with a fixed texture, a model scale and (optionally) a translucent render type. */
public class SimpleMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float scale;
    private final boolean translucent;

    public SimpleMobRenderer(EntityRendererProvider.Context ctx, M model, ResourceLocation texture, float scale, float shadow, boolean translucent) {
        super(ctx, model, shadow);
        this.texture = texture;
        this.scale = scale;
        this.translucent = translucent;
    }

    @Override
    protected void scale(T mob, PoseStack pose, float partialTick) {
        float s = scale * (mob.isBaby() ? 0.5F : 1F);
        pose.scale(s, s, s);
    }

    @Override
    protected RenderType getRenderType(T mob, boolean bodyVisible, boolean translucentFlag, boolean glowing) {
        if (translucent && bodyVisible) {
            return RenderType.entityTranslucent(texture);
        }
        return super.getRenderType(mob, bodyVisible, translucentFlag, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(T mob) {
        return texture;
    }
}
