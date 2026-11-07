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
 * Open two-seat rover in the spirit of the crewed lunar rovers. A dark grey chassis frame with white side panels and
 * a nose sloping down to the bumper and headlights; two bucket seats with side bolsters and headrests, a small control
 * console in front of the driver; stepped octagonal wheels with a patterned tread, rim and hub, under fenders that
 * wrap over them, on dampers; a rear deck with the cargo box, the optional RTG and, on posts clear of the passenger,
 * the optional solar panel; a small dish antenna at the back. Orange warning stripes only on the side panels and the
 * bumper. Front is model -Z; the ground is model y = 24. UVs (256 x 256): tools/textures/modules/rover.py.
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

    private static void seat(CubeListBuilder b, float z0) {
        b.texOffs(0, 40).addBox(-4F, 12F, z0, 8F, 3F, 7F)                // cushion on a pedestal down to the frame
                .texOffs(54, 40).addBox(-5F, 9F, z0, 1F, 4F, 7F)            // side bolsters
                .texOffs(54, 40).addBox(4F, 9F, z0, 1F, 4F, 7F)
                .texOffs(72, 40).addBox(-3F, 0F, z0 + 7.2F, 6F, 3F, 1F);    // headrest
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder b = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11F, 15F, -18F, 22F, 2F, 36F)             // chassis frame (dark)
                .texOffs(120, 0).addBox(-10F, 14F, 18F, 20F, 2F, 12F)           // rear deck
                .texOffs(120, 16).addBox(-11F, 11F, -14F, 1F, 4F, 28F)          // side panels
                .texOffs(120, 16).addBox(10F, 11F, -14F, 1F, 4F, 28F)
                .texOffs(240, 0).addBox(-8F, 13F, -21.5F, 3F, 2F, 1F)           // headlights
                .texOffs(240, 0).addBox(5F, 13F, -21.5F, 3F, 2F, 1F)
                .texOffs(184, 12).addBox(-10F, 15F, -23F, 20F, 2F, 2F)          // bumper
                .texOffs(184, 20).addBox(-5F, 9F, -13F, 10F, 4F, 3F)            // control console
                .texOffs(212, 20).addBox(2.5F, 9F, -10F, 1F, 3F, 1F)            // hand controller
                .texOffs(0, 56).addBox(-5F, 9F, 20F, 14F, 5F, 5F)               // cargo box, on the rear deck
                .texOffs(68, 56).addBox(13F, 2F, 28F, 1F, 12F, 1F)              // rear antenna mast
                .texOffs(74, 56).addBox(10.5F, -4F, 29F, 6F, 6F, 1F);           // small dish
        seat(b, -8F);
        seat(b, 5F);
        b.texOffs(32, 40).addBox(-4F, 3F, -1F, 8F, 10F, 2F)                     // seat backs
                .texOffs(32, 40).addBox(-4F, 3F, 12F, 8F, 10F, 2F);
        PartDefinition body = root.addOrReplaceChild("body", b, PartPose.ZERO);
        // the nose: a plate sloping up from the bumper to the console
        body.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(184, 0).addBox(-9F, 0F, 0F, 18F, 1F, 8F),
                PartPose.offsetAndRotation(0F, 15F, -21F, 0.5F, 0F, 0F));
        for (int s = -1; s <= 1; s += 2) {
            for (int a = -1; a <= 1; a += 2) {
                String n = (s < 0 ? "l" : "r") + (a < 0 ? "f" : "b");
                // fender over the top of the wheel with flaps down its front and back
                body.addOrReplaceChild("fender_" + n, CubeListBuilder.create()
                        .texOffs(180, 40).addBox(-3F, 0F, -7F, 6F, 1F, 14F)
                        .texOffs(184, 30).addBox(-3F, 1F, -7F, 6F, 3F, 1F)
                        .texOffs(184, 30).addBox(-3F, 1F, 6F, 6F, 3F, 1F), PartPose.offset(13F * s, 13F, 12F * a));
                // damper from the fender down to the hub, outside the wheel
                body.addOrReplaceChild("strut_" + n, CubeListBuilder.create()
                        .texOffs(220, 20).addBox(-0.5F, 0F, -0.5F, 1F, 5F, 1F), PartPose.offset(16.5F * s, 14F, 12F * a));
            }
        }
        // the solar panel rides flat on posts over the rear deck, behind the passenger's head
        CubeListBuilder solar = CubeListBuilder.create().texOffs(0, 72).addBox(-12F, 5F, 15F, 24F, 1F, 16F);
        for (float px : new float[]{-10F, 9F}) {
            for (float pz : new float[]{19F, 28F}) {
                solar.texOffs(62, 56).addBox(px, 6F, pz, 1F, 8F, 1F);
            }
        }
        body.addOrReplaceChild("solar", solar, PartPose.ZERO);
        body.addOrReplaceChild("rtg", CubeListBuilder.create()
                .texOffs(40, 56).addBox(-11F, 6F, 21F, 5F, 8F, 5F), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            // stepped octagon: three crossed blocks, tread on the rim faces, the hub sticking out of both sides
            root.addOrReplaceChild("wheel" + i, CubeListBuilder.create()
                    .texOffs(0, 100).addBox(-2F, -5F, -3F, 4F, 10F, 6F)
                    .texOffs(24, 100).addBox(-2F, -3F, -5F, 4F, 6F, 10F)
                    .texOffs(56, 100).addBox(-1.9F, -4F, -4F, 3.8F, 8F, 8F)
                    .texOffs(84, 100).addBox(-3F, -1.5F, -1.5F, 6F, 3F, 3F),
                    PartPose.offset(WHEELS[i][0], WHEELS[i][1], WHEELS[i][2]));
        }
        return LayerDefinition.create(mesh, 256, 256);
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
