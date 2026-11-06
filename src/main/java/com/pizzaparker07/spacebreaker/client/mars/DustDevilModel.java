package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.DustDevil;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Dust Devil: a funnel of translucent dust rings, each spinning at its own speed and swaying, widest at the top. */
public class DustDevilModel extends HierarchicalModel<DustDevil> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("dust_devil"), "main");
    private static final int[] WIDTH = {5, 7, 9, 11, 13, 16};
    private final ModelPart root;
    private final ModelPart[] tiers = new ModelPart[WIDTH.length];

    public DustDevilModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < WIDTH.length; i++) {
            tiers[i] = root.getChild("tier" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < WIDTH.length; i++) {
            float w = WIDTH[i];
            root.addOrReplaceChild("tier" + i, CubeListBuilder.create().texOffs(0, (i % 3) * 21).addBox(-w / 2, -5F, -w / 2, w, 5F, w),
                    PartPose.offset(0F, 24F - i * 4.6F, 0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DustDevil devil, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        for (int i = 0; i < tiers.length; i++) {
            tiers[i].yRot = ageInTicks * (0.55F - i * 0.05F) * (i % 2 == 0 ? 1 : 1.15F);
            float sway = i * 0.45F;
            tiers[i].x = Mth.sin(ageInTicks * 0.13F + i * 0.7F) * sway;
            tiers[i].z = Mth.cos(ageInTicks * 0.11F + i * 0.9F) * sway;
        }
    }
}
