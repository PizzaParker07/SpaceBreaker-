package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.HeatSiphon;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Heat Siphon: a low frost-crystal body with three leaning dorsal shards, a blunt feeding head and six thin legs. */
public class HeatSiphonModel<T extends HeatSiphon> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("heat_siphon"), "main");
    private static final String[] LEGS = {"right_leg_0", "right_leg_1", "right_leg_2", "left_leg_0", "left_leg_1", "left_leg_2"};
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart shards;
    private final ModelPart[] legs = new ModelPart[6];

    public HeatSiphonModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.shards = root.getChild("shards");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild(LEGS[i]);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -5F, 8F, 4F, 10F),
                PartPose.offset(0F, 21F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-3F, -2F, -4F, 6F, 3F, 4F),
                PartPose.offset(0F, 19.5F, -5F));
        PartDefinition shards = root.addOrReplaceChild("shards", CubeListBuilder.create(), PartPose.offset(0F, 17F, 0F));
        shards.addOrReplaceChild("shard_0", CubeListBuilder.create().texOffs(36, 0).addBox(-1F, -7F, -1F, 2F, 7F, 2F),
                PartPose.offsetAndRotation(0F, 0F, -2F, -0.35F, 0F, 0.1F));
        shards.addOrReplaceChild("shard_1", CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-2F, 0F, 1.5F, -0.5F, 0F, -0.45F));
        shards.addOrReplaceChild("shard_2", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(2F, 0F, 2.5F, -0.6F, 0F, 0.45F));
        CubeListBuilder right = CubeListBuilder.create().texOffs(20, 14).addBox(-6F, 0F, -0.5F, 6F, 1F, 1F);
        CubeListBuilder left = CubeListBuilder.create().texOffs(20, 14).mirror().addBox(0F, 0F, -0.5F, 6F, 1F, 1F);
        float[] z = {-3F, 0F, 3F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild(LEGS[i], right, PartPose.offset(-3.5F, 20F, z[i]));
            root.addOrReplaceChild(LEGS[i + 3], left, PartPose.offset(3.5F, 20F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        // the shards breathe slowly, faster when full of stolen warmth
        shards.y = 17F - 0.3F * Mth.sin(ageInTicks * (0.05F + 0.1F * mob.glow()));
        float[] spread = {-0.4F, 0F, 0.4F};
        for (int i = 0; i < 3; i++) {
            float swing = Mth.cos(limbSwing * 1.3F + i * 2.1F) * 0.55F * limbSwingAmount;
            float lift = Mth.sin(limbSwing * 1.3F + i * 2.1F) * 0.3F * limbSwingAmount;
            legs[i].yRot = spread[i] + swing;
            legs[i].zRot = -0.55F - Math.max(0F, lift);
            legs[i + 3].yRot = -spread[i] - swing;
            legs[i + 3].zRot = 0.55F + Math.max(0F, -lift);
        }
    }
}
