package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.SurveyDrone;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Survey Drone: a cube probe body with a camera lens, antenna, two solar wings and four thruster nozzles. */
public class SurveyDroneModel<T extends SurveyDrone> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("survey_drone"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart antenna;

    public SurveyDroneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.antenna = body.getChild("antenna");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -4F, -4F, 8F, 8F, 8F)
                .texOffs(32, 0).addBox(-2F, -2F, -5F, 4F, 4F, 1F), PartPose.offset(0F, 16F, 0F));
        body.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F)
                .texOffs(48, 0).addBox(-1F, -6F, -1F, 2F, 1F, 2F), PartPose.offset(1.5F, -4F, 1.5F));
        // a sensor ring round the lens
        body.addOrReplaceChild("sensor_ring", CubeListBuilder.create()
                .texOffs(32, 6).addBox(-3F, -3F, -5.5F, 6F, 1F, 1F)
                .texOffs(32, 6).addBox(-3F, 2F, -5.5F, 6F, 1F, 1F)
                .texOffs(48, 6).addBox(-3F, -2F, -5.5F, 1F, 4F, 1F)
                .texOffs(48, 6).addBox(2F, -2F, -5.5F, 1F, 4F, 1F), PartPose.ZERO);
        // solar wings held out on short booms
        body.addOrReplaceChild("booms", CubeListBuilder.create()
                .texOffs(56, 0).addBox(4F, -0.5F, -0.5F, 1F, 1F, 1F)
                .texOffs(56, 0).addBox(-5F, -0.5F, -0.5F, 1F, 1F, 1F), PartPose.ZERO);
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 16).addBox(0F, -0.5F, -3F, 9F, 1F, 6F), PartPose.offset(5F, 0F, 0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-9F, -0.5F, -3F, 9F, 1F, 6F), PartPose.offset(-5F, 0F, 0F));
        CubeListBuilder nozzle = CubeListBuilder.create().texOffs(12, 24).addBox(-1F, 0F, -1F, 2F, 2F, 2F);
        body.addOrReplaceChild("nozzle_0", nozzle, PartPose.offset(-2.5F, 4F, -2.5F));
        body.addOrReplaceChild("nozzle_1", nozzle, PartPose.offset(2.5F, 4F, -2.5F));
        body.addOrReplaceChild("nozzle_2", nozzle, PartPose.offset(-2.5F, 4F, 2.5F));
        body.addOrReplaceChild("nozzle_3", nozzle, PartPose.offset(2.5F, 4F, 2.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T drone, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        body.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
        body.y = 16F + Mth.sin(ageInTicks * 0.12F) * 0.8F;
        // solar wings slowly track the light
        float tilt = Mth.sin(ageInTicks * 0.02F) * 0.25F;
        leftWing.xRot = tilt;
        rightWing.xRot = tilt;
        antenna.zRot = Mth.sin(ageInTicks * 0.3F) * 0.03F;
    }
}
