package com.pizzaparker07.spacebreaker.content.machines.multiblock;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Controller of a multiblock machine. Placing it fills the structure with {@link PartBlock}s (only if all the
 * space is free); breaking any part breaks the whole machine.
 */
public class MultiblockMachineBlock extends MachineBlock {
    /** A part position relative to the controller, for a machine facing north (front = -z). */
    public record Part(int dx, int dy, int dz, PartShape shape) {
    }

    private final List<Part> parts;

    public MultiblockMachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type,
                                  BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory, VoxelShape shape, List<Part> parts) {
        super(properties, type, factory, shape, 0);
        this.parts = parts;
    }

    public List<Part> parts() {
        return parts;
    }

    public static BlockPos offset(BlockPos controller, Direction facing, Part p) {
        // rotate (dx, dz) from north-facing to the actual facing
        int dx = p.dx(), dz = p.dz();
        int rx, rz;
        switch (facing) {
            case SOUTH -> { rx = -dx; rz = -dz; }
            case EAST -> { rx = -dz; rz = dx; }
            case WEST -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        return controller.offset(rx, p.dy(), rz);
    }

    // ------------------------------------------------------------------ data-driven layout (core/multiblock)

    /** The structure definition data/spacebreaker/spacebreaker/multiblock/&lt;block id&gt;.json, if loaded. */
    public java.util.Optional<com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition> definition(net.minecraft.world.level.LevelReader level) {
        if (!(level instanceof Level l)) {
            return java.util.Optional.empty();
        }
        return com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(l.registryAccess(),
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this));
    }

    /** Non-controller positions of the structure: from the JSON definition when present, else the built-in part list. */
    private List<com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition.Placement> partPlacements(
            com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition def, BlockPos controller, Direction facing) {
        char c = def.controller().charAt(0);
        return def.placements(controller, facing).stream().filter(p -> p.symbol() != c && p.symbol() != '_').toList();
    }

    public List<BlockPos> partPositions(BlockPos controller, Direction facing) {
        return partPositions(null, controller, facing);
    }

    public List<BlockPos> partPositions(@Nullable net.minecraft.world.level.LevelReader level, BlockPos controller, Direction facing) {
        var def = level == null ? java.util.Optional.<com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition>empty() : definition(level);
        if (def.isPresent()) {
            return partPlacements(def.get(), controller, facing).stream().map(com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition.Placement::pos).toList();
        }
        List<BlockPos> out = new ArrayList<>();
        for (Part p : parts) {
            out.add(offset(controller, facing, p));
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        int minX = 0, maxX = 0, maxY = 0, minZ = 0, maxZ = 0;
        for (Part p : parts) {
            minX = Math.min(minX, p.dx());
            maxX = Math.max(maxX, p.dx());
            maxY = Math.max(maxY, p.dy());
            minZ = Math.min(minZ, p.dz());
            maxZ = Math.max(maxZ, p.dz());
        }
        String size = (maxX - minX + 1) + "×" + (maxZ - minZ + 1) + "×" + (maxY + 1);
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.multiblock", size)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) {
            return null;
        }
        Level level = ctx.getLevel();
        for (BlockPos p : partPositions(level, ctx.getClickedPos(), state.getValue(FACING))) {
            if (!level.getBlockState(p).canBeReplaced(ctx) || !level.isInWorldBounds(p)) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        build(level, pos, state);
    }

    /** Places the part blocks (also used by structure code and tests). */
    public void build(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        var def = definition(level);
        if (def.isPresent()) {
            var rot = com.pizzaparker07.spacebreaker.core.multiblock.MultiblockDefinition.rotation(facing);
            for (var p : partPlacements(def.get(), pos, facing)) {
                level.setBlock(p.pos(), p.spec().display(rot), Block.UPDATE_ALL);
                if (level.getBlockEntity(p.pos()) instanceof PartBlockEntity be) {
                    be.setController(pos);
                }
            }
            return;
        }
        for (Part p : parts) {
            BlockPos at = offset(pos, facing, p);
            level.setBlock(at, MachinesModule.PART.get().defaultBlockState().setValue(PartBlock.SHAPE, p.shape()).setValue(FACING, facing), Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof PartBlockEntity be) {
                be.setController(pos);
            }
        }
    }

    /** Complete = the JSON structure validates (when defined) and every part belongs to this controller. */
    public boolean isComplete(Level level, BlockPos pos, BlockState state) {
        var def = definition(level);
        if (def.isPresent() && !com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.validate(level, def.get(), pos, state.getValue(FACING)).complete()) {
            return false;
        }
        for (BlockPos p : partPositions(level, pos, state.getValue(FACING))) {
            if (!pos.equals(PartBlock.controller(level, p))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            for (BlockPos p : partPositions(level, pos, state.getValue(FACING))) {
                if (pos.equals(PartBlock.controller(level, p))) {
                    level.removeBlock(p, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
