package com.pizzaparker07.spacebreaker.client.venus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.entity.AcidRay;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Acid Ray: a flat body, two broad wings that ripple in a slow wave, a long thin tail. UVs: tools/textures/modules/venus.py. */
public class AcidRayModel extends HierarchicalModel<AcidRay> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("acid_ray"), "main");
    private final ModelPart root;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart wingTipL;
    private final ModelPart wingTipR;
    private final ModelPart tail;

    public AcidRayModel(ModelPart root) {
        this.root = root;
        this.wingL = root.getChild("wing_left");
        this.wingR = root.getChild("wing_right");
        this.wingTipL = wingL.getChild("tip");
        this.wingTipR = wingR.getChild("tip");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -1F, -6F, 10F, 2F, 12F), PartPose.offset(0F, 22F, 0F));
        PartDefinition l = root.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(0, 16).addBox(0F, -0.5F, -5F, 6F, 1F, 10F),
                PartPose.offset(5F, 22F, 0F));
        l.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(6, 16).addBox(0F, -0.5F, -4F, 6F, 1F, 8F), PartPose.offset(6F, 0F, 0F));
        PartDefinition r = root.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-6F, -0.5F, -5F, 6F, 1F, 10F),
                PartPose.offset(-5F, 22F, 0F));
        r.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(6, 16).mirror().addBox(-6F, -0.5F, -4F, 6F, 1F, 8F), PartPose.offset(-6F, 0F, 0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 10F), PartPose.offset(0F, 22F, 6F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AcidRay ray, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = ray.getTarget() != null ? 0.32F : 0.14F;
        float flap = Mth.sin(ageInTicks * speed) * 0.35F;
        float lag = Mth.sin(ageInTicks * speed - 0.9F) * 0.4F;
        wingL.zRot = -flap;
        wingR.zRot = flap;
        wingTipL.zRot = -lag;
        wingTipR.zRot = lag;
        tail.yRot = Mth.sin(ageInTicks * 0.1F) * 0.25F;
        tail.xRot = Mth.sin(ageInTicks * speed - 1.6F) * 0.15F;
    }
}
