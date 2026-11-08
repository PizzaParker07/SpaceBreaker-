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
 * Tether Spinner: a light, domed body built for a world with almost no weight, three pairs of long jointed legs with
 * high knees, a small head with two dark eyes and, at the back, a big pale spinneret with the coil of its tether line.
 * In the air the legs draw in under the body. UVs: tools/textures/modules/mars_moons.py.
 */
public class TetherSpinnerModel extends HierarchicalModel<TetherSpinner> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("tether_spinner"), "main");
    /** Upper leg angle up from horizontal on the ground, and drawn in when in the air. */
    private static final float KNEE = 0.75F;
    private static final float TUCK = -0.2F;
    /** Lower leg angle relative to the upper one (down to the ground). */
    private static final float SHIN = 2.0F;
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
        // the dome: a broad base, a narrower middle and a cap
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -2F, -3F, 6F, 2F, 6F), PartPose.offset(0F, 19F, 0F));
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, -2F, -2F, 4F, 2F, 4F), PartPose.offset(0F, 17F, 0F));
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(18, 16).addBox(-1F, -1F, -1F, 2F, 1F, 2F), PartPose.offset(0F, 15F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -1.5F, -2F, 3F, 2F, 2F), PartPose.offset(0F, 18.5F, -3F));
        // the spinneret: a pale bulb at the back with the coiled line
        root.addOrReplaceChild("spinneret", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 3F), PartPose.offset(0F, 18F, 3F));
        float[] z = {-2F, 0F, 2F};
        for (int i = 0; i < 3; i++) {
            float yaw = (i - 1) * 0.55F;
            PartDefinition l = root.addOrReplaceChild("leg_left_" + i, CubeListBuilder.create().texOffs(0, 12).addBox(0F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offsetAndRotation(3F, 18.5F, z[i], 0F, -yaw, -KNEE));
            l.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 26).addBox(0F, -0.5F, -0.5F, 9F, 1F, 1F),
                    PartPose.offsetAndRotation(5F, 0F, 0F, 0F, 0F, SHIN));
            PartDefinition r = root.addOrReplaceChild("leg_right_" + i, CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-5F, -0.5F, -0.5F, 5F, 1F, 1F),
                    PartPose.offsetAndRotation(-3F, 18.5F, z[i], 0F, yaw, KNEE));
            r.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 26).mirror().addBox(-9F, -0.5F, -0.5F, 9F, 1F, 1F),
                    PartPose.offsetAndRotation(-5F, 0F, 0F, 0F, 0F, -SHIN));
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
            float step = air ? 0F : Mth.sin(limbSwing * 1.4F + i * 2.1F) * 0.3F * limbSwingAmount;
            // high knees on the ground; in the air the legs draw in under the body
            float up = air ? TUCK : KNEE;
            left[i].zRot = -up + step;
            right[i].zRot = up + step;
        }
    }
}
