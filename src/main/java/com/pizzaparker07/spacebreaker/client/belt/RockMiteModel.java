package com.pizzaparker07.spacebreaker.client.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.entity.RockMite;
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
 * Rock Mite: a flat, armoured grazer built from three overlapping carapace plates and a short tail plate, crusted with
 * pale carbonate along the plate edges and a low salt ridge along the spine. A broad, blunt head carries a pair of
 * mandibles and two short feelers; six short legs stick out to the sides. UVs: tools/textures/modules/belt.py.
 */
public class RockMiteModel extends HierarchicalModel<RockMite> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rock_mite"), "main");
    private static final float Y = 22F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rear;
    private final ModelPart tail;
    private final ModelPart feelerLeft;
    private final ModelPart feelerRight;
    private final ModelPart jawLeft;
    private final ModelPart jawRight;
    private final ModelPart[] legs = new ModelPart[6];

    public RockMiteModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rear = body.getChild("rear");
        this.tail = rear.getChild("tail");
        this.feelerLeft = head.getChild("feeler_left");
        this.feelerRight = head.getChild("feeler_right");
        this.jawLeft = head.getChild("jaw_left");
        this.jawRight = head.getChild("jaw_right");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // middle plate is the body; the front and rear plates overlap it, lower, like roof tiles
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 6).addBox(-4.5F, -3F, -2F, 9F, 3F, 4F),
                PartPose.offset(0F, Y, 0F));
        body.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(48, 0).addBox(-1F, -0.5F, -2F, 2F, 1F, 4F),
                PartPose.offset(0F, -3F, 0F));
        body.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -2F, -4F, 8F, 2F, 4F),
                PartPose.offsetAndRotation(0F, -0.3F, -1.5F, 0.08F, 0F, 0F));
        PartDefinition rear = body.addOrReplaceChild("rear", CubeListBuilder.create().texOffs(0, 13).addBox(-4F, -2F, 0F, 8F, 2F, 4F),
                PartPose.offsetAndRotation(0F, -0.3F, 1.5F, -0.08F, 0F, 0F));
        rear.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 19).addBox(-3F, -1F, 0F, 6F, 1F, 3F),
                PartPose.offsetAndRotation(0F, 0.2F, 3.5F, -0.12F, 0F, 0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-3F, -2.5F, -4F, 6F, 3F, 4F),
                PartPose.offset(0F, 0F, -5F));
        head.addOrReplaceChild("jaw_left", CubeListBuilder.create().texOffs(28, 8).addBox(-0.5F, -0.5F, -2F, 1F, 1F, 2F),
                PartPose.offsetAndRotation(1.5F, 0F, -3.8F, 0F, 0.35F, 0F));
        head.addOrReplaceChild("jaw_right", CubeListBuilder.create().texOffs(28, 8).mirror().addBox(-0.5F, -0.5F, -2F, 1F, 1F, 2F),
                PartPose.offsetAndRotation(-1.5F, 0F, -3.8F, 0F, -0.35F, 0F));
        head.addOrReplaceChild("feeler_left", CubeListBuilder.create().texOffs(36, 8).addBox(-0.5F, -0.5F, -4F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(2F, -2F, -3.5F, 0.5F, 0.55F, 0F));
        head.addOrReplaceChild("feeler_right", CubeListBuilder.create().texOffs(36, 8).mirror().addBox(-0.5F, -0.5F, -4F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(-2F, -2F, -3.5F, 0.5F, -0.55F, 0F));
        float[] z = {-3F, 0F, 3F};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(28, 14).addBox(0F, -0.5F, -0.5F, 4F, 1F, 1F),
                    PartPose.offset(3.5F, Y - 0.5F, z[i]));
            root.addOrReplaceChild("leg" + (i + 3), CubeListBuilder.create().texOffs(28, 14).mirror().addBox(-4F, -0.5F, -0.5F, 4F, 1F, 1F),
                    PartPose.offset(-3.5F, Y - 0.5F, z[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(RockMite mite, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean hidden = mite.isHidden();
        float sink = hidden ? 2.5F : 0F;
        body.y = Y + sink;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        // hidden: the head and tail tuck under the plates; walking: the plates flex a little
        head.xRot = hidden ? 0.35F : 0F;
        float flex = hidden ? 0F : Mth.sin(limbSwing * 0.8F) * 0.06F * limbSwingAmount;
        rear.xRot = -0.08F + flex;
        tail.xRot = hidden ? 0.4F : -0.12F + flex * 2;
        float feel = hidden ? 0F : Mth.sin(ageInTicks * 0.25F) * 0.15F;
        feelerLeft.yRot = 0.55F + feel;
        feelerRight.yRot = -0.55F + feel;
        float chew = hidden ? 0F : Math.max(0F, Mth.sin(ageInTicks * 0.5F)) * 0.25F;
        jawLeft.yRot = 0.35F - chew;
        jawRight.yRot = -0.35F + chew;
        float[] fan = {0.45F, 0F, -0.45F};
        for (int i = 0; i < 3; i++) {
            float phase = limbSwing * 1.7F + (i % 2 == 0 ? 0F : Mth.PI);
            float wave = Mth.cos(phase) * 0.45F * limbSwingAmount;
            float lift = Math.max(0, Mth.sin(phase)) * 0.4F * limbSwingAmount;
            legs[i].y = legs[i + 3].y = Y - 0.5F + sink * 0.8F;
            legs[i].yRot = fan[i] + wave;
            legs[i + 3].yRot = -fan[i] - wave;
            legs[i].zRot = hidden ? 0.05F : 0.35F - lift;
            legs[i + 3].zRot = hidden ? -0.05F : -0.35F + lift;
        }
    }
}
