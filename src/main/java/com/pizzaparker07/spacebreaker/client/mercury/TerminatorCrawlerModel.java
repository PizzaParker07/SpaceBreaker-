package com.pizzaparker07.spacebreaker.client.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mercury.TerminatorCrawler;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Terminator Crawler: a long segmented body of heat-shield plates, a wedge head with ember eyes, ten short legs. */
public class TerminatorCrawlerModel extends HierarchicalModel<TerminatorCrawler> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("terminator_crawler"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[10];

    public TerminatorCrawlerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        for (int i = 0; i < 10; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -3F, -9F, 8F, 3F, 18F), PartPose.offset(0F, 22F, 1F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 22).addBox(-3F, -1.5F, -4F, 6F, 3F, 4F), PartPose.offset(0F, 20.5F, -8F));
        for (int i = 0; i < 5; i++) {
            float z = -6F + i * 3.5F;
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(22, 22).addBox(0F, -0.5F, -0.5F, 6F, 1F, 1F),
                    PartPose.offset(3.5F, 21.5F, z));
            root.addOrReplaceChild("leg" + (i + 5), CubeListBuilder.create().texOffs(22, 22).mirror().addBox(-6F, -0.5F, -0.5F, 6F, 1F, 1F),
                    PartPose.offset(-3.5F, 21.5F, z));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TerminatorCrawler c, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean down = c.isBurrowed();
        float sink = down ? 2.5F : 0F;
        body.y = 22F + sink;
        head.y = 20.5F + sink;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        for (int i = 0; i < 5; i++) {
            float wave = Mth.cos(limbSwing * 1.4F - i * 0.9F) * 0.45F * limbSwingAmount;
            legs[i].y = legs[i + 5].y = 21.5F + sink;
            legs[i].yRot = wave;
            legs[i + 5].yRot = wave;
            legs[i].zRot = down ? 0F : 0.45F;
            legs[i + 5].zRot = down ? 0F : -0.45F;
        }
    }
}
