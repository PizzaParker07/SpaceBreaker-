package com.pizzaparker07.spacebreaker.content.suit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Shared base of the oxygen machines: one item slot (a tank), an FE buffer, client sync of the slot. */
public abstract class GasMachineBlockEntity extends BlockEntity {
    protected final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }
    };
    protected final Energy energy;
    /** Oxygen piped in (gas or liquid oxygen), in breathing ticks. */
    protected int storedOxygen;
    public static final int OXYGEN_BUFFER = 64000;
    /** Breathing ticks per mB: oxygen gas, and liquid oxygen (which expands ~860x when it boils). */
    public static final int TICKS_PER_MB_GAS = 2;
    public static final int TICKS_PER_MB_LIQUID = 16;

    /** Pipes can push oxygen gas or liquid oxygen in; nothing comes out. */
    protected final IFluidHandler oxygenInput = new IFluidHandler() {
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
            return OXYGEN_BUFFER / TICKS_PER_MB_GAS;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return ticksPerMb(stack) > 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int per = ticksPerMb(resource);
            if (per == 0 || resource.isEmpty()) {
                return 0;
            }
            int accepted = Math.min(resource.getAmount(), (OXYGEN_BUFFER - storedOxygen) / per);
            if (accepted > 0 && action.execute()) {
                storedOxygen += accepted * per;
                setChanged();
            }
            return Math.max(0, accepted);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    /** Breathing ticks one mB of this fluid is worth, 0 when it is not oxygen. */
    public static int ticksPerMb(FluidStack stack) {
        if (stack.is(FluidsModule.TAG_LOX)) {
            return TICKS_PER_MB_LIQUID;
        }
        if (stack.is(FluidsModule.TAG_OXYGEN)) {
            return TICKS_PER_MB_GAS;
        }
        return 0;
    }

    public IFluidHandler getOxygenInput() {
        return oxygenInput;
    }

    public int storedOxygen() {
        return storedOxygen;
    }

    /** Takes up to {@code amount} breathing ticks from the piped-in buffer. */
    protected int takeStored(int amount) {
        int take = Math.min(amount, storedOxygen);
        storedOxygen -= take;
        return take;
    }

    protected GasMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyCapacity) {
        super(type, pos, state);
        this.energy = new Energy(energyCapacity);
    }

    public abstract boolean accepts(ItemStack stack);

    /** Whether an empty-handed use takes the tank out (otherwise it shows the status). */
    public boolean removeOnUse(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    /** Status line shown when the machine is used with an empty hand and no tank. */
    public void describe(net.minecraft.world.entity.player.Player player) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.machine.empty",
                energy.getEnergyStored(), energy.getMaxEnergyStored(), com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.time(storedOxygen)), true);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public EnergyStorage getEnergy() {
        return energy;
    }

    public ItemStack getStack() {
        return items.getStackInSlot(0);
    }

    public void setStack(ItemStack stack) {
        items.setStackInSlot(0, stack);
    }

    protected void markForSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("stored_oxygen", storedOxygen);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        energy.set(tag.getInt("energy"));
        storedOxygen = tag.getInt("stored_oxygen");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("items", items.serializeNBT(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** FE buffer that accepts power from cables/generators and is drained by the machine itself. */
    public static final class Energy extends EnergyStorage {
        Energy(int capacity) {
            super(capacity, capacity, 0);
        }

        public boolean consume(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }
    }
}
