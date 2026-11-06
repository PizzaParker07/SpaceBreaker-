package com.pizzaparker07.spacebreaker.client.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.saturn.entity.TholinWorm;
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
 * Tholin Worm: five ringed segments tapering from a blunt head to a short tail. The head ends in a round, ridged mouth
 * plate that sifts sand. Crawling, a slow wave runs down the body; burrowed, it sinks until only the back shows.
 * UVs: tools/textures/modules/saturn.py.
 */
public class TholinWormModel extends HierarchicalModel<TholinWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("tholin_worm"), "main");
    private static final float Y = 24F;
    /** z centre of each segment, head first. */
    private static final float[] Z = {-6F, -2.5F, 0.5F, 3.5F, 6.5F};
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[5];

    public TholinWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < 5; i++) {
            segments[i] = root.getChild("segment" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int[][] uv = {{0, 0}, {0, 9}, {20, 0}, {20, 9}, {36, 0}};
        float[][] size = {{6, 5, 4}, {6, 5, 3}, {5, 4, 3}, {4, 4, 3}, {3, 3, 3}};
        for (int i = 0; i < 5; i++) {
            float w = size[i][0];
            float h = size[i][1];
            float d = size[i][2];
            root.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(uv[i][0], uv[i][1]).addBox(-w / 2, -h, -d / 2, w, h, d),
                    PartPose.offset(0F, Y, Z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TholinWorm worm, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean burrowed = worm.isBurrowed();
        // burrowed: sunk into the sand, the back segments a little higher, as if the worm lies curled just under the surface
        float breathe = Mth.sin(ageInTicks * 0.1F) * 0.3F;
        for (int i = 0; i < 5; i++) {
            ModelPart s = segments[i];
            float wave = Mth.sin(limbSwing * 0.9F - i * 0.9F) * 1.4F * Math.min(1F, limbSwingAmount * 1.5F);
            float idle = Mth.sin(ageInTicks * 0.08F - i * 0.7F) * 0.35F;
            s.x = burrowed ? Mth.sin(i * 1.3F) * 1.2F : wave + idle;
            s.y = burrowed ? Y + 2.6F - (i == 1 || i == 2 ? 0.8F : 0F) + breathe * 0.3F : Y - Math.max(0F, Mth.cos(limbSwing * 0.9F - i * 0.9F)) * 0.6F * limbSwingAmount;
            s.yRot = burrowed ? 0F : Mth.cos(limbSwing * 0.9F - i * 0.9F) * 0.25F * limbSwingAmount;
        }
        // the head looks about a little while crawling
        if (!burrowed) {
            segments[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
            segments[0].xRot = headPitch * Mth.DEG_TO_RAD * 0.3F + Mth.sin(ageInTicks * 0.15F) * 0.05F;
        } else {
            segments[0].xRot = 0.25F;
        }
    }
}
