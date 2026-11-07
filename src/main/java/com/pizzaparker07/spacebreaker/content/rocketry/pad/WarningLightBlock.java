package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Rotating amber warning beacon for launch pads and stations. It turns and flashes (an animated texture, so it
 * is safe with shader packs, and real block light) while a redstone signal reaches it, or by itself whenever a
 * rocket within {@link #RANGE} blocks is being fuelled, counting down, lifting off or docking.
 */
public class WarningLightBlock extends DirectionalBlock {
    public static final MapCodec<WarningLightBlock> CODEC = simpleCodec(WarningLightBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** How far the light watches for rockets, blocks. */
    public static final int RANGE = 32;
    /** How often it looks, ticks. */
    public static final int CHECK_TICKS = 10;

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, Block.box(3, 0, 3, 13, 10, 13),
            Direction.DOWN, Block.box(3, 6, 3, 13, 16, 13),
            Direction.NORTH, Block.box(3, 3, 6, 13, 13, 16),
            Direction.SOUTH, Block.box(3, 3, 0, 13, 13, 10),
            Direction.WEST, Block.box(6, 3, 3, 16, 13, 13),
            Direction.EAST, Block.box(0, 3, 3, 10, 13, 13));

    public WarningLightBlock(Properties properties) {
        super(properties.lightLevel(s -> s.getValue(LIT) ? 13 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(LIT, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return defaultBlockState().setValue(FACING, context.getClickedFace()).setValue(POWERED, powered).setValue(LIT, powered);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                BlockState next = state.setValue(POWERED, powered).setValue(LIT, powered || rocketBusy(level, pos));
                level.setBlock(pos, next, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean lit = powered || rocketBusy(level, pos);
        if (lit != state.getValue(LIT) || powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(LIT, lit).setValue(POWERED, powered), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, CHECK_TICKS);
    }

    /** A rocket nearby is taking on propellant, counting down, climbing away or flying in to dock. */
    public static boolean rocketBusy(Level level, BlockPos pos) {
        for (RocketEntity r : level.getEntitiesOfClass(RocketEntity.class, new AABB(pos).inflate(RANGE))) {
            FlightState st = r.state();
            if (st == FlightState.COUNTDOWN || st == FlightState.ASCENT && r.timer() < 200 || r.docking() || r.fuelledRecently(40)) {
                return true;
            }
        }
        return false;
    }
}
