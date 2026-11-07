package com.pizzaparker07.spacebreaker.content.suit.block;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * Two blocks tall airlock door: a steel frame with two panels that slide apart into the frame
 * (animated by client.suit.AirlockDoorRenderer). Closed it is airtight for sealed rooms; opens with a
 * hand or a redstone signal.
 */
public class AirlockDoorBlock extends BaseEntityBlock {
    public static final MapCodec<AirlockDoorBlock> CODEC = simpleCodec(AirlockDoorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape POSTS_NS = Shapes.or(box(0, 0, 5, 2, 16, 11), box(14, 0, 5, 16, 16, 11));
    private static final VoxelShape POSTS_EW = Shapes.or(box(5, 0, 0, 11, 16, 2), box(5, 0, 14, 11, 16, 16));
    private static final VoxelShape CLOSED_NS = Shapes.or(POSTS_NS, box(2, 0, 6, 14, 16, 10));
    private static final VoxelShape CLOSED_EW = Shapes.or(POSTS_EW, box(6, 0, 2, 10, 16, 14));

    public AirlockDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, POWERED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (state.getValue(OPEN)) {
            return ns ? POSTS_NS : POSTS_EW;
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
        setOpen(level, pos, state, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

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
        // the upper half follows through updateShape
        // the airlock cycle: hatch, latches and the hiss of air (closing plays it a touch lower)
        level.playSound(null, lower, com.pizzaparker07.spacebreaker.content.audio.SbSounds.AIRLOCK_CYCLE.get(), SoundSource.BLOCKS,
                0.8F, open ? 1.0F : 0.92F);
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
                setOpen(level, lower, level.getBlockState(lower), powered);
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirlockDoorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, SuitModule.AIRLOCK_DOOR_BE.get(), AirlockDoorBlockEntity::clientTick) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker."
                + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
