package com.pizzaparker07.spacebreaker.content.machines.multiblock;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Filler block of a multiblock machine: forwards use, picking and breaking to its controller. */
public class PartBlock extends BaseEntityBlock {
    public static final EnumProperty<PartShape> SHAPE = EnumProperty.create("shape", PartShape.class);

    public PartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, PartShape.PUMPJACK_POST).setValue(MachineBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, MachineBlock.FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(MachineBlock.FACING, rotation.rotate(state.getValue(MachineBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MachineBlock.FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MachineBlock.rotateShape(state.getValue(SHAPE).shape, state.getValue(MachineBlock.FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PartBlockEntity(pos, state);
    }

    public static @Nullable BlockPos controller(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PartBlockEntity be ? be.controller() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos c = controller(level, pos);
        return c == null ? InteractionResult.PASS : MachineBlock.openMenu(level, c, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        BlockPos c = controller(level, pos);
        if (c != null) {
            BlockState cs = level.getBlockState(c);
            return cs.useItemOn(stack, level, player, hand, hit.withPosition(c));
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos c = controller(level, pos);
        if (c != null && !level.isClientSide) {
            BlockState cs = level.getBlockState(c);
            if (cs.getBlock() instanceof MultiblockMachineBlock) {
                if (!player.isCreative()) {
                    Block.dropResources(cs, level, c, level.getBlockEntity(c), player, player.getMainHandItem());
                }
                level.destroyBlock(c, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        BlockPos c = controller(level, pos);
        return c != null ? new ItemStack(level.getBlockState(c).getBlock()) : ItemStack.EMPTY;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos c = controller(level, pos);
        if (c != null) {
            BlockState cs = level.getBlockState(c);
            if (cs.getBlock() instanceof MultiblockMachineBlock) {
                return cs.getDestroyProgress(player, level, c);
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
