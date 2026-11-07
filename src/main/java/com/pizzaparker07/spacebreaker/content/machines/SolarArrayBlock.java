package com.pizzaparker07.spacebreaker.content.machines;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Station solar array: three tracker modules on one long torque tube (north-south), one block of footprint, wings
 * reaching a block out on either side. Made for station trusses and outposts; same rules as the single tracker.
 */
public class SolarArrayBlock extends SolarPanelBlock {
    public static final MapCodec<SolarArrayBlock> CODEC = simpleCodec(SolarArrayBlock::new);
    public static final int PEAK_OUTPUT = 60;
    private static final VoxelShape SHAPE = Shapes.or(box(4, 0, 4, 12, 2, 12), box(6.5, 2, 6.5, 9.5, 11.5, 9.5), box(0, 11.5, 0, 16, 15, 16));

    public SolarArrayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int peak() {
        return PEAK_OUTPUT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(MachinesModule.SOLAR_ARRAY_BE.get(), pos, state, PEAK_OUTPUT);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MachinesModule.SOLAR_ARRAY_BE.get(),
                level.isClientSide ? SolarPanelBlockEntity::clientTick : SolarPanelBlockEntity::serverTick);
    }
}
