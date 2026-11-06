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

/** Cloud Jelly: a gas sac above a frilled rim, six long tentacles that sway. The sac pulses slowly. */
public class CloudJellyModel extends HierarchicalModel<CloudJelly> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("cloud_jelly"), "main");
    private final ModelPart root;
    private final ModelPart sac;
    private final ModelPart[] tentacles = new ModelPart[6];

    public CloudJellyModel(ModelPart root) {
        this.root = root;
        this.sac = root.getChild("sac");
        for (int i = 0; i < 6; i++) {
            tentacles[i] = root.getChild("tentacle" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("sac", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -12F, -7F, 14F, 12F, 14F), PartPose.offset(0F, -4F, 0F));
        root.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(0, 26).addBox(-8F, 0F, -8F, 16F, 2F, 16F), PartPose.offset(0F, -4F, 0F));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, 0F, -0.5F, 1F, 14F, 1F),
                    PartPose.offset((float) Math.cos(a) * 5F, -2F, (float) Math.sin(a) * 5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CloudJelly jelly, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = 1F + Mth.sin(ageInTicks * 0.08F) * 0.05F;
        sac.xScale = pulse;
        sac.zScale = pulse;
        sac.yScale = 2F - pulse;
        for (int i = 0; i < 6; i++) {
            tentacles[i].xRot = Mth.sin(ageInTicks * 0.06F + i) * 0.18F;
            tentacles[i].zRot = Mth.cos(ageInTicks * 0.05F + i * 1.7F) * 0.18F;
        }
    }
}
