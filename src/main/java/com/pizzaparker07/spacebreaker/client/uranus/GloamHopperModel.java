package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.GloamHopper;
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
 * Gloam Hopper: a compact dark body sloping up to the shoulders, a blunt head with two pale eyes and two flat ear plates,
 * short fore legs and long folded hind legs that stretch out while it is in the air. UVs: tools/textures/modules/uranus.py.
 */
public class GloamHopperModel extends HierarchicalModel<GloamHopper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("gloam_hopper"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftHind;
    private final ModelPart rightHind;
    private final ModelPart leftFore;
    private final ModelPart rightFore;

    public GloamHopperModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftHind = root.getChild("left_hind");
        this.rightHind = root.getChild("right_hind");
        this.leftFore = root.getChild("left_fore");
        this.rightFore = root.getChild("right_fore");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -4F, 6F, 5F, 8F),
                PartPose.offsetAndRotation(0F, 18F, 0F, -0.2F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, -2.5F, -4F, 5F, 4F, 4F),
                PartPose.offset(0F, 15.5F, -3.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(46, 0).addBox(0F, -4F, -0.5F, 2F, 4F, 1F),
                PartPose.offsetAndRotation(1F, -2F, -1F, 0F, 0F, 0.35F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(46, 0).mirror().addBox(-2F, -4F, -0.5F, 2F, 4F, 1F),
                PartPose.offsetAndRotation(-1F, -2F, -1F, 0F, 0F, -0.35F));
        root.addOrReplaceChild("left_hind", CubeListBuilder.create().texOffs(0, 14).addBox(-1F, 0F, -1.5F, 2F, 5F, 5F),
                PartPose.offset(2.5F, 19F, 2F));
        root.addOrReplaceChild("right_hind", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-1F, 0F, -1.5F, 2F, 5F, 5F),
                PartPose.offset(-2.5F, 19F, 2F));
        root.addOrReplaceChild("left_fore", CubeListBuilder.create().texOffs(16, 14).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F),
                PartPose.offset(1.8F, 19F, -3F));
        root.addOrReplaceChild("right_fore", CubeListBuilder.create().texOffs(16, 14).mirror().addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F),
                PartPose.offset(-1.8F, 19F, -3F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GloamHopper h, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // in the air the hind legs stretch back and the fore legs reach forward
        float air = h.onGround() ? 0F : 1F;
        leftHind.xRot = rightHind.xRot = air * 0.9F + Mth.cos(limbSwing * 0.8F) * 0.3F * limbSwingAmount;
        leftFore.xRot = rightFore.xRot = -air * 0.7F - Mth.cos(limbSwing * 0.8F) * 0.3F * limbSwingAmount;
        body.xRot = -0.2F - air * 0.15F;
    }
}
