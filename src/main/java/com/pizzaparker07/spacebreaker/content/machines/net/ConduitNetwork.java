package com.pizzaparker07.spacebreaker.content.machines.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A connected group of conduits of one type. Conduits store nothing: energy or fluid offered to any conduit is
 * passed straight on to the blocks attached to the network (never back to where it came from), limited to the
 * tier's rate per tick. Networks are built lazily on first use and dropped whenever a member or neighbour changes.
 */
public final class ConduitNetwork {
    /**
     * @param filter     only this fluid passes through this end (null = anything)
     * @param itemFilter only this item passes through this end of an item duct (null = anything)
     */
    public record Endpoint(BlockPos pos, Direction face, boolean extract, @org.jetbrains.annotations.Nullable net.minecraft.world.level.material.Fluid filter,
                           @org.jetbrains.annotations.Nullable net.minecraft.world.item.Item itemFilter) {
        public Endpoint(BlockPos pos, Direction face, boolean extract, @org.jetbrains.annotations.Nullable net.minecraft.world.level.material.Fluid filter) {
            this(pos, face, extract, filter, null);
        }

        public boolean passes(FluidStack stack) {
            return filter == null || stack.getFluid().isSame(filter);
        }

        public boolean passes(net.minecraft.world.item.ItemStack stack) {
            return itemFilter == null || stack.is(itemFilter);
        }
    }

    public static final int MAX_SIZE = 4096;

    private final Level level;
    private final ConduitType type;
    final Set<BlockPos> members = new HashSet<>();
    final List<Endpoint> endpoints = new ArrayList<>();
    private long budgetTick = -1;
    private int budget;
    private long lastTick = -1;
    private int rotate;
    private boolean busy;
    /** Per-endpoint capability caches (same order as {@link #endpoints}); NeoForge drops a cache's value when the block changes. */
    private net.neoforged.neoforge.capabilities.BlockCapabilityCache<?, Direction>[] caches;

    private long resolvedTick = -1;
    private net.neoforged.neoforge.capabilities.BlockCapability<?, Direction> resolvedCap;
    private Object[] resolved = new Object[0];
    private int[] resolvedIndex = new int[0];
    private int resolvedCount;

    /**
     * Resolves every loaded endpoint's capability once per tick: a big base offers energy to the same network from
     * dozens of generators each tick, and each offer then only walks this short array. A block change mid-tick drops
     * the whole network (see {@link ConduitBlock#neighborChanged}), so nothing stale outlives the tick.
     */
    private int resolve(net.neoforged.neoforge.capabilities.BlockCapability<?, Direction> capability) {
        long now = level.getGameTime();
        if (now == resolvedTick && capability == resolvedCap) {
            return resolvedCount;
        }
        int n = endpoints.size();
        if (resolved.length < n) {
            resolved = new Object[n];
            resolvedIndex = new int[n];
        }
        int count = 0;
        for (int k = 0; k < n; k++) {
            if (!level.isLoaded(endpoints.get(k).pos())) {
                continue;
            }
            Object c = cap(capability, k);
            if (c != null) {
                resolved[count] = c;
                resolvedIndex[count] = k;
                count++;
            }
        }
        for (int k = count; k < resolvedCount; k++) {
            resolved[k] = null;
        }
        resolvedTick = now;
        resolvedCap = capability;
        resolvedCount = count;
        return count;
    }

    /** The capability on an endpoint's face, looked up once and then kept until that block changes. */
    @SuppressWarnings("unchecked")
    private <T> @org.jetbrains.annotations.Nullable T cap(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> capability, int index) {
        Endpoint e = endpoints.get(index);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return level.getCapability(capability, e.pos(), e.face());
        }
        if (caches == null || caches.length != endpoints.size()) {
            caches = new net.neoforged.neoforge.capabilities.BlockCapabilityCache[endpoints.size()];
        }
        var cache = caches[index];
        if (cache == null || cache.getCapability() != capability) {
            cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(capability, server, e.pos(), e.face());
            caches[index] = cache;
        }
        return (T) cache.getCapability();
    }

    private ConduitNetwork(Level level, ConduitType type) {
        this.level = level;
        this.type = type;
    }

    public static ConduitNetwork build(Level level, BlockPos start, ConduitType type) {
        ConduitNetwork net = new ConduitNetwork(level, type);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        net.members.add(start);
        while (!queue.isEmpty() && net.members.size() < MAX_SIZE) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ConduitBlock conduit) || conduit.type != type) {
                continue;
            }
            ConduitBlockEntity here = level.getBlockEntity(pos) instanceof ConduitBlockEntity be ? be : null;
            if (here != null) {
                here.network = net;
            }
            for (Direction dir : Direction.values()) {
                ConduitSide side = state.getValue(ConduitBlock.SIDES.get(dir));
                if (!side.connected()) {
                    continue;
                }
                BlockPos next = pos.relative(dir);
                BlockState ns = level.getBlockState(next);
                if (ns.getBlock() instanceof ConduitBlock other && other.type == type) {
                    if (net.members.add(next.immutable())) {
                        queue.add(next.immutable());
                    }
                } else if (type.items && side != ConduitSide.EXTRACT && ns.getBlock() instanceof HopperBlock
                        && ns.getValue(HopperBlock.FACING) == dir.getOpposite()) {
                    // a hopper pointing into the duct only feeds it: delivering back into it would just cycle the items
                    continue;
                } else {
                    net.endpoints.add(new Endpoint(next.immutable(), dir.getOpposite(), side == ConduitSide.EXTRACT,
                            here == null ? null : here.filter(dir), here == null ? null : here.itemFilter(dir)));
                }
            }
        }
        return net;
    }

    public int size() {
        return members.size();
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public void invalidate() {
        for (BlockPos pos : members) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ConduitBlockEntity be && be.network == this) {
                be.network = null;
            }
        }
        members.clear();
    }

    private int budget() {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            budget = type.rate;
        }
        return budget;
    }

    // ------------------------------------------------------------------ energy

    public int distributeEnergy(int amount, boolean simulate, BlockPos source) {
        if (busy || amount <= 0) {
            return 0;
        }
        int left = Math.min(amount, budget());
        int moved = 0;
        busy = true;
        try {
            List<IEnergyStorage> targets = new ArrayList<>();
            int count = left > 0 ? resolve(Capabilities.EnergyStorage.BLOCK) : 0;
            for (int j = 0; j < count; j++) {
                Endpoint e = endpoints.get(resolvedIndex[j]);
                if (e.pos().equals(source)) {
                    continue;
                }
                IEnergyStorage t = (IEnergyStorage) resolved[j];
                if (t.canReceive()) {
                    targets.add(t);
                }
            }
            int n = targets.size();
            // round robin: each offer starts at the next consumer and fills it before moving on, so a hundred
            // generators feeding a hundred machines make a hundred deliveries a tick, not ten thousand 1 FE trickles;
            // over a few ticks every consumer gets its turn (a dry run does not move the start)
            int start = n == 0 ? 0 : Math.floorMod(simulate ? rotate : rotate++, n);
            for (int i = 0; i < n && left > 0; i++) {
                int got = targets.get((start + i) % n).receiveEnergy(left, simulate);
                left -= got;
                moved += got;
            }
        } finally {
            busy = false;
        }
        if (!simulate) {
            budget -= moved;
        }
        return moved;
    }

    // ------------------------------------------------------------------ fluids

    public int distributeFluid(FluidStack stack, IFluidHandler.FluidAction action, BlockPos source) {
        if (busy || stack.isEmpty() || !type.accepts(stack)) {
            return 0;
        }
        int left = Math.min(stack.getAmount(), budget());
        int moved = 0;
        busy = true;
        try {
            List<IFluidHandler> targets = new ArrayList<>();
            int count = left > 0 ? resolve(Capabilities.FluidHandler.BLOCK) : 0;
            for (int j = 0; j < count; j++) {
                Endpoint e = endpoints.get(resolvedIndex[j]);
                if (e.extract() || e.pos().equals(source) || !e.passes(stack)) {
                    continue;
                }
                IFluidHandler t = (IFluidHandler) resolved[j];
                targets.add(t);
            }
            int n = targets.size();
            int start = n == 0 ? 0 : Math.floorMod(rotate++, n);
            for (int i = 0; i < n && left > 0; i++) {
                IFluidHandler t = targets.get((start + i) % n);
                int got = t.fill(stack.copyWithAmount(left), action);
                left -= got;
                moved += got;
            }
        } finally {
            busy = false;
        }
        if (action.execute()) {
            budget -= moved;
        }
        return moved;
    }

    /** Water drawn per tick from an endless water source (like filling a bucket over and over). */
    public static final int WELL_RATE = 100;

    /**
     * An endless water source: a still water block with at least two water sources beside it, the same rule that lets
     * a bucket refill from it forever. A fluid pipe can pull from one without using it up.
     */
    public static boolean isWaterWell(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (!level.getFluidState(pos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)) {
            return false;
        }
        int sources = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(d)).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)) {
                sources++;
            }
        }
        return sources >= 2;
    }

    private void pumpWell(Endpoint e) {
        FluidStack water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, Math.min(WELL_RATE, Math.min(type.rate, budget())));
        if (water.isEmpty() || !type.accepts(water) || !e.passes(water) || !isWaterWell(level, e.pos())) {
            return;
        }
        int accepted = distributeFluid(water, IFluidHandler.FluidAction.SIMULATE, e.pos());
        if (accepted > 0) {
            distributeFluid(water.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE, e.pos());
        }
    }

    // ------------------------------------------------------------------ items

    /**
     * Offers a stack to the inventories on the network's delivering ends (round robin, never back to its source).
     *
     * @return what is left over
     */
    public net.minecraft.world.item.ItemStack distributeItem(net.minecraft.world.item.ItemStack stack, boolean simulate, @org.jetbrains.annotations.Nullable BlockPos source) {
        if (busy || stack.isEmpty() || !type.items) {
            return stack;
        }
        net.minecraft.world.item.ItemStack left = stack.copy();
        busy = true;
        try {
            List<net.neoforged.neoforge.items.IItemHandler> targets = new ArrayList<>();
            int count = resolve(Capabilities.ItemHandler.BLOCK);
            for (int j = 0; j < count; j++) {
                Endpoint e = endpoints.get(resolvedIndex[j]);
                if (e.extract() || e.pos().equals(source) || !e.passes(stack)) {
                    continue;
                }
                net.neoforged.neoforge.items.IItemHandler t = (net.neoforged.neoforge.items.IItemHandler) resolved[j];
                targets.add(t);
            }
            int n = targets.size();
            int start = n == 0 ? 0 : Math.floorMod(rotate, n);
            if (!simulate) {
                rotate++;
            }
            for (int i = 0; i < n && !left.isEmpty(); i++) {
                left = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(targets.get((start + i) % n), left, simulate);
            }
        } finally {
            busy = false;
        }
        return left;
    }

    /** Pulls up to the duct's rate from one inventory into the network; returns how many items moved. */
    private int pullItems(Endpoint e) {
        net.neoforged.neoforge.items.IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, e.pos(), e.face());
        if (from == null) {
            return 0;
        }
        for (int slot = 0; slot < from.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack offer = from.extractItem(slot, type.rate, true);
            if (offer.isEmpty() || !e.passes(offer)) {
                continue;
            }
            int accepted = offer.getCount() - distributeItem(offer, true, e.pos()).getCount();
            if (accepted <= 0) {
                continue;
            }
            net.minecraft.world.item.ItemStack taken = from.extractItem(slot, accepted, false);
            net.minecraft.world.item.ItemStack rest = distributeItem(taken, false, e.pos());
            if (!rest.isEmpty()) {
                // whatever could not be delivered after all goes back where it came from
                rest = from.insertItem(slot, rest, false);
                if (!rest.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, e.pos().getX() + 0.5, e.pos().getY() + 1, e.pos().getZ() + 0.5, rest);
                }
            }
            return taken.getCount() - rest.getCount();
        }
        return 0;
    }

    /** Runs once per tick per network: pulls fluid (or items) from blocks attached on EXTRACT sides. */
    public void tick() {
        long now = level.getGameTime();
        if (now == lastTick || type.energy) {
            return;
        }
        lastTick = now;
        if (type.items) {
            if (now % ConduitType.ITEM_INTERVAL == 0) {
                for (Endpoint e : endpoints) {
                    if (e.extract() && level.isLoaded(e.pos())) {
                        pullItems(e);
                    }
                }
            }
            return;
        }
        for (Endpoint e : endpoints) {
            if (!e.extract() || !level.isLoaded(e.pos())) {
                continue;
            }
            IFluidHandler from = level.getCapability(Capabilities.FluidHandler.BLOCK, e.pos(), e.face());
            if (from == null) {
                pumpWell(e);
                continue;
            }
            int want = Math.min(type.rate, budget());
            // a filtered pulling end only takes its fluid out of a mixed tank
            FluidStack offer = e.filter() == null ? from.drain(want, IFluidHandler.FluidAction.SIMULATE)
                    : from.drain(new FluidStack(e.filter(), want), IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty() || !type.accepts(offer)) {
                continue;
            }
            int accepted = distributeFluid(offer, IFluidHandler.FluidAction.SIMULATE, e.pos());
            if (accepted > 0) {
                FluidStack drained = from.drain(offer.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
                distributeFluid(drained, IFluidHandler.FluidAction.EXECUTE, e.pos());
            }
        }
    }
}
