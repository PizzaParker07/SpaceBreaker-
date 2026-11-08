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

/**
 * Rust Beetle: two domed wing cases (each stepping up in three tiers from a broad lower shell to a narrow crown, a dark seam between them) over a
 * squat chitin body, a lower thorax shield, a small head with curved two-part mandibles and elbowed antennae, six legs
 * bent high at the knee. UVs: mars_creatures.py.
 */
public class RustBeetleModel extends HierarchicalModel<RustBeetle> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rust_beetle"), "main");
    private static final float[] LEG_Z = {-3F, 0F, 3F};
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
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -4.5F, 6F, 3F, 9F), PartPose.offset(0F, 21F, 0.5F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            CubeListBuilder b = CubeListBuilder.create().texOffs(0, 14);
            if (s == 1) {
                b.mirror();
            }
            // the crown hugs the inner edge, so the two crowns meet along the seam and the dome steps down only at the flanks
            b.addBox(-2F, -2F, -5F, 4F, 2F, 10F).texOffs(0, 28).addBox(s == 0 ? -2F : -1F, -3F, -4.8F, 3F, 1F, 9F)
                    .texOffs(0, 40).addBox(s == 0 ? -2F : 0F, -4F, -4.2F, 2F, 1F, 7F);
            // the cases sit edge to edge along the back and roll a little outward
            root.addOrReplaceChild(s == 0 ? "case_left" : "case_right", b, PartPose.offsetAndRotation(2F * side, 18.6F, 1.2F, 0.05F, 0F, 0.08F * side));
        }
        root.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(30, 0).addBox(-3F, -2F, -4F, 6F, 2F, 4F)
                .texOffs(46, 20).addBox(-2F, -3F, -3.5F, 4F, 1F, 3F), PartPose.offsetAndRotation(0F, 19.2F, -3.6F, 0.12F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 8).addBox(-2F, -1.5F, -3F, 4F, 2F, 3F),
                PartPose.offset(0F, 19.8F, -7.2F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            CubeListBuilder base = CubeListBuilder.create().texOffs(44, 8);
            CubeListBuilder tip = CubeListBuilder.create().texOffs(44, 12);
            CubeListBuilder ant = CubeListBuilder.create().texOffs(50, 8);
            CubeListBuilder ant2 = CubeListBuilder.create().texOffs(50, 8);
            if (s == 1) {
                base.mirror(); tip.mirror(); ant.mirror(); ant2.mirror();
            }
            PartDefinition jaw = head.addOrReplaceChild(s == 0 ? "jaw_left" : "jaw_right", base.addBox(-0.5F, -0.5F, -2F, 1F, 1F, 2F),
                    PartPose.offset(1.2F * side, 0F, -2.8F));
            jaw.addOrReplaceChild("tip", tip.addBox(-0.5F, -0.5F, -2F, 1F, 1F, 2F), PartPose.offsetAndRotation(0F, 0F, -1.8F, 0F, 0.7F * side, 0F));
            PartDefinition a = head.addOrReplaceChild(s == 0 ? "antenna_left" : "antenna_right", ant.addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                    PartPose.offsetAndRotation(1.1F * side, -1.2F, -2.6F, -0.7F, -0.45F * side, 0F));
            a.addOrReplaceChild("whip", ant2.addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 0F, -2.8F, 1.0F, -0.3F * side, 0F));
            for (int i = 0; i < 3; i++) {
                CubeListBuilder femur = CubeListBuilder.create().texOffs(28, 28);
                CubeListBuilder tibia = CubeListBuilder.create().texOffs(28, 31);
                if (s == 1) {
                    femur.mirror(); tibia.mirror();
                }
                PartDefinition leg = root.addOrReplaceChild("leg" + (i + 3 * s), femur.addBox(s == 0 ? 0F : -4F, -0.5F, -0.5F, 4F, 1F, 1F),
                        PartPose.offset(2.8F * side, 20F, LEG_Z[i]));
                leg.addOrReplaceChild("tibia", tibia.addBox(-0.5F, 0F, -0.5F, 1F, 6F, 1F), PartPose.offset(4F * side, 0F, 0F));
            }
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
        jawL.yRot = -0.3F - Math.abs(chew) * 0.35F;
        jawR.yRot = 0.3F + Math.abs(chew) * 0.35F;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            for (int i = 0; i < 3; i++) {
                // alternating tripod gait
                float phase = (i + s) % 2 == 0 ? 0 : Mth.PI;
                float swing = Mth.cos(limbSwing * 1.6F + phase) * 0.5F * limbSwingAmount;
                float lift = Math.max(0, Mth.sin(limbSwing * 1.6F + phase)) * 0.35F * limbSwingAmount;
                ModelPart leg = legs[i + 3 * s];
                leg.yRot = -(i - 1) * 0.45F * side + swing;
                leg.zRot = (-0.5F - lift) * side;
                leg.getChild("tibia").zRot = (0.85F + lift) * side;
            }
        }
    }
}
