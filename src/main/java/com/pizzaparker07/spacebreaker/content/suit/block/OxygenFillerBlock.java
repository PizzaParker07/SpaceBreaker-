package com.pizzaparker07.spacebreaker.content.suit.block;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
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

/** Oxygen Filler: put a tank (or Maneuvering Unit) into its cradle and it is refilled. */
public class OxygenFillerBlock extends GasMachineBlock {
    public static final MapCodec<OxygenFillerBlock> CODEC = simpleCodec(OxygenFillerBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 12, 15));

    public OxygenFillerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenFillerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SuitModule.OXYGEN_FILLER_BE.get(), OxygenFillerBlockEntity::serverTick);
    }
}
