package com.pizzaparker07.spacebreaker.client.uranus;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.uranus.entity.CliffClinger;
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
 * Cliff Clinger: a flat central disc with five arms spread round it like a star. The arms ripple as it walks; on a wall
 * the whole body tips up flat against it. UVs: tools/textures/modules/uranus.py.
 */
public class CliffClingerModel extends HierarchicalModel<CliffClinger> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("cliff_clinger"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] arms = new ModelPart[5];

    public CliffClingerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 5; i++) {
            arms[i] = body.getChild("arm" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -2F, -3F, 6F, 2F, 6F),
                PartPose.offset(0F, 24F, 0F));
        for (int i = 0; i < 5; i++) {
            float a = i * Mth.TWO_PI / 5;
            body.addOrReplaceChild("arm" + i, CubeListBuilder.create().texOffs(0, 8).addBox(-1F, -1F, -8F, 2F, 1F, 6F),
                    PartPose.rotation(0F, a, 0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CliffClinger c, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        body.xRot = c.isClimbing() ? -Mth.HALF_PI : 0F;
        body.z = c.isClimbing() ? -2F : 0F;
        body.y = c.isClimbing() ? 19F : 24F;
        for (int i = 0; i < 5; i++) {
            arms[i].xRot = Mth.sin(limbSwing * 1.2F + i * 1.25F) * 0.35F * limbSwingAmount + Mth.sin(ageInTicks * 0.07F + i) * 0.05F;
        }
    }
}
