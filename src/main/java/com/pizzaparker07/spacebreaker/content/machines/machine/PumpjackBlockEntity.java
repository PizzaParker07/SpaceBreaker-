package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import com.pizzaparker07.spacebreaker.content.machines.base.TankSpec;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.MultiblockMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Pumpjack: lifts crude oil from a reservoir below its wellhead. It scans a 5x5 column under the well (spread
 * over many ticks), then drains the oil-filled rock it found; each oil block yields 8 buckets before it is spent.
 */
public class PumpjackBlockEntity extends MachineBlockEntity {
    public static final int ENERGY_PER_TICK = 30;
    public static final int CYCLE = 40;
    public static final int PER_CYCLE = 250;
    public static final int PER_BLOCK = 8_000;
    public static final int RADIUS = 2;
    public static final MachineLayout LAYOUT = new MachineLayout().energy(10, 17).progress(78, 35, MachineLayout.Progress.ARROW).tank(110, 17);

    private final List<BlockPos> deposit = new ArrayList<>();
    /** Client-side animation state (crank angle in radians, last render time). */
    public float animPhase;
    public long animLastNanos;
    private int scanY = Integer.MIN_VALUE;
    private int scanCooldown;
    private int drainedFromCurrent;

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.PUMPJACK.type().get(), pos, state, LAYOUT, 30_000, 1_000, 0, List.of(TankSpec.out(16_000)));
    }

    public BlockPos wellhead() {
        return MultiblockMachineBlock.offset(worldPosition, getBlockState().getValue(MachineBlock.FACING),
                new MultiblockMachineBlock.Part(0, 0, -1, null));
    }

    public int depositSize() {
        return deposit.size();
    }

    public boolean scanning() {
        return scanY != Integer.MIN_VALUE;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        pushFrom(level, pos, state);
        if (!(state.getBlock() instanceof MultiblockMachineBlock mb) || !mb.isComplete(level, pos, state)) {
            status = 6;
            return false;
        }
        scanStep(level);
        if (deposit.isEmpty()) {
            status = scanning() ? 0 : 4;
            progress = 0;
            return false;
        }
        maxProgress = CYCLE;
        if (tanks.get(0).getSpace() < PER_CYCLE) {
            status = 2;
            return false;
        }
        if (!energy.consume(ENERGY_PER_TICK)) {
            status = 1;
            return false;
        }
        status = 0;
        if (++progress >= CYCLE) {
            progress = 0;
            pump(level);
        }
        return true;
    }

    private void pump(Level level) {
        while (!deposit.isEmpty()) {
            BlockPos p = deposit.get(deposit.size() - 1);
            BlockState s = level.getBlockState(p);
            if (!s.getFluidState().is(FluidsModule.CRUDE_OIL.get()) || !s.getFluidState().isSource()) {
                deposit.remove(deposit.size() - 1);
                drainedFromCurrent = 0;
                continue;
            }
            tanks.get(0).fillInternal(FluidsModule.CRUDE_OIL.stack(PER_CYCLE), IFluidHandler.FluidAction.EXECUTE);
            drainedFromCurrent += PER_CYCLE;
            if (drainedFromCurrent >= PER_BLOCK) {
                level.setBlock(p, (p.getY() < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState(), Block.UPDATE_ALL);
                deposit.remove(deposit.size() - 1);
                drainedFromCurrent = 0;
            }
            return;
        }
    }

    /** Scans one horizontal 5x5 layer per tick, from the well down to the bottom of the world. */
    private void scanStep(Level level) {
        if (!scanning()) {
            if (!deposit.isEmpty() || --scanCooldown > 0) {
                return;
            }
            scanY = wellhead().getY() - 1;
            return;
        }
        BlockPos well = wellhead();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos p = new BlockPos(well.getX() + dx, scanY, well.getZ() + dz);
                var fs = level.getFluidState(p);
                if (fs.isSource() && fs.is(FluidsModule.CRUDE_OIL.get())) {
                    deposit.add(0, p);   // deepest last: pump from the top of the reservoir down
                }
            }
        }
        if (--scanY < level.getMinBuildHeight()) {
            scanY = Integer.MIN_VALUE;
            scanCooldown = 600;
        }
    }

    private void pushFrom(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 4 != 0) {
            return;
        }
        pushFluids(level, pos, 500);
        if (state.getBlock() instanceof MultiblockMachineBlock mb) {
            for (BlockPos p : mb.partPositions(pos, state.getValue(MachineBlock.FACING))) {
                if (p.getY() == pos.getY()) {
                    pushFluids(level, p, 500);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("deposit", deposit.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("drained", drainedFromCurrent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        deposit.clear();
        for (long l : tag.getLongArray("deposit")) {
            deposit.add(BlockPos.of(l));
        }
        drainedFromCurrent = tag.getInt("drained");
    }

}
