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
 * Methane Skater: a strider of Titan's lakes. A three-part body held just above the liquid - a small head with two big
 * glossy eyes and short feelers, a thorax, and a long abdomen with a pale blue stripe - on thin jointed legs: a short
 * grasping front pair, and very long middle and rear pairs splayed wide with the knees raised above the body, each ending
 * in a glossy pad that dimples the surface. Skating, the middle and rear pairs row together like oars.
 *
 * <p>Each long leg is a hip (turns only about the vertical, to splay and row), a femur rising to the knee, a tibia falling
 * to the surface and a pad; femur, tibia and pad turn only about the hip's own z axis, so the pad's angle can cancel the
 * other two and stay flat on the surface. UVs: tools/textures/modules/saturn.py.
 */
public class MethaneSkaterModel extends HierarchicalModel<MethaneSkater> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("methane_skater"), "main");
    /** Per leg pair (front, middle, rear): hip z, femur and tibia length, femur rise, tibia bend, rest splay, uv rows. */
    private static final float[][] PAIRS = {
            {-3.5F, 3F, 3F, -0.2F, 0.975F, 1.1F},
            {-1F, 8F, 9F, -0.6F, 1.33F, 0.2F},
            {2F, 7F, 8F, -0.6F, 1.30F, -0.6F}};
    private static final int[][] UV = {{40, 24, 40, 24}, {0, 24, 0, 26}, {20, 24, 20, 26}};
    private static final float HIP_Y = 22.5F;
    private final ModelPart root;
    private final ModelPart body;
    /** [pair][side: 0 right (+x), 1 left] */
    private final ModelPart[][] hips = new ModelPart[3][2];
    private final ModelPart[][] femurs = new ModelPart[3][2];
    private final ModelPart[][] tibias = new ModelPart[3][2];
    private final ModelPart[][] pads = new ModelPart[3][2];

    public MethaneSkaterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int p = 0; p < 3; p++) {
            for (int side = 0; side < 2; side++) {
                hips[p][side] = root.getChild("hip" + p + side);
                femurs[p][side] = hips[p][side].getChild("femur");
                tibias[p][side] = femurs[p][side].getChild("tibia");
                pads[p][side] = p == 0 ? null : tibias[p][side].getChild("pad");
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // thorax at the pivot; head in front, abdomen behind, all close to the surface (the body's underside ~1 px up)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 6).addBox(-2F, -2F, -2F, 4F, 2F, 4F),
                PartPose.offset(0F, 23F, -1.5F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2F, -3F, 3F, 2F, 3F),
                PartPose.offset(0F, 0F, -2F));
        head.addOrReplaceChild("eye_right", CubeListBuilder.create().texOffs(12, 0).addBox(-1F, -2F, -1F, 2F, 2F, 2F),
                PartPose.offset(1.6F, -0.6F, -1.8F));
        head.addOrReplaceChild("eye_left", CubeListBuilder.create().texOffs(12, 0).mirror().addBox(-1F, -2F, -1F, 2F, 2F, 2F),
                PartPose.offset(-1.6F, -0.6F, -1.8F));
        head.addOrReplaceChild("feeler_right", CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(0.8F, -1.6F, -2.8F, 0.5F, -0.35F, 0F));
        head.addOrReplaceChild("feeler_left", CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(-0.8F, -1.6F, -2.8F, 0.5F, 0.35F, 0F));
        PartDefinition abdomen = body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -1.8F, 0F, 3F, 2F, 9F),
                PartPose.offsetAndRotation(0F, 0F, 2F, -0.05F, 0F, 0F));
        abdomen.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(28, 0).addBox(-1F, -1.3F, 0F, 2F, 1F, 2F), PartPose.offset(0F, 0F, 9F));
        for (int p = 0; p < 3; p++) {
            float[] g = PAIRS[p];
            for (int side = 0; side < 2; side++) {
                float s = side == 0 ? 1F : -1F;
                PartDefinition hip = root.addOrReplaceChild("hip" + p + side, CubeListBuilder.create(),
                        PartPose.offsetAndRotation(s * (p == 0 ? 1.5F : 2F), HIP_Y, g[0] - 1.5F, 0F, s * g[5], 0F));
                CubeListBuilder fc = CubeListBuilder.create().texOffs(UV[p][0], UV[p][1]);
                CubeListBuilder tc = CubeListBuilder.create().texOffs(UV[p][2], UV[p][3]);
                if (side == 1) {
                    fc.mirror();
                    tc.mirror();
                }
                PartDefinition femur = hip.addOrReplaceChild("femur", fc.addBox(side == 0 ? 0F : -g[1], -0.5F, -0.5F, g[1], 1F, 1F),
                        PartPose.rotation(0F, 0F, s * g[3]));
                PartDefinition tibia = femur.addOrReplaceChild("tibia", tc.addBox(side == 0 ? 0F : -g[2], -0.5F, -0.5F, g[2], 1F, 1F),
                        PartPose.offsetAndRotation(s * g[1], 0F, 0F, 0F, 0F, s * g[4]));
                if (p > 0) {
                    tibia.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, 0F, -1.5F, 3F, 1F, 3F),
                            PartPose.offsetAndRotation(s * g[2], 0F, 0F, 0F, 0F, -s * (g[3] + g[4])));
                }
            }
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MethaneSkater s, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.15F;
        // rowing: the middle and rear pairs sweep back together (both sides at once), lifting a little on the return
        float phase = limbSwing * 0.55F;
        float sweep = Mth.sin(phase) * 0.45F * limbSwingAmount;
        float lift = Math.max(0F, Mth.cos(phase)) * 0.25F * limbSwingAmount;
        float bob = Mth.sin(ageInTicks * 0.1F) * 0.02F;
        for (int p = 0; p < 3; p++) {
            float[] g = PAIRS[p];
            for (int side = 0; side < 2; side++) {
                float sg = side == 0 ? 1F : -1F;
                float row = p == 0 ? Mth.sin(ageInTicks * 0.12F) * 0.08F : (p == 1 ? sweep : sweep * 0.7F);
                hips[p][side].yRot = sg * (g[5] - row);
                float femurZ = sg * (g[3] - (p == 0 ? 0F : lift) + bob);
                float tibiaZ = sg * (g[4] - (p == 0 ? 0F : lift * 0.5F));
                femurs[p][side].zRot = femurZ;
                tibias[p][side].zRot = tibiaZ;
                if (pads[p][side] != null) {
                    pads[p][side].zRot = -(femurZ + tibiaZ);
                }
            }
        }
    }
}
