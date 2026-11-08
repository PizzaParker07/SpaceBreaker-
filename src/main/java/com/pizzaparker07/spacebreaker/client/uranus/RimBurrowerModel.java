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
 * Rim Burrower: a hunched digger of Oberon's crater rims - a broad body rising to a hump over the hips with a banded
 * ridge along the spine, a flat wide head ending in a pale shovel snout, big fore limbs with broad three-tined pale
 * claws, and short hind legs. Walking, the claws reach and pull; buried, the whole model sinks so only the hump and the
 * ridge show. UVs: tools/textures/modules/uranus.py.
 */
public class RimBurrowerModel extends HierarchicalModel<RimBurrower> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rim_burrower"), "main");
    private final ModelPart root;
    private final ModelPart all;
    private final ModelPart head;
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] hind = new ModelPart[2];

    public RimBurrowerModel(ModelPart root) {
        this.root = root;
        this.all = root.getChild("all");
        this.head = all.getChild("head");
        for (int i = 0; i < 2; i++) {
            arms[i] = all.getChild("arm" + i);
            claws[i] = arms[i].getChild("claw");
            hind[i] = all.getChild("hind" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition all = mesh.getRoot().addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -5F, -4F, 7F, 4F, 8F)
                        .texOffs(30, 0).addBox(-2.5F, -7F, -0.5F, 5F, 2F, 5F),
                PartPose.rotation(0.1F, 0F, 0F));
        // the banded ridge down the spine, climbing onto the hump
        float[][] ridge = {{-6F, -3F}, {-6F, -1F}, {-8F, 1F}, {-8F, 3F}};
        for (int i = 0; i < ridge.length; i++) {
            body.addOrReplaceChild("ridge" + i, CubeListBuilder.create().texOffs(50, 0).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                    PartPose.offset(0F, ridge[i][0] + 0.5F, ridge[i][1]));
        }
        PartDefinition head = all.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-3F, -1F, -4F, 6F, 2F, 4F)
                .texOffs(20, 12).addBox(-3F, 0F, -6F, 6F, 1F, 2F), PartPose.offset(0F, -3F, -3.6F));
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? 1F : -1F;
            CubeListBuilder arm = CubeListBuilder.create().texOffs(36, 12);
            CubeListBuilder pad = CubeListBuilder.create().texOffs(44, 12);
            CubeListBuilder leg = CubeListBuilder.create().texOffs(20, 16);
            if (side < 0) {
                arm.mirror();
                pad.mirror();
                leg.mirror();
            }
            PartDefinition a = all.addOrReplaceChild("arm" + i, arm.addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offset(side * 3.2F, -3F, -2.5F));
            PartDefinition c = a.addOrReplaceChild("claw", pad.addBox(-1.5F, -1F, -2.5F, 3F, 1F, 3F), PartPose.offset(side * 0.3F, 3F, 0F));
            for (int t = -1; t <= 1; t++) {
                c.addOrReplaceChild("tine" + (t + 1), CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -1F, -2F, 1F, 1F, 2F),
                        PartPose.offsetAndRotation(t * 1F, 0F, -2.5F, 0.15F, t * 0.15F, 0F));
            }
            all.addOrReplaceChild("hind" + i, leg.addBox(-1F, 0F, -1F, 2F, 2F, 2F), PartPose.offset(side * 2.5F, -2F, 3F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RimBurrower b, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean buried = b.isBuried();
        all.y = buried ? 29F : 24F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F;
        // the claws reach forward and pull back in turn, the hind legs push against them
        float swing = Mth.cos(limbSwing * 1.3F) * 0.8F * limbSwingAmount;
        for (int i = 0; i < 2; i++) {
            float sign = i == 0 ? 1F : -1F;
            arms[i].xRot = -swing * sign;
            claws[i].xRot = Math.max(0F, swing * sign) * 0.5F + (buried ? Mth.sin(ageInTicks * 0.9F + i) * 0.4F : 0F);
            hind[i].xRot = swing * sign;
        }
    }
}
