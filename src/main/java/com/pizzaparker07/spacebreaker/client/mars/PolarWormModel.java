package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.PolarWorm;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Polar Worm: a round-mawed head and a tapering chain of ring segments that slides in sideways waves. */
public class PolarWormModel extends HierarchicalModel<PolarWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("polar_worm"), "main");
    /** (u, v, size) of head and segments, front to back. */
    private static final int[][] PARTS = {{0, 0, 6}, {0, 14, 5}, {0, 14, 5}, {24, 14, 4}, {24, 14, 4}, {44, 14, 3}};
    private final ModelPart root;
    private final ModelPart[] parts = new ModelPart[PARTS.length];

    public PolarWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < PARTS.length; i++) {
            parts[i] = root.getChild("part" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float z = -8F;
        for (int i = 0; i < PARTS.length; i++) {
            int s = PARTS[i][2];
            root.addOrReplaceChild("part" + i, CubeListBuilder.create().texOffs(PARTS[i][0], PARTS[i][1]).addBox(-s / 2F, -s, -s / 2F, s, s, s),
                    PartPose.offset(0F, 24F, z));
            z += s * 0.5F + (i + 1 < PARTS.length ? PARTS[i + 1][2] * 0.5F : 0) + 0.2F;
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(PolarWorm worm, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float amp = 0.6F + 1.6F * Math.min(1F, limbSwingAmount * 2);
        float speed = 0.12F + 0.5F * Math.min(1F, limbSwingAmount * 2);
        for (int i = 0; i < parts.length; i++) {
            float w = Mth.sin(ageInTicks * speed - i * 0.9F);
            parts[i].x = w * amp * Math.min(1F, i * 0.5F + 0.3F);
            parts[i].yRot = Mth.cos(ageInTicks * speed - i * 0.9F) * 0.25F;
        }
        // the head rears up a little and looks around
        parts[0].xRot = -0.15F + headPitch * Mth.DEG_TO_RAD * 0.3F;
        parts[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        parts[0].y = 24F - Math.max(0, Mth.sin(ageInTicks * 0.05F)) * 1.5F;
    }
}
