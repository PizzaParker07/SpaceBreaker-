package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.GloamHopper;
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
 * Gloam Hopper: a jerboa-like hopper of Umbriel's dark plains - a rounded mauve-grey body with two overlapping rock plates
 * on the back, a short-snouted head with big pale eyes (glowing, see the renderer) and wide flat bat-like ears, tiny fore
 * paws, long hind legs on long feet, and a long tail ending in a tuft that balances it. In the air the hind legs kick back
 * and the tail streams out straight. UVs: tools/textures/modules/uranus.py.
 */
public class GloamHopperModel extends HierarchicalModel<GloamHopper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("gloam_hopper"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftHind;
    private final ModelPart rightHind;
    private final ModelPart leftFore;
    private final ModelPart rightFore;
    private final ModelPart tail;
    private final ModelPart leftEar;
    private final ModelPart rightEar;

    public GloamHopperModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftHind = root.getChild("left_hind");
        this.rightHind = root.getChild("right_hind");
        this.leftFore = root.getChild("left_fore");
        this.rightFore = root.getChild("right_fore");
        this.tail = root.getChild("tail");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3F, -3F, 5F, 5F, 6F),
                PartPose.offsetAndRotation(0F, 18.5F, 0F, -0.25F, 0F, 0F));
        // two rock plates on the back, the rear one overlapping the front like shingles
        body.addOrReplaceChild("plate_front", CubeListBuilder.create().texOffs(22, 0).addBox(-2F, -1F, -1.5F, 4F, 1F, 3F),
                PartPose.offsetAndRotation(0F, -3F, -1.2F, 0.12F, 0F, 0F));
        body.addOrReplaceChild("plate_back", CubeListBuilder.create().texOffs(22, 0).addBox(-2F, -1F, -1.5F, 4F, 1F, 3F),
                PartPose.offsetAndRotation(0F, -2.8F, 1.4F, 0.2F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-2F, -3F, -4F, 4F, 4F, 4F)
                .texOffs(52, 4).addBox(-1F, -1F, -5F, 2F, 1F, 1F), PartPose.offset(0F, 16F, -2.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).addBox(0F, -3F, -0.5F, 3F, 3F, 1F),
                PartPose.offsetAndRotation(1.2F, -2.5F, -1.5F, 0F, -0.2F, 0.7F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-3F, -3F, -0.5F, 3F, 3F, 1F),
                PartPose.offsetAndRotation(-1.2F, -2.5F, -1.5F, 0F, 0.2F, -0.7F));
        for (int side = -1; side <= 1; side += 2) {
            String name = side > 0 ? "left" : "right";
            CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 12);
            CubeListBuilder foot = CubeListBuilder.create().texOffs(0, 20);
            CubeListBuilder fore = CubeListBuilder.create().texOffs(12, 12);
            if (side < 0) {
                thigh.mirror();
                foot.mirror();
                fore.mirror();
            }
            PartDefinition hind = root.addOrReplaceChild(name + "_hind", thigh.addBox(-1F, 0F, -2F, 2F, 4F, 4F),
                    PartPose.offset(side * 2.2F, 18.5F, 1.5F));
            hind.addOrReplaceChild("foot", foot.addBox(-1F, 0F, -3.5F, 2F, 1F, 4F), PartPose.offset(0F, 4F, 0F));
            root.addOrReplaceChild(name + "_fore", fore.addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offset(side * 1.2F, 19.5F, -3.2F));
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 12).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 8F),
                PartPose.offsetAndRotation(0F, 18F, 2.8F, 0.3F, 0F, 0F));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(36, 12).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offset(0F, 0F, 8F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GloamHopper h, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // ears twitch now and then
        float twitch = Mth.sin(ageInTicks * 0.07F) > 0.92F ? Mth.sin(ageInTicks * 1.3F) * 0.15F : 0F;
        leftEar.zRot = 0.7F + twitch;
        rightEar.zRot = -0.7F - twitch;
        // in the air the hind legs kick back, the paws tuck in and the tail streams out straight behind
        float air = h.onGround() ? 0F : 1F;
        leftHind.xRot = rightHind.xRot = air * 0.9F + Mth.cos(limbSwing * 0.8F) * 0.3F * limbSwingAmount;
        leftFore.xRot = rightFore.xRot = -air * 0.7F - Mth.cos(limbSwing * 0.8F) * 0.3F * limbSwingAmount;
        body.xRot = -0.25F - air * 0.15F;
        tail.xRot = air > 0 ? 0.05F : 0.3F + Mth.sin(ageInTicks * 0.06F) * 0.05F;
        tail.yRot = air > 0 ? 0F : Mth.sin(ageInTicks * 0.1F) * 0.25F;
    }
}
