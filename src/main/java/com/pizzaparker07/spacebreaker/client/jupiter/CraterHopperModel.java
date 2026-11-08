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
 * Crater Hopper: a low, long-backed hopper of Callisto's cratered plains. Three white ring plates like crater rims run
 * down its dark brown-grey back; a blunt head carries two bony horn plates swept back over the neck; spring-jointed hind
 * legs (thigh forward, shin back, long flat foot), tiny forelegs and a short stubby tail. In a leap the hind legs kick
 * straight out behind. UVs: tools/textures/modules/jupiter.py.
 */
public class CraterHopperModel extends HierarchicalModel<CraterHopper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("crater_hopper"), "main");
    private static final float THIGH = -0.5F;
    private static final float SHIN = 1.0F;
    private static final float FOOT = -0.5F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] thighs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart tail;

    public CraterHopperModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        for (int i = 0; i < 2; i++) {
            thighs[i] = root.getChild(i == 0 ? "leg_left" : "leg_right");
            shins[i] = thighs[i].getChild("shin");
            feet[i] = shins[i].getChild("foot");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -4F, -3.5F, 6F, 4F, 7F),
                PartPose.offsetAndRotation(0F, 19.5F, 0F, -0.12F, 0F, 0F));
        // three ring plates down the back, like the rims of craters
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(26, 0).addBox(-2F, -1F, -1F, 4F, 1F, 2F),
                    PartPose.offsetAndRotation(0F, -4F, -2.3F + i * 2.4F, 0.1F, 0F, 0F));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(38, 0).addBox(-2F, -3F, -4F, 4F, 3F, 4F)
                .texOffs(54, 0).addBox(-1F, -1F, -5F, 2F, 1F, 1F), PartPose.offset(0F, 17F, -3.2F));
        // bony horn plates swept back over the neck
        head.addOrReplaceChild("plate_left", CubeListBuilder.create().texOffs(26, 4).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(1.2F, -2.6F, -2.5F, 0.15F, 0.3F, 0F));
        head.addOrReplaceChild("plate_right", CubeListBuilder.create().texOffs(26, 4).mirror().addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(-1.2F, -2.6F, -2.5F, 0.15F, -0.3F, 0F));
        for (int side = -1; side <= 1; side += 2) {
            String name = side > 0 ? "leg_left" : "leg_right";
            CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 12);
            CubeListBuilder shin = CubeListBuilder.create().texOffs(10, 12);
            CubeListBuilder foot = CubeListBuilder.create().texOffs(14, 12);
            CubeListBuilder arm = CubeListBuilder.create().texOffs(24, 12);
            if (side < 0) {
                thigh.mirror();
                shin.mirror();
                foot.mirror();
                arm.mirror();
            }
            PartDefinition t = root.addOrReplaceChild(name, thigh.addBox(-1F, 0F, -1.5F, 2F, 3F, 3F),
                    PartPose.offsetAndRotation(side * 3F, 18F, 2F, THIGH, 0F, 0F));
            PartDefinition sh = t.addOrReplaceChild("shin", shin.addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, 3F, 1F, SHIN, 0F, 0F));
            sh.addOrReplaceChild("foot", foot.addBox(-1F, 0F, -2.5F, 2F, 1F, 3F), PartPose.offsetAndRotation(0F, 3F, 0F, FOOT, 0F, 0F));
            root.addOrReplaceChild(side > 0 ? "arm_left" : "arm_right", arm.addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F),
                    PartPose.offsetAndRotation(side * 1.5F, 19F, -3F, -0.3F, 0F, 0F));
        }
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 12).addBox(-1F, -1F, 0F, 2F, 2F, 3F),
                PartPose.offsetAndRotation(0F, 17.5F, 3.2F, -0.5F, 0F, 0F));
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
        boolean leap = h.leaping();
        // crouched on the ground the leg is folded (thigh forward, shin back, foot flat); in a leap it straightens out behind
        for (int i = 0; i < 2; i++) {
            thighs[i].xRot = leap ? 0.9F : THIGH + Mth.sin(ageInTicks * 0.08F) * 0.03F;
            shins[i].xRot = leap ? 0.3F : SHIN;
            feet[i].xRot = leap ? 0.4F : FOOT;
        }
        body.xRot = leap ? -0.3F : -0.12F;
        tail.xRot = leap ? -0.1F : -0.5F + Mth.sin(ageInTicks * 0.1F) * 0.05F;
    }
}
