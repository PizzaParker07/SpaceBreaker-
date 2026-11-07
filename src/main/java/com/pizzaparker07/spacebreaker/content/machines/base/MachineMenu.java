package com.pizzaparker07.spacebreaker.content.machines.base;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** One menu for every machine; the slot positions come from the machine's {@link MachineLayout}. */
public class MachineMenu extends AbstractContainerMenu {
    public final MachineBlockEntity machine;
    public final ContainerData data;
    private final int machineSlots;

    public MachineMenu(int id, Inventory inventory, MachineBlockEntity machine, ContainerData data) {
        super(MachinesModule.MACHINE_MENU.get(), id);
        this.machine = machine;
        this.data = data;
        MachineLayout layout = machine.layout();
        for (int i = 0; i < layout.slots.size(); i++) {
            MachineLayout.SlotPos s = layout.slots.get(i);
            boolean output = s.role() == MachineLayout.Role.OUTPUT;
            addSlot(new SlotItemHandler(machine.items(), i, s.x() + 1, s.y() + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !output && super.mayPlace(stack);
                }
            });
        }
        machineSlots = layout.slots.size();
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

    public static MachineMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        var be = inventory.player.level().getBlockEntity(buf.readBlockPos());
        if (!(be instanceof MachineBlockEntity machine)) {
            throw new IllegalStateException("machine block entity missing on the client");
        }
        return new MachineMenu(id, inventory, machine, new SimpleContainerData(MachineBlockEntity.dataCount(machine.tanks().size())));
    }

    /** Button 0 (sent by vanilla's container button packet): cycle the redstone mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            machine.cycleRedstoneMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return !machine.isRemoved() && player.distanceToSqr(machine.getBlockPos().getCenter()) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (target.mayPlace(stack)) {
                    moved |= moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                int playerEnd = slots.size();
                int hotbarStart = playerEnd - 9;
                if (index < hotbarStart ? !moveItemStackTo(stack, hotbarStart, playerEnd, false) : !moveItemStackTo(stack, machineSlots, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
