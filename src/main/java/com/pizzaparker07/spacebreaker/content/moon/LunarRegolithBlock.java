package com.pizzaparker07.spacebreaker.content.moon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Lunar regolith: fine, powdery soil. With no wind or rain on the Moon, boot prints stay forever,
 * so walking on it leaves a permanent trail.
 */
public class LunarRegolithBlock extends Block {
    public static final MapCodec<LunarRegolithBlock> CODEC = simpleCodec(LunarRegolithBlock::new);
    public static final EnumProperty<Footprint> PRINT = EnumProperty.create("print", Footprint.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public LunarRegolithBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRINT, Footprint.NONE).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRINT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(PRINT) == Footprint.NONE && canLeavePrint(entity)) {
            level.setBlock(pos, printFor(state, pos, entity.getDirection()), Block.UPDATE_CLIENTS);
        }
        super.stepOn(level, pos, state, entity);
    }

    public static boolean canLeavePrint(Entity entity) {
        return entity instanceof LivingEntity living && !living.isSpectator() && living.getBbWidth() >= 0.4F && !entity.isPassenger();
    }

    public static BlockState printFor(BlockState state, BlockPos pos, Direction walking) {
        Footprint foot = ((pos.getX() + pos.getZ()) & 1) == 0 ? Footprint.LEFT : Footprint.RIGHT;
        return state.setValue(PRINT, foot).setValue(FACING, walking.getAxis().isHorizontal() ? walking : Direction.NORTH);
    }
}
