package com.pizzaparker07.spacebreaker.content.rocketry.assembler;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class RocketAssemblerMenu extends AbstractContainerMenu {
    public static final int BUTTON_ASSEMBLE = 0;
    /** Top-left of each row in the screen (stage rows are drawn top stage first). */
    public static final int SLOT_X = 26;
    public static final int PAYLOAD_Y = 24;
    public static final int STAGE_Y0 = 50;   // stage 3 row; stage 2 and 1 follow 22 px apart
    public static final int ROW_GAP = 22;
    public static final int OUTPUT_X = 160, OUTPUT_Y = 94;    // on the stage-1 line, after the assemble arrow
    public static final int INV_Y = 140;

    @Nullable
    private final RocketAssemblerBlockEntity be;
    private final IItemHandler items;
    private final Inventory inventory;

    public RocketAssemblerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof RocketAssemblerBlockEntity b ? b : null);
    }

    public RocketAssemblerMenu(int id, Inventory inventory, @Nullable RocketAssemblerBlockEntity be) {
        super(RocketryModule.ASSEMBLER_MENU.get(), id);
        this.be = be;
        this.inventory = inventory;
        this.items = be != null ? be.items() : new ItemStackHandler(AssemblerSlots.COUNT);
        for (int s = 0; s < AssemblerSlots.STAGES; s++) {
            int y = STAGE_Y0 + (AssemblerSlots.STAGES - 1 - s) * ROW_GAP;
            for (int k = 0; k < AssemblerSlots.PER_STAGE; k++) {
                addSlot(new PartSlot(items, AssemblerSlots.stageSlot(s, k), SLOT_X + k * 18, y));
            }
        }
        // payload row, left to right: heat shield, capsule, parachute / nose cone
        for (int k = 0; k < 3; k++) {
            addSlot(new PartSlot(items, AssemblerSlots.PAYLOAD_START + k, SLOT_X + 18 + k * 18, PAYLOAD_Y));
        }
        addSlot(new SlotItemHandler(items, AssemblerSlots.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inventory, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inventory, c, 8 + c * 18, INV_Y + 58));
        }
    }

    /** The menu slot index of a block-entity slot (they were added in a different order). */
    public ItemStack partIn(int beSlot) {
        return items.getStackInSlot(beSlot);
    }

    private final class PartSlot extends SlotItemHandler {
        private final int beSlot;

        PartSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.beSlot = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RocketParts.forStack(inventory.player.level().registryAccess(), stack)
                    .map(p -> AssemblerSlots.accepts(beSlot, p.type())).orElse(false)
                    && com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.unlocked(inventory.player, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 16;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ASSEMBLE && be != null) {
            return be.assemble(player);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = AssemblerSlots.COUNT;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // into the first free matching part slot
            boolean moved = false;
            for (int i = 0; i < machineSlots - 1 && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (target.mayPlace(stack) && !target.hasItem()) {
                    target.set(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || !be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
