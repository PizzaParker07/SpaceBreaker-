package com.pizzaparker07.spacebreaker.content.rocketry.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** A bright floodlight that mounts on any face, for pads, yards and the dark side of a moon. */
public class FloodlightBlock extends DirectionalBlock {
    public static final MapCodec<FloodlightBlock> CODEC = simpleCodec(FloodlightBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, Block.box(2, 0, 2, 14, 6, 14),
            Direction.DOWN, Block.box(2, 10, 2, 14, 16, 14),
            Direction.NORTH, Block.box(2, 2, 10, 14, 14, 16),
            Direction.SOUTH, Block.box(2, 2, 0, 14, 14, 6),
            Direction.WEST, Block.box(10, 2, 2, 16, 14, 14),
            Direction.EAST, Block.box(0, 2, 2, 6, 14, 14));

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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
}
