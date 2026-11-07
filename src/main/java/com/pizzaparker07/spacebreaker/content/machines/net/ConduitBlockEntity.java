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

    private final Map<Direction, net.minecraft.world.level.material.Fluid> filters = new EnumMap<>(Direction.class);
    private final Map<Direction, net.minecraft.world.item.Item> itemFilters = new EnumMap<>(Direction.class);
    private final Map<Direction, net.neoforged.neoforge.items.IItemHandler> itemSides = new EnumMap<>(Direction.class);

    /** Item allowed through the end on this side of an item duct, or null for any. */
    public @Nullable net.minecraft.world.item.Item itemFilter(Direction side) {
        return itemFilters.get(side);
    }

    public void setItemFilter(Direction side, @Nullable net.minecraft.world.item.Item item) {
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            itemFilters.remove(side);
        } else {
            itemFilters.put(side, item);
        }
        setChanged();
        invalidateNetwork();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Fluid allowed through the end on this side, or null for any. */
    public @Nullable net.minecraft.world.level.material.Fluid filter(Direction side) {
        return filters.get(side);
    }

    public void setFilter(Direction side, @Nullable net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            filters.remove(side);
        } else {
            filters.put(side, fluid);
        }
        setChanged();
        invalidateNetwork();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filters.isEmpty()) {
            net.minecraft.nbt.CompoundTag f = new net.minecraft.nbt.CompoundTag();
            filters.forEach((d, fluid) -> f.putString(d.getSerializedName(),
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString()));
            tag.put("filters", f);
        }
        if (!itemFilters.isEmpty()) {
            net.minecraft.nbt.CompoundTag f = new net.minecraft.nbt.CompoundTag();
            itemFilters.forEach((d, item) -> f.putString(d.getSerializedName(),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
            tag.put("item_filters", f);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filters.clear();
        net.minecraft.nbt.CompoundTag f = tag.getCompound("filters");
        for (Direction d : Direction.values()) {
            if (f.contains(d.getSerializedName())) {
                var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(f.getString(d.getSerializedName())));
                if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    filters.put(d, fluid);
                }
            }
        }
        itemFilters.clear();
        net.minecraft.nbt.CompoundTag fi = tag.getCompound("item_filters");
        for (Direction d : Direction.values()) {
            if (fi.contains(d.getSerializedName())) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(fi.getString(d.getSerializedName())));
                if (item != net.minecraft.world.item.Items.AIR) {
                    itemFilters.put(d, item);
                }
            }
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
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

    /**
     * Items pushed into an item duct (by a hopper or a machine's output) go straight on to the inventories on its
     * delivering ends; the duct itself holds nothing.
     */
    public @Nullable net.neoforged.neoforge.items.IItemHandler items(@Nullable Direction side) {
        if (!type().items || side == null) {
            return null;
        }
        return itemSides.computeIfAbsent(side, s -> new net.neoforged.neoforge.items.IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            @Override
            public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
                ConduitNetwork net = network();
                if (net == null || getBlockState().getValue(ConduitBlock.SIDES.get(s)) == ConduitSide.EXTRACT) {
                    return stack;
                }
                return net.distributeItem(stack, simulate, worldPosition.relative(s));
            }

            @Override
            public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
                return true;
            }
        });
    }

    public @Nullable IFluidHandler fluid(@Nullable Direction side) {
        if (type().energy || type().items || side == null) {
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
