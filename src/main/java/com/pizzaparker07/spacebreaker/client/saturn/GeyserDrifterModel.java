package com.pizzaparker07.spacebreaker.client.saturn;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.saturn.entity.GeyserDrifter;
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
 * Geyser Drifter: a translucent bell with a frilled rim and four trailing threads; the bell contracts and the threads
 * sway with each pulse. UVs: tools/textures/modules/saturn.py.
 */
public class GeyserDrifterModel extends HierarchicalModel<GeyserDrifter> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("geyser_drifter"), "main");
    private final ModelPart root;
    private final ModelPart bell;
    private final ModelPart[] threads = new ModelPart[4];

    public GeyserDrifterModel(ModelPart root) {
        this.root = root;
        this.bell = root.getChild("bell");
        for (int i = 0; i < 4; i++) {
            threads[i] = root.getChild("thread" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bell = root.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -5F, -4F, 8F, 5F, 8F)
                .texOffs(0, 13).addBox(-3F, -7F, -3F, 6F, 2F, 6F), PartPose.offset(0F, 14F, 0F));
        bell.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(24, 13).addBox(-4.5F, 0F, -4.5F, 9F, 1F, 9F), PartPose.offset(0F, 0F, 0F));
        float[][] at = {{2F, 2F}, {-2F, 2F}, {2F, -2F}, {-2F, -2F}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("thread" + i, CubeListBuilder.create().texOffs(32 + i * 4, 0).addBox(-0.5F, 0F, -0.5F, 1F, 8F, 1F),
                    PartPose.offset(at[i][0], 15F, at[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GeyserDrifter d, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = Mth.sin(ageInTicks * 0.15F);
        bell.y = 14F + pulse * 0.6F;
        bell.xScale = bell.zScale = 1F - 0.08F * pulse;
        bell.yScale = 1F + 0.06F * pulse;
        for (int i = 0; i < 4; i++) {
            threads[i].y = 15F + pulse * 0.6F;
            threads[i].xRot = Mth.sin(ageInTicks * 0.1F + i * 1.7F) * 0.25F;
            threads[i].zRot = Mth.cos(ageInTicks * 0.08F + i * 2.3F) * 0.25F;
        }
    }
}
