package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Slot-less menu that syncs the console readouts (ints) and receives the launch/abort buttons. */
public class LaunchConsoleMenu extends AbstractContainerMenu {
    public static final int FLAGS = 0, FUEL = 1, DV = 2, COST = 3, TWR = 4, STATE = 5, TIMER = 6, TOWER_HEIGHT = 7,
            CREW = 8, MISSING = 9, ROCKET_ID = 10, DATA_COUNT = 11;
    public static final int BUTTON_LAUNCH = 0, BUTTON_ABORT = 1, BUTTON_REPORT = 2;

    @Nullable
    private final LaunchConsoleBlockEntity console;
    private final ContainerData data;

    public LaunchConsoleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, null, new SimpleContainerData(DATA_COUNT));
    }

    public LaunchConsoleMenu(int id, Inventory inventory, @Nullable LaunchConsoleBlockEntity console, ContainerData data) {
        super(RocketryModule.LAUNCH_CONSOLE_MENU.get(), id);
        this.console = console;
        this.data = data;
        addDataSlots(data);
    }

    public int get(int index) {
        return data.get(index);
    }

    public boolean flag(int bit) {
        return (data.get(FLAGS) & bit) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (console != null && player instanceof ServerPlayer sp) {
            console.press(sp, id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return console == null || !console.isRemoved() && player.distanceToSqr(console.getBlockPos().getCenter()) < 64;
    }
}
