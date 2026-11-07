package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A sealed hatch panel in a station module's wall. Nine of them (3 × 3, the middle one marked {@link #CENTER}) close
 * one standard connection mouth; {@link #FACING} points out of the module. A new module is attached by using its kit on
 * a mouth: the hatches of both walls open into a walk-through passage (see {@link StationModule}).
 */
public class StationHatchBlock extends DirectionalBlock {
    public static final MapCodec<StationHatchBlock> CODEC = simpleCodec(StationHatchBlock::new);
    public static final BooleanProperty CENTER = BooleanProperty.create("center");

    public StationHatchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CENTER, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CENTER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * The middle hatch of the mouth this hatch belongs to (itself, or a neighbour in the same wall with the same
     * facing), or null if the mouth is no longer whole.
     */
    @Nullable
    public static BlockPos mouthCenter(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof StationHatchBlock)) {
            return null;
        }
        Direction f = s.getValue(FACING);
        if (s.getValue(CENTER)) {
            return pos;
        }
        for (Direction d : Direction.values()) {
            if (d.getAxis() == f.getAxis()) {
                continue;
            }
            for (Direction e : Direction.values()) {
                if (e.getAxis() == f.getAxis() || e.getAxis() == d.getAxis()) {
                    continue;
                }
                for (BlockPos q : new BlockPos[]{pos.relative(d), pos.relative(d).relative(e)}) {
                    BlockState t = level.getBlockState(q);
                    if (t.getBlock() instanceof StationHatchBlock && t.getValue(CENTER) && t.getValue(FACING) == f) {
                        return q;
                    }
                }
            }
        }
        return null;
    }
}
