package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Stores 1 MFE. Charges from every side except the front; discharges only through the front. */
public class BatteryBankBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 1_000_000;
    public static final int RATE = 2_048;
    public static final MachineLayout LAYOUT = new MachineLayout().energy(82, 17).progress(0, 0, MachineLayout.Progress.NONE);

    private final IEnergyStorage input = new View(true);
    private final IEnergyStorage output = new View(false);

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.BATTERY_BANK_BE.get(), pos, state, LAYOUT, CAPACITY, RATE, RATE, List.of());
    }

    @Override
    public @Nullable IEnergyStorage energyCapability(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        return side == getBlockState().getValue(MachineBlock.FACING) ? output : input;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        Direction front = state.getValue(MachineBlock.FACING);
        pushEnergy(level, pos, List.of(front), RATE);
        int charge = Math.round(energy.fraction() * 4);
        if (state.getValue(BatteryBankBlock.CHARGE) != charge) {
            level.setBlock(pos, state.setValue(BatteryBankBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
        return false;
    }

    private final class View implements IEnergyStorage {
        private final boolean in;

        View(boolean in) {
            this.in = in;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return in ? energy.receiveEnergy(amount, simulate) : 0;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return in ? 0 : energy.extractEnergy(amount, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return !in;
        }

        @Override
        public boolean canReceive() {
            return in;
        }
    }
}
