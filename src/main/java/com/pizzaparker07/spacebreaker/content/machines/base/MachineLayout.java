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
}
