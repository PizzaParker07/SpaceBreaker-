package com.pizzaparker07.spacebreaker.content.machines.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for every powered machine: FE buffer, item slots with roles, fluid tanks, progress, GUI sync
 * (ContainerData, values split into 16-bit halves) and client sync of tank contents for renderers.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final MachineLayout layout;
    protected final SbEnergyStorage energy;
    protected final ItemStackHandler items;
    protected final List<MachineTank> tanks = new ArrayList<>();
    protected int progress;
    protected int maxProgress;
    /** Free status code shown by the screen (0 = ok); see {@link #statusKey}. */
    protected int status;
    private boolean syncPending;
    /** Who placed the machine; research gating follows this player's known technologies. */
    @Nullable private java.util.UUID owner;
    private boolean locked;
    private boolean contentsChanged = true;
    private int litTimer;
    private boolean lockCheckPending = true;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout,
                                 int energyCapacity, int maxReceive, int maxExtract, List<TankSpec> tankSpecs) {
        super(type, pos, state);
        this.layout = layout;
        this.energy = new SbEnergyStorage(energyCapacity, maxReceive, maxExtract, this::onEnergyChanged);
        this.items = new ItemStackHandler(layout.slots.size()) {
            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged = true;
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MachineBlockEntity.this.isItemValid(slot, stack);
            }
        };
        for (TankSpec spec : tankSpecs) {
            tanks.add(new MachineTank(spec.capacity(), spec.input(), spec.filter(), this::onTankChanged));
        }
    }

    // ------------------------------------------------------------------ accessors

    public MachineLayout layout() {
        return layout;
    }

    public SbEnergyStorage energy() {
        return energy;
    }

    public ItemStackHandler items() {
        return items;
    }

    public List<MachineTank> tanks() {
        return tanks;
    }

    public MachineTank tank(int i) {
        return tanks.get(i);
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    protected boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    /** Translation key for the status line, or null when there is nothing to report. */
    public static @Nullable String statusKey(int status) {
        return switch (status) {
            case 1 -> "gui.spacebreaker.status.no_power";
            case 2 -> "gui.spacebreaker.status.output_full";
            case 3 -> "gui.spacebreaker.status.no_oxygen";
            case 4 -> "gui.spacebreaker.status.no_oil";
            case 5 -> "gui.spacebreaker.status.no_fuel";
            case 6 -> "gui.spacebreaker.status.incomplete";
            case 7 -> "gui.spacebreaker.status.locked";
            case 8 -> "gui.spacebreaker.status.no_atmosphere";
            default -> null;
        };
    }

    protected int slotIndex(MachineLayout.Role role, int n) {
        int k = 0;
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role() == role) {
                if (k++ == n) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected List<Integer> slots(MachineLayout.Role role) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < layout.slots.size(); i++) {
            if (layout.slots.get(i).role() == role) {
                out.add(i);
            }
        }
        return out;
    }

    protected boolean consumeContentsChanged() {
        boolean c = contentsChanged;
        contentsChanged = false;
        return c;
    }

    protected void markContentsChanged() {
        contentsChanged = true;
    }

    // ------------------------------------------------------------------ ticking

    public void setOwner(@Nullable java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public @Nullable java.util.UUID owner() {
        return owner;
    }

    public boolean isLocked() {
        return locked;
    }

    /** The technologies that unlock this machine (data-driven via each technology's "unlocks" list). */
    public com.pizzaparker07.spacebreaker.core.tech.TechRequirement techRequirement() {
        return level == null ? com.pizzaparker07.spacebreaker.core.tech.TechRequirement.NONE
                : com.pizzaparker07.spacebreaker.core.tech.TechRequirement.forUnlock(level.registryAccess(),
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()));
    }

    /** Re-checks the owner's research (online owners only; offline owners keep the last result). */
    private void updateLock(Level level) {
        var req = techRequirement();
        if (req.isEmpty() || owner == null || level.getServer() == null) {
            locked = false;
            return;
        }
        net.minecraft.world.entity.player.Player player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            player = level.getPlayerByUUID(owner);
        }
        if (player != null) {
            refreshLock(player);
        }
    }

    /** Recomputes the research lock against the given (owner) player. */
    public void refreshLock(net.minecraft.world.entity.player.Player player) {
        var req = techRequirement();
        locked = !req.isEmpty() && !player.isCreative()
                && req.techs().stream().noneMatch(t -> com.pizzaparker07.spacebreaker.core.tech.Tech.knows(player, t));
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (level.getGameTime() % 40 == Math.floorMod(pos.hashCode(), 40) || be.lockCheckPending) {
            be.lockCheckPending = false;
            be.updateLock(level);
        }
        if (be.locked) {
            be.status = 7;
            be.updateLit(level, pos, state, false);
            return;
        }
        boolean working = be.tickServer(level, pos, state);
        state = level.getBlockState(pos);
        be.updateLit(level, pos, state, working);
        if (be.syncPending && level.getGameTime() % 10 == 0) {
            be.syncPending = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** @return whether the machine is actively working this tick (drives the LIT state). */
    protected abstract boolean tickServer(Level level, BlockPos pos, BlockState state);

    private void updateLit(Level level, BlockPos pos, BlockState state, boolean working) {
        if (!state.hasProperty(MachineBlock.LIT)) {
            return;
        }
        boolean lit = state.getValue(MachineBlock.LIT);
        if (working) {
            litTimer = 10;
        } else if (litTimer > 0) {
            litTimer--;
        }
        boolean want = litTimer > 0;
        if (want != lit) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, want), Block.UPDATE_CLIENTS);
        }
    }

    protected void onEnergyChanged() {
        setChanged();
    }

    protected void onTankChanged() {
        contentsChanged = true;
        syncPending = true;
        setChanged();
    }

    /** Pushes energy into neighbours on the given sides. */
    protected void pushEnergy(Level level, BlockPos pos, Iterable<Direction> sides, int max) {
        for (Direction dir : sides) {
            if (energy.getEnergyStored() <= 0) {
                return;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int offer = Math.min(max, energy.getEnergyStored());
                int accepted = target.receiveEnergy(offer, false);
                energy.consume(accepted);
            }
        }
    }

    /** Pushes output-tank contents into neighbouring fluid handlers (pipes, tanks, other machines). */
    protected void pushFluids(Level level, BlockPos pos, int maxPerTank) {
        for (int ti = 0; ti < tanks.size(); ti++) {
            MachineTank tank = tanks.get(ti);
            if (tank.input || tank.isEmpty()) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (tank.isEmpty()) {
                    break;
                }
                if (!canPush(ti, dir)) {
                    continue;
                }
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite());
                if (target == null || target == fluidHandler || isOwnPart(level, pos.relative(dir))) {
                    continue;
                }
                FluidStack offer = tank.drain(maxPerTank, IFluidHandler.FluidAction.SIMULATE);
                int filled = target.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    /** Which sides an output tank may push to (default: all). */
    protected boolean canPush(int tank, Direction side) {
        return true;
    }

    /** Whether a position belongs to this machine (multiblock parts forward to us). */
    protected boolean isOwnPart(Level level, BlockPos p) {
        return p.equals(worldPosition) || worldPosition.equals(com.pizzaparker07.spacebreaker.content.machines.multiblock.PartBlock.controller(level, p));
    }

    // ------------------------------------------------------------------ capabilities

    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (layout.slots.get(slot).role() == MachineLayout.Role.OUTPUT) {
                return stack;
            }
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (layout.slots.get(slot).role() != MachineLayout.Role.OUTPUT) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return layout.slots.get(slot).role() != MachineLayout.Role.OUTPUT && items.isItemValid(slot, stack);
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return tanks.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks.get(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks.get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tanks.get(tank).input && tanks.get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (MachineTank t : tanks) {   // a tank that already holds this fluid
                if (t.input && !t.isEmpty() && FluidStack.isSameFluidSameComponents(t.getFluid(), resource)) {
                    return t.fill(resource, action);
                }
            }
            for (MachineTank t : tanks) {   // otherwise the first empty tank that accepts it
                if (t.input && t.isEmpty() && t.isFluidValid(resource)) {
                    int filled = t.fill(resource, action);
                    if (filled > 0) {
                        return filled;
                    }
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (MachineTank t : tanks) {
                if (!t.input && !t.isEmpty() && FluidStack.isSameFluidSameComponents(t.getFluid(), resource)) {
                    return t.drain(resource, action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (MachineTank t : tanks) {
                if (!t.input && !t.isEmpty()) {
                    return t.drain(maxDrain, action);
                }
            }
            return FluidStack.EMPTY;
        }
    };

    public @Nullable IItemHandler itemCapability(@Nullable Direction side) {
        return items.getSlots() > 0 ? externalItems : null;
    }

    public @Nullable IFluidHandler fluidCapability(@Nullable Direction side) {
        return tanks.isEmpty() ? null : fluidHandler;
    }

    public @Nullable IEnergyStorage energyCapability(@Nullable Direction side) {
        return energy.getMaxEnergyStored() > 0 ? energy : null;
    }

    // ------------------------------------------------------------------ GUI sync

    /** Values: progress, maxProgress, energy lo/hi, capacity lo/hi, status, then per tank: fluid id, amount lo/hi. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            switch (i) {
                case 0: return progress;
                case 1: return maxProgress;
                case 2: return energy.getEnergyStored() & 0xFFFF;
                case 3: return energy.getEnergyStored() >>> 16;
                case 4: return energy.getMaxEnergyStored() & 0xFFFF;
                case 5: return energy.getMaxEnergyStored() >>> 16;
                case 6: return status;
                default:
                    int t = (i - 7) / 3;
                    int k = (i - 7) % 3;
                    FluidStack f = tanks.get(t).getFluid();
                    return switch (k) {
                        case 0 -> f.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(f.getFluid());
                        case 1 -> f.getAmount() & 0xFFFF;
                        default -> f.getAmount() >>> 16;
                    };
            }
        }

        @Override
        public void set(int i, int value) {
        }

        @Override
        public int getCount() {
            return 7 + tanks.size() * 3;
        }
    };

    public static int dataCount(int tanks) {
        return 7 + tanks * 3;
    }

    /** Client-side decoding helpers for ContainerData values (shorts are sign-extended on the way). */
    public static int combine(ContainerData d, int lo) {
        return (d.get(lo) & 0xFFFF) | ((d.get(lo + 1) & 0xFFFF) << 16);
    }

    public static FluidStack fluid(ContainerData d, int tank) {
        int base = 7 + tank * 3;
        int id = d.get(base) & 0xFFFF;
        int amount = combine(d, base + 1);
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        if (id == 0 || fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        ListTag list = new ListTag();
        for (MachineTank t : tanks) {
            list.add(t.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("tanks", list);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putBoolean("locked", locked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            CompoundTag inv = tag.getCompound("items");
            inv.putInt("Size", items.getSlots());
            items.deserializeNBT(registries, inv);
        }
        energy.setStored(tag.getInt("energy"));
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), tanks.size()); i++) {
            tanks.get(i).readFromNBT(registries, list.getCompound(i));
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        locked = tag.getBoolean("locked");
        contentsChanged = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (MachineTank t : tanks) {
            list.add(t.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("tanks", list);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        }
    }
}
