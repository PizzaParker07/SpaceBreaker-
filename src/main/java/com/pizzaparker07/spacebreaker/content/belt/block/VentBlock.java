package com.pizzaparker07.spacebreaker.content.belt.block;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An active vent: a geyser of water-ice grains (Europa, Enceladus) or a volcanic plume of sulfur and sulfur dioxide (Io).
 * It erupts in cycles; while erupting it throws a column of particles high into the sky and lifts anything standing over
 * it - in low gravity, a long way. Volcanic vents also burn. Eruptions follow the world clock, so every player sees the
 * same plume at the same time.
 */
public class VentBlock extends BaseEntityBlock {
    /** How a vent looks and behaves. */
    public enum Style { ICE, SULFUR, NITROGEN }

    /**
     * @param style   particle style
     * @param launch  upward push per server pulse while erupting (blocks/tick)
     * @param damage  damage per pulse to anything in the column (0 = none)
     * @param period  eruption cycle length in ticks
     * @param active  ticks per cycle the vent erupts
     * @param height  column height in blocks
     */
    public record Kind(Style style, double launch, float damage, int period, int active, int height) {
    }

    private final Kind kind;

    public VentBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new VentBlock(kind, p));
    }

    /** Whether a vent at this position is erupting at this game time. */
    public static boolean erupting(Kind kind, long gameTime, BlockPos pos) {
        long offset = Math.floorMod(pos.asLong() * 0x9E3779B97F4A7C15L >>> 20, kind.period());
        return (gameTime + offset) % kind.period() < kind.active();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VentBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, OuterWorlds.VENT.get(), level.isClientSide ? VentBlockEntity::clientTick : VentBlockEntity::serverTick);
    }
}
