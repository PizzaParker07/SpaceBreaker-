package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.block.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.item.OxygenTankItem;
import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModDataComponents;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the style-sample showcase in a superflat world (ground surface at y = -61). */
public final class ShowcaseBuilder {
    static final int FLOOR = -61;

    private ShowcaseBuilder() {
    }

    static void build(ServerLevel level) {
        // clear the stage and lay a regolith floor
        for (int x = -12; x <= 12; x++) {
            for (int z = -8; z <= 14; z++) {
                for (int y = FLOOR + 1; y <= FLOOR + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                boolean stage = x >= -9 && x <= 9 && z >= -5 && z <= 11;
                level.setBlock(new BlockPos(x, FLOOR, z), stage ? ModBlocks.LUNAR_REGOLITH.get().defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // back wall of mare basalt with item frames
        for (int x = -7; x <= 7; x++) {
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                level.setBlock(new BlockPos(x, y, -4), ModBlocks.MARE_BASALT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        ItemStack sunVisor = new ItemStack(ModItems.EVA_HELMET.get());
        sunVisor.set(ModDataComponents.SUN_VISOR.get(), true);
        ItemStack[] framed = {
                new ItemStack(ModItems.RAW_BAUXITE.get()), new ItemStack(ModItems.ALUMINUM_INGOT.get()),
                new ItemStack(ModItems.ALUMINUM_PLATE.get()), OxygenTankItem.filled(ModItems.OXYGEN_TANK.get()),
                new ItemStack(ModItems.EVA_HELMET.get())};
        for (int i = 0; i < framed.length; i++) {
            ItemFrame frame = new ItemFrame(level, new BlockPos(-4 + i * 2, FLOOR + 3, -3), Direction.SOUTH);
            frame.setItem(framed[i], false);
            level.addFreshEntity(frame);
        }
        // ore and rock columns
        Block[] column = {ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), ModBlocks.RAW_BAUXITE_BLOCK.get(),
                ModBlocks.MARE_BASALT.get(), ModBlocks.ILMENITE_ORE.get()};
        for (int i = 0; i < column.length; i++) {
            for (int y = 1; y <= 2; y++) {
                level.setBlock(new BlockPos(-4 + i * 2, FLOOR + y, 0), column[i].defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // solar tracker array
        for (int x = -7; x <= -5; x++) {
            for (int z = 4; z <= 6; z++) {
                level.setBlock(new BlockPos(x, FLOOR + 1, z), ModBlocks.SOLAR_PANEL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // helmets on stands: visor raised and sun visor lowered
        spawnStand(level, 3.5, 5.5, new ItemStack(ModItems.EVA_HELMET.get()));
        spawnStand(level, 5.5, 5.5, sunVisor);
        // a walk across the regolith
        for (int z = 10; z >= 2; z--) {
            BlockPos pos = new BlockPos(0, FLOOR, z);
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, LunarRegolithBlock.printFor(state, pos, Direction.NORTH), Block.UPDATE_CLIENTS);
        }
        for (int i = 0; i < 6; i++) {
            BlockPos pos = new BlockPos(1 + i, FLOOR, 9 - (i / 2));
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, LunarRegolithBlock.printFor(state, pos, Direction.EAST), Block.UPDATE_CLIENTS);
        }
    }

    private static void spawnStand(ServerLevel level, double x, double z, ItemStack helmet) {
        ArmorStand stand = new ArmorStand(level, x, FLOOR + 1, z);
        stand.setYRot(-15F);
        stand.setYBodyRot(-15F);
        stand.setYHeadRot(-15F);
        stand.setItemSlot(EquipmentSlot.HEAD, helmet);
        level.addFreshEntity(stand);
    }
}
