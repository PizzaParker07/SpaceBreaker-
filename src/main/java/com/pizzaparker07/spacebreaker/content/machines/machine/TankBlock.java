package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/** Storage tank with a window that shows the fill level. Each kind accepts one class of fluid. */
public class TankBlock extends BaseEntityBlock {
    public enum Kind {
        FLUID(32_000, f -> !f.getFluidType().isLighterThanAir() && f.getFluidType().getTemperature() > 200),
        PRESSURIZED(64_000, f -> f.getFluidType().isLighterThanAir()),
        CRYOGENIC(32_000, f -> !f.getFluidType().isLighterThanAir() && f.getFluidType().getTemperature() <= 200);

        public final int capacity;
        public final Predicate<FluidStack> filter;

        Kind(int capacity, Predicate<FluidStack> filter) {
            this.capacity = capacity;
            this.filter = filter;
        }
    }

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);
    public final Kind kind;

    public TankBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException();
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
        return new TankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MachinesModule.TANK_BE.get(), TankBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.getFluidHandler(stack).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TankBlockEntity be) {
            FluidStack f = be.tank().getFluid();
            player.displayClientMessage(f.isEmpty()
                    ? Component.translatable("gui.spacebreaker.tank_empty", be.tank().getCapacity())
                    : Component.translatable("gui.spacebreaker.tank", f.getHoverName(), f.getAmount(), be.tank().getCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TankBlockEntity be ? Math.round(be.tank().fraction() * 15) : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.tank." + kind.name().toLowerCase(java.util.Locale.ROOT), kind.capacity)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

}
