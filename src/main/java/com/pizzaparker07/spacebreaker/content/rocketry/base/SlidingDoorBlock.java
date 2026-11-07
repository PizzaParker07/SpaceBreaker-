package com.pizzaparker07.spacebreaker.content.rocketry.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Sliding door for bases: two blocks tall, its two leaves slide apart into the frame. Opens with a hand or a
 * redstone signal. Closed it holds air (airtight tag, closed state); open it lets air and people through.
 *
 * <p>Two sliding doors facing each other along the same line, at most {@link #AIRLOCK_REACH} blocks apart, make an
 * airlock: one never opens while the other is open. Asking it to open shuts the other door first, runs the airlock
 * cycle ({@code SbSounds.AIRLOCK_CYCLE}) and opens it {@link #CYCLE_TICKS} ticks later.
 */
public class SlidingDoorBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** How far apart (blocks, along the door's facing) two doors may be and still form an airlock. */
    public static final int AIRLOCK_REACH = 4;
    /** How long the cycle takes between one door shutting and the other opening, ticks. */
    public static final int CYCLE_TICKS = 40;

    private static final VoxelShape JAMBS_NS = Shapes.or(box(0, 0, 6, 2, 16, 10), box(14, 0, 6, 16, 16, 10));
    private static final VoxelShape JAMBS_EW = Shapes.or(box(6, 0, 0, 10, 16, 2), box(6, 0, 14, 10, 16, 16));
    private static final VoxelShape CLOSED_NS = box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape CLOSED_EW = box(6, 0, 0, 10, 16, 16);

    public SlidingDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (state.getValue(OPEN)) {
            return ns ? JAMBS_NS : JAMBS_EW;
        }
        return ns ? CLOSED_NS : CLOSED_EW;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(OPEN, powered).setValue(POWERED, powered);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** Places a whole door (both halves) at {@code lower}; for kits, structures and tests. */
    public static void place(Level level, BlockPos lower, BlockState state) {
        level.setBlock(lower, state.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        level.setBlock(lower.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
            return neighbor.is(this) && neighbor.getValue(HALF) != half
                    ? state.setValue(OPEN, neighbor.getValue(OPEN)).setValue(POWERED, neighbor.getValue(POWERED))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(this)) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            request(level, pos, state, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Asks the door to open or close, keeping the airlock rule: closing always goes through; opening while the
     * partner door is open shuts the partner, runs the cycle and opens this door after {@link #CYCLE_TICKS}.
     */
    public void request(Level level, BlockPos pos, BlockState state, boolean open) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lower);
        if (!lowerState.is(this)) {
            return;
        }
        if (!open) {
            setOpen(level, lower, lowerState, false);
            return;
        }
        boolean partnerOpen = false;
        for (BlockPos partner : partners(level, lower, lowerState)) {
            BlockState ps = level.getBlockState(partner);
            if (ps.getValue(OPEN)) {
                partnerOpen = true;
                setOpen(level, partner, ps, false);
            }
        }
        if (!partnerOpen) {
            setOpen(level, lower, lowerState, true);
        } else if (!level.getBlockTicks().hasScheduledTick(lower, this)) {
            level.playSound(null, lower, com.pizzaparker07.spacebreaker.content.audio.SbSounds.AIRLOCK_CYCLE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            level.scheduleTick(lower, this, CYCLE_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // the end of an airlock cycle: open, unless the partner was opened again meanwhile
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && !state.getValue(OPEN)
                && partners(level, pos, state).stream().noneMatch(p -> level.getBlockState(p).getValue(OPEN))) {
            setOpen(level, pos, state, true);
        }
    }

    /** Lower halves of the doors that make an airlock with this one: same line, same axis, within reach. */
    public java.util.List<BlockPos> partners(Level level, BlockPos lower, BlockState state) {
        Direction facing = state.getValue(FACING);
        java.util.List<BlockPos> found = new java.util.ArrayList<>();
        for (Direction d : new Direction[]{facing, facing.getOpposite()}) {
            for (int k = 1; k <= AIRLOCK_REACH; k++) {
                BlockPos p = lower.relative(d, k);
                BlockState ps = level.getBlockState(p);
                if (ps.is(this) && ps.getValue(HALF) == DoubleBlockHalf.LOWER && ps.getValue(FACING).getAxis() == facing.getAxis()) {
                    found.add(p);
                    break;
                }
            }
        }
        return found;
    }

    /** Opens or closes the whole door (both halves), with a hiss of the leaves sliding. */
    public void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lower);
        if (!lowerState.is(this)) {
            return;
        }
        level.setBlock(lower, lowerState.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, lower, open ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, open ? 1.4F : 1.2F);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(other);
        if (powered != state.getValue(POWERED)) {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState lowerState = level.getBlockState(lower);
            if (lowerState.is(this)) {
                level.setBlock(lower, lowerState.setValue(POWERED, powered), Block.UPDATE_ALL);
                request(level, lower, level.getBlockState(lower), powered);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.sliding_door").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
