package com.pizzaparker07.spacebreaker.content.machines.net;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class ConduitBlockEntity extends BlockEntity {
    @Nullable ConduitNetwork network;
    private final Map<Direction, IEnergyStorage> energySides = new EnumMap<>(Direction.class);
    private final Map<Direction, IFluidHandler> fluidSides = new EnumMap<>(Direction.class);

    public ConduitBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.CONDUIT_BE.get(), pos, state);
    }

    public ConduitType type() {
        return ((ConduitBlock) getBlockState().getBlock()).type;
    }

    public @Nullable ConduitNetwork network() {
        if (level == null || isRemoved()) {
            return null;
        }
        if (network == null) {
            network = ConduitNetwork.build(level, worldPosition, type());
        }
        return network;
    }

    public void invalidateNetwork() {
        if (network != null) {
            network.invalidate();
        }
        network = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConduitBlockEntity be) {
        if (!be.type().energy && ((ConduitBlock) state.getBlock()).hasExtract(state)) {
            ConduitNetwork net = be.network();
            if (net != null) {
                net.tick();
            }
        }
    }

    public @Nullable IEnergyStorage energy(@Nullable Direction side) {
        if (!type().energy || side == null) {
            return null;
        }
        return energySides.computeIfAbsent(side, s -> new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                ConduitNetwork net = network();
                return net == null ? 0 : net.distributeEnergy(amount, simulate, worldPosition.relative(s));
            }

            @Override
            public int extractEnergy(int amount, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return type().rate;
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        });
    }

    public @Nullable IFluidHandler fluid(@Nullable Direction side) {
        if (type().energy || side == null) {
            return null;
        }
        return fluidSides.computeIfAbsent(side, s -> new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return type().rate;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return type().accepts(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                ConduitNetwork net = network();
                return net == null ? 0 : net.distributeFluid(resource, action, worldPosition.relative(s));
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        });
    }

    @Override
    public void setRemoved() {
        invalidateNetwork();
        super.setRemoved();
    }
}
