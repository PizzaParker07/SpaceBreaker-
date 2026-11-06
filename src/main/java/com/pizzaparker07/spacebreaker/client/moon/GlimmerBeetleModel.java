package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.GlimmerBeetle;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Glimmer Beetle: domed abdomen (glows at the rear), small thorax and head with antennae, six short legs. */
public class GlimmerBeetleModel<T extends GlimmerBeetle> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("glimmer_beetle"), "main");
    private static final String[] LEGS = {"right_leg_0", "right_leg_1", "right_leg_2", "left_leg_0", "left_leg_1", "left_leg_2"};
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[6];

    public GlimmerBeetleModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild(LEGS[i]);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -3F, 6F, 3F, 7F),
                PartPose.offset(0F, 23F, 1F));
        root.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 10).addBox(-2F, -2F, -2F, 4F, 2F, 3F),
                PartPose.offset(0F, 22.5F, -2F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 10).addBox(-1F, -1F, -2F, 2F, 2F, 2F),
                PartPose.offset(0F, 22F, -4F));
        head.addOrReplaceChild("antennae", CubeListBuilder.create()
                .texOffs(22, 10).addBox(-1.5F, -3F, -2F, 1F, 2F, 1F)
                .texOffs(22, 10).mirror().addBox(0.5F, -3F, -2F, 1F, 2F, 1F), PartPose.ZERO);
        CubeListBuilder right = CubeListBuilder.create().texOffs(0, 16).addBox(-4F, 0F, -0.5F, 4F, 1F, 1F);
        CubeListBuilder left = CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0F, 0F, -0.5F, 4F, 1F, 1F);
        float[] z = {-2.5F, -1F, 0.5F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild(LEGS[i], right, PartPose.offset(-1.5F, 22.5F, z[i]));
            root.addOrReplaceChild(LEGS[i + 3], left, PartPose.offset(1.5F, 22.5F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T beetle, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        float[] spread = {-0.45F, 0F, 0.45F};
        for (int i = 0; i < 3; i++) {
            float swing = Mth.cos(limbSwing * 1.6F + i * 2.1F) * 0.5F * limbSwingAmount;
            legs[i].yRot = spread[i] + swing;
            legs[i].zRot = -0.45F;
            legs[i + 3].yRot = -spread[i] - swing;
            legs[i + 3].zRot = 0.45F;
        }
    }
}
