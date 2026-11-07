package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.ScreeRoller;
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
 * Scree Roller: three overlapping arched plates over a soft body, a small pointed head and four short legs. Curled, the
 * plates close into a ball (head and legs hidden) that spins as it rolls. UVs: tools/textures/modules/uranus.py.
 */
public class ScreeRollerModel extends HierarchicalModel<ScreeRoller> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("scree_roller"), "main");
    private final ModelPart root;
    private final ModelPart shell;
    private final ModelPart ball;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[4];

    public ScreeRollerModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
        this.ball = root.getChild("ball");
        this.head = root.getChild("head");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -5F, -5F, 8F, 5F, 4F)
                        .texOffs(0, 9).addBox(-4.5F, -5.5F, -1.5F, 9F, 6F, 3F)
                        .texOffs(0, 18).addBox(-4F, -5F, 1F, 8F, 5F, 4F),
                PartPose.offset(0F, 21F, 0F));
        root.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(28, 0).addBox(-3.5F, -3.5F, -3.5F, 7F, 7F, 7F),
                PartPose.offset(0F, 20.5F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 14).addBox(-1.5F, -1.5F, -4F, 3F, 3F, 4F),
                PartPose.offset(0F, 19.5F, -5F));
        float[][] at = {{3F, -3F}, {-3F, -3F}, {3F, 3F}, {-3F, 3F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(42, 14).addBox(-1F, 0F, -1F, 2F, 3F, 2F),
                    PartPose.offset(at[i][0], 21F, at[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ScreeRoller r, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean rolled = r.isRolled();
        shell.visible = !rolled;
        head.visible = !rolled;
        ball.visible = rolled;
        for (ModelPart leg : legs) {
            leg.visible = !rolled;
        }
        ball.xRot = limbSwing * 0.9F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F;
        for (int i = 0; i < 4; i++) {
            float sign = (i == 0 || i == 3) ? 1F : -1F;
            legs[i].xRot = Mth.cos(limbSwing * 1.4F) * 0.7F * limbSwingAmount * sign;
        }
    }
}
