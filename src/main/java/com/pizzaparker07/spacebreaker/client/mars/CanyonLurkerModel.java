package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.CanyonLurker;
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
 * Canyon Lurker: a low, stepped carapace layered like the canyon walls, rock knobs on the crest, a short head with
 * amber eyes and two mandibles, two jointed arms ending in two-fingered pincers, six legs bent high at the knee.
 * Folded (hidden) it pulls everything under the carapace and sinks into a rock-like lump. UVs: mars_creatures.py.
 */
public class CanyonLurkerModel extends HierarchicalModel<CanyonLurker> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("canyon_lurker"), "main");
    private static final float BODY_Y = 21F;
    private static final float[] LEG_Z = {-4.5F, 0F, 4.5F};
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] mandibles = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[6];

    public CanyonLurkerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        mandibles[0] = head.getChild("mandible_left");
        mandibles[1] = head.getChild("mandible_right");
        arms[0] = root.getChild("arm_left");
        arms[1] = root.getChild("arm_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7F, -3F, -8F, 14F, 3F, 16F)
                .texOffs(0, 20).addBox(-5.5F, -5F, -6F, 11F, 2F, 12F)
                .texOffs(0, 35).addBox(-3.5F, -7F, -4.5F, 7F, 2F, 8F), PartPose.offset(0F, BODY_Y, 0F));
        float[][] knobs = {{-2.5F, -8.5F, -3F}, {1.2F, -8.2F, 0.5F}, {-1F, -8.4F, 2.5F}, {4F, -6.4F, -3.5F}, {-5F, -6.3F, 3F}};
        for (int i = 0; i < knobs.length; i++) {
            body.addOrReplaceChild("knob" + i, CubeListBuilder.create().texOffs(32, 35).addBox(-1F, 0F, -1F, 2F, 2F, 2F),
                    PartPose.offsetAndRotation(knobs[i][0], knobs[i][1], knobs[i][2], 0.2F * i, 0.6F * i, -0.15F * i));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-3F, -1.5F, -4F, 6F, 3F, 4F),
                PartPose.offset(0F, 19.5F, -7.5F));
        head.addOrReplaceChild("mandible_left", CubeListBuilder.create().texOffs(64, 8).addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offset(1.8F, 1F, -3.8F));
        head.addOrReplaceChild("mandible_right", CubeListBuilder.create().texOffs(64, 8).mirror().addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offset(-1.8F, 1F, -3.8F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            CubeListBuilder upper = CubeListBuilder.create().texOffs(64, 13);
            CubeListBuilder fore = CubeListBuilder.create().texOffs(64, 22);
            CubeListBuilder palm = CubeListBuilder.create().texOffs(84, 0);
            CubeListBuilder finger = CubeListBuilder.create().texOffs(84, 7);
            CubeListBuilder lower = CubeListBuilder.create().texOffs(84, 7);
            if (s == 1) {
                upper.mirror(); fore.mirror(); palm.mirror(); finger.mirror(); lower.mirror();
            }
            PartDefinition arm = root.addOrReplaceChild(s == 0 ? "arm_left" : "arm_right", upper.addBox(-1F, -1F, -6F, 2F, 2F, 6F),
                    PartPose.offset(5F * side, 19.5F, -6.5F));
            PartDefinition forearm = arm.addOrReplaceChild("forearm", fore.addBox(-1F, -1F, -5F, 2F, 2F, 5F), PartPose.offset(0F, 0F, -5.5F));
            PartDefinition hand = forearm.addOrReplaceChild("palm", palm.addBox(-1.5F, -1.5F, -3F, 3F, 3F, 3F), PartPose.offset(0F, 0F, -4.5F));
            hand.addOrReplaceChild("finger", finger.addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, -0.8F, -2.8F, -0.15F, 0F, 0F));
            hand.addOrReplaceChild("thumb", lower.addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F), PartPose.offset(0F, 1F, -2.8F));
            for (int i = 0; i < 3; i++) {
                CubeListBuilder femur = CubeListBuilder.create().texOffs(96, 0);
                CubeListBuilder tibia = CubeListBuilder.create().texOffs(96, 3);
                if (s == 1) {
                    femur.mirror(); tibia.mirror();
                }
                PartDefinition leg = root.addOrReplaceChild("leg" + (i + 3 * s),
                        femur.addBox(s == 0 ? 0F : -5F, -0.5F, -0.5F, 5F, 1F, 1F), PartPose.offset(6.5F * side, 20.5F, LEG_Z[i]));
                leg.addOrReplaceChild("tibia", tibia.addBox(-0.5F, 0F, -0.5F, 1F, 7F, 1F), PartPose.offset(5F * side, 0F, 0F));
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CanyonLurker lurker, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean hidden = lurker.isHidden();
        float sink = hidden ? 4F : 0F;
        body.y = BODY_Y + sink;
        head.y = 19.5F + sink + (hidden ? 0.5F : 0F);
        head.z = hidden ? -5.5F : -7.5F;
        head.yRot = hidden ? 0F : netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = hidden ? 0F : headPitch * Mth.DEG_TO_RAD * 0.3F;
        float chew = hidden ? 0F : Mth.sin(ageInTicks * 0.25F) * 0.15F;
        mandibles[0].yRot = 0.35F + chew;
        mandibles[1].yRot = -0.35F - chew;
        float snap = hidden ? 0F : Mth.sin(ageInTicks * 0.3F) * 0.08F;
        float attack = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0F;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            ModelPart arm = arms[s];
            ModelPart forearm = arm.getChild("forearm");
            ModelPart palm = forearm.getChild("palm");
            arm.y = 19.5F + sink;
            arm.z = hidden ? -5F : -6.5F;
            if (hidden) {
                arm.yRot = -1.5F * side;
                arm.xRot = 0.1F;
                forearm.yRot = 2.8F * side;
                palm.yRot = 0F;
                palm.getChild("thumb").xRot = 0F;
            } else {
                // the upper arm reaches out and forward, the forearm folds back in so the pincers sit before the head;
                // a strike straightens the elbow and throws both pincers forward
                arm.yRot = (-0.55F + attack * 0.35F) * side;
                arm.xRot = -0.2F - attack * 0.4F;
                forearm.yRot = (1.15F - attack * 0.9F + snap) * side;
                palm.yRot = 0.25F * side;
                palm.getChild("thumb").xRot = 0.2F + attack * 0.5F + Math.abs(snap) * 2F;
            }
        }
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1F : -1F;
            for (int i = 0; i < 3; i++) {
                ModelPart leg = legs[i + 3 * s];
                ModelPart tibia = leg.getChild("tibia");
                float phase = (i + s) % 2 == 0 ? 0 : Mth.PI;
                float swing = Mth.cos(limbSwing * 1.5F + phase) * 0.5F * limbSwingAmount;
                float lift = Math.max(0F, Mth.sin(limbSwing * 1.5F + phase)) * 0.3F * limbSwingAmount;
                leg.y = 20.5F + sink * 0.75F;
                leg.yRot = (i - 1) * 0.45F * side * -1F + swing;
                tibia.visible = !hidden;
                leg.zRot = hidden ? 0.1F * side : (-0.7F - lift) * side;
                tibia.zRot = (0.95F + lift) * side;
            }
        }
    }
}
