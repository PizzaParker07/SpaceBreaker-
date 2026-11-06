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
                energy.getEnergyStored(), energy.getMaxEnergyStored()), true);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        energy.set(tag.getInt("energy"));
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
