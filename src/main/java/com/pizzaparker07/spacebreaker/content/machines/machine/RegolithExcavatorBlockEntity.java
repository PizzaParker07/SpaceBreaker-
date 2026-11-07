package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Regolith excavator: a bucket-wheel drill that works the ground under it, a 5x5 patch layer by layer down to
 * {@link #DEPTH} blocks, and hands what it digs to the machines beside it (a regolith heater, an ice melter, a
 * chest). The first link of a base that runs by itself. It leaves fluids, block entities and unbreakable blocks alone.
 */
public class RegolithExcavatorBlockEntity extends MachineBlockEntity {
    public static final int RADIUS = 2;
    public static final int DEPTH = 16;
    public static final int DIG_TICKS = 40;
    public static final int ENERGY_PER_TICK = 30;
    public static final MachineLayout LAYOUT = new MachineLayout().energy(10, 17).progress(40, 35, MachineLayout.Progress.ARROW)
            .slot(70, 25, MachineLayout.Role.OUTPUT).slot(88, 25, MachineLayout.Role.OUTPUT).slot(106, 25, MachineLayout.Role.OUTPUT)
            .slot(70, 43, MachineLayout.Role.OUTPUT).slot(88, 43, MachineLayout.Role.OUTPUT).slot(106, 43, MachineLayout.Role.OUTPUT);
    private static final int SIDE = RADIUS * 2 + 1;
    private int cursor;

    public RegolithExcavatorBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.REGOLITH_EXCAVATOR.type().get(), pos, state, LAYOUT, 30_000, 1_000, 0, List.of());
    }

    /** The block the cursor points at: layer by layer from just below the machine, row by row inside each layer. */
    public static BlockPos target(BlockPos machine, int cursor) {
        int layer = cursor / (SIDE * SIDE);
        int inLayer = cursor % (SIDE * SIDE);
        return machine.offset(inLayer % SIDE - RADIUS, -1 - layer, inLayer / SIDE - RADIUS);
    }

    public static boolean diggable(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0
                && !state.is(Blocks.BEDROCK);
    }

    public int cursor() {
        return cursor;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 10 == 0) {
            export(level, pos);
        }
        maxProgress = DIG_TICKS;
        // skip what there is nothing to dig (air, fluids, machines), a whole layer per tick at most
        int skipped = 0;
        while (cursor < SIDE * SIDE * DEPTH && skipped < SIDE * SIDE) {
            BlockPos t = target(pos, cursor);
            if (t.getY() < level.getMinBuildHeight() || diggable(level, t, level.getBlockState(t))) {
                break;
            }
            cursor++;
            skipped++;
        }
        BlockPos t = target(pos, cursor);
        if (cursor >= SIDE * SIDE * DEPTH || t.getY() < level.getMinBuildHeight()) {
            status = 0;
            progress = 0;
            return false;
        }
        if (!diggable(level, t, level.getBlockState(t))) {
            return false; // still skipping a long run of air
        }
        if (!hasRoom()) {
            status = 2;
            return false;
        }
        if (!energy.consume(ENERGY_PER_TICK)) {
            status = 1;
            return false;
        }
        status = 0;
        if (++progress >= DIG_TICKS) {
            progress = 0;
            dig((ServerLevel) level, t);
            cursor++;
        }
        return true;
    }

    private boolean hasRoom() {
        for (int i : slots(MachineLayout.Role.OUTPUT)) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize() - 4) {
                return true;
            }
        }
        return false;
    }

    private void dig(ServerLevel level, BlockPos t) {
        BlockState state = level.getBlockState(t);
        // dug as if with an iron pickaxe: ores give their raw ore, soils give themselves
        List<ItemStack> drops = Block.getDrops(state, level, t, null, null, new ItemStack(Items.IRON_PICKAXE));
        level.levelEvent(2001, t, Block.getId(state));
        level.setBlock(t, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack drop : drops) {
            ItemStack left = store(drop);
            if (!left.isEmpty()) {
                Block.popResource(level, worldPosition.above(), left);
            }
        }
    }

    /** Puts a dug stack into the output slots (players and hoppers cannot insert there, the machine can). */
    private ItemStack store(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
            for (int i : slots(MachineLayout.Role.OUTPUT)) {
                ItemStack in = items.getStackInSlot(i);
                if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, left)) {
                    int move = Math.min(left.getCount(), in.getMaxStackSize() - in.getCount());
                    if (move > 0) {
                        items.setStackInSlot(i, in.copyWithCount(in.getCount() + move));
                        left.shrink(move);
                    }
                } else if (pass == 1 && in.isEmpty()) {
                    items.setStackInSlot(i, left);
                    return ItemStack.EMPTY;
                }
                if (left.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return left;
    }

    /** Hands what was dug to the blocks beside and above (never back into the hole). */
    private void export(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) {
                continue;
            }
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(d), d.getOpposite());
            if (target == null) {
                continue;
            }
            for (int i : slots(MachineLayout.Role.OUTPUT)) {
                ItemStack stack = items.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                if (left.getCount() != stack.getCount()) {
                    items.setStackInSlot(i, left);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("cursor", cursor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cursor = tag.getInt("cursor");
    }
}
