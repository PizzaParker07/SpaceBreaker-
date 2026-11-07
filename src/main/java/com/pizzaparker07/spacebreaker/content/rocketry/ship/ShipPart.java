package com.pizzaparker07.spacebreaker.content.rocketry.ship;

import net.minecraft.util.StringRepresentable;

/**
 * A section of a spacecraft, built in a shipyard (draft for the spacecraft, see docs/ASTRO.md). Sections stack nose
 * to tail along the craft; the size is in blocks (length along the craft, width, height), masses in kg, thrust in kN,
 * Isp in s (vacuum). Radial parts (RCS, solar wings) mount around the hull and are counted, not stacked.
 */
public enum ShipPart implements StringRepresentable {
    /** The flight deck with the nose docking port: every craft has exactly one, at the front. */
    COCKPIT("cockpit", 3, 5, 5, 2500, 0, 0, 0, Kind.COCKPIT),
    CABIN("cabin", 4, 5, 5, 1800, 0, 0, 0, Kind.HULL),
    CARGO_BAY("cargo_bay", 4, 7, 5, 1200, 0, 0, 0, Kind.HULL),
    TANK_SMALL("tank_small", 3, 5, 5, 400, 4000, 0, 0, Kind.TANK),
    TANK_LARGE("tank_large", 5, 7, 7, 900, 12000, 0, 0, Kind.TANK),
    /** Chemical main engine (methane and oxygen). */
    ENGINE_CHEMICAL("engine_chemical", 3, 5, 5, 1500, 0, 900, 380, Kind.ENGINE),
    /** Nuclear thermal main engine: weak but very efficient. */
    ENGINE_NUCLEAR("engine_nuclear", 4, 5, 5, 3200, 0, 300, 900, Kind.ENGINE),
    /** A cluster of attitude thrusters, around the hull. */
    RCS("rcs", 0, 0, 0, 120, 0, 0, 0, Kind.RADIAL),
    /** A pair of solar wings, out to the sides (adds 3 blocks each side to the width). */
    SOLAR_WINGS("solar_wings", 0, 0, 0, 300, 0, 0, 0, Kind.RADIAL);

    public enum Kind { COCKPIT, HULL, TANK, ENGINE, RADIAL }

    public static final com.mojang.serialization.Codec<ShipPart> CODEC = StringRepresentable.fromEnum(ShipPart::values);

    private final String name;
    public final int length, width, height;
    public final double massKg, propellantKg, thrustKn, isp;
    public final Kind kind;

    ShipPart(String name, int length, int width, int height, double massKg, double propellantKg, double thrustKn, double isp, Kind kind) {
        this.name = name;
        this.length = length;
        this.width = width;
        this.height = height;
        this.massKg = massKg;
        this.propellantKg = propellantKg;
        this.thrustKn = thrustKn;
        this.isp = isp;
        this.kind = kind;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "ship.spacebreaker.part." + name;
    }
}
