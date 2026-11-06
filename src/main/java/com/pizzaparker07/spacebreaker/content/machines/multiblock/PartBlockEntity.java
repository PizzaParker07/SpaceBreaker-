package com.pizzaparker07.spacebreaker.content.machines.multiblock;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Remembers which controller this part belongs to. */
public class PartBlockEntity extends BlockEntity {
    @Nullable private BlockPos controller;

    public PartBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.PART_BE.get(), pos, state);
    }

    public @Nullable BlockPos controller() {
        return controller;
    }

    public void setController(BlockPos controller) {
        this.controller = controller.immutable();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) {
            tag.put("controller", NbtUtils.writeBlockPos(controller));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "controller").orElse(null);
    }
}
