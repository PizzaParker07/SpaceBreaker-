package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.RimBurrower;
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
 * Rim Burrower: a low, broad body with a ridged back, a flat wedge head, short hind legs and wide digging fore claws.
 * Buried, the whole model sinks so only the ridged back shows above the ground. UVs: tools/textures/modules/uranus.py.
 */
public class RimBurrowerModel extends HierarchicalModel<RimBurrower> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rim_burrower"), "main");
    private final ModelPart root;
    private final ModelPart all;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[4];

    public RimBurrowerModel(ModelPart root) {
        this.root = root;
        this.all = root.getChild("all");
        this.head = all.getChild("head");
        for (int i = 0; i < 4; i++) {
            legs[i] = all.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition all = mesh.getRoot().addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        all.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -5F, -5F, 8F, 4F, 10F)
                        .texOffs(36, 0).addBox(-1F, -6F, -4F, 2F, 1F, 8F),
                PartPose.ZERO);
        all.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-2.5F, -1.5F, -4F, 5F, 3F, 4F),
                PartPose.offset(0F, -3.5F, -5F));
        float[][] at = {{3F, -3.5F}, {-3F, -3.5F}, {3F, 3.5F}, {-3F, 3.5F}};
        for (int i = 0; i < 4; i++) {
            boolean fore = i < 2;
            all.addOrReplaceChild("leg" + i, fore
                            ? CubeListBuilder.create().texOffs(20, 14).addBox(-1.5F, 0F, -2F, 3F, 2F, 3F)
                            : CubeListBuilder.create().texOffs(32, 14).addBox(-1F, 0F, -1F, 2F, 2F, 2F),
                    PartPose.offset(at[i][0], -2F, at[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RimBurrower b, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        all.y = b.isBuried() ? 28.5F : 24F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F;
        for (int i = 0; i < 4; i++) {
            float sign = (i == 0 || i == 3) ? 1F : -1F;
            legs[i].xRot = Mth.cos(limbSwing * 1.3F) * 0.8F * limbSwingAmount * sign;
        }
    }
}
