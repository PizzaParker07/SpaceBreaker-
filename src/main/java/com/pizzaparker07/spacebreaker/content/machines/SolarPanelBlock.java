package com.pizzaparker07.spacebreaker.content.machines;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
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

    /** Peak output of this kind of panel at Earth's distance, FE/t. */
    public int peak() {
        return SolarPanelBlockEntity.PEAK_OUTPUT;
    }

    /** Sunlight at this body's distance from the sun (and local weather) as a whole percentage of Earth's. */
    public static String sunlightPercent(double factor) {
        double pct = factor * 100;
        return pct < 10 ? String.format(java.util.Locale.ROOT, "%.1f", pct) : Long.toString(Math.round(pct));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   net.minecraft.world.entity.player.Player player,
                                                                   net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be) {
            if (player.isShiftKeyDown() && be.dust() > 0) {
                be.clean(1F);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BRUSH_SAND_COMPLETED, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.solar_panel.wiped"), true);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if (be.dust() >= 0.01F) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.solar_panel.dusty",
                        Math.round(be.dust() * 100), String.format(java.util.Locale.ROOT, "%.1f", be.getExactOutput())), true);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            double factor = com.pizzaparker07.spacebreaker.core.env.Environment.solarFactor(level, pos);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.solar_panel",
                    sunlightPercent(factor), String.format(java.util.Locale.ROOT, "%.1f", be.getExactOutput())), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.solar_panel", peak())
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        Level level = context.level();
        if (level != null) {
            double factor = com.pizzaparker07.spacebreaker.core.env.Environment.solarFactor(level);
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.solar_panel.here", sunlightPercent(factor),
                    String.format(java.util.Locale.ROOT, "%.1f", peak() * Math.min(SolarPanelBlockEntity.MAX_SOLAR_FACTOR, factor)))
                    .withStyle(factor < 0.25 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MachinesModule.SOLAR_PANEL_BE.get(),
                level.isClientSide ? SolarPanelBlockEntity::clientTick : SolarPanelBlockEntity::serverTick);
    }
}
