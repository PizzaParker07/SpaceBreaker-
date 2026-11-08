package com.pizzaparker07.spacebreaker.content.rocketry.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.rocketry.station.Berths;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A spacecraft design from a shipyard: the stacked sections nose to tail, how many RCS clusters and solar wing pairs.
 * The numbers the spacecraft needs (mass, propellant, thrust, Isp, Δv, size) are worked out here; {@link #problems}
 * lists what keeps it from being built (no cockpit at the nose, no engine at the tail, too big for a berth...).
 */
public record ShipDesign(List<ShipPart> stack, int rcs, int solarWings) {
    public static final double G0 = 9.80665;
    public static final ShipDesign EMPTY = new ShipDesign(List.of(), 0, 0);
    public static final Codec<ShipDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            ShipPart.CODEC.listOf().fieldOf("stack").forGetter(ShipDesign::stack),
            Codec.INT.optionalFieldOf("rcs", 0).forGetter(ShipDesign::rcs),
            Codec.INT.optionalFieldOf("solar_wings", 0).forGetter(ShipDesign::solarWings)
    ).apply(i, ShipDesign::new));
    public static final StreamCodec<ByteBuf, ShipDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> ShipPart.values()[i], ShipPart::ordinal).apply(ByteBufCodecs.list(64)), ShipDesign::stack,
            ByteBufCodecs.VAR_INT, ShipDesign::rcs,
            ByteBufCodecs.VAR_INT, ShipDesign::solarWings,
            ShipDesign::new);
    /** Most RCS clusters and solar wing pairs a craft takes. */
    public static final int MAX_RCS = 8, MAX_WINGS = 2;

    public ShipDesign {
        stack = List.copyOf(stack);
    }

    public ShipDesign with(ShipPart part) {
        if (part.kind == ShipPart.Kind.RADIAL) {
            return part == ShipPart.RCS ? new ShipDesign(stack, Math.min(MAX_RCS, rcs + 1), solarWings)
                    : new ShipDesign(stack, rcs, Math.min(MAX_WINGS, solarWings + 1));
        }
        List<ShipPart> s = new ArrayList<>(stack);
        s.add(part);
        return new ShipDesign(s, rcs, solarWings);
    }

    public ShipDesign withoutLast() {
        if (stack.isEmpty()) {
            return this;
        }
        return new ShipDesign(stack.subList(0, stack.size() - 1), rcs, solarWings);
    }

    public ShipDesign withRcs(int n) {
        return new ShipDesign(stack, Math.max(0, Math.min(MAX_RCS, n)), solarWings);
    }

    public ShipDesign withWings(int n) {
        return new ShipDesign(stack, rcs, Math.max(0, Math.min(MAX_WINGS, n)));
    }

    public int length() {
        return stack.stream().mapToInt(p -> p.length).sum();
    }

    public int width() {
        int hull = stack.stream().mapToInt(p -> p.width).max().orElse(0);
        return solarWings > 0 ? hull + 6 : hull; // wings reach 3 blocks out on each side
    }

    public int height() {
        return stack.stream().mapToInt(p -> p.height).max().orElse(0);
    }

    public double dryMassKg() {
        return stack.stream().mapToDouble(p -> p.massKg).sum() + rcs * ShipPart.RCS.massKg + solarWings * ShipPart.SOLAR_WINGS.massKg;
    }

    /**
     * Liquid hydrogen is bulky (a fifteenth of the density of methane and oxygen): a tank feeding a nuclear engine
     * holds this share of its rated propellant mass. Kept at a half rather than the real sixth so a transfer craft
     * still flies far, and so its hydrogen does not cost tens of millions of FE to liquefy (docs/BALANCE_SHIPYARD.md).
     */
    public static final double HYDROGEN_FILL = 0.5;

    /** True when the main engines are nuclear thermal: the tanks then carry liquid hydrogen. */
    public boolean hydrogen() {
        return stack.contains(ShipPart.ENGINE_NUCLEAR);
    }

    public double propellantKg() {
        double rated = stack.stream().mapToDouble(p -> p.propellantKg).sum();
        return hydrogen() ? rated * HYDROGEN_FILL : rated;
    }

    public double wetMassKg() {
        return dryMassKg() + propellantKg();
    }

    public double thrustKn() {
        return stack.stream().mapToDouble(p -> p.thrustKn).sum();
    }

    /** Combined vacuum Isp of the main engines (thrust-weighted, as for engines firing together). */
    public double isp() {
        double thrust = 0, flow = 0;
        for (ShipPart p : stack) {
            if (p.kind == ShipPart.Kind.ENGINE) {
                thrust += p.thrustKn;
                flow += p.thrustKn / p.isp;
            }
        }
        return flow <= 0 ? 0 : thrust / flow;
    }

    /** Ideal Δv with full tanks, m/s. */
    public double deltaV() {
        double dry = dryMassKg(), wet = wetMassKg();
        return isp() <= 0 || dry <= 0 || wet <= dry ? 0 : isp() * G0 * Math.log(wet / dry);
    }

    /** Acceleration at full thrust with full tanks, m/s². */
    public double acceleration() {
        double wet = wetMassKg();
        return wet <= 0 ? 0 : thrustKn() * 1000 / wet;
    }

    /** What is wrong with the design (translation keys); empty when it can be built. */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        String k = "ship.spacebreaker.problem.";
        if (stack.isEmpty() || stack.get(0) != ShipPart.COCKPIT) {
            out.add(k + "cockpit_front");
        }
        if (stack.stream().filter(p -> p == ShipPart.COCKPIT).count() > 1) {
            out.add(k + "one_cockpit");
        }
        if (stack.isEmpty() || stack.get(stack.size() - 1).kind != ShipPart.Kind.ENGINE) {
            out.add(k + "engine_tail");
        }
        if (hydrogen() && stack.contains(ShipPart.ENGINE_CHEMICAL)) {
            out.add(k + "one_fuel");
        }
        if (stack.stream().noneMatch(p -> p.kind == ShipPart.Kind.TANK)) {
            out.add(k + "tank");
        }
        if (rcs < 2) {
            out.add(k + "rcs");
        }
        if (length() > Berths.MAX_LENGTH || width() > Berths.MAX_WIDTH || height() > Berths.MAX_HEIGHT) {
            out.add(k + "too_big");
        }
        return out;
    }
}
