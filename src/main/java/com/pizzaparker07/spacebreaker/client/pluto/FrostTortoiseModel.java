package com.pizzaparker07.spacebreaker.client.pluto;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.pluto.entity.FrostTortoise;
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
 * Frost Tortoise: a stepped dome of a shell (a wide base plate and two smaller tiers, crusted dark red on top), a blunt
 * head on a short neck and four stubby legs. In its shell, head and legs are drawn in under the rim.
 * UVs: tools/textures/modules/pluto.py.
 */
public class FrostTortoiseModel extends HierarchicalModel<FrostTortoise> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("frost_tortoise"), "main");
    private final ModelPart root;
    private final ModelPart shell;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[4];

    public FrostTortoiseModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
        this.head = root.getChild("head");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -4F, -7F, 14F, 3F, 14F),
                PartPose.offset(0F, 22F, 0F));
        shell.addOrReplaceChild("tier1", CubeListBuilder.create().texOffs(0, 17).addBox(-5F, -3F, -5F, 10F, 3F, 10F),
                PartPose.offset(0F, -4F, 0F));
        shell.addOrReplaceChild("tier2", CubeListBuilder.create().texOffs(40, 17).addBox(-3F, -2F, -3F, 6F, 2F, 6F),
                PartPose.offset(0F, -7F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(42, 0).addBox(-2F, -2F, -5F, 4F, 4F, 5F),
                PartPose.offset(0F, 20F, -6F));
        float[][] at = {{4.5F, -4.5F}, {-4.5F, -4.5F}, {4.5F, 4.5F}, {-4.5F, 4.5F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(42, 9).addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F),
                    PartPose.offset(at[i][0], 21F, at[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FrostTortoise t, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean in = t.inShell();
        shell.y = in ? 23F : 22F;
        head.z = in ? -3F : -6F;
        head.y = in ? 21.5F : 20F;
        head.yRot = in ? 0F : netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = in ? 0F : headPitch * Mth.DEG_TO_RAD * 0.4F + Mth.sin(ageInTicks * 0.06F) * 0.05F;
        for (int i = 0; i < 4; i++) {
            float sign = (i == 0 || i == 3) ? 1F : -1F;
            legs[i].xRot = in ? 0F : Mth.cos(limbSwing * 1.0F) * 0.6F * limbSwingAmount * sign;
            legs[i].y = in ? 22.5F : 21F;
        }
    }
}
