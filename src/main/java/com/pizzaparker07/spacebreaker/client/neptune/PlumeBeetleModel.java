package com.pizzaparker07.spacebreaker.client.neptune;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.neptune.entity.PlumeBeetle;
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
 * Plume Beetle: a round body under two domed wing cases that open sideways in flight, a small head with two short
 * horns, six short legs. On the ground the cases are shut; airborne they swing out and the legs tuck in.
 * UVs: tools/textures/modules/neptune.py.
 */
public class PlumeBeetleModel extends HierarchicalModel<PlumeBeetle> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("plume_beetle"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart caseLeft;
    private final ModelPart caseRight;
    private final ModelPart[] legs = new ModelPart[6];

    public PlumeBeetleModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.caseLeft = body.getChild("case_left");
        this.caseRight = body.getChild("case_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -3F, -5F, 8F, 3F, 10F),
                PartPose.offset(0F, 22F, 0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 13).addBox(-2.5F, -2F, -3F, 5F, 3F, 3F)
                        .texOffs(16, 13).addBox(-2F, -4F, -2.5F, 1F, 2F, 1F)
                        .texOffs(16, 13).addBox(1F, -4F, -2.5F, 1F, 2F, 1F),
                PartPose.offset(0F, -1F, -5F));
        body.addOrReplaceChild("case_left", CubeListBuilder.create().texOffs(28, 0).addBox(0F, -3F, -4.5F, 4F, 3F, 9F),
                PartPose.offset(0F, -3F, 0F));
        body.addOrReplaceChild("case_right", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(-4F, -3F, -4.5F, 4F, 3F, 9F),
                PartPose.offset(0F, -3F, 0F));
        float[] z = {-3F, 0F, 3F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 20).addBox(0F, -0.5F, -0.5F, 4F, 1F, 1F),
                    PartPose.offset(3.5F, 22.5F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-4F, -0.5F, -0.5F, 4F, 1F, 1F),
                    PartPose.offset(-3.5F, 22.5F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(PlumeBeetle b, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean air = b.isGliding();
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        float open = air ? 0.9F + Mth.sin(ageInTicks * 0.6F) * 0.08F : 0F;
        caseLeft.zRot = open;
        caseRight.zRot = -open;
        float[] fan = {0.45F, 0F, -0.45F};
        for (int i = 0; i < 3; i++) {
            float phase = limbSwing * 1.8F + (i % 2 == 0 ? 0F : Mth.PI);
            float wave = air ? 0F : Mth.cos(phase) * 0.45F * limbSwingAmount;
            legs[i].yRot = fan[i] + wave;
            legs[i + 3].yRot = -fan[i] - wave;
            legs[i].zRot = air ? -0.6F : 0.4F - Math.max(0, Mth.sin(phase)) * 0.4F * limbSwingAmount;
            legs[i + 3].zRot = air ? 0.6F : -0.4F + Math.max(0, Mth.sin(phase)) * 0.4F * limbSwingAmount;
        }
    }
}
