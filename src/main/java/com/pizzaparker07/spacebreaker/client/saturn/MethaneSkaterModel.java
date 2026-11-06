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
 * Methane Skater: a slim, flat body held high on four very long, thin legs splayed far out to the sides (the feet spread
 * its weight on the liquid), a small head with two long feelers. UVs: tools/textures/modules/saturn.py.
 */
public class MethaneSkaterModel extends HierarchicalModel<MethaneSkater> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("methane_skater"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] legs = new ModelPart[4];

    public MethaneSkaterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2F, -6F, 5F, 2F, 12F),
                PartPose.offset(0F, 18F, 0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, -3F, 3F, 2F, 3F),
                PartPose.offset(0F, -0.5F, -6F));
        head.addOrReplaceChild("feeler_left", CubeListBuilder.create().texOffs(12, 14).addBox(0F, -0.5F, -6F, 0F, 1F, 6F),
                PartPose.offsetAndRotation(1F, -1F, -2.5F, -0.4F, -0.35F, 0F));
        head.addOrReplaceChild("feeler_right", CubeListBuilder.create().texOffs(12, 14).addBox(0F, -0.5F, -6F, 0F, 1F, 6F),
                PartPose.offsetAndRotation(-1F, -1F, -2.5F, -0.4F, 0.35F, 0F));
        // middle pair: the longest, pushing it along; rear pair trailing behind
        root.addOrReplaceChild("leg0", CubeListBuilder.create().texOffs(0, 22).addBox(0F, -0.5F, -0.5F, 14F, 1F, 1F), PartPose.offset(2F, 17.5F, -2F));
        root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-14F, -0.5F, -0.5F, 14F, 1F, 1F),
                PartPose.offset(-2F, 17.5F, -2F));
        root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0, 25).addBox(0F, -0.5F, -0.5F, 12F, 1F, 1F), PartPose.offset(2F, 17.5F, 3F));
        root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-12F, -0.5F, -0.5F, 12F, 1F, 1F),
                PartPose.offset(-2F, 17.5F, 3F));
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
    }
}
