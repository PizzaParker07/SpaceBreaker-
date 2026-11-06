package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.RegolithTick;
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
 * Regolith Tick: flat oval body with a hard dorsal shield, a small head with mouthparts and eight thin legs.
 * UV layout matches tools/textures/modules/moon_creatures.py.
 */
public class RegolithTickModel<T extends RegolithTick> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("regolith_tick"), "main");
    private static final String[] LEGS = {"right_leg_0", "right_leg_1", "right_leg_2", "right_leg_3", "left_leg_0", "left_leg_1", "left_leg_2", "left_leg_3"};

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[8];

    public RegolithTickModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        for (int i = 0; i < 8; i++) {
            legs[i] = root.getChild(LEGS[i]);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -3F, -5F, 8F, 3F, 10F), PartPose.offset(0F, 21F, 1F));
        body.addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-3F, -4F, -5F, 6F, 1F, 5F), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(22, 13).addBox(-2F, -1F, -3F, 4F, 2F, 3F), PartPose.offset(0F, 19.5F, -4F));
        head.addOrReplaceChild("mouth", CubeListBuilder.create()
                .texOffs(36, 13).addBox(-1.5F, 0F, -5F, 1F, 1F, 2F)
                .texOffs(36, 13).mirror().addBox(0.5F, 0F, -5F, 1F, 1F, 2F), PartPose.ZERO);
        CubeListBuilder right = CubeListBuilder.create().texOffs(0, 20).addBox(-8F, -0.5F, -0.5F, 8F, 1F, 1F);
        CubeListBuilder left = CubeListBuilder.create().texOffs(0, 20).mirror().addBox(0F, -0.5F, -0.5F, 8F, 1F, 1F);
        float[] z = {-2.5F, -0.5F, 1.5F, 3.5F};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild(LEGS[i], right, PartPose.offset(-3.5F, 20F, z[i]));
            root.addOrReplaceChild(LEGS[i + 4], left, PartPose.offset(3.5F, 20F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T tick, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        boolean buried = tick.isBurrowed();
        // legs splay sideways; front pair angled forward, rear pair backward
        float[] spread = {-0.55F, -0.18F, 0.18F, 0.55F};
        float droop = buried ? 0.05F : 0.5F;
        for (int i = 0; i < 4; i++) {
            float phase = i * Mth.HALF_PI;
            float swing = Mth.cos(limbSwing * 1.3F + phase) * 0.45F * limbSwingAmount;
            float lift = Math.abs(Mth.sin(limbSwing * 0.65F + phase)) * 0.4F * limbSwingAmount;
            legs[i].yRot = spread[i] + swing;
            legs[i].zRot = -droop - lift;
            legs[i + 4].yRot = -spread[i] - swing;
            legs[i + 4].zRot = droop + lift;
        }
        // breathing / feeling the ground
        float bob = buried ? 0F : Mth.sin(ageInTicks * 0.15F) * 0.15F;
        body.y = 21F + bob;
        head.y = 19.5F + bob;
        // tuck the legs when airborne in a hop
        if (!tick.onGround() && !buried) {
            for (int i = 0; i < 8; i++) {
                legs[i].zRot *= 1.6F;
            }
        }
    }
}
