package com.pizzaparker07.spacebreaker.content.machines.base;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/** FE buffer with a change callback and direct access for the owning machine. */
public class SbEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public SbEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int r = super.receiveEnergy(toReceive, simulate);
        if (r > 0 && !simulate) {
            onChange.run();
        }
        return r;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int r = super.extractEnergy(toExtract, simulate);
        if (r > 0 && !simulate) {
            onChange.run();
        }
        return r;
    }

    /** Machine-internal: add energy ignoring the receive limit. Returns the amount stored. */
    public int generate(int amount) {
        int add = Math.min(capacity - energy, Math.max(0, amount));
        energy += add;
        if (add > 0) {
            onChange.run();
        }
        return add;
    }

    /** Machine-internal: use energy ignoring the extract limit. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        if (amount > 0) {
            onChange.run();
        }
        return true;
    }

    public void setStored(int amount) {
        energy = Mth.clamp(amount, 0, capacity);
    }

    public float fraction() {
        return capacity == 0 ? 0F : (float) energy / capacity;
    }
}
