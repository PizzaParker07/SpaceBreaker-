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

    /** Loads a dispenser at a world position. */
    public static void load(WorldGenLevel level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof DispenserBlockEntity d) {
            d.setItem(0, stack);
        }
    }
}
