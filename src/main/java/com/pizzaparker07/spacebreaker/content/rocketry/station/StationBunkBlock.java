package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Station bunk: a strapped-down sleeping bag. Beds do not work off Earth, so this is how a crew makes a station
 * (or any base) home: using it sets the respawn point on top of it, in any dimension.
 */
public class StationBunkBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<StationBunkBlock> CODEC = simpleCodec(StationBunkBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 7, 16);

    public StationBunkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
        return SHAPE;
    }

    /** Makes the space above the bunk this player's respawn point (forced, so no bed is needed). */
    public static void setSpawn(ServerPlayer player, BlockPos bunk) {
        player.setRespawnPosition(player.level().dimension(), bunk.above(), player.getYRot(), true, false);
        player.displayClientMessage(Component.translatable("message.spacebreaker.station_bunk.set"), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            setSpawn(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
