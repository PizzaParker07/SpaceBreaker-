package com.pizzaparker07.spacebreaker.content.rocketry.design;

import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A design resolved against the part registry: its stages (bottom first) and the payload on top.
 * Pure data, no game state, so the maths can be tested on its own.
 */
public final class RocketLayout {
    /** One stage: everything that is dropped together when the next separator fires. */
    public record Stage(int index, int firstPart, int lastPart, double dryKg, double propellantKg, double thrustKn,
                        double ispSeaLevel, double ispVacuum, boolean fins, boolean gridFins, boolean legs, boolean rcs) {
        public boolean hasEngine() {
            return thrustKn > 0;
        }

        /** Stage Isp, either at sea level, in vacuum or a blend for an ascent through the atmosphere. */
        public double isp(double atmosphere) {
            return ispVacuum - (ispVacuum - ispSeaLevel) * atmosphere;
        }
    }

    /** Crew/payload section: survives staging and comes home. */
    public record Payload(int firstPart, int lastPart, double massKg, int heatShield, int seats, boolean parachute, boolean rcs) {
        public boolean present() {
            return lastPart >= firstPart;
        }
    }

    private final RocketDesign design;
    private final List<RocketPart> parts;
    private final List<Stage> stages;
    private final Payload payload;
    private final List<String> problems;

    private RocketLayout(RocketDesign design, List<RocketPart> parts, List<Stage> stages, Payload payload, List<String> problems) {
        this.design = design;
        this.parts = parts;
        this.stages = stages;
        this.payload = payload;
        this.problems = problems;
    }

    public static RocketLayout of(RocketDesign design, Function<ResourceLocation, RocketPart> lookup) {
        List<RocketPart> parts = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (ResourceLocation id : design.parts()) {
            RocketPart part = lookup.apply(id);
            if (part == null) {
                problems.add("unknown_part");
                part = new RocketPart(PartType.TANK, 0, 0, 0, 0, 0, 0, 0, 0, id, 0);
            }
            parts.add(part);
        }
        // the payload starts at the first payload-type axial part above the last separator
        int lastSeparator = -1;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).type().separator()) {
                lastSeparator = i;
            }
        }
        int payloadStart = parts.size();
        for (int i = lastSeparator + 1; i < parts.size(); i++) {
            if (parts.get(i).type().payload()) {
                payloadStart = i;
                break;
            }
        }
        List<Stage> stages = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < payloadStart; i++) {
            boolean closes = parts.get(i).type().separator() || i == payloadStart - 1;
            if (closes) {
                stages.add(stage(stages.size(), start, i, parts));
                start = i + 1;
            }
        }
        double mass = 0;
        int shield = 0;
        int seats = 0;
        boolean chute = false;
        boolean rcs = false;
        for (int i = payloadStart; i < parts.size(); i++) {
            RocketPart p = parts.get(i);
            mass += p.massKg();
            shield = Math.max(shield, p.heatShield());
            seats += p.seats();
            chute |= p.type() == PartType.PARACHUTE;
            rcs |= p.type() == PartType.RCS;
        }
        Payload payload = new Payload(payloadStart, parts.size() - 1, mass, shield, seats, chute, rcs);
        for (Stage s : stages) {
            if (!s.hasEngine() && s.propellantKg() > 0) {
                problems.add("stage_without_engine");
            }
        }
        return new RocketLayout(design, List.copyOf(parts), List.copyOf(stages), payload, List.copyOf(problems));
    }

    private static Stage stage(int index, int from, int to, List<RocketPart> parts) {
        double dry = 0, prop = 0, thrust = 0, flowSl = 0, flowVac = 0;
        boolean fins = false, grid = false, legs = false, rcs = false;
        for (int i = from; i <= to; i++) {
            RocketPart p = parts.get(i);
            dry += p.massKg();
            prop += p.propellantKg();
            if (p.isEngine()) {
                thrust += p.thrustKn();
                // engines burning together: Isp = total thrust / total mass flow
                flowSl += p.thrustKn() / Math.max(1, p.ispSeaLevel());
                flowVac += p.thrustKn() / Math.max(1, p.ispVacuum());
            }
            fins |= p.type() == PartType.FINS;
            grid |= p.type() == PartType.GRID_FINS;
            legs |= p.type() == PartType.LANDING_LEGS;
            rcs |= p.type() == PartType.RCS;
        }
        double ispSl = flowSl > 0 ? thrust / flowSl : 0;
        double ispVac = flowVac > 0 ? thrust / flowVac : 0;
        return new Stage(index, from, to, dry, prop, thrust, ispSl, ispVac, fins, grid, legs, rcs);
    }

    public RocketDesign design() {
        return design;
    }

    public List<RocketPart> parts() {
        return parts;
    }

    public List<Stage> stages() {
        return stages;
    }

    public Payload payload() {
        return payload;
    }

    public List<String> problems() {
        return problems;
    }

    /** Full propellant load for every stage. */
    public double[] fullFuel() {
        double[] fuel = new double[stages.size()];
        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = stages.get(i).propellantKg();
        }
        return fuel;
    }

    public double totalPropellant() {
        return stages.stream().mapToDouble(Stage::propellantKg).sum();
    }

    /** Index of the topmost axial part of the stage stack that is still attached once stage {@code active} is the lowest. */
    public int firstPartOf(int activeStage) {
        if (activeStage >= stages.size()) {
            return payload.firstPart();
        }
        return stages.get(activeStage).firstPart();
    }

    /** Whether any stage carries landing legs at or above the active stage. */
    public boolean hasLegsFrom(int activeStage) {
        for (int i = activeStage; i < stages.size(); i++) {
            if (stages.get(i).legs()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRcsFrom(int activeStage) {
        if (payload.rcs()) {
            return true;
        }
        for (int i = activeStage; i < stages.size(); i++) {
            if (stages.get(i).rcs()) {
                return true;
            }
        }
        return false;
    }

    /** Height of the stack in blocks from the given stage up. */
    public double heightBlocks(int activeStage) {
        int px = 0;
        for (int i = firstPartOf(activeStage); i < parts.size(); i++) {
            if (!parts.get(i).type().radial()) {
                px += parts.get(i).height();
            }
        }
        return px / 16.0;
    }
}
