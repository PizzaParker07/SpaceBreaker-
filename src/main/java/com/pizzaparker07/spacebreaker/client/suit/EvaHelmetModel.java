package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * EVA helmet: a rounded composite shell built from three crossing slabs, a steel visor rim,
 * a blue anodised neck ring and two side lamps. The UV layout is mirrored in
 * tools/textures/sbtex/suit.py (HELMET_BOXES) — keep them in sync.
 * Glass, gold sun visor and lamp lenses live in {@link #createVisorLayer()} and are drawn
 * by {@code HelmetVisorLayer} with translucent/emissive render types.
 */
public class EvaHelmetModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("eva_helmet"), "main");
    public static final ModelLayerLocation VISOR_LAYER = new ModelLayerLocation(Spacebreaker.id("eva_helmet"), "visor");

    public EvaHelmetModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -9F, -5F, 8, 9, 10)          // shell_z
                        .texOffs(0, 19).addBox(-5F, -9F, -4F, 10, 9, 8)         // shell_x
                        .texOffs(0, 36).addBox(-4F, -10F, -4F, 8, 1, 8)         // cap
                        .texOffs(0, 45).addBox(-4.5F, -9.5F, -4.5F, 9, 9, 9)    // corner rounder
                        .texOffs(60, 20).addBox(-3.5F, -8F, -3.5F, 7, 7, 7)     // padded liner (hidden by a player's head)
                        .texOffs(40, 0).addBox(-5.5F, -0.5F, -5.5F, 11, 2, 11)  // neck ring
                        .texOffs(40, 13).addBox(-6F, -8F, -3F, 1, 2, 3)         // lamp left
                        .texOffs(48, 13).addBox(5F, -8F, -3F, 1, 2, 3),         // lamp right
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createVisorLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -8F, -4.75F, 6, 6, 0), PartPose.ZERO);
        root.addOrReplaceChild("sun_visor", CubeListBuilder.create().texOffs(0, 8).addBox(-3.5F, -8.5F, -5.25F, 7, 7, 0), PartPose.ZERO);
        root.addOrReplaceChild("lenses", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-6F, -7.5F, -3.05F, 1, 1, 0)
                .texOffs(0, 16).addBox(5F, -7.5F, -3.05F, 1, 1, 0), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
