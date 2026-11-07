package com.pizzaparker07.spacebreaker.content.rocketry.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Landing beacon: marks a base's landing spot. A rocket descending from orbit onto this world comes down on
 * the nearest beacon instead of wherever its orbit happens to be, so a shuttle can fly between an orbit station
 * and a surface base again and again.
 */
public class LandingBeaconBlock extends Block {
    public LandingBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server) {
            LandingBeacons.get(server).add(pos);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server && placer instanceof net.minecraft.world.entity.player.Player player) {
            LandingBeacons.get(server).setOwner(pos, player.getGameProfile().getName());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel server && !newState.is(this)) {
            LandingBeacons.get(server).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
