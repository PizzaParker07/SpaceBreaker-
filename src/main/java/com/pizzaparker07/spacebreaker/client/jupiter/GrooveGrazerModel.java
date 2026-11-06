package com.pizzaparker07.spacebreaker.client.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.GrooveGrazer;
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
 * Groove Grazer: a long, low body under three raised plate bands, a blunt head with a wide shovel snout held close to
 * the ground, six short legs. UVs: tools/textures/modules/jupiter.py.
 */
public class GrooveGrazerModel extends HierarchicalModel<GrooveGrazer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("groove_grazer"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[6];

    public GrooveGrazerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -5F, -7F, 8F, 5F, 14F),
                PartPose.offset(0F, 19F, 0F));
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(0, 19).addBox(-4.5F, -1F, -1.5F, 9F, 2F, 3F),
                    PartPose.offset(0F, -5F, -4.5F + i * 4.5F));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(44, 0).addBox(-3F, -2F, -4F, 6F, 4F, 4F),
                PartPose.offset(0F, 17F, -7F));
        head.addOrReplaceChild("scoop", CubeListBuilder.create().texOffs(24, 19).addBox(-3.5F, 0F, -2F, 7F, 1F, 2F), PartPose.offset(0F, 1F, -4F));
        float[] z = {-4.5F, 0F, 4.5F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(44, 8).addBox(-1F, 0F, -1F, 2F, 5F, 2F), PartPose.offset(3F, 19F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(44, 8).mirror().addBox(-1F, 0F, -1F, 2F, 5F, 2F),
                    PartPose.offset(-3F, 19F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GrooveGrazer g, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        float graze = Mth.clamp((Mth.sin(ageInTicks * 0.03F + g.getId()) - 0.5F) * 3F, 0F, 1F) * (1 - Math.min(1, limbSwingAmount * 3));
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F + graze * 0.5F;
        for (int i = 0; i < 6; i++) {
            float phase = ((i % 3) + (i >= 3 ? 1 : 0)) % 2 == 0 ? 0 : Mth.PI;
            legs[i].xRot = Mth.cos(limbSwing * 0.8F + phase) * 0.7F * limbSwingAmount;
        }
    }
}
