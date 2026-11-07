package com.pizzaparker07.spacebreaker.client.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.saturn.entity.MethaneSkater;
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
 * Methane Skater: a slim, flat body with a raised thorax and a pale stripe down the back, held high on four very long,
 * thin legs splayed far out to the sides; each foot ends in a broad glossy pad that dimples the liquid and spreads its
 * weight. A small head with two long feelers. UVs: tools/textures/modules/saturn.py.
 */
public class MethaneSkaterModel extends HierarchicalModel<MethaneSkater> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("methane_skater"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] pads = new ModelPart[4];

    public MethaneSkaterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
            pads[i] = legs[i].getChild("pad");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2F, -6F, 5F, 2F, 12F)
                .texOffs(34, 0).addBox(-1.5F, -3F, -4.5F, 3F, 1F, 4F), PartPose.offset(0F, 18F, 0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, -3F, 3F, 2F, 3F),
                PartPose.offset(0F, -0.5F, -6F));
        head.addOrReplaceChild("feeler_left", CubeListBuilder.create().texOffs(12, 14).addBox(0F, -0.5F, -6F, 0F, 1F, 6F),
                PartPose.offsetAndRotation(1F, -1F, -2.5F, -0.4F, -0.35F, 0F));
        head.addOrReplaceChild("feeler_right", CubeListBuilder.create().texOffs(12, 14).addBox(0F, -0.5F, -6F, 0F, 1F, 6F),
                PartPose.offsetAndRotation(-1F, -1F, -2.5F, -0.4F, 0.35F, 0F));
        // middle pair: the longest, pushing it along; rear pair trailing behind
        float[][] leg = {{2F, -2F, 14F, 22F}, {-2F, -2F, 14F, 22F}, {2F, 3F, 12F, 25F}, {-2F, 3F, 12F, 25F}};
        for (int i = 0; i < 4; i++) {
            float side = leg[i][0] > 0 ? 1F : -1F;
            float len = leg[i][2];
            CubeListBuilder c = CubeListBuilder.create().texOffs(0, (int) leg[i][3]);
            if (side < 0) {
                c.mirror();
            }
            PartDefinition l = root.addOrReplaceChild("leg" + i, c.addBox(side > 0 ? 0F : -len, -0.5F, -0.5F, len, 1F, 1F),
                    PartPose.offset(leg[i][0], 17.5F, leg[i][1]));
            // the glossy foot pad at the tip
            l.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(34, 8).addBox(-1.5F, 0F, -1.5F, 3F, 1F, 3F),
                    PartPose.offset(side * len, 0F, 0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MethaneSkater s, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.2F;
        float stroke = Mth.cos(limbSwing * 0.9F) * 0.5F * limbSwingAmount;
        float bob = Mth.sin(ageInTicks * 0.1F) * 0.03F;
        // legs angle down to the surface: the feet touch the ground (or lake) about six pixels out
        legs[0].yRot = -0.35F + stroke;
        legs[1].yRot = 0.35F - stroke;
        legs[2].yRot = 0.45F - stroke * 0.6F;
        legs[3].yRot = -0.45F + stroke * 0.6F;
        legs[0].zRot = 0.42F + bob;
        legs[1].zRot = -0.42F - bob;
        legs[2].zRot = 0.5F + bob;
        legs[3].zRot = -0.5F - bob;
        // the pads stay flat on the surface whatever the legs do
        for (int i = 0; i < 4; i++) {
            pads[i].zRot = -legs[i].zRot;
        }
    }
}
