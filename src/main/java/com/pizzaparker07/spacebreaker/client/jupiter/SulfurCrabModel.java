package com.pizzaparker07.spacebreaker.client.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.SulfurCrab;
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
 * Sulfur Crab: a wide, flat carapace with a raised ridge, two eye stalks, two heavy claws held forward and three legs on
 * each side. Walks sideways-ish with a scuttling leg cycle; the claws open and close. UVs: tools/textures/modules/jupiter.py.
 */
public class SulfurCrabModel extends HierarchicalModel<SulfurCrab> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("sulfur_crab"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart clawLeft;
    private final ModelPart clawRight;
    private final ModelPart[] legs = new ModelPart[6];

    public SulfurCrabModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.clawLeft = root.getChild("claw_left");
        this.clawRight = root.getChild("claw_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -4F, -5F, 12F, 4F, 10F),
                PartPose.offset(0F, 20F, 0F));
        body.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(0, 14).addBox(-4F, -1F, -3F, 8F, 1F, 6F), PartPose.offset(0F, -4F, 0F));
        body.addOrReplaceChild("eye_left", CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offset(2F, -4F, -4.5F));
        body.addOrReplaceChild("eye_right", CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offset(-2F, -4F, -4.5F));
        root.addOrReplaceChild("claw_left", CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, -1.5F, -5F, 4F, 3F, 5F),
                PartPose.offsetAndRotation(5F, 19F, -4.5F, 0F, -0.35F, 0F));
        root.addOrReplaceChild("claw_right", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-2.5F, -1.5F, -5F, 4F, 3F, 5F),
                PartPose.offsetAndRotation(-5F, 19F, -4.5F, 0F, 0.35F, 0F));
        float[] z = {-2.5F, 0.5F, 3.5F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(32, 14).addBox(0F, -0.5F, -0.5F, 6F, 1F, 1F),
                    PartPose.offset(5.5F, 19.5F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(32, 14).mirror().addBox(-6F, -0.5F, -0.5F, 6F, 1F, 1F),
                    PartPose.offset(-5.5F, 19.5F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SulfurCrab crab, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.15F;
        float snap = crab.getTarget() != null ? Mth.sin(ageInTicks * 0.6F) * 0.3F : Mth.sin(ageInTicks * 0.08F) * 0.06F;
        clawLeft.yRot = -0.35F + snap;
        clawRight.yRot = 0.35F - snap;
        clawLeft.xRot = clawRight.xRot = -0.1F;
        for (int i = 0; i < 3; i++) {
            float wave = Mth.cos(limbSwing * 1.5F + i * 2.1F) * 0.45F * limbSwingAmount;
            float lift = Math.max(0, Mth.sin(limbSwing * 1.5F + i * 2.1F)) * 0.35F * limbSwingAmount;
            legs[i].yRot = (i - 1) * -0.35F + wave;
            legs[i + 3].yRot = (i - 1) * 0.35F - wave;
            legs[i].zRot = 0.55F - lift;
            legs[i + 3].zRot = -0.55F + lift;
        }
    }
}
