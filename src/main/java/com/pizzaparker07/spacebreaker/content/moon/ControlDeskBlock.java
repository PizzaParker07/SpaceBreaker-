package com.pizzaparker07.spacebreaker.content.moon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A control desk: a work surface with a switch panel in front and a screen standing at the back. Decoration for bases
 * (the survey outposts are fitted with them); it does nothing by itself. Faces the person sitting at it.
 */
public class ControlDeskBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ControlDeskBlock> CODEC = simpleCodec(ControlDeskBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // modelled facing north: desk 1..15 x 0..9, screen housing along the back (south) half up to the top
        for (Direction d : Direction.Plane.HORIZONTAL) {
            SHAPES.put(d, Shapes.or(box(d, 1, 0, 1, 15, 9, 15), box(d, 1, 9, 9, 15, 16, 15)));
        }
    }

    private static VoxelShape box(Direction d, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (d) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public ControlDeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
