package com.pizzaparker07.spacebreaker.content.rocketry.assembler;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.item.RocketItem;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class RocketAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(AssemblerSlots.COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == AssemblerSlots.OUTPUT) {
                return stack.is(RocketryModule.ROCKET.get());
            }
            if (level == null) {
                return true;
            }
            return RocketParts.forStack(level.registryAccess(), stack).map(p -> AssemblerSlots.accepts(slot, p.type())).orElse(false);
        }
    };

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.ASSEMBLER_BE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    /** Builds the rocket from the slots: consumes one of every part and fills the output slot. */
    public boolean assemble(Player player) {
        if (level == null || !items.getStackInSlot(AssemblerSlots.OUTPUT).isEmpty()) {
            return false;
        }
        AssemblerSlots.Build build = AssemblerSlots.build(items::getStackInSlot, level.registryAccess());
        if (!build.valid()) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.assembler." + build.problems().get(0)), true);
            return false;
        }
        for (int i = 0; i < AssemblerSlots.OUTPUT; i++) {
            items.extractItem(i, 1, false);
        }
        items.setStackInSlot(AssemblerSlots.OUTPUT, RocketItem.of(build.design()));
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 0.9F);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.spacebreaker.rocket_assembler");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RocketAssemblerMenu(id, inventory, this);
    }
}
