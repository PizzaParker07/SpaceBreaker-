package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Docking port: the hatch a station offers to visiting rockets. A rocket in orbit within {@link #RANGE} blocks
 * counts as docked: its crew can climb out into the station and back in, and liquids piped into the port go
 * into the rocket's tanks. A full block, so it keeps the station airtight.
 */
public class DockingPortBlock extends BaseEntityBlock {
    public static final MapCodec<DockingPortBlock> CODEC = simpleCodec(DockingPortBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** How close a rocket must be to count as docked, blocks. */
    public static final double RANGE = 12;

    public DockingPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof net.minecraft.server.level.ServerLevel server && placer instanceof net.minecraft.world.entity.player.Player player) {
            DockingPorts.get(server).setOwner(pos, player.getGameProfile().getName());
        }
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            DockingPorts.get(server).add(pos, state.getValue(FACING));
        }
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            if (newState.is(this)) {
                DockingPorts.get(server).add(pos, newState.getValue(FACING));
            } else {
                DockingPorts.get(server).remove(pos);
                Berths.get(server).removePort(pos); // a shipyard without its port has no berth
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DockingPortBlockEntity(pos, state);
    }
}
