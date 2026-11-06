package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

/** Small helpers for puzzles and traps in the inner-planet dungeons (call after the block itself is placed). */
public final class DungeonBits {
    private DungeonBits() {
    }

    /** Writes the four translated lines {@code sign.spacebreaker.<key>.0..3} on a sign at a world position. */
    public static void signText(WorldGenLevel level, BlockPos pos, String key) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity be) {
            SignText text = new SignText();
            for (int i = 0; i < 4; i++) {
                text = text.setMessage(i, Component.translatable("sign.spacebreaker." + key + "." + i));
            }
            be.setText(text, true);
        }
    }

    /**
     * Places a directional block so it points from {@code at} toward {@code toward} (both world positions), whatever
     * the piece's rotation and mirror: a dispenser aimed at a target cell, or (with the wall as {@code toward} and
     * {@code away} true) a lever or wall sign facing out of its wall.
     */
    public static void placeFacing(WorldGenLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox box, BlockPos at, BlockPos toward,
                                   net.minecraft.world.level.block.state.BlockState state, boolean away) {
        if (!box.isInside(at)) {
            return;
        }
        net.minecraft.core.Direction d = net.minecraft.core.Direction.fromDelta(toward.getX() - at.getX(), toward.getY() - at.getY(), toward.getZ() - at.getZ());
        if (d == null) {
            return;
        }
        if (away) {
            d = d.getOpposite();
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, d);
        } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) && d.getAxis().isHorizontal()) {
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, d);
        }
        level.setBlock(at, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** Loads a dispenser at a world position. */
    public static void load(WorldGenLevel level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof DispenserBlockEntity d) {
            d.setItem(0, stack);
        }
    }
}
