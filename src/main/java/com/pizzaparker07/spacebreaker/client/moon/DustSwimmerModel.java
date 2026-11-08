package com.pizzaparker07.spacebreaker.client.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.entity.DustSwimmer;
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
 * Dust Swimmer: a flat, broad glider of the regolith. A shovel snout with a pale sifting slit and two eyes on stalks,
 * wide pectoral fins with pale edges that ripple, a ridge of three dark plates along the spine (all that shows above
 * the dust while it swims), and a long tail with an upright fin. UV layout matches tools/textures/modules/moon_creatures.py.
 */
public class DustSwimmerModel<T extends DustSwimmer> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("dust_swimmer"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart tail;
    private final ModelPart tailFin;
    private final ModelPart ridge;

    public DustSwimmerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
        this.tail = body.getChild("tail");
        this.tailFin = tail.getChild("tail_fin");
        this.ridge = body.getChild("ridge");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -3F, -7F, 8F, 3F, 14F), PartPose.offset(0F, 22F, 0F));
        // shovel snout, wider than it is tall, and eye stalks
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 17).addBox(-3.5F, -2F, -4F, 7F, 2F, 4F), PartPose.offset(0F, -0.5F, -7F));
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(22, 17).addBox(-3F, -5F, -2.5F, 1F, 3F, 1F)
                .texOffs(22, 17).mirror().addBox(2F, -5F, -2.5F, 1F, 3F, 1F), PartPose.ZERO);
        // broad pectoral fins with a swept tip
        body.addOrReplaceChild("left_fin", CubeListBuilder.create()
                .texOffs(0, 34).addBox(0F, 0F, -4F, 7F, 1F, 8F)
                .texOffs(30, 34).addBox(7F, 0F, -1F, 3F, 1F, 5F), PartPose.offset(4F, -1F, -1F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create()
                .texOffs(0, 34).mirror().addBox(-7F, 0F, -4F, 7F, 1F, 8F)
                .texOffs(30, 34).mirror().addBox(-10F, 0F, -1F, 3F, 1F, 5F), PartPose.offset(-4F, -1F, -1F));
        // the spine ridge: three plates, tallest in front, that cut the dust like fins
        body.addOrReplaceChild("ridge", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-0.5F, -4F, -5F, 1F, 4F, 3F)
                .texOffs(52, 0).addBox(-0.5F, -3F, -1F, 1F, 3F, 3F)
                .texOffs(44, 8).addBox(-0.5F, -2F, 3F, 1F, 2F, 3F), PartPose.offset(0F, -3F, 0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 25).addBox(-1.5F, -1F, 0F, 3F, 2F, 7F), PartPose.offset(0F, -1.5F, 7F));
        tail.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(20, 25).addBox(-0.5F, -3F, 0F, 1F, 5F, 4F), PartPose.offset(0F, 0F, 6F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T swimmer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = 0.25F + Math.min(1F, limbSwingAmount * 2F);
        float t = ageInTicks * 0.22F * (0.6F + speed);
        float wave = Mth.sin(t) * 0.35F * speed;
        leftFin.zRot = wave;
        rightFin.zRot = -wave;
        tail.yRot = Mth.sin(t - 0.8F) * 0.3F * speed;
        tailFin.yRot = Mth.sin(t - 1.6F) * 0.35F * speed;
        ridge.zRot = Mth.sin(t * 0.5F) * 0.05F;
        body.yRot = Mth.sin(t + 1F) * 0.05F * speed;
    }
}
