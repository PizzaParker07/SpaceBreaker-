package com.pizzaparker07.spacebreaker.client.jupiter;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.CraterHopper;
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
 * Crater Hopper: a compact body tilted up at the front, a small head with two tall sensory plates, big folded hind
 * legs, tiny forelegs and a stiff balancing tail. In a leap the hind legs kick straight back. UVs:
 * tools/textures/modules/jupiter.py.
 */
public class CraterHopperModel extends HierarchicalModel<CraterHopper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("crater_hopper"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart tail;

    public CraterHopperModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -6F, -4F, 6F, 6F, 8F),
                PartPose.offsetAndRotation(0F, 18F, 0F, -0.3F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2F, -4F, -3F, 4F, 4F, 4F),
                PartPose.offset(0F, 13F, -4F));
        head.addOrReplaceChild("plate_left", CubeListBuilder.create().texOffs(44, 0).addBox(0F, -5F, 0F, 1F, 5F, 2F),
                PartPose.offsetAndRotation(1F, -3.5F, -1F, -0.2F, 0F, 0.25F));
        head.addOrReplaceChild("plate_right", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-1F, -5F, 0F, 1F, 5F, 2F),
                PartPose.offsetAndRotation(-1F, -3.5F, -1F, -0.2F, 0F, -0.25F));
        root.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(0, 14).addBox(-1F, 0F, -2F, 2F, 7F, 4F), PartPose.offset(3F, 17F, 2F));
        root.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-1F, 0F, -2F, 2F, 7F, 4F),
                PartPose.offset(-3F, 17F, 2F));
        root.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(12, 14).addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(1.5F, 17F, -3.5F, -0.3F, 0F, 0F));
        root.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(12, 14).mirror().addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(-1.5F, 17F, -3.5F, -0.3F, 0F, 0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 14).addBox(-1F, -1F, 0F, 2F, 2F, 6F),
                PartPose.offsetAndRotation(0F, 17F, 3.5F, 0.35F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CraterHopper h, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        float kick = h.leaping() ? 0.9F : 0F;
        legLeft.xRot = kick;
        legRight.xRot = kick;
        tail.xRot = 0.35F - (h.leaping() ? 0.3F : 0F) + Mth.sin(ageInTicks * 0.1F) * 0.05F;
    }
}
