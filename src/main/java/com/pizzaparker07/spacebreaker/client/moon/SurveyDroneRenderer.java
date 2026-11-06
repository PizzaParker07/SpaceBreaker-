package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.SurveyDrone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Survey drone with emissive status lights and lens. */
public class SurveyDroneRenderer extends MobRenderer<SurveyDrone, SurveyDroneModel<SurveyDrone>> {
    private static final ResourceLocation TEXTURE = Spacebreaker.id("textures/entity/moon/survey_drone.png");
    private static final RenderType LIGHTS = RenderType.eyes(Spacebreaker.id("textures/entity/moon/survey_drone_lights.png"));

    public SurveyDroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SurveyDroneModel<>(ctx.bakeLayer(SurveyDroneModel.LAYER)), 0.4F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return LIGHTS;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SurveyDrone drone) {
        return TEXTURE;
    }
}
