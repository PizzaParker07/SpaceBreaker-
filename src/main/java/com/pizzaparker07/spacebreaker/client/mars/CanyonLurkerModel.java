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
 * Canyon Lurker: a flat, wide carapace, a low head with amber eyes, two long arms ending in heavy claws, six legs.
 * Folded (hidden) it pulls everything under the carapace and sinks into a rock-like lump. UVs: mars_creatures.py.
 */
public class CanyonLurkerModel extends HierarchicalModel<CanyonLurker> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("canyon_lurker"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart[] legs = new ModelPart[6];

    public CanyonLurkerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.armL = root.getChild("arm_left");
        this.armR = root.getChild("arm_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -5F, -7F, 12F, 5F, 14F), PartPose.offset(0F, 22F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-3F, -2F, -4F, 6F, 4F, 4F), PartPose.offset(0F, 20F, -6.5F));
        PartDefinition l = root.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(24, 20).addBox(-1F, -1F, -8F, 2F, 2F, 8F),
                PartPose.offset(4.5F, 19.5F, -5F));
        l.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(44, 20).addBox(-1.5F, -1.5F, -4F, 3F, 3F, 4F), PartPose.offset(0F, 0F, -8F));
        PartDefinition r = root.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(24, 20).mirror().addBox(-1F, -1F, -8F, 2F, 2F, 8F),
                PartPose.offset(-4.5F, 19.5F, -5F));
        r.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(44, 20).mirror().addBox(-1.5F, -1.5F, -4F, 3F, 3F, 4F), PartPose.offset(0F, 0F, -8F));
        float[] z = {-3F, 1F, 5F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 30).addBox(0F, -0.5F, -0.5F, 6F, 1F, 1F), PartPose.offset(5.5F, 21F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-6F, -0.5F, -0.5F, 6F, 1F, 1F),
                    PartPose.offset(-5.5F, 21F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CanyonLurker lurker, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean hidden = lurker.isHidden();
        float sink = hidden ? 2F : 0F;
        body.y = 22F + sink;
        head.y = 20F + sink + (hidden ? 1F : 0F);
        head.z = hidden ? -5F : -6.5F;
        head.yRot = hidden ? 0F : netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        float snap = hidden ? 0F : Mth.sin(ageInTicks * 0.3F) * 0.08F;
        float attack = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0F;
        armL.y = armR.y = 19.5F + sink;
        armL.yRot = hidden ? 1.25F : 0.25F + snap - attack * 0.4F;
        armR.yRot = hidden ? -1.25F : -0.25F - snap + attack * 0.4F;
        armL.xRot = armR.xRot = hidden ? 0.15F : -0.15F - attack * 0.5F;
        float[] spread = {-0.4F, 0F, 0.4F};
        for (int i = 0; i < 3; i++) {
            float phase = i % 2 == 0 ? 0 : Mth.PI;
            float swing = Mth.cos(limbSwing * 1.5F + phase) * 0.5F * limbSwingAmount;
            legs[i].y = legs[i + 3].y = 21F + sink * 0.5F;
            legs[i].yRot = spread[i] + swing;
            legs[i + 3].yRot = -spread[i] + swing;
            legs[i].zRot = hidden ? -0.1F : 0.45F;
            legs[i + 3].zRot = hidden ? 0.1F : -0.45F;
        }
    }
}
