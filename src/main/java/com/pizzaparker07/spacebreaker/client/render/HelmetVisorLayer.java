package com.pizzaparker07.spacebreaker.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.model.EvaHelmetModel;
import com.pizzaparker07.spacebreaker.content.item.SpaceSuitItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Draws the parts of the EVA helmet that need special render types: smoked glass (translucent),
 *  the gold sun visor and the glowing lamp lenses. Runs after the armour layer. */
public class HelmetVisorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public static final ResourceLocation TEXTURE = Spacebreaker.id("textures/models/armor/eva_helmet_visor.png");

    private final ModelPart glass;
    private final ModelPart sunVisor;
    private final ModelPart lenses;

    public HelmetVisorLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(EvaHelmetModel.VISOR_LAYER);
        this.glass = root.getChild("glass");
        this.sunVisor = root.getChild("sun_visor");
        this.lenses = root.getChild("lenses");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof SpaceSuitItem suit) || suit.getType() != SpaceSuitItem.Type.HELMET) {
            return;
        }
        ModelPart head = getParentModel().head;
        if (!head.visible) {
            return;
        }
        pose.pushPose();
        head.translateAndRotate(pose);
        if (SpaceSuitItem.isSunVisorDown(helmet)) {
            sunVisor.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        } else {
            glass.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        }
        lenses.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
