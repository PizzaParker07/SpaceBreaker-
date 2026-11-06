package com.pizzaparker07.spacebreaker.client.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mercury.HollowSnail;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Hollow Snail: a long grey foot, a tall pale stepped shell, a small head with two eye stalks. UVs: mercury.py. */
public class HollowSnailModel extends HierarchicalModel<HollowSnail> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("hollow_snail"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart shell;
    private final ModelPart stalkL;
    private final ModelPart stalkR;

    public HollowSnailModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.shell = root.getChild("shell");
        this.stalkL = head.getChild("stalk_left");
        this.stalkR = head.getChild("stalk_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2F, -6F, 5F, 2F, 11F), PartPose.offset(0F, 24F, 0F));
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -6F, -3.5F, 7F, 6F, 7F),
                PartPose.offset(0F, 22F, 1.5F));
        shell.addOrReplaceChild("apex", CubeListBuilder.create().texOffs(30, 14).addBox(-2F, -2F, -2F, 4F, 2F, 4F),
                PartPose.offsetAndRotation(0.5F, -6F, 0.5F, 0F, 0.4F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5F, -3F, -2F, 3F, 3F, 2F),
                PartPose.offset(0F, 22F, -5F));
        head.addOrReplaceChild("stalk_left", CubeListBuilder.create().texOffs(46, 0).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(1F, -3F, -1F, -0.3F, 0F, 0.3F));
        head.addOrReplaceChild("stalk_right", CubeListBuilder.create().texOffs(46, 0).mirror().addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(-1F, -3F, -1F, -0.3F, 0F, -0.3F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(HollowSnail snail, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        // grazing: the head dips to rasp at the crust; moving: the shell rocks a little
        float rasp = snail.onHollow() ? Math.max(0, Mth.sin(ageInTicks * 0.15F)) * 0.5F : 0F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F + rasp;
        shell.zRot = Mth.sin(limbSwing * 0.8F) * 0.06F * limbSwingAmount;
        float wave = Mth.sin(ageInTicks * 0.1F) * 0.12F;
        stalkL.xRot = -0.3F + wave;
        stalkR.xRot = -0.3F - wave;
    }
}
