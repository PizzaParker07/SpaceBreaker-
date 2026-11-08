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
 * Tholin Worm: six ringed segments, thickest in the middle and tapering to a blunt head and a short tail, a row of paired
 * bristles along the back, and a mouth cone of four lips round a dark orange gullet. Crawling, a slow wave runs down the
 * body and the lips open and close as it sifts; burrowed, it sinks until only the bristles break the sand.
 * UVs: tools/textures/modules/saturn.py.
 */
public class TholinWormModel extends HierarchicalModel<TholinWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("tholin_worm"), "main");
    private static final float Y = 24F;
    /** z centre, width, height and depth of each segment, head first. */
    private static final float[] Z = {-8F, -5F, -2F, 1F, 3.5F, 5.5F};
    private static final int[][] SIZE = {{5, 4, 3}, {6, 5, 3}, {7, 6, 3}, {6, 5, 3}, {5, 4, 2}, {3, 3, 2}};
    private static final int[][] UV = {{0, 0}, {0, 8}, {0, 17}, {0, 27}, {20, 0}, {36, 0}};
    /** Rest angle of the lips (tips leaning in to make a cone) and how far they open. */
    private static final float LIP_REST = 0.3F;
    private static final float LIP_OPEN = 0.7F;
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[6];
    private final ModelPart lipTop;
    private final ModelPart lipBottom;
    private final ModelPart lipLeft;
    private final ModelPart lipRight;

    public TholinWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < segments.length; i++) {
            segments[i] = root.getChild("segment" + i);
        }
        lipTop = segments[0].getChild("lip_top");
        lipBottom = segments[0].getChild("lip_bottom");
        lipLeft = segments[0].getChild("lip_left");
        lipRight = segments[0].getChild("lip_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < SIZE.length; i++) {
            float w = SIZE[i][0];
            float h = SIZE[i][1];
            float d = SIZE[i][2];
            PartDefinition seg = root.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(UV[i][0], UV[i][1])
                    .addBox(-w / 2, -h, -d / 2, w, h, d), PartPose.offset(0F, Y, Z[i]));
            if (i >= 1 && i <= 3) {
                // a pair of bristles on the back of each thick segment, leaning outward
                for (int side = -1; side <= 1; side += 2) {
                    seg.addOrReplaceChild("bristle" + (side < 0 ? "_l" : "_r"), CubeListBuilder.create().texOffs(48, 0)
                            .addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offsetAndRotation(side * 1.2F, -h, 0F, -0.25F, 0F, side * 0.35F));
                }
            }
        }
        PartDefinition head = root.getChild("segment0");
        // the mouth cone: lips hinged on the rim of the head's front face (z = -1.5), reaching forward
        head.addOrReplaceChild("lip_top", CubeListBuilder.create().texOffs(20, 12).addBox(-2.5F, -0.5F, -2F, 5, 1, 2),
                PartPose.offset(0F, -3.5F, -1.5F));
        head.addOrReplaceChild("lip_bottom", CubeListBuilder.create().texOffs(20, 16).addBox(-2.5F, -0.5F, -2F, 5, 1, 2),
                PartPose.offset(0F, -0.5F, -1.5F));
        head.addOrReplaceChild("lip_left", CubeListBuilder.create().texOffs(36, 8).addBox(-0.5F, -1F, -2F, 1, 2, 2),
                PartPose.offset(-2F, -2F, -1.5F));
        head.addOrReplaceChild("lip_right", CubeListBuilder.create().texOffs(36, 8).mirror().addBox(-0.5F, -1F, -2F, 1, 2, 2),
                PartPose.offset(2F, -2F, -1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TholinWorm worm, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean burrowed = worm.isBurrowed();
        float breathe = Mth.sin(ageInTicks * 0.1F) * 0.3F;
        for (int i = 0; i < segments.length; i++) {
            ModelPart s = segments[i];
            float wave = Mth.sin(limbSwing * 0.9F - i * 0.9F) * 1.4F * Math.min(1F, limbSwingAmount * 1.5F);
            float idle = Mth.sin(ageInTicks * 0.08F - i * 0.7F) * 0.35F;
            s.xRot = 0F;
            if (burrowed) {
                // sunk until the back is level with the sand: only the bristles show, rising and falling with its breath
                s.x = Mth.sin(i * 1.3F) * 1.2F;
                s.y = Y + SIZE[i][1] - 0.2F + breathe * 0.3F;
                s.yRot = 0F;
            } else {
                s.x = wave + idle;
                s.y = Y - Math.max(0F, Mth.cos(limbSwing * 0.9F - i * 0.9F)) * 0.6F * limbSwingAmount;
                s.yRot = Mth.cos(limbSwing * 0.9F - i * 0.9F) * 0.25F * limbSwingAmount;
            }
        }
        // the lips open and close as it sifts (faster while crawling); shut tight while burrowed
        float open = burrowed ? 0F : (0.5F + 0.5F * Mth.sin(ageInTicks * (0.12F + 0.2F * limbSwingAmount))) * LIP_OPEN;
        lipTop.xRot = LIP_REST - open;
        lipBottom.xRot = -LIP_REST + open;
        lipLeft.yRot = -LIP_REST + open;
        lipRight.yRot = LIP_REST - open;
        // the head looks about a little while crawling
        if (!burrowed) {
            segments[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
            segments[0].xRot = headPitch * Mth.DEG_TO_RAD * 0.3F + Mth.sin(ageInTicks * 0.15F) * 0.05F;
        }
    }
}
