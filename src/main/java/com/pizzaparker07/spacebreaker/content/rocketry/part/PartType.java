package com.pizzaparker07.spacebreaker.content.rocketry.part;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** What a rocket part does. Radial parts are mounted around the axial stack instead of on top of it. */
public enum PartType implements StringRepresentable {
    NOSE_CONE("nose_cone", false),
    CAPSULE("capsule", false),
    HEAT_SHIELD("heat_shield", false),
    PARACHUTE("parachute", false),
    TANK("tank", false),
    ENGINE("engine", false),
    DECOUPLER("decoupler", false),
    INTERSTAGE("interstage", false),
    FINS("fins", true),
    GRID_FINS("grid_fins", true),
    LANDING_LEGS("landing_legs", true),
    RCS("rcs", true),
    /** An uncrewed satellite: released in the orbit the rocket flies it to. */
    SATELLITE("satellite", false),
    /** An uncrewed probe: flies to a world, lands and sends samples and data home. */
    PROBE("probe", false),
    /** An uncrewed cargo can: delivers its hold to a station's docking port or a base's landing beacon. */
    CARGO("cargo", false);

    public static final Codec<PartType> CODEC = StringRepresentable.fromEnum(PartType::values);
    private final String name;
    private final boolean radial;

    PartType(String name, boolean radial) {
        this.name = name;
        this.radial = radial;
    }

    public boolean radial() {
        return radial;
    }

    /** Decouplers and interstages close a stage: everything below them is dropped when they fire. */
    public boolean separator() {
        return this == DECOUPLER || this == INTERSTAGE;
    }

    /** Parts that belong to the crew/payload section on top of the last stage. */
    public boolean payload() {
        return this == NOSE_CONE || this == CAPSULE || this == HEAT_SHIELD || this == PARACHUTE || this == SATELLITE || this == PROBE || this == CARGO;
    }

    /** Uncrewed payloads that ride in the capsule slot. */
    public boolean uncrewed() {
        return this == SATELLITE || this == PROBE || this == CARGO;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
