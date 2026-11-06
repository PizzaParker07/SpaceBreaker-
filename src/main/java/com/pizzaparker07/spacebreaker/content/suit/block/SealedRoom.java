package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flood fill of the air volume around an oxygen distributor. The search is bounded (a room larger than
 * {@link #maxVolume} counts as leaking — it is open to the outside) and runs a limited number of blocks
 * per tick, so even big bases never cause a lag spike. Re-run whenever a block in or around the room
 * changes.
 */
public final class SealedRoom {
    public enum State { SCANNING, SEALED, LEAKING }

    /** Blocks examined per tick. */
    public static final int BUDGET_PER_TICK = 600;

    private final BlockPos origin;
    private final int maxVolume;
    private LongOpenHashSet visited = new LongOpenHashSet();
    private LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private LongOpenHashSet volume = new LongOpenHashSet();
    private LongOpenHashSet air = new LongOpenHashSet();
    private State state = State.SCANNING;
    private boolean scanning;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private BlockPos leak;

    public SealedRoom(BlockPos origin, int maxVolume) {
        this.origin = origin.immutable();
        this.maxVolume = maxVolume;
        restart();
    }

    /** Can air pass through this block? Full solid blocks, closed airlock doors and closed iron doors/trapdoors are airtight. */
    public static boolean airtight(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof AirlockDoorBlock) {
            return !state.getValue(AirlockDoorBlock.OPEN);
        }
        if (state.is(SuitModule.AIRTIGHT)) {
            return !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                    || !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);
        }
        if (state.is(SuitModule.LEAKY)) {
            return false;
        }
        return state.isCollisionShapeFullBlock(level, pos);
    }

    /** Starts a fresh scan; the previous volume stays valid (breathable) until the new one is done. */
    public void restart() {
        visited = new LongOpenHashSet();
        queue = new LongArrayFIFOQueue();
        leak = null;
        air = new LongOpenHashSet();
        scanning = true;
        for (Direction d : Direction.values()) {
            long p = origin.relative(d).asLong();
            visited.add(p);
            queue.enqueue(p);
        }
        visited.add(origin.asLong());
    }

    /** Continues the scan within the per-tick budget. Returns true when a scan just finished. */
    public boolean tick(Level level) {
        if (!scanning) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int budget = BUDGET_PER_TICK;
        while (!queue.isEmpty() && budget-- > 0) {
            long l = queue.dequeueLong();
            pos.set(l);
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                finish(State.LEAKING, pos.immutable());
                return true;
            }
            BlockState bs = level.getBlockState(pos);
            if (airtight(level, pos, bs)) {
                continue;
            }
            if (air.size() >= maxVolume) {
                finish(State.LEAKING, pos.immutable());
                return true;
            }
            air.add(l);
            for (Direction d : Direction.values()) {
                long n = BlockPos.offset(l, d);
                if (visited.add(n)) {
                    queue.enqueue(n);
                }
            }
        }
        if (queue.isEmpty()) {
            volume = air;
            computeBounds();
            finish(air.isEmpty() ? State.LEAKING : State.SEALED, null);
            return true;
        }
        return false;
    }

    private void finish(State result, BlockPos leakPos) {
        scanning = false;
        air = new LongOpenHashSet();
        state = result;
        leak = leakPos;
        if (result != State.SEALED) {
            volume = new LongOpenHashSet();
            computeBounds();
        }
        visited = new LongOpenHashSet();
        queue = new LongArrayFIFOQueue();
    }

    private void computeBounds() {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (long l : volume) {
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        if (volume.isEmpty()) {
            minX = maxX = origin.getX();
            minY = maxY = origin.getY();
            minZ = maxZ = origin.getZ();
        }
    }

    public boolean isScanning() {
        return scanning;
    }

    public State state() {
        return scanning && state == State.SCANNING ? State.SCANNING : state;
    }

    public boolean sealed() {
        return state == State.SEALED;
    }

    public int volume() {
        return volume.size();
    }

    public BlockPos leak() {
        return leak;
    }

    public boolean contains(BlockPos pos) {
        return volume.contains(pos.asLong());
    }

    /** Whether a change at this position could affect the room (inside or on its shell). */
    public boolean near(BlockPos pos) {
        return pos.getX() >= minX - 1 && pos.getX() <= maxX + 1 && pos.getY() >= minY - 1 && pos.getY() <= maxY + 1
                && pos.getZ() >= minZ - 1 && pos.getZ() <= maxZ + 1;
    }
}
