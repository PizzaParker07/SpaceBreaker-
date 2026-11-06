package com.pizzaparker07.spacebreaker.content.machines.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
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
    public record Endpoint(BlockPos pos, Direction face, boolean extract) {
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
            if (level.getBlockEntity(pos) instanceof ConduitBlockEntity be) {
                be.network = net;
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
                } else {
                    net.endpoints.add(new Endpoint(next.immutable(), dir.getOpposite(), side == ConduitSide.EXTRACT));
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
            for (Endpoint e : endpoints) {
                if (e.pos().equals(source) || !level.isLoaded(e.pos())) {
                    continue;
                }
                IEnergyStorage t = level.getCapability(Capabilities.EnergyStorage.BLOCK, e.pos(), e.face());
                if (t != null && t.canReceive()) {
                    targets.add(t);
                }
            }
            int n = targets.size();
            int start = n == 0 ? 0 : Math.floorMod(rotate++, n);
            for (int i = 0; i < n && left > 0; i++) {
                IEnergyStorage t = targets.get((start + i) % n);
                int share = Math.max(1, left / (n - i));
                int got = t.receiveEnergy(share, simulate);
                left -= got;
                moved += got;
            }
            // second pass: let anyone take what the others could not
            for (int i = 0; i < n && left > 0; i++) {
                int got = targets.get(i).receiveEnergy(left, simulate);
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
            for (Endpoint e : endpoints) {
                if (e.extract() || e.pos().equals(source) || !level.isLoaded(e.pos())) {
                    continue;
                }
                IFluidHandler t = level.getCapability(Capabilities.FluidHandler.BLOCK, e.pos(), e.face());
                if (t != null) {
                    targets.add(t);
                }
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

    /** Runs once per tick per network: pulls fluid from blocks attached on EXTRACT sides. */
    public void tick() {
        long now = level.getGameTime();
        if (now == lastTick || type.energy) {
            return;
        }
        lastTick = now;
        for (Endpoint e : endpoints) {
            if (!e.extract() || !level.isLoaded(e.pos())) {
                continue;
            }
            IFluidHandler from = level.getCapability(Capabilities.FluidHandler.BLOCK, e.pos(), e.face());
            if (from == null) {
                continue;
            }
            FluidStack offer = from.drain(Math.min(type.rate, budget()), IFluidHandler.FluidAction.SIMULATE);
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
