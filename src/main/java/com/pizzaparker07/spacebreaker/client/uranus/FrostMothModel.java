package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.FrostMoth;
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
 * Frost Moth: a slim body with two feathery antennae and two pairs of broad flat wings. In flight the wings beat; resting
 * (hanging under an overhang) they fold flat over the back. UVs: tools/textures/modules/uranus.py.
 */
public class FrostMothModel extends HierarchicalModel<FrostMoth> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("frost_moth"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] wings = new ModelPart[4];

    public FrostMothModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            wings[i] = body.getChild("wing" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -1.5F, -3F, 2F, 2F, 6F)
                        .texOffs(16, 0).addBox(-1.5F, -2F, -5F, 3F, 3F, 2F),
                PartPose.offset(0F, 20F, 0F));
        body.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(26, 0).addBox(0F, -3F, -1F, 2F, 3F, 0F),
                PartPose.offsetAndRotation(0.5F, -2F, -5F, -0.5F, 0F, 0.3F));
        body.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(26, 0).mirror().addBox(-2F, -3F, -1F, 2F, 3F, 0F),
                PartPose.offsetAndRotation(-0.5F, -2F, -5F, -0.5F, 0F, -0.3F));
        // wings: front pair larger, hind pair rounder; flat planes
        body.addOrReplaceChild("wing0", CubeListBuilder.create().texOffs(0, 10).addBox(0F, 0F, -4F, 9F, 0F, 6F), PartPose.offset(1F, -1F, -1F));
        body.addOrReplaceChild("wing1", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-9F, 0F, -4F, 9F, 0F, 6F), PartPose.offset(-1F, -1F, -1F));
        body.addOrReplaceChild("wing2", CubeListBuilder.create().texOffs(0, 18).addBox(0F, 0F, 0F, 7F, 0F, 5F), PartPose.offset(1F, -1F, 1F));
        body.addOrReplaceChild("wing3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-7F, 0F, 0F, 7F, 0F, 5F), PartPose.offset(-1F, -1F, 1F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FrostMoth m, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (m.isResting()) {
            body.xRot = Mth.PI;
            body.y = 22F;
            for (int i = 0; i < 4; i++) {
                wings[i].zRot = (i % 2 == 0 ? -1 : 1) * 0.15F;
            }
        } else {
            body.xRot = 0.1F;
            body.y = 20F + Mth.sin(ageInTicks * 0.3F) * 0.6F;
            float flap = Mth.cos(ageInTicks * 1.4F) * 0.9F;
            wings[0].zRot = flap;
            wings[1].zRot = -flap;
            wings[2].zRot = flap * 0.8F;
            wings[3].zRot = -flap * 0.8F;
        }
    }
}
