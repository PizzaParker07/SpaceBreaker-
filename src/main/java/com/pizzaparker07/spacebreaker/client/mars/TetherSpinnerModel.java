package com.pizzaparker07.spacebreaker.client.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.entity.TetherSpinner;
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
 * Tether Spinner: a low round body with a pale back plate, a small blunt head, a pale spinneret at the tail, two
 * short front pairs of legs and a long hind pair for hopping. In the air all legs fold in under the body.
 * UVs: tools/textures/modules/mars_moons.py.
 */
public class TetherSpinnerModel extends HierarchicalModel<TetherSpinner> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("tether_spinner"), "main");
    /** Leg spread on the ground (radians down from horizontal) and when tucked in the air. */
    private static final float SPREAD = 0.93F;
    private static final float HIND = 0.6F;
    private static final float TUCK = 1.35F;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] left = new ModelPart[3];
    private final ModelPart[] right = new ModelPart[3];

    public TetherSpinnerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < 3; i++) {
            left[i] = root.getChild("leg_left_" + i);
            right[i] = root.getChild("leg_right_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -3F, 6F, 3F, 6F), PartPose.offset(0F, 21F, 0F));
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, -1F, -2F, 4F, 1F, 4F), PartPose.offset(0F, 18F, 0.5F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-2F, -2F, -3F, 4F, 2F, 3F), PartPose.offset(0F, 20.5F, -3F));
        root.addOrReplaceChild("spinneret", CubeListBuilder.create().texOffs(40, 0).addBox(-1F, -1F, 0F, 2F, 2F, 2F), PartPose.offset(0F, 20F, 3F));
        float[] z = {-2F, 0F, 2.2F};
        for (int i = 0; i < 3; i++) {
            boolean hind = i == 2;
            int v = hind ? 26 : 12;
            float len = hind ? 7F : 5F;
            float down = hind ? HIND : SPREAD;
            float yaw = (i - 1) * 0.45F;
            root.addOrReplaceChild("leg_left_" + i, CubeListBuilder.create().texOffs(0, v).addBox(0F, -0.5F, -0.5F, len, 1F, 1F),
                    PartPose.offsetAndRotation(3F, 20F, z[i], 0F, -yaw, down));
            root.addOrReplaceChild("leg_right_" + i, CubeListBuilder.create().texOffs(0, v).mirror().addBox(-len, -0.5F, -0.5F, len, 1F, 1F),
                    PartPose.offsetAndRotation(-3F, 20F, z[i], 0F, yaw, -down));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TetherSpinner spinner, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        boolean air = !spinner.onGround();
        for (int i = 0; i < 3; i++) {
            float base = i == 2 ? HIND : SPREAD;
            float step = air ? 0F : Mth.sin(limbSwing * 1.4F + i * 2.1F) * 0.35F * limbSwingAmount;
            // in the air the legs fold steeply in under the body; on the ground they splay and step
            float down = air ? TUCK : base;
            left[i].zRot = down + step;
            right[i].zRot = -down + step;
        }
    }
}
