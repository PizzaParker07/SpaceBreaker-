package com.pizzaparker07.spacebreaker.client.venus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.entity.AcidRay;
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
 * Acid Ray: a diamond-shaped glider. The body tapers to a blunt snout and a narrow rump, with a raised eye hump and a
 * spine ridge on the back; two short horn lobes curl up and in from the snout. Each wing sweeps back in three thin
 * panels that ripple one after another, and a two-part whip tail ends in a flat barb. UVs: tools/textures/modules/venus.py.
 */
public class AcidRayModel extends HierarchicalModel<AcidRay> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("acid_ray"), "main");
    private final ModelPart root;
    private final ModelPart[][] wings = new ModelPart[2][3];
    private final ModelPart[] horns = new ModelPart[2];
    private final ModelPart tail;
    private final ModelPart tail2;

    public AcidRayModel(ModelPart root) {
        this.root = root;
        for (int s = 0; s < 2; s++) {
            ModelPart w = root.getChild(s == 0 ? "wing_left" : "wing_right");
            wings[s][0] = w;
            wings[s][1] = w.getChild("mid");
            wings[s][2] = wings[s][1].getChild("tip");
            horns[s] = root.getChild(s == 0 ? "horn_left" : "horn_right");
        }
        this.tail = root.getChild("tail");
        this.tail2 = tail.getChild("whip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -2F, -6F, 6F, 2F, 12F)
                .texOffs(40, 0).addBox(-2F, -1.5F, -8F, 4F, 1F, 2F)
                .texOffs(40, 4).addBox(-2F, -1.5F, 6F, 4F, 1F, 2F)
                .texOffs(56, 0).addBox(-2.5F, -3F, -5.5F, 5F, 1F, 3F)
                .texOffs(56, 6).addBox(-1F, -3F, -2.5F, 2F, 1F, 8F), PartPose.offset(0F, 22F, 0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            CubeListBuilder a = CubeListBuilder.create().texOffs(0, 16);
            CubeListBuilder b = CubeListBuilder.create().texOffs(0, 30);
            CubeListBuilder c = CubeListBuilder.create().texOffs(0, 40);
            CubeListBuilder h = CubeListBuilder.create().texOffs(40, 8);
            if (s == 1) {
                a.mirror(); b.mirror(); c.mirror(); h.mirror();
            }
            // each panel is shallower and set further back than the one inside it: the wing sweeps back to a point
            PartDefinition w = root.addOrReplaceChild(s == 0 ? "wing_left" : "wing_right",
                    a.addBox(s == 0 ? 0F : -4F, -0.5F, -6F, 4F, 1F, 12F), PartPose.offset(3F * side, 21F, 0F));
            PartDefinition mid = w.addOrReplaceChild("mid", b.addBox(s == 0 ? 0F : -4F, -0.5F, -4.5F, 4F, 1F, 8F), PartPose.offset(4F * side, 0F, 1.5F));
            mid.addOrReplaceChild("tip", c.addBox(s == 0 ? 0F : -3F, -0.5F, -2.5F, 3F, 1F, 4F), PartPose.offset(4F * side, 0F, 1.5F));
            root.addOrReplaceChild(s == 0 ? "horn_left" : "horn_right", h.addBox(-0.5F, -0.5F, -2F, 1F, 1F, 2F),
                    PartPose.offsetAndRotation(1.5F * side, 21F, -7.5F, -0.3F, 0.3F * side, 0F));
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(80, 0).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 7F),
                PartPose.offset(0F, 21.5F, 7.5F));
        PartDefinition whip = tail.addOrReplaceChild("whip", CubeListBuilder.create().texOffs(80, 8).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 6F),
                PartPose.offset(0F, 0F, 6.8F));
        whip.addOrReplaceChild("barb", CubeListBuilder.create().texOffs(80, 16).addBox(-1F, -0.5F, -1F, 2F, 1F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 6.3F, 0F, Mth.PI / 4F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AcidRay ray, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = ray.getTarget() != null ? 0.32F : 0.14F;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            // a wave runs out along each wing: every panel lags the one inside it
            for (int k = 0; k < 3; k++) {
                wings[s][k].zRot = -Mth.sin(ageInTicks * speed - k * 0.8F) * (0.3F + k * 0.08F) * side;
            }
            horns[s].xRot = -0.3F + Mth.sin(ageInTicks * speed * 0.7F) * 0.12F;
        }
        tail.yRot = Mth.sin(ageInTicks * 0.1F) * 0.2F;
        tail.xRot = Mth.sin(ageInTicks * speed - 1.6F) * 0.12F;
        tail2.yRot = Mth.sin(ageInTicks * 0.1F - 0.8F) * 0.3F;
        tail2.xRot = Mth.sin(ageInTicks * speed - 2.4F) * 0.15F;
    }
}
