package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Crew access arm: a walkway that reaches from the tower to the capsule hatch while a rocket stands on
 * the pad, and swings back against the tower when the countdown starts. The moving part is drawn by a
 * block entity renderer; the block itself is the hinge housing.
 */
public class CrewArmBlock extends TowerAttachmentBlock {
    public static final MapCodec<CrewArmBlock> CODEC = simpleCodec(CrewArmBlock::new);
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 4, 14, 16, 16);

    public CrewArmBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(EXTENDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXTENDED);
    }

    @Override
    protected VoxelShape shapeNorth() {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrewArmBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RocketryModule.CREW_ARM_BE.get(),
                level.isClientSide ? CrewArmBlockEntity::clientTick : CrewArmBlockEntity::serverTick);
    }
}
