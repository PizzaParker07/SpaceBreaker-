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

/**
 * Polar Worm: a chain of clear ice rings, each a translucent shell round a dark slate core that shows through, widest
 * behind the head and tapering to a frosted crystal at the tail; small ice nodules stud the flanks of every other
 * ring. The blunt, frost-white head opens in a round maw ringed with six ice teeth around a dark orange gullet.
 * It slides in sideways waves; the teeth flex open and shut as it feeds. UVs: mars_creatures.py.
 */
public class PolarWormModel extends HierarchicalModel<PolarWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("polar_worm"), "main");
    /** Shell width, height, depth of each ring behind the head, and the texture slot (0-3) of that size. */
    private static final int[][] RINGS = {{6, 5, 3, 0}, {6, 5, 3, 0}, {5, 4, 3, 1}, {5, 4, 3, 1}, {4, 3, 3, 2}, {4, 3, 3, 2}, {3, 2, 3, 3}};
    private static final int[][] SHELL_UV = {{0, 12}, {20, 12}, {40, 12}, {56, 12}};
    private static final int[][] CORE_UV = {{0, 24}, {20, 24}, {40, 24}, {56, 24}};
    private static final int TEETH = 6;
    private final ModelPart root;
    private final ModelPart[] parts = new ModelPart[RINGS.length + 1];
    private final ModelPart[] teeth = new ModelPart[TEETH];

    public PolarWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < parts.length; i++) {
            parts[i] = root.getChild("part" + i);
        }
        for (int i = 0; i < TEETH; i++) {
            teeth[i] = parts[0].getChild("tooth" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float z = -9.5F;
        PartDefinition head = root.addOrReplaceChild("part0", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -5F, -2.5F, 6F, 5F, 5F),
                PartPose.offset(0F, 24F, z));
        for (int i = 0; i < TEETH; i++) {
            double a = i * Math.PI * 2 / TEETH + Math.PI / 6;
            float tx = (float) Math.cos(a) * 1.4F;
            float ty = (float) Math.sin(a) * 1.2F - 2.5F;
            head.addOrReplaceChild("tooth" + i, CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1.5F),
                    PartPose.offset(tx, ty, -2.3F));
        }
        z += 2.5F;
        for (int i = 0; i < RINGS.length; i++) {
            int w = RINGS[i][0], h = RINGS[i][1], d = RINGS[i][2], slot = RINGS[i][3];
            z += d / 2F + 0.1F;
            CubeListBuilder b = CubeListBuilder.create()
                    // the core first: drawn before the clear shell, it stays visible through it
                    .texOffs(CORE_UV[slot][0], CORE_UV[slot][1]).addBox(-(w - 2) / 2F, -h + 1F, -(d + 1) / 2F, w - 2, Math.max(1, h - 2), d + 1)
                    .texOffs(SHELL_UV[slot][0], SHELL_UV[slot][1]).addBox(-w / 2F, -h, -d / 2F, w, h, d);
            PartDefinition ring = root.addOrReplaceChild("part" + (i + 1), b, PartPose.offset(0F, 24F, z));
            if (i % 2 == 0 && i < RINGS.length - 1) {
                ring.addOrReplaceChild("nodule_left", CubeListBuilder.create().texOffs(70, 0).addBox(0F, -1F, -1F, 1F, 2F, 2F),
                        PartPose.offsetAndRotation(w / 2F - 0.3F, -h * 0.55F, 0F, 0.6F, 0F, 0.3F));
                ring.addOrReplaceChild("nodule_right", CubeListBuilder.create().texOffs(70, 0).mirror().addBox(-1F, -1F, -1F, 1F, 2F, 2F),
                        PartPose.offsetAndRotation(-w / 2F + 0.3F, -h * 0.55F, 0F, -0.6F, 0F, -0.3F));
            }
            if (i == RINGS.length - 1) {
                ring.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(70, 12).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 3F),
                        PartPose.offsetAndRotation(0F, -1F, d / 2F - 0.2F, 0.35F, 0F, 0.785F));
            }
            z += d / 2F;
        }
        return LayerDefinition.create(mesh, 128, 64);
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
            float w = Mth.sin(ageInTicks * speed - i * 0.8F);
            parts[i].x = w * amp * Math.min(1F, i * 0.4F + 0.3F);
            parts[i].yRot = Mth.cos(ageInTicks * speed - i * 0.8F) * 0.25F;
        }
        // the head rears up a little and looks around
        parts[0].xRot = -0.15F + headPitch * Mth.DEG_TO_RAD * 0.3F;
        parts[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        parts[0].y = 24F - Math.max(0, Mth.sin(ageInTicks * 0.05F)) * 1.5F;
        // the teeth splay open and draw shut, each leaning out from the maw's centre
        float open = 0.1F + Math.max(0F, Mth.sin(ageInTicks * 0.15F)) * 0.3F;
        for (int i = 0; i < TEETH; i++) {
            double a = i * Math.PI * 2 / TEETH + Math.PI / 6;
            teeth[i].yRot = (float) -Math.cos(a) * open;
            teeth[i].xRot = (float) Math.sin(a) * open;
        }
    }
}
