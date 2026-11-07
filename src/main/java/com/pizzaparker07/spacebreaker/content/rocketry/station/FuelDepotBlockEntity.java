package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

/**
 * Propellant store of a station. Four tanks in the rocket's order (0 RP-1, 1 liquid oxygen, 2 liquid methane,
 * 3 liquid hydrogen), filled by pipes or buckets from any side. While powered (Forge Energy from any side, e.g.
 * solar panels through conduits) it pumps into every rocket docked at a docking port within {@link #RANGE}
 * blocks, taking what each stage's engines burn.
 */
public class FuelDepotBlockEntity extends BlockEntity {
    public static final int CAPACITY = 32_000;
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT = 400;
    /** Pump cost while moving propellant, FE per tick. */
    public static final int PUMP_FE_PER_TICK = 40;
    /** How often the pump runs, and how much it moves per run and per liquid. */
    public static final int PUMP_INTERVAL = 10, PUMP_MB = 2_500;
    /** Docking ports this close are served. */
    public static final int RANGE = 8;
    private static final int PORT_RESCAN = 100;

    private final FluidTank[] tanks = new FluidTank[4];
    private final Energy energy = new Energy();
    private final List<BlockPos> ports = new ArrayList<>();
    private int sinceScan = PORT_RESCAN;
    private int sincePump;

    private final IFluidHandler fluids = new IFluidHandler() {
        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tanks[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (FluidTank t : tanks) {
                if (t.isFluidValid(resource)) {
                    int n = t.fill(resource, action);
                    if (n > 0 && action.execute()) {
                        setChanged();
                    }
                    return n;
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (FluidTank t : tanks) {
                if (!t.getFluid().isEmpty() && FluidStack.isSameFluidSameComponents(t.getFluid(), resource)) {
                    FluidStack out = t.drain(resource, action);
                    if (!out.isEmpty() && action.execute()) {
                        setChanged();
                    }
                    return out;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (FluidTank t : tanks) {
                if (!t.getFluid().isEmpty()) {
                    FluidStack out = t.drain(maxDrain, action);
                    if (action.execute()) {
                        setChanged();
                    }
                    return out;
                }
            }
            return FluidStack.EMPTY;
        }
    };

    public FuelDepotBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.FUEL_DEPOT_BE.get(), pos, state);
        RocketFluidHandler valid = new RocketFluidHandler(null);
        for (int i = 0; i < tanks.length; i++) {
            int tank = i;
            tanks[i] = new FluidTank(CAPACITY, s -> valid.isFluidValid(tank, s));
        }
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public int stored(int tank) {
        return tanks[tank].getFluidAmount();
    }

    public Component status() {
        return Component.translatable("message.spacebreaker.fuel_depot.status", stored(0), stored(1), stored(2), stored(3),
                energy.getEnergyStored(), ports.size());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FuelDepotBlockEntity be) {
        if (++be.sinceScan >= PORT_RESCAN) {
            be.sinceScan = 0;
            be.ports.clear();
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
                if (level.getBlockState(p).is(RocketryModule.DOCKING_PORT.get())) {
                    be.ports.add(p.immutable());
                }
            }
        }
        if (++be.sincePump < PUMP_INTERVAL) {
            return;
        }
        be.sincePump = 0;
        int cost = PUMP_FE_PER_TICK * PUMP_INTERVAL;
        if (be.energy.getEnergyStored() < cost || be.ports.isEmpty()) {
            return;
        }
        boolean moved = false;
        List<RocketEntity> served = new ArrayList<>();
        for (BlockPos port : be.ports) {
            RocketEntity rocket = DockingPortBlockEntity.dockedAt(level, port);
            if (rocket == null || served.contains(rocket)) {
                continue;
            }
            served.add(rocket);
            for (FluidTank tank : be.tanks) {
                if (tank.isEmpty()) {
                    continue;
                }
                int n = rocket.fill(tank.getFluid().copyWithAmount(Math.min(PUMP_MB, tank.getFluidAmount())), false);
                if (n > 0) {
                    tank.drain(n, IFluidHandler.FluidAction.EXECUTE);
                    moved = true;
                }
            }
        }
        if (moved) {
            be.energy.use(cost);
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (FluidTank t : tanks) {
            list.add(t.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("tanks", list);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag list = tag.getList("tanks", 10);
        for (int i = 0; i < tanks.length && i < list.size(); i++) {
            tanks[i].readFromNBT(registries, list.getCompound(i));
        }
        energy.set(tag.getInt("energy"));
    }

    /** Takes power from outside only; the pump uses it. */
    private final class Energy extends EnergyStorage {
        Energy() {
            super(ENERGY_CAPACITY, MAX_INPUT, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int n = super.receiveEnergy(toReceive, simulate);
            if (n > 0 && !simulate) {
                setChanged();
            }
            return n;
        }

        void use(int amount) {
            energy = Math.max(0, energy - amount);
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }
}
