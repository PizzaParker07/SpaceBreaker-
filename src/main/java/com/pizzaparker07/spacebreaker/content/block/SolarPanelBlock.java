package com.pizzaparker07.spacebreaker.content.block;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.blockentity.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Single-axis solar tracker. The panel turns east-west to follow the sun and stows flat at night;
 * output depends on how directly the sun hits the cells, the weather and the sky above it.
 */
public class SolarPanelBlock extends BaseEntityBlock {
    public static final MapCodec<SolarPanelBlock> CODEC = simpleCodec(SolarPanelBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(2, 0, 2, 14, 2, 14),
            box(6.5, 2, 6.5, 9.5, 11.5, 9.5),
            box(0, 11.5, 0, 16, 15, 16));

    public SolarPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SOLAR_PANEL.get(),
                level.isClientSide ? SolarPanelBlockEntity::clientTick : SolarPanelBlockEntity::serverTick);
    }
}
