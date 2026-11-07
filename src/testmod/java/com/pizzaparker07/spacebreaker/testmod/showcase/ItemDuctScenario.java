package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitSide;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** A Moon base sorting line: the excavator's chest emptied by an item duct into two chests, one filtered. */
final class ItemDuctScenario implements ShowcaseScenario {
    private static final int X0 = 24;
    private static final int Z0 = 24;
    private int gy;

    @Override
    public ResourceKey<Level> dimension() {
        return com.pizzaparker07.spacebreaker.content.moon.MoonModule.MOON;
    }

    private int ground(ServerLevel level) {
        return (int) Math.floor(((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain().height(X0, Z0)) + 1;
    }

    private static void duct(ServerLevel level, BlockPos pos) {
        Block duct = MachinesModule.ITEM_DUCT.get();
        level.setBlock(pos, Block.updateFromNeighbourShapes(duct.defaultBlockState(), level, pos), 3);
    }

    @Override
    public void build(ServerLevel level) {
        gy = ground(level);
        var floor = level.getBlockState(new BlockPos(X0, gy - 1, Z0));
        for (int x = X0 - 6; x <= X0 + 8; x++) {
            for (int z = Z0 - 4; z <= Z0 + 6; z++) {
                for (int y = gy - 2; y <= gy + 5; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < gy ? (floor.isAir() ? Blocks.STONE.defaultBlockState() : floor) : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos source = new BlockPos(X0, gy, Z0);
        BlockPos plain = new BlockPos(X0 + 5, gy, Z0);
        BlockPos sorted = new BlockPos(X0 + 3, gy, Z0 + 2);
        for (BlockPos p : new BlockPos[]{source, plain, sorted}) {
            level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
        }
        if (level.getBlockEntity(source) instanceof ChestBlockEntity c) {
            c.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
            c.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        }
        for (int x = 1; x <= 4; x++) {
            duct(level, new BlockPos(X0 + x, gy, Z0));
        }
        duct(level, new BlockPos(X0 + 3, gy, Z0 + 1));
        // a raised run over the walkway
        BlockPos first = new BlockPos(X0 + 1, gy, Z0);
        level.setBlock(first, level.getBlockState(first).setValue(ConduitBlock.SIDES.get(Direction.WEST), ConduitSide.EXTRACT), 3);
        if (level.getBlockEntity(new BlockPos(X0 + 3, gy, Z0 + 1)) instanceof ConduitBlockEntity be) {
            be.setItemFilter(Direction.SOUTH, Items.COBBLESTONE);
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        gy = ground(level);
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        s.add(new ShowcaseDriver.Shot("item_duct_01_line", 0, X0 + 2.5, gy + 3.2, Z0 - 3.2, 0F, 38F, true,
                CameraType.FIRST_PERSON, 60, p -> PlanetTime.setDayFraction(p.serverLevel(), 0.3), null));
        return s;
    }
}
