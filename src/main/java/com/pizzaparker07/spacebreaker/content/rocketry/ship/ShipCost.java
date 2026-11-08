package com.pizzaparker07.spacebreaker.content.rocketry.ship;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What each spacecraft section is built from (industry's proposal, docs/BALANCE_SHIPYARD.md): light structure from
 * aluminium-lithium, hot parts from superalloy and tungsten, electronics from circuits. {@code kfe} is the rough energy
 * the materials took to make (thousands of FE), shown to the player as a sense of scale. The build step itself comes
 * with the spacecraft; this is the bill it will charge.
 */
public final class ShipCost {
    /** One line of a bill: an item and how many. */
    public record Line(ResourceLocation item, int count) {
    }

    private record Bill(List<Line> lines, int kfe) {
    }

    private static final Map<ShipPart, Bill> BILLS = new EnumMap<>(ShipPart.class);

    static {
        bill(ShipPart.COCKPIT, 175, "spacebreaker:aluminum_lithium_plate", 12, "spacebreaker:titanium_plate", 4, "spacebreaker:advanced_circuit", 2,
                "spacebreaker:basic_circuit", 2, "spacebreaker:pressure_glass", 4, "spacebreaker:docking_port", 1, "spacebreaker:silicone_seal", 2);
        bill(ShipPart.CABIN, 70, "spacebreaker:aluminum_lithium_plate", 10, "spacebreaker:orange_foam_insulation", 4, "spacebreaker:silicone_seal", 2,
                "spacebreaker:basic_circuit", 1, "spacebreaker:pressure_glass", 2);
        bill(ShipPart.CARGO_BAY, 55, "spacebreaker:aluminum_plate", 8, "spacebreaker:steel_rod", 4, "spacebreaker:basic_circuit", 1, "minecraft:chest", 1);
        bill(ShipPart.TANK_SMALL, 56, "spacebreaker:aluminum_lithium_plate", 8, "spacebreaker:orange_foam_insulation", 4, "spacebreaker:silicone_seal", 1);
        bill(ShipPart.TANK_LARGE, 160, "spacebreaker:aluminum_lithium_plate", 20, "spacebreaker:orange_foam_insulation", 8, "spacebreaker:titanium_plate", 2,
                "spacebreaker:silicone_seal", 2);
        bill(ShipPart.ENGINE_CHEMICAL, 225, "spacebreaker:nickel_superalloy_plate", 6, "spacebreaker:titanium_plate", 4, "spacebreaker:tungsten_plate", 2,
                "spacebreaker:electric_motor", 2, "spacebreaker:advanced_circuit", 1);
        // the first fuel element is loaded separately (nuclear thermal engine rules)
        bill(ShipPart.ENGINE_NUCLEAR, 285, "spacebreaker:nickel_superalloy_plate", 6, "spacebreaker:tungsten_plate", 8, "spacebreaker:depleted_uranium_plate", 4,
                "spacebreaker:electric_motor", 2, "spacebreaker:advanced_circuit", 2);
        bill(ShipPart.RCS, 42, "spacebreaker:titanium_plate", 2, "spacebreaker:steel_rod", 1, "spacebreaker:basic_circuit", 1, "spacebreaker:silicone_seal", 1);
        bill(ShipPart.SOLAR_WINGS, 90, "spacebreaker:solar_cell", 8, "spacebreaker:aluminum_plate", 4, "spacebreaker:iron_rod", 2, "spacebreaker:basic_circuit", 1);
    }

    private ShipCost() {
    }

    private static void bill(ShipPart part, int kfe, Object... itemsAndCounts) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < itemsAndCounts.length; i += 2) {
            lines.add(new Line(ResourceLocation.parse((String) itemsAndCounts[i]), (Integer) itemsAndCounts[i + 1]));
        }
        BILLS.put(part, new Bill(List.copyOf(lines), kfe));
    }

    /** The materials of one section. */
    public static List<Line> of(ShipPart part) {
        return BILLS.get(part).lines();
    }

    /** Rough energy behind one section's materials, thousands of FE. */
    public static int kfe(ShipPart part) {
        return BILLS.get(part).kfe();
    }

    /** Every section of the design, radial parts included, in order. */
    private static List<ShipPart> parts(ShipDesign d) {
        List<ShipPart> out = new ArrayList<>(d.stack());
        for (int i = 0; i < d.rcs(); i++) {
            out.add(ShipPart.RCS);
        }
        for (int i = 0; i < d.solarWings(); i++) {
            out.add(ShipPart.SOLAR_WINGS);
        }
        return out;
    }

    /** The whole bill of a design, items merged, in first-seen order. */
    public static List<Line> of(ShipDesign d) {
        Map<ResourceLocation, Integer> sum = new LinkedHashMap<>();
        for (ShipPart p : parts(d)) {
            for (Line l : of(p)) {
                sum.merge(l.item(), l.count(), Integer::sum);
            }
        }
        List<Line> out = new ArrayList<>();
        sum.forEach((item, n) -> out.add(new Line(item, n)));
        return out;
    }

    /** Rough energy behind a design's materials, thousands of FE. */
    public static int kfe(ShipDesign d) {
        return parts(d).stream().mapToInt(ShipCost::kfe).sum();
    }
}
