package com.pizzaparker07.spacebreaker.content.rocketry.assembler;

import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Slot layout of the rocket assembler and the rules that turn slot contents into a {@link RocketDesign}.
 * Three stage rows (engine, three tanks, a radial set, the separator on top of the stage) and a payload row
 * (heat shield, capsule, parachute or nose cone), plus the output slot.
 */
public final class AssemblerSlots {
    public static final int STAGES = 3;
    public static final int PER_STAGE = 6;
    public static final int ENGINE = 0, TANK_A = 1, TANK_B = 2, TANK_C = 3, RADIAL = 4, SEPARATOR = 5;
    public static final int PAYLOAD_START = STAGES * PER_STAGE;           // 18
    public static final int HEAT_SHIELD = PAYLOAD_START, CAPSULE = PAYLOAD_START + 1, TOP = PAYLOAD_START + 2;
    public static final int OUTPUT = PAYLOAD_START + 3;                   // 21
    public static final int COUNT = OUTPUT + 1;                           // 22

    private AssemblerSlots() {
    }

    public static int stageSlot(int stage, int which) {
        return stage * PER_STAGE + which;
    }

    /** Which part types a slot accepts. */
    public static boolean accepts(int slot, PartType type) {
        if (slot == OUTPUT) {
            return false;
        }
        if (slot >= PAYLOAD_START) {
            return switch (slot - PAYLOAD_START) {
                case 0 -> type == PartType.HEAT_SHIELD;
                case 1 -> type == PartType.CAPSULE;
                default -> type == PartType.PARACHUTE || type == PartType.NOSE_CONE;
            };
        }
        return switch (slot % PER_STAGE) {
            case ENGINE -> type == PartType.ENGINE;
            case TANK_A, TANK_B, TANK_C -> type == PartType.TANK;
            case RADIAL -> type.radial();
            default -> type.separator();
        };
    }

    /** Result of reading the slots: the design (possibly empty) and the first problem found, if any. */
    public record Build(RocketDesign design, List<String> problems) {
        public boolean valid() {
            return problems.isEmpty() && !design.isEmpty();
        }
    }

    public static Build build(IntFunction<ItemStack> slots, RegistryAccess access) {
        List<ResourceLocation> parts = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        int topStage = -1;
        for (int s = 0; s < STAGES; s++) {
            if (stageUsed(slots, s)) {
                topStage = s;
            }
        }
        if (topStage < 0) {
            problems.add("no_stage");
        }
        boolean gap = false;
        for (int s = 0; s <= topStage; s++) {
            if (!stageUsed(slots, s)) {
                gap = true;
                continue;
            }
            ItemStack engine = slots.apply(stageSlot(s, ENGINE));
            if (engine.isEmpty()) {
                problems.add("no_engine");
            } else {
                parts.add(id(engine));
            }
            boolean anyTank = false;
            for (int t = TANK_A; t <= TANK_C; t++) {
                ItemStack tank = slots.apply(stageSlot(s, t));
                if (!tank.isEmpty()) {
                    parts.add(id(tank));
                    anyTank = true;
                }
            }
            if (!anyTank) {
                problems.add("no_tank");
            }
            ItemStack radial = slots.apply(stageSlot(s, RADIAL));
            if (!radial.isEmpty()) {
                parts.add(id(radial));
            }
            ItemStack sep = slots.apply(stageSlot(s, SEPARATOR));
            if (!sep.isEmpty()) {
                parts.add(id(sep));
            } else if (s < topStage) {
                problems.add("no_separator");
            }
        }
        if (gap) {
            problems.add("stage_gap");
        }
        ItemStack shield = slots.apply(HEAT_SHIELD);
        ItemStack capsule = slots.apply(CAPSULE);
        ItemStack top = slots.apply(TOP);
        if (!shield.isEmpty()) {
            parts.add(id(shield));
        }
        if (!capsule.isEmpty()) {
            parts.add(id(capsule));
        }
        if (!top.isEmpty()) {
            parts.add(id(top));
        }
        if (capsule.isEmpty() && top.isEmpty()) {
            problems.add("no_payload");
        }
        if (!shield.isEmpty() && capsule.isEmpty()) {
            problems.add("shield_without_capsule");
        }
        // all items must be known parts of the right kind
        for (int i = 0; i < OUTPUT; i++) {
            ItemStack st = slots.apply(i);
            if (!st.isEmpty()) {
                RocketPart p = RocketParts.forStack(access, st).orElse(null);
                if (p == null || !accepts(i, p.type())) {
                    problems.add("wrong_part");
                    break;
                }
            }
        }
        return new Build(new RocketDesign(parts), problems.stream().distinct().toList());
    }

    private static boolean stageUsed(IntFunction<ItemStack> slots, int stage) {
        for (int i = 0; i < PER_STAGE; i++) {
            if (!slots.apply(stageSlot(stage, i)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation id(ItemStack stack) {
        return RocketParts.idOf(stack.getItem());
    }
}
