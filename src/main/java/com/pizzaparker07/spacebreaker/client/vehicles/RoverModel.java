package com.pizzaparker07.spacebreaker.client.vehicles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Open two-seat rover in the spirit of the Apollo LRV: a flat aluminium chassis on four wire-mesh wheels with
 * fenders, tandem seats, a console with headlights at the front, a cargo box, a high-gain antenna mast, and the
 * optional roof solar panel and rear RTG. Front is model -Z; the ground is model y = 24. UVs: tools/textures/modules/rover.py.
 */
public class RoverModel extends EntityModel<RoverEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Spacebreaker.id("rover"), "main");
    private static final float[][] WHEELS = {{13, 19, -12}, {-13, 19, -12}, {13, 19, 12}, {-13, 19, 12}};
    private final ModelPart body;
    private final ModelPart solar;
    private final ModelPart rtg;
    private final ModelPart[] wheels = new ModelPart[4];

    public RoverModel(ModelPart root) {
        this.body = root.getChild("body");
        this.solar = body.getChild("solar");
        this.rtg = body.getChild("rtg");
        for (int i = 0; i < 4; i++) {
            wheels[i] = root.getChild("wheel" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11F, 14F, -18F, 22F, 3F, 36F)                 // chassis deck
                .texOffs(0, 40).addBox(-4F, 12F, -8F, 8F, 2F, 7F)                    // driver seat
                .texOffs(0, 50).addBox(-4F, 4F, -1F, 8F, 8F, 1F)
                .texOffs(0, 40).addBox(-4F, 12F, 5F, 8F, 2F, 7F)                     // passenger seat
                .texOffs(0, 50).addBox(-4F, 4F, 12F, 8F, 8F, 1F)
                .texOffs(32, 40).addBox(-7F, 9F, -14F, 14F, 5F, 3F)                  // console
                .texOffs(32, 49).addBox(-9F, 12F, -19F, 18F, 3F, 2F)                 // headlight bar
                .texOffs(72, 56).addBox(-7F, 9F, 13F, 14F, 5F, 5F)                   // cargo box
                .texOffs(72, 40).addBox(8F, 0F, 14F, 1F, 14F, 1F)                    // antenna mast
                .texOffs(78, 40).addBox(4.5F, -4F, 15F, 8F, 8F, 1F),                 // high-gain dish
                PartPose.ZERO);
        for (int s = -1; s <= 1; s += 2) {
            for (int a = -1; a <= 1; a += 2) {
                body.addOrReplaceChild("fender_" + (s < 0 ? "l" : "r") + (a < 0 ? "f" : "b"), CubeListBuilder.create()
                        .texOffs(30, 60).addBox(-3F, 0F, -7F, 6F, 1F, 14F), PartPose.offset(13F * s, 13F, 12F * a));
            }
        }
        body.addOrReplaceChild("solar", CubeListBuilder.create()
                .texOffs(0, 82).addBox(-12F, 2F, 2F, 24F, 1F, 16F)
                .texOffs(72, 40).addBox(-11F, 3F, 3F, 1F, 11F, 1F)
                .texOffs(72, 40).addBox(10F, 3F, 3F, 1F, 11F, 1F)
                .texOffs(72, 40).addBox(-11F, 3F, 16F, 1F, 11F, 1F), PartPose.ZERO);
        body.addOrReplaceChild("rtg", CubeListBuilder.create()
                .texOffs(82, 82).addBox(-12F, 6F, 12F, 5F, 8F, 5F), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            PartDefinition w = root.addOrReplaceChild("wheel" + i, CubeListBuilder.create()
                    .texOffs(0, 60).addBox(-2F, -5F, -5F, 4F, 10F, 10F), PartPose.offset(WHEELS[i][0], WHEELS[i][1], WHEELS[i][2]));
            w.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(0, 60).addBox(-1.9F, -5F, -5F, 3.8F, 10F, 10F),
                    PartPose.rotation(Mth.PI / 4F, 0F, 0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Body bounce from the suspension (model units, y down), so overlays can follow the roof panel. */
    public float bodyY() {
        return body.y;
    }

    @Override
    public void setupAnim(RoverEntity rover, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pt = ageInTicks - rover.tickCount;
        float spin = Mth.lerp(pt, rover.wheelSpinO, rover.wheelSpin);
        float squash = Mth.lerp(pt, rover.suspensionO, rover.suspension);
        body.y = Mth.clamp(squash, -0.5F, 1F) * 2.5F;
        for (ModelPart w : wheels) {
            w.xRot = spin;
        }
        solar.visible = rover.hasSolar();
        rtg.visible = rover.hasRtg();
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        body.render(pose, buffer, light, overlay, color);
        for (ModelPart w : wheels) {
            w.render(pose, buffer, light, overlay, color);
        }
    }
}
