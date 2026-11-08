package com.pizzaparker07.spacebreaker.client.venus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.entity.CloudJelly;
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
 * Cloud Jelly: a translucent domed bell in stepped tiers over a frilled skirt, a warm glowing core inside it, two
 * frilly oral arms and eight tentacles of different lengths in two jointed segments that trail and sway. The bell
 * pulses slowly as it drifts. UVs: tools/textures/modules/venus.py.
 */
public class CloudJellyModel extends HierarchicalModel<CloudJelly> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("cloud_jelly"), "main");
    private static final int TENTACLES = 8;
    private final ModelPart root;
    private final ModelPart bell;
    private final ModelPart[] upper = new ModelPart[TENTACLES];
    private final ModelPart[] lower = new ModelPart[TENTACLES];

    public CloudJellyModel(ModelPart root) {
        this.root = root;
        this.bell = root.getChild("bell");
        for (int i = 0; i < TENTACLES; i++) {
            upper[i] = root.getChild("tentacle" + i);
            lower[i] = upper[i].getChild("end");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bell = root.addOrReplaceChild("bell", CubeListBuilder.create(), PartPose.offset(0F, -4F, 0F));
        bell.addOrReplaceChild("tier0", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -4F, -8F, 16F, 4F, 16F), PartPose.ZERO);
        bell.addOrReplaceChild("tier1", CubeListBuilder.create().texOffs(64, 0).addBox(-6.5F, -4F, -6.5F, 13F, 4F, 13F), PartPose.offset(0F, -4F, 0F));
        bell.addOrReplaceChild("tier2", CubeListBuilder.create().texOffs(0, 20).addBox(-4.5F, -3F, -4.5F, 9F, 3F, 9F), PartPose.offset(0F, -8F, 0F));
        bell.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(36, 20).addBox(-2.5F, -2F, -2.5F, 5F, 2F, 5F), PartPose.offset(0F, -11F, 0F));
        bell.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 36).addBox(-9F, 0F, -9F, 18F, 1F, 18F), PartPose.ZERO);
        bell.addOrReplaceChild("core", CubeListBuilder.create().texOffs(56, 20).addBox(-2.5F, -2.5F, -2.5F, 5F, 5F, 5F), PartPose.offset(0F, -4F, 0F));
        for (int k = 0; k < 2; k++) {
            root.addOrReplaceChild("arm" + k, CubeListBuilder.create().texOffs(84, 20).addBox(-1F, 0F, -1F, 2F, 8F, 2F),
                    PartPose.offsetAndRotation(k == 0 ? -1.5F : 1.5F, -3F, 0F, 0F, 0F, k == 0 ? 0.1F : -0.1F));
        }
        for (int i = 0; i < TENTACLES; i++) {
            double a = i * Math.PI * 2 / TENTACLES;
            float l1 = 6F + (i % 3) * 2F;
            float l2 = 6F + (i % 2) * 4F;
            PartDefinition t = root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create().texOffs(76, 20).addBox(-0.5F, 0F, -0.5F, 1F, l1, 1F),
                    PartPose.offset((float) Math.cos(a) * 6.5F, -3.5F, (float) Math.sin(a) * 6.5F));
            t.addOrReplaceChild("end", CubeListBuilder.create().texOffs(80, 20).addBox(-0.5F, 0F, -0.5F, 1F, l2, 1F), PartPose.offset(0F, l1, 0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CloudJelly jelly, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = 1F + Mth.sin(ageInTicks * 0.08F) * 0.06F;
        bell.xScale = pulse;
        bell.zScale = pulse;
        bell.yScale = 2F - pulse;
        for (int i = 0; i < TENTACLES; i++) {
            float ph = ageInTicks * 0.06F + i * 0.9F;
            upper[i].xRot = Mth.sin(ph) * 0.16F;
            upper[i].zRot = Mth.cos(ph * 0.8F + i) * 0.16F;
            // the ends trail a beat behind, so each tentacle waves along its length
            lower[i].xRot = Mth.sin(ph - 1.2F) * 0.3F;
            lower[i].zRot = Mth.cos(ph * 0.8F + i - 1.2F) * 0.3F;
        }
    }
}
