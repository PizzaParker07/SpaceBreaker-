package com.pizzaparker07.spacebreaker.content.rocketry.part;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * One rocket part, loaded from {@code data/<ns>/spacebreaker/rocket_part/<name>.json}. The part id is the
 * id of the item that carries it. All values are real units.
 *
 * @param type          what the part does
 * @param massKg        dry mass in kg
 * @param propellantKg  fuel + LOX capacity in kg (tanks)
 * @param thrustKn      engine thrust in kN (vacuum)
 * @param ispSeaLevel   specific impulse at sea level, s
 * @param ispVacuum     specific impulse in vacuum, s
 * @param heatShield    reentry rating: 1 = from low orbit, 2 = from the Moon
 * @param seats         crew seats
 * @param height        height of the part in model pixels (16 = one block), axial parts only
 * @param model         item whose 3D model draws the part on a rocket
 * @param nozzle        engine nozzle exit radius in model pixels (exhaust plume size)
 * @param propellant    what the engine burns (kerolox or methalox); tanks take their stage engine's propellant
 */
public record RocketPart(PartType type, double massKg, double propellantKg, double thrustKn, double ispSeaLevel,
                         double ispVacuum, int heatShield, int seats, int height, ResourceLocation model, int nozzle,
                         Propellant propellant) {
    /** Standard gravity, used by the rocket equation. */
    public static final double G0 = 9.80665;
    /** RP-1 : LOX mass mixture ratio is 1 : 2.56. */
    public static final double RP1_FRACTION = Propellant.KEROLOX.fuelFraction();
    /** Densities in kg per litre (1 mB = 1 L). */
    public static final double RP1_DENSITY = Propellant.KEROLOX.fuelDensity();
    public static final double LOX_DENSITY = Propellant.LOX_DENSITY;

    public static final Codec<RocketPart> CODEC = RecordCodecBuilder.create(i -> i.group(
            PartType.CODEC.fieldOf("type").forGetter(RocketPart::type),
            Codec.DOUBLE.fieldOf("mass_kg").forGetter(RocketPart::massKg),
            Codec.DOUBLE.optionalFieldOf("propellant_kg", 0.0).forGetter(RocketPart::propellantKg),
            Codec.DOUBLE.optionalFieldOf("thrust_kn", 0.0).forGetter(RocketPart::thrustKn),
            Codec.DOUBLE.optionalFieldOf("isp_sea_level_s", 0.0).forGetter(RocketPart::ispSeaLevel),
            Codec.DOUBLE.optionalFieldOf("isp_vacuum_s", 0.0).forGetter(RocketPart::ispVacuum),
            Codec.INT.optionalFieldOf("heat_shield", 0).forGetter(RocketPart::heatShield),
            Codec.INT.optionalFieldOf("seats", 0).forGetter(RocketPart::seats),
            Codec.INT.optionalFieldOf("height", 16).forGetter(RocketPart::height),
            ResourceLocation.CODEC.fieldOf("model").forGetter(RocketPart::model),
            Codec.INT.optionalFieldOf("nozzle", 8).forGetter(RocketPart::nozzle),
            Propellant.CODEC.optionalFieldOf("propellant", Propellant.KEROLOX).forGetter(RocketPart::propellant)
    ).apply(i, RocketPart::new));

    public boolean isEngine() {
        return type == PartType.ENGINE && thrustKn > 0;
    }

    /** Litres (mB) of RP-1 that fill this part's propellant capacity. */
    public int rp1Millibuckets() {
        return Propellant.KEROLOX.fuelMillibuckets(propellantKg);
    }

    /** Litres (mB) of liquid oxygen that fill this part's propellant capacity. */
    public int loxMillibuckets() {
        return Propellant.KEROLOX.loxMillibuckets(propellantKg);
    }
}
