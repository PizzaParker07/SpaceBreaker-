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
 * Regolith Tick: a low, round tick. A stepped dome of a shell, a small head with two palps and a dark orange mouth, and
 * eight short two-part legs bunched toward the front, the front pair flattened into digging shovels. Built low and
 * round so it reads as a tick (not a spider, and not the tall, armoured elder).
 * UV layout matches tools/textures/modules/moon_creatures.py (tick_sheet).
 */
public class RegolithTickModel<T extends RegolithTick> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("regolith_tick"), "main");
    private static final float[] LEG_Z = {-3F, -1.5F, 0.5F, 2.5F};
    private static final float[] SPREAD = {-0.9F, -0.4F, 0.1F, 0.6F};
    private static final float THIGH = 0.3F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[8];

    public RegolithTickModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        for (int i = 0; i < 8; i++) {
            legs[i] = root.getChild("leg_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -1F, -4.5F, 8F, 2F, 9F)
                .texOffs(0, 11).addBox(-3F, -3F, -3.5F, 6F, 2F, 7F)
                .texOffs(26, 11).addBox(-2F, -4F, -2.5F, 4F, 1F, 5F), PartPose.offset(0F, 21F, 0.5F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(34, 0).addBox(-1.5F, -1F, -3F, 3F, 2F, 3F)
                .texOffs(46, 0).addBox(-0.5F, 0F, -3.5F, 1F, 1F, 1F), PartPose.offset(0F, 21F, -4F));
        head.addOrReplaceChild("palps", CubeListBuilder.create()
                .texOffs(50, 0).addBox(-2F, -0.5F, -4.5F, 1F, 1F, 2F)
                .texOffs(50, 0).mirror().addBox(1F, -0.5F, -4.5F, 1F, 1F, 2F), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            boolean front = i == 0;
            // left (+x) and right (-x): a short thigh, then a shin down to the ground (a flat shovel on the front pair)
            PartDefinition l = root.addOrReplaceChild("leg_" + (i + 4), CubeListBuilder.create().texOffs(0, 20).addBox(0F, -0.5F, -0.5F, 3F, 1F, 1F),
                    PartPose.offsetAndRotation(3.5F, 21F, LEG_Z[i], 0F, -SPREAD[i], -THIGH));
            l.addOrReplaceChild("shin", front
                    ? CubeListBuilder.create().texOffs(12, 20).addBox(-1F, 0F, -0.5F, 2F, 4F, 1F)
                    : CubeListBuilder.create().texOffs(8, 20).addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                    PartPose.offsetAndRotation(3F, 0F, 0F, 0F, 0F, THIGH - 0.35F));
            PartDefinition r = root.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-3F, -0.5F, -0.5F, 3F, 1F, 1F),
                    PartPose.offsetAndRotation(-3.5F, 21F, LEG_Z[i], 0F, SPREAD[i], THIGH));
            r.addOrReplaceChild("shin", front
                    ? CubeListBuilder.create().texOffs(12, 20).mirror().addBox(-1F, 0F, -0.5F, 2F, 4F, 1F)
                    : CubeListBuilder.create().texOffs(8, 20).mirror().addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                    PartPose.offsetAndRotation(-3F, 0F, 0F, 0F, 0F, -THIGH + 0.35F));
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
        // quick scuttle: neighbouring legs out of step; legs pulled in when buried, tucked in a hop
        boolean airborne = !tick.onGround() && !buried;
        for (int i = 0; i < 4; i++) {
            float phase = i * Mth.HALF_PI;
            float swing = Mth.cos(limbSwing * 1.6F + phase) * 0.4F * limbSwingAmount;
            float lift = Math.max(0F, Mth.sin(limbSwing * 1.6F + phase)) * 0.4F * limbSwingAmount;
            float fold = buried ? 0.6F : airborne ? 0.5F : 0F;
            legs[i + 4].yRot = -SPREAD[i] - swing;
            legs[i + 4].zRot = -THIGH - lift + fold;
            legs[i].yRot = SPREAD[i] + swing;
            legs[i].zRot = THIGH + lift - fold;
        }
        // breathing / feeling the ground
        float bob = buried ? 0F : Mth.sin(ageInTicks * 0.15F) * 0.15F;
        body.y = 21F + bob;
        head.y = 21F + bob;
    }
}
