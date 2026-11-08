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
    public static final ModelLayerLocation SPRINT_LAYER = new ModelLayerLocation(Spacebreaker.id("sprint_rover"), "main");

    /**
     * Proportions of one rover model.
     *
     * @param halfWidth  half the chassis width
     * @param wheelX     wheel centre off the middle
     * @param wheelScale wheel size (1 = 10 px across)
     * @param fenderY    top of the fenders
     * @param noseLength length of the sloping nose plate
     * @param noseAngle  its slope, radians
     */
    public record Style(float halfWidth, float wheelX, float wheelScale, float fenderY, float noseLength, float noseAngle, boolean cabin,
                        boolean floats) {
        public Style(float halfWidth, float wheelX, float wheelScale, float fenderY, float noseLength, float noseAngle) {
            this(halfWidth, wheelX, wheelScale, fenderY, noseLength, noseAngle, false, false);
        }

        public Style(float halfWidth, float wheelX, float wheelScale, float fenderY, float noseLength, float noseAngle, boolean cabin) {
            this(halfWidth, wheelX, wheelScale, fenderY, noseLength, noseAngle, cabin, false);
        }

        public float wheelY() {
            return 24F - 5F * wheelScale;
        }

        /** Top face of the solar panel (model y): on posts over the rear deck, or on the cabin roof. */
        public float panelTop() {
            return cabin ? -13F : 5F;
        }

        /** Front edge of the solar panel (model z); it is 16 deep. */
        public float panelFront() {
            return cabin ? -12F : 15F;
        }
    }

    public static final Style STANDARD = new Style(11F, 13F, 1F, 13F, 8F, 0.5F);
    /** Narrow and low with big wheels and a long shallow nose. */
    public static final Style SPRINT = new Style(9F, 12F, 1.25F, 10.5F, 10F, 0.38F);
    /** The standard chassis under a closed cabin with windows; the solar panel goes on the cabin roof. */
    public static final Style PRESSURIZED = new Style(11F, 13F, 1F, 13F, 8F, 0.5F, true);
    public static final ModelLayerLocation PRESSURIZED_LAYER = new ModelLayerLocation(Spacebreaker.id("pressurized_rover"), "main");
    /** The pressurised cabin with sealed side floats and a stern propeller. */
    public static final Style AMPHIBIOUS = new Style(11F, 13F, 1F, 13F, 8F, 0.5F, true, true);
    public static final ModelLayerLocation AMPHIBIOUS_LAYER = new ModelLayerLocation(Spacebreaker.id("amphibious_rover"), "main");

    private ModelPart propeller;

    public Style style() {
        return style;
    }

    private final Style style;
    private final ModelPart body;
    private final ModelPart solar;
    private final ModelPart rtg;
    private final ModelPart[] wheels = new ModelPart[4];

    public RoverModel(ModelPart root) {
        this(root, STANDARD);
    }

    public RoverModel(ModelPart root, Style style) {
        this.style = style;
        this.body = root.getChild("body");
        this.solar = body.getChild("solar");
        this.rtg = body.getChild("rtg");
        for (int i = 0; i < 4; i++) {
            wheels[i] = root.getChild("wheel" + i);
            wheels[i].xScale = wheels[i].yScale = wheels[i].zScale = style.wheelScale();
        }
        this.propeller = style.floats() ? body.getChild("propeller") : null;
        if (style.cabin()) {
            this.dash = body.getChild("dash");
            for (int i = 0; i < 3; i++) {
                dashLamps[i] = body.getChild("dash_lamp" + i);
            }
        } else {
            this.dash = null;
        }
    }

    private final ModelPart dash;
    private final ModelPart[] dashLamps = new ModelPart[3];

    /**
     * The cabin dashboard's lights (with the lights texture, full bright): the screen glows steadily, the three lamps
     * blink in their own rhythms. Nothing for open rovers.
     */
    public void renderDashLights(PoseStack pose, VertexConsumer vc, float ageInTicks) {
        if (dash == null) {
            return;
        }
        pose.pushPose();
        body.translateAndRotate(pose);
        dash.render(pose, vc, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        int t = (int) ageInTicks;
        boolean[] on = {true, (t / 10) % 2 == 0, (t / 23) % 3 != 0};
        for (int i = 0; i < 3; i++) {
            if (on[i]) {
                dashLamps[i].visible = true;
                dashLamps[i].render(pose, vc, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                dashLamps[i].visible = false;
            }
        }
        pose.popPose();
    }

    private static void seat(CubeListBuilder b, float z0) {
        b.texOffs(0, 40).addBox(-4F, 12F, z0, 8F, 3F, 7F)                // cushion on a pedestal down to the frame
                .texOffs(54, 40).addBox(-5F, 9F, z0, 1F, 4F, 7F)            // side bolsters
                .texOffs(54, 40).addBox(4F, 9F, z0, 1F, 4F, 7F)
                .texOffs(72, 40).addBox(-3F, 0F, z0 + 7.2F, 6F, 3F, 1F);    // headrest
    }

    public static LayerDefinition createLayer() {
        return createLayer(STANDARD);
    }

    public static LayerDefinition createSprintLayer() {
        return createLayer(SPRINT);
    }

    public static LayerDefinition createPressurizedLayer() {
        return createLayer(PRESSURIZED);
    }

    public static LayerDefinition createAmphibiousLayer() {
        return createLayer(AMPHIBIOUS);
    }

    public static LayerDefinition createLayer(Style st) {
        float hw = st.halfWidth();
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder b = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-hw, 15F, -18F, 2 * hw, 2F, 36F)          // chassis frame (dark)
                .texOffs(120, 0).addBox(-hw + 1, 14F, 18F, 2 * hw - 2, 2F, 12F) // rear deck
                .texOffs(120, 16).addBox(-hw, 11F, -14F, 1F, 4F, 28F)           // side panels
                .texOffs(120, 16).addBox(hw - 1, 11F, -14F, 1F, 4F, 28F)
                .texOffs(240, 0).addBox(-8F, 13F, -21.5F, 3F, 2F, 1F)           // headlights
                .texOffs(240, 0).addBox(5F, 13F, -21.5F, 3F, 2F, 1F)
                .texOffs(184, 12).addBox(-hw + 1, 15F, -23F, 2 * hw - 2, 2F, 2F) // bumper
                .texOffs(184, 20).addBox(-5F, 9F, -13F, 10F, 4F, 3F)            // control console
                .texOffs(212, 20).addBox(2.5F, 9F, -10F, 1F, 3F, 1F)            // hand controller
                .texOffs(0, 56).addBox(-5F, 9F, 20F, 14F, 5F, 5F)               // cargo box, on the rear deck
                .texOffs(68, 56).addBox(hw + 2, 2F, 28F, 1F, 12F, 1F)           // rear antenna mast
                .texOffs(74, 56).addBox(hw - 0.5F, -4F, 29F, 6F, 6F, 1F);       // small dish
        seat(b, -8F);
        seat(b, 5F);
        b.texOffs(32, 40).addBox(-4F, 3F, -1F, 8F, 10F, 2F)                     // seat backs
                .texOffs(32, 40).addBox(-4F, 3F, 12F, 8F, 10F, 2F);
        PartDefinition body = root.addOrReplaceChild("body", b, PartPose.ZERO);
        // the nose: a plate sloping up from the bumper to the console
        body.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(184, 0).addBox(-hw + 2, 0F, 0F, 2 * hw - 4, 1F, st.noseLength()),
                PartPose.offsetAndRotation(0F, 15F, -21F - (st.noseLength() - 8F), st.noseAngle(), 0F, 0F));
        for (int s = -1; s <= 1; s += 2) {
            for (int a = -1; a <= 1; a += 2) {
                String n = (s < 0 ? "l" : "r") + (a < 0 ? "f" : "b");
                // fender over the top of the wheel with flaps down its front and back
                body.addOrReplaceChild("fender_" + n, CubeListBuilder.create()
                        .texOffs(180, 40).addBox(-3F, 0F, -7F, 6F, 1F, 14F)
                        .texOffs(184, 30).addBox(-3F, 1F, -7F, 6F, 3F, 1F)
                        .texOffs(184, 30).addBox(-3F, 1F, 6F, 6F, 3F, 1F), PartPose.offset(st.wheelX() * s, st.fenderY(), 12F * a));
                // damper from the fender down to the hub, outside the wheel
                body.addOrReplaceChild("strut_" + n, CubeListBuilder.create()
                        .texOffs(220, 20).addBox(-0.5F, 0F, -0.5F, 1F, st.wheelY() - st.fenderY() - 1F, 1F),
                        PartPose.offset((st.wheelX() + 3F * st.wheelScale() + 0.5F) * s, st.fenderY() + 1F, 12F * a));
            }
        }
        if (st.cabin()) {
            // closed cabin over both seats: thin walls whose windows are cut out of the texture, so the crew shows
            body.addOrReplaceChild("cabin", CubeListBuilder.create()
                    // the cabin sits 2 px forward of the seats' centre so a seated driver's boots stay inside the windscreen wall
                    .texOffs(0, 140).addBox(-hw, -11F, -18F, 2 * hw, 1F, 32F)              // roof
                    .texOffs(110, 140).addBox(-hw, -10F, -18F, 1F, 23F, 32F)               // side walls
                    .texOffs(110, 140).addBox(hw - 1, -10F, -18F, 1F, 23F, 32F)
                    .texOffs(180, 140).addBox(-hw + 1, -10F, -18F, 2 * hw - 2, 23F, 1F)    // windscreen wall
                    .texOffs(180, 170).addBox(-hw + 1, -10F, 13F, 2 * hw - 2, 23F, 1F),    // rear wall
                    PartPose.ZERO);
            // low dashboard across the cabin under the windscreen, below the crew's eye line, with three lamps on it
            body.addOrReplaceChild("dash", CubeListBuilder.create().texOffs(130, 200).addBox(-hw + 1, -1F, -17F, 2 * hw - 2, 2F, 3F), PartPose.ZERO);
            for (int i = 0; i < 3; i++) {
                body.addOrReplaceChild("dash_lamp" + i, CubeListBuilder.create().texOffs(160 + i * 4, 210)
                        .addBox(-hw + 3.2F + i * 1.6F, -1.5F, -16.2F, 0.6F, 0.5F, 0.6F), PartPose.ZERO);
            }
        }
        if (st.floats()) {
            // sealed floats along both sides, outside the wheels, and a propeller at the stern
            for (int sd = -1; sd <= 1; sd += 2) {
                body.addOrReplaceChild("float_" + (sd < 0 ? "l" : "r"), CubeListBuilder.create()
                        .texOffs(0, 200).addBox(-1.5F, -2F, -18F, 3F, 4F, 36F), PartPose.offset((st.wheelX() + 4.5F) * sd, 15F, 0F));
            }
            body.addOrReplaceChild("propeller", CubeListBuilder.create()
                    .texOffs(90, 200).addBox(-1F, -1F, -1F, 2F, 2F, 2F)
                    .texOffs(100, 200).addBox(-0.5F, -3.5F, -0.3F, 1F, 7F, 0.6F)
                    .texOffs(100, 200).addBox(-3.5F, -0.5F, -0.3F, 7F, 1F, 0.6F), PartPose.offset(0F, 18F, 31F));
        }
        // the solar panel: flat on posts over the rear deck, behind the passenger's head; on the roof of a cabin
        CubeListBuilder solar = CubeListBuilder.create().texOffs(0, 72).addBox(-12F, st.panelTop(), st.panelFront(), 24F, 1F, 16F);
        if (!st.cabin()) {
            for (float px : new float[]{-10F, 9F}) {
                for (float pz : new float[]{19F, 28F}) {
                    solar.texOffs(62, 56).addBox(px, 6F, pz, 1F, 8F, 1F);
                }
            }
        }
        body.addOrReplaceChild("solar", solar, PartPose.ZERO);
        body.addOrReplaceChild("rtg", CubeListBuilder.create()
                .texOffs(40, 56).addBox(-hw, 6F, 21F, 5F, 8F, 5F), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            // stepped octagon: three crossed blocks, tread on the rim faces, the hub sticking out of both sides
            root.addOrReplaceChild("wheel" + i, CubeListBuilder.create()
                    .texOffs(0, 100).addBox(-2F, -5F, -3F, 4F, 10F, 6F)
                    .texOffs(24, 100).addBox(-2F, -3F, -5F, 4F, 6F, 10F)
                    .texOffs(56, 100).addBox(-1.9F, -4F, -4F, 3.8F, 8F, 8F)
                    .texOffs(84, 100).addBox(-3F, -1.5F, -1.5F, 6F, 3F, 3F),
                    PartPose.offset(st.wheelX() * (i % 2 == 0 ? 1 : -1), st.wheelY(), i < 2 ? -12F : 12F));
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
        if (propeller != null) {
            propeller.zRot = spin * 3F;
        }
        rtg.visible = rover.hasRtg();
        // the dashboard lamps only draw in their own lit pass (renderDashLights), never with the body
        for (ModelPart lamp : dashLamps) {
            if (lamp != null) {
                lamp.visible = false;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        body.render(pose, buffer, light, overlay, color);
        for (ModelPart w : wheels) {
            w.render(pose, buffer, light, overlay, color);
        }
    }
}
