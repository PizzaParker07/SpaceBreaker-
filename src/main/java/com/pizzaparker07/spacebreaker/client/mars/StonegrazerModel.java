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

/** Stonegrazer: barrel body with an armoured back, low heavy head, six pillar legs and a stubby tail. UVs: mars_creatures.py. */
public class StonegrazerModel extends HierarchicalModel<Stonegrazer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("stonegrazer"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart[] legs = new ModelPart[6];

    public StonegrazerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -10F, -11F, 14F, 10F, 22F),
                PartPose.offset(0F, 16F, 0F));
        body.addOrReplaceChild("plates", CubeListBuilder.create().texOffs(0, 32).addBox(-6F, -3F, -9F, 12F, 3F, 18F), PartPose.offset(0F, -10F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(72, 0).addBox(-4F, -3F, -8F, 8F, 7F, 8F), PartPose.offset(0F, 11F, -10F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(72, 30).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 6F),
                PartPose.offsetAndRotation(0F, 9F, 10.5F, -0.6F, 0F, 0F));
        float[] z = {-7F, 0F, 7F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(72, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F), PartPose.offset(5F, 16F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(72, 16).mirror().addBox(-2F, 0F, -2F, 4F, 8F, 4F),
                    PartPose.offset(-5F, 16F, z[i]));
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
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F + graze * 0.9F;
        tail.yRot = Mth.sin(ageInTicks * 0.08F) * 0.2F;
        for (int i = 0; i < 6; i++) {
            float phase = ((i % 3) + (i >= 3 ? 1 : 0)) % 2 == 0 ? 0 : Mth.PI;
            legs[i].xRot = Mth.cos(limbSwing * 0.6662F + phase) * 0.8F * limbSwingAmount;
        }
    }
}
