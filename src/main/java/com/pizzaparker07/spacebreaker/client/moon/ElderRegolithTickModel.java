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
 * Elder Regolith Tick, the nest guardian: a high dome of overlapping breccia plates with KREEP crystals grown through
 * them, a broad armoured head with two heavy pincers around a dark orange mouth, and eight thick legs that rise to a
 * knee and come down to the ground, so it stands tall like a crab rather than sprawling like the young ticks.
 * UV layout matches tools/textures/modules/moon_creatures.py (elder_sheet).
 */
public class ElderRegolithTickModel<T extends RegolithTick> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("elder_regolith_tick"), "main");
    private static final float[] LEG_Z = {-3.5F, -1F, 1.5F, 4F};
    private static final float[] SPREAD = {-0.5F, -0.15F, 0.2F, 0.55F};
    private static final float THIGH = 0.85F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftPincer;
    private final ModelPart rightPincer;
    private final ModelPart[] thighs = new ModelPart[8];

    public ElderRegolithTickModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftPincer = head.getChild("left_pincer");
        this.rightPincer = head.getChild("right_pincer");
        for (int i = 0; i < 8; i++) {
            thighs[i] = root.getChild("leg_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5F, -2F, -5F, 10F, 4F, 11F), PartPose.offset(0F, 18F, 0F));
        body.addOrReplaceChild("plate_mid", CubeListBuilder.create().texOffs(0, 15).addBox(-5.5F, -5F, -2F, 11F, 3F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("plate_front", CubeListBuilder.create().texOffs(30, 15).addBox(-4.5F, -4F, -5.5F, 9F, 2F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("plate_rear", CubeListBuilder.create().texOffs(0, 22).addBox(-4.5F, -4F, 2F, 9F, 2F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("crystal_0", CubeListBuilder.create().texOffs(44, 8).addBox(-1F, -4F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(-2F, -4.5F, 0F, 0.15F, 0F, -0.35F));
        body.addOrReplaceChild("crystal_1", CubeListBuilder.create().texOffs(52, 8).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(2F, -4.5F, -0.5F, -0.2F, 0F, 0.4F));
        body.addOrReplaceChild("crystal_2", CubeListBuilder.create().texOffs(52, 8).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(0.5F, -3.5F, 3.5F, 0.5F, 0F, 0.1F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(26, 22).addBox(-3F, -1.5F, -3F, 6F, 3F, 3F)
                .texOffs(44, 22).addBox(-1.5F, -0.5F, -3.5F, 3F, 1F, 1F), PartPose.offset(0F, 17.5F, -5F));
        head.addOrReplaceChild("left_pincer", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-1F, -1F, -4F, 2F, 2F, 4F)
                .texOffs(56, 0).addBox(-1.5F, -0.5F, -6F, 1F, 1F, 2F), PartPose.offsetAndRotation(2.2F, 0.5F, -2.5F, 0F, 0.3F, 0F));
        head.addOrReplaceChild("right_pincer", CubeListBuilder.create()
                .texOffs(44, 0).mirror().addBox(-1F, -1F, -4F, 2F, 2F, 4F)
                .texOffs(56, 0).mirror().addBox(0.5F, -0.5F, -6F, 1F, 1F, 2F), PartPose.offsetAndRotation(-2.2F, 0.5F, -2.5F, 0F, -0.3F, 0F));
        for (int i = 0; i < 4; i++) {
            // left (+x): thigh out and up to a knee, shin straight down to the ground
            PartDefinition l = root.addOrReplaceChild("leg_" + (i + 4), CubeListBuilder.create().texOffs(0, 28).addBox(0F, -1F, -1F, 7F, 2F, 2F),
                    PartPose.offsetAndRotation(4.5F, 18F, LEG_Z[i], 0F, -SPREAD[i], -THIGH));
            l.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(18, 28).addBox(-1F, 0F, -1F, 2F, 5F, 2F)
                    .texOffs(26, 28).addBox(-0.5F, 5F, -0.5F, 1F, 6F, 1F), PartPose.offsetAndRotation(7F, 0F, 0F, 0F, 0F, THIGH - 0.15F));
            PartDefinition r = root.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(0, 28).mirror().addBox(-7F, -1F, -1F, 7F, 2F, 2F),
                    PartPose.offsetAndRotation(-4.5F, 18F, LEG_Z[i], 0F, SPREAD[i], THIGH));
            r.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(18, 28).mirror().addBox(-1F, 0F, -1F, 2F, 5F, 2F)
                    .texOffs(26, 28).mirror().addBox(-0.5F, 5F, -0.5F, 1F, 6F, 1F), PartPose.offsetAndRotation(-7F, 0F, 0F, 0F, 0F, -THIGH + 0.15F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T tick, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        boolean buried = tick.isBurrowed();
        // a slow, heavy gait: alternate legs lift at the thigh and swing a little
        for (int i = 0; i < 4; i++) {
            float phase = (i & 1) * Mth.PI;
            float swing = Mth.cos(limbSwing * 0.9F + phase) * 0.3F * limbSwingAmount;
            float lift = Math.max(0F, Mth.sin(limbSwing * 0.9F + phase)) * 0.35F * limbSwingAmount;
            float fold = buried ? 0.45F : 0F; // pulled in under the dust
            thighs[i + 4].yRot = -SPREAD[i] - swing;
            thighs[i + 4].zRot = -THIGH - lift - fold;
            thighs[i].yRot = SPREAD[i] + swing;
            thighs[i].zRot = THIGH + lift + fold;
        }
        // pincers work slowly, faster when it moves
        float snap = 0.12F + Mth.sin(ageInTicks * (0.08F + limbSwingAmount * 0.3F)) * 0.12F;
        leftPincer.yRot = 0.3F - snap;
        rightPincer.yRot = -0.3F + snap;
        float breathe = buried ? 0F : Mth.sin(ageInTicks * 0.06F) * 0.2F;
        body.y = 18F + breathe;
        head.y = 17.5F + breathe;
    }
}
