package com.pizzaparker07.spacebreaker.client.pluto;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.pluto.entity.NitrogenSlider;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Nitrogen Slider: a long, low body with a flat belly, a round head with a short snout and two dark eye spots, a pair of
 * broad front flippers and a forked tail fin. Walking, the flippers paddle; sliding, they lie flat against the sides and
 * the head is held low. UVs: tools/textures/modules/pluto.py.
 */
public class NitrogenSliderModel extends HierarchicalModel<NitrogenSlider> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("nitrogen_slider"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart flipperLeft;
    private final ModelPart flipperRight;
    private final ModelPart tail;

    public NitrogenSliderModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.flipperLeft = body.getChild("flipper_left");
        this.flipperRight = body.getChild("flipper_right");
        this.tail = body.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -5F, -6F, 8F, 5F, 13F),
                PartPose.offset(0F, 24F, 0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3F, -4F, -5F, 6F, 5F, 5F)
                        .texOffs(22, 18).addBox(-1.5F, -2F, -7F, 3F, 2F, 2F),
                PartPose.offset(0F, -1.5F, -6F));
        body.addOrReplaceChild("flipper_left", CubeListBuilder.create().texOffs(34, 0).addBox(0F, -0.5F, -1.5F, 5F, 1F, 3F),
                PartPose.offset(3.5F, -1F, -3F));
        body.addOrReplaceChild("flipper_right", CubeListBuilder.create().texOffs(34, 0).mirror().addBox(-5F, -0.5F, -1.5F, 5F, 1F, 3F),
                PartPose.offset(-3.5F, -1F, -3F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(34, 6).addBox(-2.5F, -3F, 0F, 5F, 3F, 4F),
                PartPose.offset(0F, 0F, 7F));
        tail.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(34, 14).addBox(-4F, -0.5F, 0F, 8F, 1F, 3F),
                PartPose.offset(0F, -0.5F, 4F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(NitrogenSlider s, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean sliding = s.isSliding() && limbSwingAmount > 0.05F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = sliding ? 0.15F : headPitch * Mth.DEG_TO_RAD * 0.5F;
        if (sliding) {
            flipperLeft.zRot = 0.05F;
            flipperRight.zRot = -0.05F;
            flipperLeft.yRot = -0.6F;
            flipperRight.yRot = 0.6F;
            tail.xRot = -0.1F;
        } else {
            float paddle = Mth.cos(limbSwing * 1.2F) * 0.7F * limbSwingAmount;
            flipperLeft.yRot = paddle;
            flipperRight.yRot = -paddle;
            flipperLeft.zRot = 0.3F;
            flipperRight.zRot = -0.3F;
            tail.xRot = Mth.sin(limbSwing * 1.2F) * 0.25F * limbSwingAmount + Mth.sin(ageInTicks * 0.08F) * 0.05F;
        }
        body.y = 24F - (sliding ? 0F : Math.abs(Mth.sin(limbSwing * 1.2F)) * 0.8F * limbSwingAmount);
    }
}
