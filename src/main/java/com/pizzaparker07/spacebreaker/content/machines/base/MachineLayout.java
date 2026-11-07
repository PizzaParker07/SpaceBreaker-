package com.pizzaparker07.spacebreaker.content.machines.base;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a machine's slots, tanks and gauges sit in its screen (standard 176x166 panel, machine area above the
 * player inventory). The same layout drives the server menu (slot positions) and the client screen.
 */
public final class MachineLayout {
    public enum Role { INPUT, TOOL, FUEL, OUTPUT }

    public enum Progress { ARROW, FLAME, NONE }

    public record SlotPos(int x, int y, Role role) {
    }

    public record TankPos(int x, int y) {
    }

    public final List<SlotPos> slots = new ArrayList<>();
    public final List<TankPos> tanks = new ArrayList<>();
    public int energyX = -1;
    public int energyY = 17;
    public int progressX = 79;
    public int progressY = 34;
    public Progress progress = Progress.ARROW;

    public MachineLayout slot(int x, int y, Role role) {
        slots.add(new SlotPos(x, y, role));
        return this;
    }

    public MachineLayout tank(int x, int y) {
        tanks.add(new TankPos(x, y));
        return this;
    }

    public MachineLayout energy(int x, int y) {
        energyX = x;
        energyY = y;
        return this;
    }

    public MachineLayout progress(int x, int y, Progress style) {
        progressX = x;
        progressY = y;
        progress = style;
        return this;
    }

    public int count(Role role) {
        return (int) slots.stream().filter(s -> s.role == role).count();
    }

    public boolean hasEnergy() {
        return energyX >= 0;
    }

    private MachineLayout tidy;

    /**
     * The same machine laid out on the shared grid every screen uses: energy bar on the far left, the progress
     * arrow (or flame) in the middle, inputs packed against its left side and outputs against its right, all on
     * one centre line. Slot order and roles are kept (they index the inventory); only the positions move.
     * Tanks count as inputs when they were declared left of the progress gauge. Deterministic, so the server menu
     * and the client screen agree.
     */
    public MachineLayout tidy() {
        if (tidy == null) {
            tidy = arrange();
        }
        return tidy;
    }

    public static final int TOP = 17, HEIGHT = 54, MID = TOP + HEIGHT / 2, GAP = 2;
    public static final int ARROW_X = 77, ARROW_W = 22;

    private MachineLayout arrange() {
        MachineLayout out = new MachineLayout();
        out.progress = progress;
        boolean onlyEnergy = slots.isEmpty() && tanks.isEmpty() && progress == Progress.NONE;
        if (hasEnergy()) {
            out.energy(onlyEnergy ? 81 : 8, TOP);
        }
        int pivot = progress == Progress.NONE ? 88 : progressX;
        // inputs: right-aligned against the gauge, in declaration order (tanks first, then items, as on most machines)
        List<Object> in = new ArrayList<>(), outs = new ArrayList<>();
        for (TankPos t : tanks) {
            (t.x() < pivot ? in : outs).add(t);
        }
        List<SlotPos> fuel = new ArrayList<>(), tools = new ArrayList<>(), inputs = new ArrayList<>();
        for (SlotPos sp : slots) {
            switch (sp.role()) {
                case OUTPUT -> outs.add(sp);
                case FUEL -> fuel.add(sp);
                case TOOL -> tools.add(sp);
                default -> inputs.add(sp);
            }
        }
        // the gauge stays in the middle unless the inputs need more room left of it (never over the energy bar)
        int inWidth = 0;
        if (progress != Progress.FLAME) {
            int items = inputs.size() + fuel.size();
            inWidth = (items > 3 ? 3 * 18 + GAP : (inputs.isEmpty() && !tools.isEmpty() ? tools.size() : items) * (18 + GAP));
        }
        for (TankPos t : tanks) {
            if (t.x() < pivot) {
                inWidth += 18 + GAP;
            }
        }
        int ax = Math.max(ARROW_X, 26 + inWidth + 4);
        java.util.Map<SlotPos, SlotPos> moved = new java.util.HashMap<>();
        List<TankPos> tankOut = new ArrayList<>(java.util.Collections.nCopies(tanks.size(), null));
        int slotY = MID - 9;
        if (progress == Progress.FLAME) {
            // generators: flame over the fuel, both on the centre column
            out.progressX = ax + 4;
            out.progressY = TOP + 6;
            fuel.addAll(inputs);
            inputs.clear();
            int x = ax + 2 - (fuel.size() - 1) * 10;
            for (SlotPos f : fuel) {
                moved.put(f, new SlotPos(x, TOP + 24, f.role()));
                x += 20;
            }
        } else {
            out.progressX = ax;
            out.progressY = MID - 6;
            inputs.addAll(fuel);
        }
        int right = ax - 4;
        // items left of the gauge: a grid when there are many, else one row; a tool sits under the last input
        if (inputs.size() > 3) {
            int cols = 3, rows = (inputs.size() + cols - 1) / cols;
            int top = MID - rows * 9, left = right - cols * 18;
            for (int i = 0; i < inputs.size(); i++) {
                moved.put(inputs.get(i), new SlotPos(left + (i % cols) * 18, top + (i / cols) * 18, inputs.get(i).role()));
            }
            right = left - GAP;
        } else {
            boolean stack = !tools.isEmpty();
            for (int i = inputs.size() - 1; i >= 0; i--) {
                right -= 18;
                moved.put(inputs.get(i), new SlotPos(right, stack ? MID - 19 : slotY, inputs.get(i).role()));
                if (stack && i == inputs.size() - 1) {
                    for (SlotPos tl : tools) {
                        moved.put(tl, new SlotPos(right, MID + 1, tl.role()));
                    }
                }
                right -= GAP;
            }
            if (inputs.isEmpty()) {
                for (SlotPos tl : tools) {
                    right -= 18;
                    moved.put(tl, new SlotPos(right, slotY, tl.role()));
                    right -= GAP;
                }
            }
        }
        for (int i = in.size() - 1; i >= 0; i--) {
            right -= 18;
            tankOut.set(tanks.indexOf((TankPos) in.get(i)), new TankPos(right, TOP));
            right -= GAP;
        }
        int left = ax + ARROW_W + 4;
        List<SlotPos> outSlots = outs.stream().filter(o -> o instanceof SlotPos).map(o -> (SlotPos) o).toList();
        if (outSlots.size() > 3) {
            // many outputs: a three-wide grid centred on the line, tanks after it
            int rows = (outSlots.size() + 2) / 3, top = MID - rows * 9;
            for (int i = 0; i < outSlots.size(); i++) {
                moved.put(outSlots.get(i), new SlotPos(left + (i % 3) * 18, top + (i / 3) * 18, outSlots.get(i).role()));
            }
            left += 3 * 18 + GAP;
            outs.removeAll(outSlots);
        }
        for (Object o : outs) {
            if (o instanceof TankPos t) {
                tankOut.set(tanks.indexOf(t), new TankPos(left, TOP));
            } else {
                SlotPos sp = (SlotPos) o;
                moved.put(sp, new SlotPos(left, slotY, sp.role()));
            }
            left += 18 + GAP;
        }
        for (SlotPos sp : slots) {
            SlotPos m = moved.get(sp);
            out.slots.add(m != null ? m : sp);
        }
        for (int i = 0; i < tanks.size(); i++) {
            out.tanks.add(tankOut.get(i) != null ? tankOut.get(i) : tanks.get(i));
        }
        out.tidy = out;
        return out;
    }
}
