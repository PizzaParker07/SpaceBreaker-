package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Launch control console: checklist, propellant and Δv readouts, countdown and the launch button. */
public class LaunchConsoleBlock extends TowerAttachmentBlock {
    public static final MapCodec<LaunchConsoleBlock> CODEC = simpleCodec(LaunchConsoleBlock::new);
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(1, 0, 1, 15, 10, 15),
            Block.box(1, 10, 6, 15, 15, 15));

    public LaunchConsoleBlock(Properties properties) {
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
        return new LaunchConsoleBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LaunchConsoleBlockEntity be) {
            be.refresh();
            player.openMenu(be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
