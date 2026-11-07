package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the EVA suit parts that need special render types, after the armour layer: smoked visor glass
 * (translucent), the gold sun visor, and the glowing helmet lamps and chest display (emissive, so they
 * light up at night and with shader packs).
 */
public class SuitOverlayLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public static final ResourceLocation TEXTURE = Spacebreaker.id("textures/models/armor/eva_suit_overlay.png");

    private final ModelPart glass;
    private final ModelPart sunVisor;
    private final ModelPart sunVisorUp;
    private final ModelPart lamps;
    private final ModelPart display;

    public SuitOverlayLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(EvaSuitModel.OVERLAY);
        this.glass = root.getChild("glass");
        this.sunVisor = root.getChild("sun_visor");
        this.sunVisorUp = root.getChild("sun_visor_up");
        this.lamps = root.getChild("lamps");
        this.display = root.getChild("display");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof SpaceSuitItem suit && suit.getType() == ArmorItem.Type.HELMET && getParentModel().head.visible) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            if (SpaceSuitItem.isSunVisorDown(helmet) || suit.tier() == com.pizzaparker07.spacebreaker.content.suit.SuitTier.THERMAL) {
                sunVisor.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            } else {
                sunVisorUp.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
                glass.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            }
            lamps.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            if (HelmetLamps.lampsOn(entity)) {
                // short faint beams from both lamps (the light pool itself is drawn by HelmetLamps)
                var beam = buffers.getBuffer(RenderType.entityTranslucentEmissive(HelmetLamps.BEAM));
                boolean longBeam = com.pizzaparker07.spacebreaker.content.suit.HelmetLamp.longBeam(helmet);
                HelmetLamps.beam(pose, beam, -5.5F, -7F, -3.05F, longBeam);
                HelmetLamps.beam(pose, beam, 5.5F, -7F, -3.05F, longBeam);
            }
            pose.popPose();
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof SpaceSuitItem suit && suit.getType() == ArmorItem.Type.CHESTPLATE && getParentModel().body.visible) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            display.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
