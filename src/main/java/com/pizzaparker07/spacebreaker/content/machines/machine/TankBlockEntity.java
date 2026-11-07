package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TankBlockEntity extends BlockEntity {
    private final MachineTank tank;
    private boolean dirty;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.TANK_BE.get(), pos, state);
        TankBlock.Kind kind = ((TankBlock) state.getBlock()).kind;
        this.tank = new MachineTank(kind.capacity, true, kind.filter, () -> {
            dirty = true;
            setChanged();
        }) {
            @Override
            public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
                return super.drain(maxDrain, action);
            }
        };
    }

    public MachineTank tank() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity be) {
        if (be.dirty && level.getGameTime() % 5 == 0) {
            be.dirty = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    // ------------------------------------------------------------------ keep the contents on the item when broken

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        net.neoforged.neoforge.fluids.SimpleFluidContent content = input.get(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.FLUID_CONTENT.get());
        if (content != null) {
            tank.setFluid(content.copy());
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) {
            components.set(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.FLUID_CONTENT.get(),
                    net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(tank.getFluid()));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
