package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kilowatt-class fission surface power unit: a small uranium reactor whose heat pipes drive Stirling
 * engines, with radiator fins on top. Many times an RTG's output for a big outpost, day and night anywhere.
 * It follows the load: fuel only burns while the power is taken. While running it is a strong radiation source;
 * every solid block between the core and a player cuts the dose, so bury it or bank regolith around it.
 */
public class FissionReactorBlockEntity extends MachineBlockEntity {
    /** FE/t at full power. */
    public static final int OUTPUT = 320;
    /** Ticks of full power one fuel element gives (ten in-game days). */
    public static final int BURN_TICKS = 24_000 * 10;
    /** Dose rate one block from the running core with nothing in between, mSv/day. */
    public static final double DOSE_AT_ONE_BLOCK = 50_000;
    /** Fraction of the dose that gets through one solid block (about a metre of rock or regolith). */
    public static final double BLOCK_TRANSMISSION = 0.3;
    /** Beyond this the reactor adds nothing worth counting, blocks. */
    public static final double REACH = 32;
    public static final MachineLayout LAYOUT = new MachineLayout()
            .energy(10, 17).slot(79, 52, MachineLayout.Role.FUEL).progress(80, 33, MachineLayout.Progress.FLAME);

    /** Running reactors per level (server only) and the game time each last made power. */
    private static final Map<Level, Map<BlockPos, Long>> RUNNING = Collections.synchronizedMap(new WeakHashMap<>());

    /** Burn left in the loaded element, ticks of full power. */
    private int burn;

    public FissionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.FISSION_REACTOR.type().get(), pos, state, LAYOUT, 64_000, 0, 1_600, List.of());
    }

    public int burnLeft() {
        return burn;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(IsruRecipes.REACTOR_FUEL);
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        maxProgress = BURN_TICKS;
        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
        boolean running = false;
        if (room >= OUTPUT) {
            if (burn <= 0) {
                ItemStack fuel = items.getStackInSlot(0);
                if (!fuel.isEmpty()) {
                    fuel.shrink(1);
                    burn = BURN_TICKS;
                    setChanged();
                }
            }
            if (burn > 0) {
                burn--;
                energy.generate(OUTPUT);
                running = true;
            }
        }
        status = burn > 0 || !items.getStackInSlot(0).isEmpty() ? 0 : 5;
        progress = burn;
        pushEnergy(level, pos, List.of(Direction.values()), 1_600);
        Map<BlockPos, Long> map = RUNNING.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        if (running) {
            map.put(pos.immutable(), level.getGameTime());
        }
        return running;
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Map<BlockPos, Long> map = RUNNING.get(level);
            if (map != null) {
                map.remove(worldPosition);
            }
        }
        super.setRemoved();
    }

    /** Solid blocks a straight line from the core to a point passes through (not counting either end). */
    public static int shieldingBlocks(Level level, Vec3 from, Vec3 to, BlockPos core, BlockPos target) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = (int) Math.ceil(len * 4);
        Set<BlockPos> seen = new java.util.HashSet<>();
        int solid = 0;
        for (int i = 1; i < steps; i++) {
            BlockPos p = BlockPos.containing(from.add(d.scale(i / (double) steps)));
            if (p.equals(core) || p.equals(target) || !seen.add(p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (s.isCollisionShapeFullBlock(level, p)) {
                solid++;
            }
        }
        return solid;
    }

    /** Dose rate at a distance with this many blocks in the way, mSv/day. */
    public static double dose(double distance, int blocks) {
        return DOSE_AT_ONE_BLOCK / Math.max(1.0, distance * distance) * Math.pow(BLOCK_TRANSMISSION, blocks);
    }

    /** {@code Environment.RadiationSource}: every reactor that made power in the last second, near this point. */
    public static double doseMsvDay(Level level, BlockPos pos) {
        Map<BlockPos, Long> map = RUNNING.get(level);
        if (map == null || map.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        Vec3 at = Vec3.atCenterOf(pos);
        double total = 0;
        for (var e : map.entrySet()) {
            if (now - e.getValue() > 20) {
                continue;
            }
            Vec3 core = Vec3.atCenterOf(e.getKey());
            double dist = core.distanceTo(at);
            if (dist > REACH) {
                continue;
            }
            total += dose(dist, shieldingBlocks(level, core, at, e.getKey(), pos));
        }
        return total;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burn", burn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burn = tag.getInt("burn");
    }
}
