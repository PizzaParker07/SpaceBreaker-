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
 * Stonegrazer: a low armoured grazer, deep and broad at the shoulders and tapering to the hindquarters. Overlapping
 * rock plates overhang its flanks, shrinking toward the tail; the wedge head hangs low on a short neck with a wide flat
 * scraper jaw for planing crusts off the rock; six short thick legs stand out at the sides; a plated stub of a tail.
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
        // a deep, broad chest and shoulders tapering to narrower, lower hindquarters
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9F, -9F, -12F, 18F, 9F, 10F)
                .texOffs(56, 0).addBox(-7F, -7F, -2F, 14F, 7F, 12F), PartPose.offset(0F, 17F, 0F));
        // shingled plates that overhang the flanks, largest over the shoulders, smaller toward the tail
        float[] pz = {-10F, -6F, -1.5F, 2.5F, 6.5F};
        float[] pw = {20F, 18F, 16F, 14F, 12F};
        float[] py = {8F, 8F, 10F, 10F, 10F};
        for (int i = 0; i < pz.length; i++) {
            root.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(0, 20).addBox(-pw[i] / 2, -2F, -2.5F, pw[i], 2F, 5F),
                    PartPose.offsetAndRotation(0F, py[i], pz[i], -0.18F, 0F, 0F));
        }
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(104, 28).addBox(-3F, -2.5F, -4F, 6F, 5F, 4F),
                PartPose.offsetAndRotation(0F, 13F, -12F, 0.25F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 28).addBox(-5F, -4F, -6F, 10F, 6F, 6F),
                PartPose.offsetAndRotation(0F, 15F, -15F, 0.4F, 0F, 0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 28).addBox(-6F, 0F, -5F, 12F, 2F, 5F),
                PartPose.offset(0F, 2F, -3F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(66, 28).addBox(-2F, -2F, 0F, 4F, 3F, 5F),
                PartPose.offsetAndRotation(0F, 13F, 10F, -0.25F, 0F, 0F));
        // legs set out beyond the flanks, so they show from the side
        float[] lz = {-9F, -4F, 6F};
        float[] lx = {8F, 8F, 6.5F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(84, 28).addBox(-2.5F, 0F, -2.5F, 5F, 7F, 5F), PartPose.offset(lx[i], 17F, lz[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(84, 28).mirror().addBox(-2.5F, 0F, -2.5F, 5F, 7F, 5F),
                    PartPose.offset(-lx[i], 17F, lz[i]));
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
        head.xRot = 0.4F + headPitch * Mth.DEG_TO_RAD * 0.3F + graze * 0.6F;
        // the scraper jaw works while it grazes
        jaw.xRot = graze * (0.15F + 0.15F * Mth.sin(ageInTicks * 0.6F));
        tail.yRot = Mth.sin(ageInTicks * 0.08F) * 0.2F;
        for (int i = 0; i < 6; i++) {
            float phase = ((i % 3) + (i >= 3 ? 1 : 0)) % 2 == 0 ? 0 : Mth.PI;
            legs[i].xRot = Mth.cos(limbSwing * 0.6662F + phase) * 0.8F * limbSwingAmount;
        }
    }
}
