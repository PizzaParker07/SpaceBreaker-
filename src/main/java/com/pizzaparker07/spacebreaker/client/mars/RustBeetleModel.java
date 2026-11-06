package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.RustBeetle;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Rust Beetle: domed wing cases over a squat body, a small head with mandibles, six splayed legs. UVs: mars_creatures.py. */
public class RustBeetleModel extends HierarchicalModel<RustBeetle> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rust_beetle"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jawL;
    private final ModelPart jawR;
    private final ModelPart[] legs = new ModelPart[6];

    public RustBeetleModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jawL = head.getChild("jaw_left");
        this.jawR = head.getChild("jaw_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -5F, 8F, 4F, 10F),
                PartPose.offset(0F, 22F, 0.5F));
        body.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -2F, -4.5F, 7F, 2F, 9F), PartPose.offset(0F, -4F, 0.5F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-2F, -1.5F, -3F, 4F, 3F, 3F),
                PartPose.offset(0F, 20F, -4.5F));
        head.addOrReplaceChild("jaw_left", CubeListBuilder.create().texOffs(36, 8).addBox(-0.5F, 0F, -2F, 1F, 1F, 2F), PartPose.offset(1F, 0.5F, -3F));
        head.addOrReplaceChild("jaw_right", CubeListBuilder.create().texOffs(36, 8).mirror().addBox(-0.5F, 0F, -2F, 1F, 1F, 2F),
                PartPose.offset(-1F, 0.5F, -3F));
        float[] z = {-3F, 0F, 3F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(14, 28).addBox(0F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offset(3.5F, 21F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(14, 28).mirror().addBox(-5F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offset(-3.5F, 21F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RustBeetle beetle, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        int eat = beetle.eatingTicks();
        float chew = eat > 0 ? Mth.sin(ageInTicks * 1.4F) : Mth.sin(ageInTicks * 0.2F) * 0.3F;
        head.xRot = eat > 0 ? 0.45F + chew * 0.05F : headPitch * Mth.DEG_TO_RAD * 0.3F;
        jawL.yRot = -0.25F - Math.abs(chew) * 0.4F;
        jawR.yRot = 0.25F + Math.abs(chew) * 0.4F;
        float[] spread = {-0.45F, 0F, 0.45F};
        for (int i = 0; i < 3; i++) {
            // alternating tripod gait
            float phase = (i % 2 == 0 ? 0 : Mth.PI);
            float swing = Mth.cos(limbSwing * 1.6F + phase) * 0.5F * limbSwingAmount;
            float lift = Math.max(0, Mth.sin(limbSwing * 1.6F + phase)) * 0.35F * limbSwingAmount;
            legs[i].yRot = spread[i] + swing;
            legs[i].zRot = 0.5F - lift;
            legs[i + 3].yRot = -spread[i] + swing;
            legs[i + 3].zRot = -0.5F + lift;
        }
    }
}
