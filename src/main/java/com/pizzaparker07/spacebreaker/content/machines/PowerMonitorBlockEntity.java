package com.pizzaparker07.spacebreaker.content.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** Samples the energy store behind the monitor once a second and keeps a smoothed net flow for the screen. */
public class PowerMonitorBlockEntity extends BlockEntity {
    public static final int SAMPLE = 20;
    private int stored;
    private int capacity;
    private double net;
    private int lastStored = -1;
    private boolean found;

    public PowerMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.POWER_MONITOR_BE.get(), pos, state);
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return capacity;
    }

    /** Smoothed net flow, FE per tick (positive = charging). */
    public double net() {
        return net;
    }

    public boolean found() {
        return found;
    }

    public float fraction() {
        return capacity <= 0 ? 0 : stored / (float) capacity;
    }

    /** Ticks until full (net > 0) or empty (net < 0); -1 when the flow is (near) zero. */
    public static long ticksLeft(int stored, int capacity, double net) {
        if (Math.abs(net) < 0.05) {
            return -1;
        }
        double room = net > 0 ? capacity - stored : stored;
        return (long) Math.ceil(room / Math.abs(net));
    }

    /** New smoothed flow from the last value and a fresh sample. */
    public static double smooth(double previous, double sample) {
        return previous * 0.5 + sample * 0.5;
    }

    @Nullable
    private IEnergyStorage target(Level level) {
        Direction back = getBlockState().getValue(PowerMonitorBlock.FACING).getOpposite();
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(back), back.getOpposite());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerMonitorBlockEntity be) {
        if ((level.getGameTime() + pos.hashCode()) % SAMPLE != 0) {
            return;
        }
        IEnergyStorage s = be.target(level);
        boolean had = be.found;
        int oldStored = be.stored;
        double oldNet = be.net;
        if (s == null) {
            be.found = false;
            be.stored = 0;
            be.capacity = 0;
            be.net = 0;
            be.lastStored = -1;
        } else {
            be.found = true;
            be.stored = s.getEnergyStored();
            be.capacity = s.getMaxEnergyStored();
            if (be.lastStored >= 0) {
                be.net = smooth(be.net, (be.stored - be.lastStored) / (double) SAMPLE);
            }
            be.lastStored = be.stored;
        }
        if (had != be.found || oldStored != be.stored || Math.abs(oldNet - be.net) > 0.01) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** "72% · +12.0 FE/t · full in 4 min" style line (also used on the screen). */
    public Component summary() {
        if (!found) {
            return Component.translatable("gui.spacebreaker.power_monitor.none");
        }
        MutableComponent line = Component.translatable("gui.spacebreaker.power_monitor.level", Math.round(fraction() * 100))
                .append(" · ").append(flowText(net));
        Component eta = etaText(stored, capacity, net);
        return eta == null ? line : line.append(" · ").append(eta);
    }

    public static Component flowText(double net) {
        return Component.translatable("gui.spacebreaker.power_monitor.flow", String.format(java.util.Locale.ROOT, "%+.1f", net));
    }

    @Nullable
    public static Component etaText(int stored, int capacity, double net) {
        long t = ticksLeft(stored, capacity, net);
        if (t < 0) {
            return null;
        }
        if (t == 0) {
            return Component.translatable(net > 0 ? "gui.spacebreaker.power_monitor.full" : "gui.spacebreaker.power_monitor.empty");
        }
        long minutes = (t + 1199) / 1200;
        return Component.translatable(net > 0 ? "gui.spacebreaker.power_monitor.full_in" : "gui.spacebreaker.power_monitor.empty_in",
                minutes >= 120 ? (minutes / 60) + " h" : minutes + " min");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stored", stored);
        tag.putInt("capacity", capacity);
        tag.putDouble("net", net);
        tag.putBoolean("found", found);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getInt("stored");
        capacity = tag.getInt("capacity");
        net = tag.getDouble("net");
        found = tag.getBoolean("found");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
