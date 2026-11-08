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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.Set;

/**
 * Dust Devil: a funnel of hollow, translucent dust curtains (eight-sided: each ring carries a second one turned 45
 * degrees), narrow and dense at the foot, wide and thin at the top, each ring spinning at its own speed and the column
 * swaying more the higher it goes. A denser core runs up the lower half, a low skirt of dust drags on the ground and a
 * few pebbles whirl around the base. Only the side faces are built, so no flat lids show. UVs: mars_creatures.py.
 */
public class DustDevilModel extends HierarchicalModel<DustDevil> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("dust_devil"), "main");
    private static final int TIERS = 11;
    private static final float STEP = 3.6F;
    private static final Set<Direction> SIDES = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final float[][] PEBBLES = {{6F, -2F}, {-5.5F, -5F}, {1F, -8F}, {-2F, -3.5F}, {4F, -11F}};
    private final ModelPart root;
    private final ModelPart[] tiers = new ModelPart[TIERS];
    private final ModelPart core;
    private final ModelPart skirt;
    private final ModelPart debris;

    public DustDevilModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < TIERS; i++) {
            tiers[i] = root.getChild("tier" + i);
        }
        core = root.getChild("core");
        skirt = root.getChild("skirt");
        debris = root.getChild("debris");
    }

    static float width(int i) {
        return 4F + i * 1.4F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < TIERS; i++) {
            float w = width(i);
            float w2 = w * 0.82F;
            // texture rows fade from dense to thin downward, and a wider ring samples lower rows: the funnel thins upward
            PartDefinition t = root.addOrReplaceChild("tier" + i, CubeListBuilder.create().texOffs(i * 5 % 40, 0).addBox(-w / 2, -4F, -w / 2, w, 4F, w, SIDES),
                    PartPose.offset(0F, 24F - i * STEP, 0F));
            t.addOrReplaceChild("turned", CubeListBuilder.create().texOffs(i * 7 % 40 + 3, 0).addBox(-w2 / 2, -4F, -w2 / 2, w2, 4F, w2, SIDES),
                    PartPose.offsetAndRotation(0F, -0.5F, 0F, 0F, Mth.PI / 4F, 0F));
        }
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(64, 48).addBox(-1F, -14F, -1F, 2F, 14F, 2F, SIDES), PartPose.offset(0F, 24F, 0F));
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 32).addBox(-7F, -2F, -7F, 14F, 2F, 14F, SIDES), PartPose.offset(0F, 24F, 0F));
        PartDefinition debris = root.addOrReplaceChild("debris", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        for (int i = 0; i < PEBBLES.length; i++) {
            float a = i * 2.4F;
            debris.addOrReplaceChild("pebble" + i, CubeListBuilder.create().texOffs(100 + (i % 3) * 4, 40).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F),
                    PartPose.offsetAndRotation(Mth.cos(a) * PEBBLES[i][0], PEBBLES[i][1], Mth.sin(a) * PEBBLES[i][0], a, a * 0.7F, 0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DustDevil devil, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        for (int i = 0; i < TIERS; i++) {
            tiers[i].yRot = ageInTicks * (0.6F - i * 0.035F) * (i % 2 == 0 ? 1F : 1.12F);
            float sway = i * i * 0.05F;
            tiers[i].x = Mth.sin(ageInTicks * 0.13F + i * 0.35F) * sway;
            tiers[i].z = Mth.cos(ageInTicks * 0.11F + i * 0.45F) * sway;
        }
        core.yRot = -ageInTicks * 0.8F;
        skirt.yRot = ageInTicks * 0.35F;
        float pulse = 1F + Mth.sin(ageInTicks * 0.2F) * 0.08F;
        skirt.xScale = skirt.zScale = pulse;
        debris.yRot = ageInTicks * 0.7F;
        for (int i = 0; i < PEBBLES.length; i++) {
            ModelPart p = debris.getChild("pebble" + i);
            p.y = PEBBLES[i][1] + Mth.sin(ageInTicks * 0.3F + i * 1.7F) * 1.5F;
            p.xRot = ageInTicks * (0.3F + i * 0.05F);
        }
    }
}
