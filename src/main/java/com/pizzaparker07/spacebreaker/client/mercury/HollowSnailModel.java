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

/**
 * Hollow Snail: a long, tapering grey foot; a coiled shell set crosswise on its back (a deep whorl with an eight-sided
 * profile, a spiral painted on each side and a knob at the coil's centre); a neck rising from the
 * front of the foot to a small head with two eye stalks and two short feelers below. UVs: mercury.py.
 */
public class HollowSnailModel extends HierarchicalModel<HollowSnail> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("hollow_snail"), "main");
    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart shell;
    private final ModelPart stalkL;
    private final ModelPart stalkR;

    public HollowSnailModel(ModelPart root) {
        this.root = root;
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.shell = root.getChild("shell");
        this.stalkL = head.getChild("stalk_left");
        this.stalkR = head.getChild("stalk_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2F, -7F, 5F, 2F, 13F)
                .texOffs(36, 0).addBox(-1.5F, -1F, 6F, 3F, 1F, 3F), PartPose.offset(0F, 24F, 0F));
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                // two crossed blocks give the coil an eight-sided profile seen from the side
                .texOffs(0, 16).addBox(-2.5F, -7F, -4F, 5F, 6F, 8F)
                .texOffs(28, 16).addBox(-2.4F, -8F, -3F, 4.8F, 8F, 6F)
                .texOffs(50, 24).addBox(2.5F, -5.5F, -1F, 1F, 2F, 2F)
                .texOffs(50, 24).mirror().addBox(-3.5F, -5.5F, -1F, 1F, 2F, 2F), PartPose.offsetAndRotation(0F, 22.5F, 2F, -0.18F, 0F, 0F));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -4F, -1.5F, 3F, 4F, 3F),
                PartPose.offsetAndRotation(0F, 22.5F, -5.5F, -0.3F, 0F, 0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 8).addBox(-1.5F, -2F, -2.5F, 3F, 2F, 3F),
                PartPose.offsetAndRotation(0F, -3.5F, -0.5F, 0.3F, 0F, 0F));
        head.addOrReplaceChild("stalk_left", CubeListBuilder.create().texOffs(36, 6).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(0.8F, -2F, -1.2F, -0.35F, 0F, 0.3F));
        head.addOrReplaceChild("stalk_right", CubeListBuilder.create().texOffs(36, 6).mirror().addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(-0.8F, -2F, -1.2F, -0.35F, 0F, -0.3F));
        head.addOrReplaceChild("feeler_left", CubeListBuilder.create().texOffs(36, 12).addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1.5F),
                PartPose.offsetAndRotation(0.9F, -0.5F, -2.3F, 0.45F, -0.2F, 0F));
        head.addOrReplaceChild("feeler_right", CubeListBuilder.create().texOffs(36, 12).mirror().addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1.5F),
                PartPose.offsetAndRotation(-0.9F, -0.5F, -2.3F, 0.45F, 0.2F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(HollowSnail snail, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        neck.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        // grazing: the neck bows to rasp at the crust; moving: the shell rocks a little
        float rasp = snail.onHollow() ? Math.max(0, Mth.sin(ageInTicks * 0.15F)) * 0.6F : 0F;
        neck.xRot = -0.3F + headPitch * Mth.DEG_TO_RAD * 0.3F + rasp;
        head.xRot = 0.3F + rasp * 0.5F;
        shell.zRot = Mth.sin(limbSwing * 0.8F) * 0.06F * limbSwingAmount;
        shell.xRot = -0.18F; // the coil leans back over the tail
        float wave = Mth.sin(ageInTicks * 0.1F) * 0.12F;
        stalkL.xRot = -0.35F + wave;
        stalkR.xRot = -0.35F - wave;
    }
}
