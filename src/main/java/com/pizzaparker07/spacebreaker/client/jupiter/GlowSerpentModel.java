package com.pizzaparker07.spacebreaker.client.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.GlowSerpent;
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
 * Under-Ice Glow Serpent: a blunt head with two feeler barbels, four tapering body segments chained one behind the
 * other, and a flat tail fin. The body swims in a travelling wave. UVs: tools/textures/modules/jupiter.py.
 */
public class GlowSerpentModel extends HierarchicalModel<GlowSerpent> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("glow_serpent"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] segments = new ModelPart[4];

    public GlowSerpentModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        ModelPart p = head;
        for (int i = 0; i < 4; i++) {
            p = p.getChild("seg" + i);
            segments[i] = p;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -2F, -5F, 4F, 4F, 5F),
                PartPose.offset(0F, 21F, -8F));
        head.addOrReplaceChild("barbel_left", CubeListBuilder.create().texOffs(0, 9).addBox(0F, 0F, -3F, 0F, 2F, 3F),
                PartPose.offsetAndRotation(1.5F, 1F, -4F, 0.3F, -0.5F, 0F));
        head.addOrReplaceChild("barbel_right", CubeListBuilder.create().texOffs(0, 9).addBox(0F, 0F, -3F, 0F, 2F, 3F),
                PartPose.offsetAndRotation(-1.5F, 1F, -4F, 0.3F, 0.5F, 0F));
        int[][] seg = {{18, 0, 4, 4, 5}, {36, 0, 3, 3, 5}, {18, 10, 3, 3, 5}, {36, 10, 2, 2, 5}};
        PartDefinition parent = head;
        for (int i = 0; i < 4; i++) {
            int w = seg[i][2];
            int h = seg[i][3];
            PartDefinition p = parent.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(seg[i][0], seg[i][1])
                    .addBox(-w / 2F, -h / 2F, 0F, w, h, seg[i][4]), PartPose.offset(0F, 0F, i == 0 ? 0F : 5F));
            parent = p;
        }
        parent.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(0, 16).addBox(0F, -3F, 0F, 0F, 6F, 6F), PartPose.offset(0F, 0F, 5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GlowSerpent s, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks * 0.3F;
        float amp = s.isInWater() ? 0.35F : 0.6F;
        head.yRot = Mth.sin(t) * 0.12F;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        for (int i = 0; i < 4; i++) {
            segments[i].yRot = Mth.sin(t - (i + 1) * 0.9F) * amp;
        }
    }
}
