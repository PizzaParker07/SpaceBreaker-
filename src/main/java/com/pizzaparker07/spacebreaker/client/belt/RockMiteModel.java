package com.pizzaparker07.spacebreaker.client.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.entity.RockMite;
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
 * Rock Mite: a broad, low carapace with a raised rear hump crusted in two pale salt crystals, a small blunt head with two
 * forward palps, and eight short legs splayed out to the sides. UVs: tools/textures/modules/belt.py.
 */
public class RockMiteModel extends HierarchicalModel<RockMite> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rock_mite"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart palpLeft;
    private final ModelPart palpRight;
    private final ModelPart[] legs = new ModelPart[8];

    public RockMiteModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.palpLeft = head.getChild("palp_left");
        this.palpRight = head.getChild("palp_right");
        for (int i = 0; i < 8; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -5F, 8F, 4F, 10F),
                PartPose.offset(0F, 21F, 0F));
        body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 14).addBox(-3F, -2F, -2.5F, 6F, 2F, 5F),
                PartPose.offset(0F, -4F, 1.5F));
        body.addOrReplaceChild("crystal_big", CubeListBuilder.create().texOffs(44, 6).addBox(-1F, -3F, -1F, 2F, 3F, 2F),
                PartPose.offsetAndRotation(1F, -5.5F, 2F, 0.2F, 0.6F, 0.25F));
        body.addOrReplaceChild("crystal_small", CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offsetAndRotation(-1.5F, -5.5F, 0.5F, -0.15F, 0.3F, -0.35F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-2.5F, -2F, -3F, 5F, 3F, 3F),
                PartPose.offset(0F, -1F, -5F));
        head.addOrReplaceChild("palp_left", CubeListBuilder.create().texOffs(36, 6).addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(1.5F, 0.5F, -2.5F, 0.35F, -0.25F, 0F));
        head.addOrReplaceChild("palp_right", CubeListBuilder.create().texOffs(36, 6).mirror().addBox(-0.5F, -0.5F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(-1.5F, 0.5F, -2.5F, 0.35F, 0.25F, 0F));
        float[] z = {-3.5F, -1.2F, 1.2F, 3.5F};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(22, 14).addBox(0F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offset(3.5F, 21F, z[i]));
            root.addOrReplaceChild("leg" + (i + 4), CubeListBuilder.create().texOffs(22, 14).mirror().addBox(-5F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offset(-3.5F, 21F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RockMite mite, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean hidden = mite.isHidden();
        float sink = hidden ? 3F : 0F;
        body.y = 21F + sink;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        // palps feel the ground while it walks or grazes
        float feel = hidden ? 0F : Mth.sin(ageInTicks * 0.3F) * 0.12F;
        palpLeft.xRot = 0.35F + feel;
        palpRight.xRot = 0.35F - feel;
        float[] fan = {0.55F, 0.18F, -0.18F, -0.55F};
        for (int i = 0; i < 4; i++) {
            float wave = Mth.cos(limbSwing * 1.6F + i * 1.3F) * 0.5F * limbSwingAmount;
            float lift = Math.max(0, Mth.sin(limbSwing * 1.6F + i * 1.3F)) * 0.4F * limbSwingAmount;
            legs[i].y = legs[i + 4].y = 21F + sink * 0.8F;
            legs[i].yRot = fan[i] + wave;
            legs[i + 4].yRot = -fan[i] + wave;
            legs[i].zRot = hidden ? 0.05F : 0.5F - lift;
            legs[i + 4].zRot = hidden ? -0.05F : -0.5F + lift;
        }
    }
}
