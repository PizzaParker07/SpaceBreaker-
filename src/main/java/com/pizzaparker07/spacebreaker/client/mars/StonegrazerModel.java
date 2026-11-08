package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.Stonegrazer;
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
 * Stonegrazer: a low, broad armoured grazer. Overlapping rock plates shingle its back, the head is a low wedge with a
 * wide flat scraper jaw for planing lichen-like crusts off the rock, six short thick legs and a plated stub of a tail.
 * UVs: mars_creatures.py.
 */
public class StonegrazerModel extends HierarchicalModel<Stonegrazer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("stonegrazer"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart[] legs = new ModelPart[6];

    public StonegrazerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail = root.getChild("tail");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -8F, -11F, 16F, 8F, 22F),
                PartPose.offset(0F, 18F, 0F));
        // shingled plates across the back, each tipped forward over the next
        float[] pz = {-8.5F, -4F, 0.5F, 5F, 9F};
        for (int i = 0; i < pz.length; i++) {
            body.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(0, 32).addBox(-8.5F, -2F, -2.5F, 17F, 2F, 5F),
                    PartPose.offsetAndRotation(0F, -8F, pz[i], -0.18F, 0F, 0F));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 0).addBox(-5F, -4F, -6F, 10F, 6F, 6F),
                PartPose.offset(0F, 16F, -11F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(80, 14).addBox(-6F, 0F, -5F, 12F, 2F, 5F),
                PartPose.offset(0F, 2F, -3F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(80, 24).addBox(-2F, -2F, 0F, 4F, 3F, 5F),
                PartPose.offsetAndRotation(0F, 14F, 11F, -0.25F, 0F, 0F));
        float[] z = {-7F, 0F, 7F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(100, 24).addBox(-2.5F, 0F, -2.5F, 5F, 6F, 5F), PartPose.offset(6F, 18F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(100, 24).mirror().addBox(-2.5F, 0F, -2.5F, 5F, 6F, 5F),
                    PartPose.offset(-6F, 18F, z[i]));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Stonegrazer grazer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        // every so often the head sinks to lick the rocks
        float graze = Mth.clamp((Mth.sin(ageInTicks * 0.025F + grazer.getId()) - 0.6F) * 4F, 0F, 1F) * (1 - Math.min(1, limbSwingAmount * 3));
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F + graze * 0.7F;
        // the scraper jaw works while it grazes
        jaw.xRot = graze * (0.15F + 0.15F * Mth.sin(ageInTicks * 0.6F));
        tail.yRot = Mth.sin(ageInTicks * 0.08F) * 0.2F;
        for (int i = 0; i < 6; i++) {
            float phase = ((i % 3) + (i >= 3 ? 1 : 0)) % 2 == 0 ? 0 : Mth.PI;
            legs[i].xRot = Mth.cos(limbSwing * 0.6662F + phase) * 0.8F * limbSwingAmount;
        }
    }
}
