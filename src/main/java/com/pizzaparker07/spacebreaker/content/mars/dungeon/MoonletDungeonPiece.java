package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The small dungeons of Mars' moons, built block by block:
 * <ul>
 *   <li>{@code probe} (Phobos): a sample-return probe that crashed into the dust, wings torn off, sample vault intact.</li>
 *   <li>{@code cache} (Deimos): an ice cache dug under the regolith by an earlier expedition: a hatch, a ladder shaft and
 *       a cold vault of water ice, dry ice and supplies.</li>
 * </ul>
 */
public class MoonletDungeonPiece extends StructurePiece {
    static final int SIZE = 15;
    private final long seed;
    private final boolean cache;

    public MoonletDungeonPiece(long seed, boolean cache, int x, int y, int z) {
        super(MarsStructures.MOONLET_PIECE.get(), 0, new BoundingBox(x, y - 10, z, x + SIZE - 1, y + 8, z + SIZE - 1));
        this.seed = seed;
        this.cache = cache;
        setOrientation(Direction.NORTH);
    }

    public MoonletDungeonPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.MOONLET_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.cache = tag.getBoolean("Cache");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putBoolean("Cache", cache);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Local y of the ground (the piece box starts 10 below it). */
    private static final int G = 10;

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        if (cache) {
            iceCache(level, box);
        } else {
            probe(level, box);
        }
    }

    private void probe(WorldGenLevel level, BoundingBox box) {
        BlockState soil = s(MarsModule.PHOBOS_REGOLITH.get());
        // the impact pit with a raised lip of ejected dust
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = Math.hypot(x - 7, z - 7);
                if (d <= 5.5) {
                    int depth = (int) Math.round(2.5 * (1 - d * d / 30.25)) + 1;
                    for (int y = G - depth + 1; y <= G + 4; y++) {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    }
                    placeBlock(level, soil, x, G - depth, z, box);
                } else if (d <= 7) {
                    placeBlock(level, soil, x, G + 1, z, box);
                }
            }
        }
        // the probe: a small octagonal capsule nosed into the pit floor, its heat shield to the east, the hatch to the
        // west torn open; the sample vault is still sealed inside
        int by = G - 3;
        BlockState hull = s(Blocks.SMOOTH_QUARTZ);
        BlockState rib = s(Blocks.POLISHED_ANDESITE);
        BlockState foil = s(com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.GOLD_FOIL_CLADDING.get());
        for (int x = 4; x <= 9; x++) {
            int sink = x >= 8 ? 1 : 0; // the nose went in deeper
            boolean cap = x == 4 || x == 9;
            for (int u = -2; u <= 2; u++) {
                int au = Math.abs(u);
                Direction in = u < 0 ? Direction.SOUTH : Direction.NORTH;
                for (int v = 0; v <= 3; v++) {
                    int y = by + v - sink;
                    BlockState st = null;
                    boolean interior = (au <= 1 && v >= 1 && v <= 2) || (au == 0 && v == 3);
                    if (v == 0 && au <= 1) {
                        st = cap ? rib : s(Blocks.SMOOTH_STONE);
                    } else if (v == 0) {
                        st = s(Blocks.SMOOTH_QUARTZ_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, in)
                                .setValue(net.minecraft.world.level.block.StairBlock.HALF, Half.TOP);
                    } else if (interior) {
                        st = cap ? rib : s(Blocks.AIR);
                    } else if (au == 2 && v <= 2) {
                        st = cap ? rib : v == 1 ? foil : hull;
                    } else if (au == 2) {
                        st = s(Blocks.SMOOTH_QUARTZ_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, in);
                    } else if (v == 3) {
                        st = cap ? rib : hull;
                    }
                    if (st != null) {
                        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, st, x, y, 7 + u, box);
                    }
                }
            }
        }
        // the heat shield: a brown dish across the nose
        for (int u = -2; u <= 2; u++) {
            for (int v = 0; v <= 3; v++) {
                if (Math.abs(u) + Math.abs(v - 1.5) <= 3) {
                    placeBlock(level, Math.abs(u) <= 1 && v >= 1 && v <= 2 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.TERRACOTTA), 10, by + v - 1, 7 + u, box);
                }
            }
        }
        // the hatch, blown open and lying in the dust
        placeBlock(level, s(Blocks.AIR), 4, by + 1, 7, box);
        placeBlock(level, s(Blocks.AIR), 4, by + 2, 7, box);
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM), 3, by + 1, 6, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 8, by, 7, Direction.WEST, MarsLoot.PHOBOS_PROBE,
                seed ^ 0x51L, box);
        // one solar wing still on its arm, the other torn off and thrown out of the pit
        placeBlock(level, s(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.Z), 6, by + 2, 4, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 6, by + 2, 3, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 6, by + 2, 2, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 11, G + 2, 12, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 12, G + 2, 12, box);
        // debris: shell plates and a bent antenna
        placeBlock(level, s(Blocks.SMOOTH_QUARTZ_SLAB), 2, G + 2, 10, box);
        placeBlock(level, s(Blocks.SMOOTH_QUARTZ_SLAB), 12, G + 2, 4, box);
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 6, by + 4, 7, box);
    }

    private void iceCache(WorldGenLevel level, BoundingBox box) {
        BlockState wall = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        // vault: 7 x 7 x 4 room, 6 blocks under the surface
        int fy = G - 7;
        for (int x = 4; x <= 10; x++) {
            for (int z = 4; z <= 10; z++) {
                for (int y = fy; y <= fy + 4; y++) {
                    boolean shell = x == 4 || x == 10 || z == 4 || z == 10 || y == fy || y == fy + 4;
                    boolean rib = (x == 4 || x == 10) && (z == 4 || z == 10) || y == fy + 4 && (x == 7 || z == 7);
                    placeBlock(level, shell ? (y == fy ? s(Blocks.SMOOTH_STONE) : rib ? s(Blocks.POLISHED_ANDESITE) : wall) : s(Blocks.AIR), x, y, z, box);
                }
            }
        }
        // ice blocks stacked along the walls, dry ice in the cold corner
        for (int z = 5; z <= 9; z++) {
            placeBlock(level, s(MarsModule.MARTIAN_ICE.get()), 5, fy + 1, z, box);
            if (z % 2 == 1) {
                placeBlock(level, s(MarsModule.MARTIAN_ICE.get()), 5, fy + 2, z, box);
            }
        }
        placeBlock(level, s(MarsModule.DRY_ICE.get()), 9, fy + 1, 9, box);
        placeBlock(level, s(MarsModule.DRY_ICE.get()), 9, fy + 1, 8, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get()), 7, fy + 4, 7, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 10, fy + 1, 6, Direction.WEST, MarsLoot.DEIMOS_CACHE, seed ^ 0x61L, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 8, fy + 1, 10, Direction.NORTH, MarsLoot.DEIMOS_CACHE, seed ^ 0x62L, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get()), 6, fy + 4, 7, box);
        placeBlock(level, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get()), 8, fy + 4, 7, box);
        // shaft with ladder up to a hatch on the surface
        for (int y = fy + 1; y <= G; y++) {
            placeBlock(level, s(Blocks.AIR), 7, y, 4, box);
            placeBlock(level, wall, 7, y, 3, box);
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, s(Blocks.LADDER),
                new BlockPos(7, y, 4), new BlockPos(7, y, 3), true, box);
        }
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), 7, G + 1, 4, box);
        // a marker beacon: a short mast with a reflector
        for (int y = G + 1; y <= G + 4; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 9, y, 3, box);
        }
        placeBlock(level, s(Blocks.POLISHED_ANDESITE), 9, G + 5, 3, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get())
                        .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.UP), 9, G + 6, 3, box);
    }

    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        createChest(level, box, RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z)), x, y, z, loot);
    }
}
