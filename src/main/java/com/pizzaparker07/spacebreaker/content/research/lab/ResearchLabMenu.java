package com.pizzaparker07.spacebreaker.content.research.lab;

import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Lab GUI: input slot, output slot, progress, power and the "share with team" toggle (button 0). */
public class ResearchLabMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_SHARE = 0;
    public static final int INPUT_X = 52, INPUT_Y = 35, OUTPUT_X = 106, OUTPUT_Y = 35;

    private final Container container;
    private final ContainerData data;
    private final @Nullable ResearchLabBlockEntity lab;
    private final BlockPos pos;

    /** Client constructor (from the network). */
    public ResearchLabMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

    private ResearchLabMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof ResearchLabBlockEntity be ? be : null,
                new SimpleContainer(2), new SimpleContainerData(6), pos);
    }

    /** Server constructor. */
    public ResearchLabMenu(int id, Inventory inventory, ResearchLabBlockEntity lab, ContainerData data) {
        this(id, inventory, lab, lab, data, lab.getBlockPos());
    }

    private ResearchLabMenu(int id, Inventory inventory, @Nullable ResearchLabBlockEntity lab, Container container, ContainerData data, BlockPos pos) {
        super(ResearchModule.RESEARCH_LAB_MENU.get(), id);
        this.container = container;
        this.data = data;
        this.lab = lab;
        this.pos = pos;
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 6);
        addSlot(new Slot(container, ResearchLabBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ResearchLabBlockEntity.isResearchable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 16;
            }
        });
        addSlot(new Slot(container, ResearchLabBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public @Nullable ResearchLabBlockEntity lab() {
        return lab;
    }

    public BlockPos pos() {
        return pos;
    }

    public int progress() {
        return data.get(0);
    }

    public int total() {
        return data.get(1);
    }

    public int energy() {
        return data.get(2);
    }

    public int status() {
        return data.get(3);
    }

    public boolean sharing() {
        return data.get(4) != 0;
    }

    public int draw() {
        return data.get(5);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_SHARE && lab != null) {
            lab.setShareWithTeam(!lab.sharesWithTeam());
            return true;
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
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ResearchLabBlockEntity.isResearchable(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 2, 29, false)) {
            return ItemStack.EMPTY;
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
        return container.stillValid(player);
    }
}
