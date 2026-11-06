package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * The four pieces of the Mk1 EVA suit. Each piece is its own model (so a lone helmet never shows a
 * backpack) and keeps the vanilla armour volume, with a few chunky extra parts: rounded helmet with a
 * wrap-around visor and side lamps, chest control box, life-support backpack with two oxygen bottles,
 * blue anodised bearing rings at the neck, shoulders, elbows, wrists, waist, knees and ankles, and
 * thick boots with soles. The UV layout of every box is mirrored in tools/textures/modules/suit.py
 * (HELMET, TORSO, LEGS, BOOTS) — keep both in sync.
 */
public class EvaSuitModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation HELMET = layer("helmet");
    public static final ModelLayerLocation TORSO = layer("torso");
    public static final ModelLayerLocation LEGS = layer("legs");
    public static final ModelLayerLocation BOOTS = layer("boots");
    /** Translucent glass, gold sun visor and glowing lenses/displays (see {@link SuitOverlayLayer}). */
    public static final ModelLayerLocation OVERLAY = layer("overlay");

    /** Thin rings: grow a lot sideways, a little vertically. */
    private static final CubeDeformation RING = new CubeDeformation(1.25F, 0.25F, 1.25F);

    private final ModelPart maneuveringUnit;

    /**
     * @param tierId which tier's extra parts to show ("eva", "thermal", "heated", "radiation"); every
     *               layer holds the parts of all tiers as children named {@code tier_<id>}
     */
    public EvaSuitModel(ModelPart root, String tierId) {
        super(root);
        this.maneuveringUnit = body.hasChild("mmu") ? body.getChild("mmu") : null;
        for (ModelPart part : new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg}) {
            for (String t : TIERS) {
                if (part.hasChild("tier_" + t)) {
                    part.getChild("tier_" + t).visible = t.equals(tierId);
                }
            }
        }
    }

    public static final String[] TIERS = {"thermal", "heated", "radiation"};

    /** Shows or hides the Maneuvering Unit frame on the backpack (torso model only). */
    public void showManeuveringUnit(boolean show) {
        if (maneuveringUnit != null) {
            maneuveringUnit.visible = show;
        }
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Spacebreaker.id("eva_suit"), name);
    }

    /** A humanoid mesh whose parts are all empty, with vanilla pivots. */
    private static MeshDefinition emptyMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return mesh;
    }

    // ------------------------------------------------------------------------------------ helmet

    public static LayerDefinition createHelmet() {
        MeshDefinition mesh = emptyMesh();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5F, -8F, -4F, 10, 8, 8)      // shell, side slab
                        .texOffs(0, 16).addBox(-4F, -8F, -5F, 8, 8, 10)     // shell, front/back slab
                        .texOffs(0, 34).addBox(-4F, -9F, -4F, 8, 1, 8)      // crown
                        .texOffs(0, 43).addBox(-3F, -10F, -3F, 6, 1, 6)     // dome top
                        .texOffs(0, 50).addBox(-5F, 0F, -5F, 10, 1, 10, new CubeDeformation(0.3F, 0.1F, 0.3F)) // neck ring
                        .texOffs(40, 16).addBox(-3F, -7F, -3F, 6, 6, 6)     // padded liner (inside a player's head)
                        .texOffs(36, 0).addBox(-6F, -8F, -3F, 1, 2, 4)      // lamp right
                        .texOffs(48, 0).addBox(5F, -8F, -3F, 1, 2, 4)       // lamp left
                        .texOffs(36, 6).addBox(-1F, -3F, 5F, 2, 1, 1),      // purge valve
                PartPose.ZERO);
        PartDefinition head = mesh.getRoot().getChild("head");
        // thermal: a reflective sun brim over the (always lowered) gold visor
        head.addOrReplaceChild("tier_thermal", CubeListBuilder.create()
                .texOffs(64, 28).addBox(-5F, -9F, -6F, 10, 1, 2), PartPose.ZERO);
        // heated: a thick insulated collar and a heater pad on the crown
        head.addOrReplaceChild("tier_heated", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-6F, -2F, -6F, 12, 2, 12)
                .texOffs(64, 14).addBox(-2F, -11F, -2F, 4, 1, 4), PartPose.ZERO);
        // radiation: armoured brow and chin plates leave a narrow visor slit; cheek guards; a crest
        head.addOrReplaceChild("tier_radiation", CubeListBuilder.create()
                .texOffs(64, 20).addBox(-5F, -8F, -6F, 10, 2, 1)
                .texOffs(64, 24).addBox(-5F, -3F, -6F, 10, 2, 1)
                .texOffs(88, 20).addBox(-6F, -5F, -4F, 1, 4, 6)
                .texOffs(104, 20).addBox(5F, -5F, -4F, 1, 4, 6)
                .texOffs(64, 30).addBox(-1F, -11F, -4F, 2, 2, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    // ------------------------------------------------------------------------------------ torso + backpack

    public static LayerDefinition createTorso() {
        MeshDefinition mesh = emptyMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, 0F, -2F, 8, 12, 4, new CubeDeformation(1.0F)) // upper torso
                        .texOffs(24, 0).addBox(-3F, 3F, -5F, 6, 3, 2)       // chest control box
                        .texOffs(0, 32).addBox(-4F, 0F, 3F, 8, 10, 2)       // life support backpack
                        .texOffs(24, 32).addBox(-3F, 1F, 5F, 2, 8, 2)       // oxygen bottle right
                        .texOffs(32, 32).addBox(1F, 1F, 5F, 2, 8, 2)        // oxygen bottle left
                        .texOffs(40, 32).addBox(-3F, -1F, 3F, 6, 1, 2)      // vent cap
                        .texOffs(56, 32).addBox(2F, -4F, 4F, 1, 3, 1),      // antenna
                PartPose.ZERO);
        body.addOrReplaceChild("mmu", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-6F, 0F, 3F, 2, 10, 3)       // thruster tower right
                        .texOffs(10, 48).addBox(4F, 0F, 3F, 2, 10, 3)       // thruster tower left
                        .texOffs(20, 48).addBox(-6F, 10F, 3F, 12, 1, 3),    // lower cross frame
                PartPose.ZERO);
        // thermal: radiator wings either side of the backpack
        body.addOrReplaceChild("tier_thermal", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-9F, 1F, 5F, 5, 8, 1)
                .texOffs(76, 0).addBox(4F, 1F, 5F, 5, 8, 1), PartPose.ZERO);
        // heated: battery heater pack under the control box
        body.addOrReplaceChild("tier_heated", CubeListBuilder.create()
                .texOffs(64, 24).addBox(-3F, 7F, -4F, 6, 3, 1), PartPose.ZERO);
        // radiation: abdomen plate and a shield slab over the backpack
        body.addOrReplaceChild("tier_radiation", CubeListBuilder.create()
                .texOffs(64, 40).addBox(-4F, 7F, -4F, 8, 4, 1)
                .texOffs(88, 32).addBox(-5F, -1F, 7F, 10, 12, 1), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", arm(false), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", arm(true), PartPose.offset(5F, 2F, 0F));
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition a = left ? leftArm : rightArm;
            float x = left ? -1F : -3F;
            a.addOrReplaceChild("tier_heated", mirrored(left).texOffs(64, 16).addBox(x, -2F, -2F, 4, 3, 4, new CubeDeformation(1.5F)), PartPose.ZERO);
            a.addOrReplaceChild("tier_radiation", mirrored(left).texOffs(64, 32).addBox(x - 1F, -3F, -3F, 6, 2, 6, new CubeDeformation(0.5F)), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static CubeListBuilder mirrored(boolean left) {
        CubeListBuilder b = CubeListBuilder.create();
        if (left) {
            b.mirror();
        }
        return b;
    }

    private static CubeListBuilder arm(boolean left) {
        float x = left ? -1F : -3F;
        CubeListBuilder b = CubeListBuilder.create();
        if (left) {
            b.mirror();
        }
        return b.texOffs(0, 16).addBox(x, -2F, -2F, 4, 8, 4, new CubeDeformation(1.0F))   // sleeve
                .texOffs(16, 16).addBox(x, 7F, -2F, 4, 3, 4, new CubeDeformation(0.75F)) // glove
                .texOffs(32, 21).addBox(x, -2F, -2F, 4, 1, 4, RING)                      // shoulder bearing
                .texOffs(32, 16).addBox(x, 3F, -2F, 4, 1, 4, RING)                       // elbow bearing
                .texOffs(48, 16).addBox(x, 6F, -2F, 4, 1, 4, RING);                      // wrist ring
    }

    // ------------------------------------------------------------------------------------ legs

    public static LayerDefinition createLegs() {
        MeshDefinition mesh = emptyMesh();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, 8F, -2F, 8, 4, 4, new CubeDeformation(0.5F))           // hips
                        .texOffs(24, 0).addBox(-4F, 11F, -2F, 8, 1, 4, new CubeDeformation(1.2F, 0.2F, 1.2F)), // waist bearing, around the torso hem
                PartPose.ZERO);
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition l = root.addOrReplaceChild(left ? "left_leg" : "right_leg", leg(left), PartPose.offset(left ? 1.9F : -1.9F, 12F, 0F));
            // thermal: gold knee pads; heated: padded thighs; radiation: shin guards
            l.addOrReplaceChild("tier_thermal", mirrored(left).texOffs(64, 0).addBox(-2F, 4F, -3F, 4, 2, 1), PartPose.ZERO);
            l.addOrReplaceChild("tier_heated", mirrored(left).texOffs(64, 4).addBox(-2F, 0F, -2F, 4, 5, 4, new CubeDeformation(0.9F)), PartPose.ZERO);
            l.addOrReplaceChild("tier_radiation", mirrored(left).texOffs(64, 16).addBox(-2F, 5F, -3F, 4, 5, 1, new CubeDeformation(0.3F, 0F, 0F)), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 32);
    }

    private static CubeListBuilder leg(boolean left) {
        CubeListBuilder b = CubeListBuilder.create();
        if (left) {
            b.mirror();
        }
        return b.texOffs(0, 8).addBox(-2F, 0F, -2F, 4, 10, 4, new CubeDeformation(0.5F))      // leg
                .texOffs(16, 8).addBox(-2F, 5F, -2F, 4, 1, 4, new CubeDeformation(0.85F, 0.2F, 0.85F)); // knee bearing
    }

    // ------------------------------------------------------------------------------------ boots

    public static LayerDefinition createBoots() {
        MeshDefinition mesh = emptyMesh();
        PartDefinition root = mesh.getRoot();
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition l = root.addOrReplaceChild(left ? "left_leg" : "right_leg", boot(left), PartPose.offset(left ? 1.9F : -1.9F, 12F, 0F));
            // heated: thick insulated cuff; radiation: armoured shin plate
            l.addOrReplaceChild("tier_heated", mirrored(left).texOffs(64, 0).addBox(-3F, 5F, -3F, 6, 2, 6, new CubeDeformation(0.5F)), PartPose.ZERO);
            l.addOrReplaceChild("tier_radiation", mirrored(left).texOffs(64, 8).addBox(-3F, 6F, -4F, 6, 4, 1), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 32);
    }

    private static CubeListBuilder boot(boolean left) {
        CubeListBuilder b = CubeListBuilder.create();
        if (left) {
            b.mirror();
        }
        return b.texOffs(0, 0).addBox(-3F, 6F, -3F, 6, 5, 6)                                    // boot upper
                .texOffs(24, 0).addBox(-3F, 11F, -4F, 6, 1, 7)                                  // sole
                .texOffs(0, 11).addBox(-3F, 9F, -4F, 6, 2, 1)                                   // toe cap
                .texOffs(24, 8).addBox(-3F, 6F, -3F, 6, 1, 6, new CubeDeformation(0.3F, 0F, 0.3F)); // ankle ring
    }

    // ------------------------------------------------------------------------------------ overlay

    /** Parts drawn with special render types; same pivots as the humanoid parts they follow. */
    public static LayerDefinition createOverlay() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("glass", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5F, -7F, -5F, 10, 5, 1, new CubeDeformation(0.02F)), PartPose.ZERO);
        root.addOrReplaceChild("sun_visor", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-5F, -7F, -5F, 10, 5, 1, new CubeDeformation(0.12F)), PartPose.ZERO);
        root.addOrReplaceChild("sun_visor_up", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4F, -8F, -5F, 8, 1, 1, new CubeDeformation(0.06F, -0.3F, 0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("lamps", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-6F, -8F, -3.04F, 1, 2, 0)
                .texOffs(26, 0).addBox(5F, -8F, -3.04F, 1, 2, 0), PartPose.ZERO);
        root.addOrReplaceChild("display", CubeListBuilder.create()
                .texOffs(24, 4).addBox(-2F, 4F, -5.04F, 4, 1, 0)       // chest display
                .texOffs(24, 6).addBox(-2F, -1.04F, 3.5F, 1, 0, 1),    // status light on the vent cap
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
