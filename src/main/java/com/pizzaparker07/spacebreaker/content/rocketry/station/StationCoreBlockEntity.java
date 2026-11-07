package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The station core's console: one row of storage where modules delivered by rocket wait to be attached, and the
 * station's owner. Every station has one core; the nearest console within {@link Stations#CLUSTER} blocks of a docking
 * port is the one that port's deliveries go to.
 */
public class StationCoreBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOTS = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private String owner = "";
    /** Which station this is (berths and the station's orbit refer to it). */
    private java.util.UUID stationId = java.util.UUID.randomUUID();

    public StationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.STATION_CORE_BE.get(), pos, state);
    }

    public java.util.UUID stationId() {
        return stationId;
    }

    public String owner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
        setChanged();
    }

    /** Puts a delivered module into the console; false if it is full. */
    public boolean store(ItemStack stack) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** The console of the station a block belongs to: the nearest within {@link Stations#CLUSTER} blocks (loaded). */
    @Nullable
    public static StationCoreBlockEntity near(Level level, BlockPos pos) {
        StationCoreBlockEntity best = null;
        double bestD = (double) Stations.CLUSTER * Stations.CLUSTER;
        int r = Stations.CLUSTER >> 4;
        for (int cx = (pos.getX() >> 4) - r; cx <= (pos.getX() >> 4) + r; cx++) {
            for (int cz = (pos.getZ() >> 4) - r; cz <= (pos.getZ() >> 4) + r; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof StationCoreBlockEntity core) {
                        double d = be.getBlockPos().distSqr(pos);
                        if (d < bestD) {
                            bestD = d;
                            best = core;
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.spacebreaker.station_core");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = tag.getString("owner");
        if (tag.hasUUID("station")) {
            stationId = tag.getUUID("station");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putString("owner", owner);
        tag.putUUID("station", stationId);
    }
}
