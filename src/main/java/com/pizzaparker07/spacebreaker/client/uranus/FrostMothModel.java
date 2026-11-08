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
 * Frost Moth: a small head, a fuzzy thorax with a pale ruff, a banded abdomen, two feathery antennae, and two pairs of
 * separate wings - broad scalloped forewings with an eye spot, smaller rounded hind wings. In flight the hind pair beats
 * a little behind the front pair; resting (hanging under an overhang) the wings fold back over the body like a roof.
 * UVs: tools/textures/modules/uranus.py.
 */
public class FrostMothModel extends HierarchicalModel<FrostMoth> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("frost_moth"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] wings = new ModelPart[4];
    private final ModelPart antennaLeft;
    private final ModelPart antennaRight;

    public FrostMothModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            wings[i] = body.getChild("wing" + i);
        }
        antennaLeft = body.getChild("antenna_left");
        antennaRight = body.getChild("antenna_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1F, -1.5F, -6F, 2F, 2F, 2F)
                        .texOffs(8, 0).addBox(-1.5F, -2F, -4F, 3F, 3F, 3F)
                        .texOffs(20, 0).addBox(-1F, -1.5F, -1F, 2F, 2F, 5F)
                        .texOffs(34, 0).addBox(-2F, -2.5F, -4.5F, 4F, 4F, 1F),
                PartPose.offset(0F, 20F, 0F));
        body.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(44, 0).addBox(0F, -4F, 0F, 3F, 4F, 0F),
                PartPose.offsetAndRotation(0.5F, -1.5F, -5.5F, -0.6F, 0F, 0.35F));
        body.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-3F, -4F, 0F, 3F, 4F, 0F),
                PartPose.offsetAndRotation(-0.5F, -1.5F, -5.5F, -0.6F, 0F, -0.35F));
        // forewings hinged at the thorax, hind wings just behind them
        body.addOrReplaceChild("wing0", CubeListBuilder.create().texOffs(0, 8).addBox(0F, 0F, -1F, 10F, 0F, 7F), PartPose.offset(1.5F, -1.5F, -3.5F));
        body.addOrReplaceChild("wing1", CubeListBuilder.create().texOffs(0, 8).mirror().addBox(-10F, 0F, -1F, 10F, 0F, 7F), PartPose.offset(-1.5F, -1.5F, -3.5F));
        body.addOrReplaceChild("wing2", CubeListBuilder.create().texOffs(0, 16).addBox(0F, 0F, 0F, 7F, 0F, 5F), PartPose.offset(1F, -1.2F, -0.5F));
        body.addOrReplaceChild("wing3", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-7F, 0F, 0F, 7F, 0F, 5F), PartPose.offset(-1F, -1.2F, -0.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FrostMoth m, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float sway = Mth.sin(ageInTicks * 0.09F) * 0.08F;
        antennaLeft.zRot = 0.35F + sway;
        antennaRight.zRot = -0.35F - sway;
        if (m.isResting()) {
            body.xRot = Mth.PI;
            body.y = 22F;
            // folded back over the abdomen and tipped down at the sides: a roof
            for (int i = 0; i < 4; i++) {
                float side = i % 2 == 0 ? 1F : -1F;
                wings[i].yRot = -side * (i < 2 ? 0.85F : 0.6F);
                wings[i].zRot = side * 0.55F;
            }
        } else {
            body.xRot = 0.1F;
            body.y = 20F + Mth.sin(ageInTicks * 0.3F) * 0.6F;
            float flap = Mth.cos(ageInTicks * 1.4F) * 0.9F;
            float hind = Mth.cos(ageInTicks * 1.4F - 0.7F) * 0.75F;
            wings[0].zRot = flap;
            wings[1].zRot = -flap;
            wings[2].zRot = hind;
            wings[3].zRot = -hind;
            for (int i = 0; i < 4; i++) {
                wings[i].yRot = -(i % 2 == 0 ? 1F : -1F) * (i < 2 ? 0.1F : 0.25F);
            }
        }
    }
}
