package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.HeatSiphon;
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
 * Heat Siphon: no legs at all. A faceted frost crystal that hovers a hand above the ground: an octagonal body (a box and
 * the same box turned 45 degrees), a crown of three leaning shards, a small feeding mouth in front and a fringe of six
 * frost tendrils trailing under it, swaying as it drifts. Nothing like the ticks' legged outline.
 * UV layout matches tools/textures/modules/moon_creatures.py (heat_siphon).
 */
public class HeatSiphonModel<T extends HeatSiphon> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("heat_siphon"), "main");
    private static final int TENDRILS = 6;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart shards;
    private final ModelPart[] tendrils = new ModelPart[TENDRILS];

    public HeatSiphonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.shards = body.getChild("shards");
        for (int i = 0; i < TENDRILS; i++) {
            tendrils[i] = body.getChild("tendril_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -5F, -3F, 6F, 5F, 6F)
                .texOffs(24, 0).addBox(-2F, 0F, -2F, 4F, 2F, 4F), PartPose.offset(0F, 18.5F, 0F));
        body.addOrReplaceChild("facets", CubeListBuilder.create().texOffs(0, 11).addBox(-3F, -4.5F, -3F, 6F, 4F, 6F),
                PartPose.rotation(0F, Mth.PI / 4F, 0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -1F, -2F, 3F, 2F, 2F),
                PartPose.offset(0F, -1.5F, -3.5F));
        PartDefinition shards = body.addOrReplaceChild("shards", CubeListBuilder.create(), PartPose.offset(0F, -5F, 0F));
        shards.addOrReplaceChild("shard_0", CubeListBuilder.create().texOffs(48, 0).addBox(-1F, -7F, -1F, 2F, 7F, 2F),
                PartPose.offsetAndRotation(0F, 0.5F, -0.5F, -0.15F, 0F, 0.1F));
        shards.addOrReplaceChild("shard_1", CubeListBuilder.create().texOffs(56, 0).addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-1.8F, 0.5F, 1.2F, 0.35F, 0F, -0.5F));
        shards.addOrReplaceChild("shard_2", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(1.8F, 0.5F, 1.2F, 0.35F, 0F, 0.5F));
        for (int i = 0; i < TENDRILS; i++) {
            float a = i * Mth.TWO_PI / TENDRILS + 0.3F;
            body.addOrReplaceChild("tendril_" + i, CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                    PartPose.offset(Mth.sin(a) * 2.2F, 0.5F, Mth.cos(a) * 2.2F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        // hovers: a slow bob, leaning into the way it drifts
        body.y = 18.5F + Mth.sin(ageInTicks * 0.08F + mob.getId()) * 0.6F;
        body.xRot = 0.2F * Math.min(1F, limbSwingAmount * 2F);
        // the shards breathe slowly, faster when full of stolen warmth
        shards.y = -5F - 0.3F * Mth.sin(ageInTicks * (0.05F + 0.1F * mob.glow()));
        // tendrils sway, trailing back as it moves
        for (int i = 0; i < TENDRILS; i++) {
            float ph = ageInTicks * 0.12F + i * 1.1F;
            tendrils[i].xRot = 0.15F * Mth.sin(ph) + 0.5F * Math.min(1F, limbSwingAmount * 2F);
            tendrils[i].zRot = 0.15F * Mth.cos(ph * 0.8F);
        }
    }
}
