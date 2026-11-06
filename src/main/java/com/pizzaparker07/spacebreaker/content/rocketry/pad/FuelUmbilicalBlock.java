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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Fuel umbilical: pipe RP-1 and liquid oxygen into it (fluid tags c:rp1 and c:liquid_oxygen) and it
 * pumps them straight into the rocket standing on the pad next to it.
 */
public class FuelUmbilicalBlock extends TowerAttachmentBlock {
    public static final MapCodec<FuelUmbilicalBlock> CODEC = simpleCodec(FuelUmbilicalBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 2, 3, 13, 14, 16);

    public FuelUmbilicalBlock(Properties properties) {
        super(properties);
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
        return new FuelUmbilicalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, RocketryModule.FUEL_UMBILICAL_BE.get(), FuelUmbilicalBlockEntity::clientTick) : null;
    }
}
