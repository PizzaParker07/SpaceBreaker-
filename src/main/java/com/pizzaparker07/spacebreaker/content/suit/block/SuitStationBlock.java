package com.pizzaparker07.spacebreaker.content.suit.block;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Suit Maintenance Station: stand on it or next to it in a suit and it tops up the tanks and the Maneuvering
 * Unit and slowly mends wear; a loose suit piece can also be put in its slot to be mended.
 */
public class SuitStationBlock extends GasMachineBlock {
    public static final MapCodec<SuitStationBlock> CODEC = simpleCodec(SuitStationBlock::new);

    public SuitStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuitStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SuitModule.SUIT_STATION_BE.get(), SuitStationBlockEntity::serverTick);
    }
}
