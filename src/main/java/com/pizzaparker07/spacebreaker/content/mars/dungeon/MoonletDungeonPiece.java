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
        // the probe body, tilted into the pit floor
        int by = G - 2;
        for (int x = 6; x <= 8; x++) {
            for (int z = 6; z <= 8; z++) {
                placeBlock(level, x == 7 && z == 7 ? s(Blocks.GOLD_BLOCK) : s(Blocks.WHITE_CONCRETE), x, by, z, box);
            }
        }
        placeBlock(level, s(Blocks.WHITE_CONCRETE), 7, by + 1, 7, box);
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 7, by + 2, 7, box);
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), 6, by + 1, 7, box);
        // torn solar wings and debris thrown out of the pit
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 2, G + 1, 6, box);
        placeBlock(level, s(MachinesModule.SOLAR_PANEL.get()), 12, G + 1, 9, box);
        placeBlock(level, s(Blocks.IRON_BARS), 11, G + 2, 3, box);
        placeBlock(level, s(Blocks.WHITE_CONCRETE), 3, G + 2, 11, box);
        chest(level, box, 8, by + 1, 7, MarsLoot.PHOBOS_PROBE);
    }

    private void iceCache(WorldGenLevel level, BoundingBox box) {
        BlockState wall = s(MarsModule.POLISHED_MARTIAN_BASALT.base().get());
        // vault: 7 x 7 x 4 room, 6 blocks under the surface
        int fy = G - 7;
        for (int x = 4; x <= 10; x++) {
            for (int z = 4; z <= 10; z++) {
                for (int y = fy; y <= fy + 4; y++) {
                    boolean shell = x == 4 || x == 10 || z == 4 || z == 10 || y == fy || y == fy + 4;
                    placeBlock(level, shell ? wall : s(Blocks.AIR), x, y, z, box);
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
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, fy + 3, 7, box);
        chest(level, box, 9, fy + 1, 5, MarsLoot.DEIMOS_CACHE);
        chest(level, box, 8, fy + 1, 9, MarsLoot.DEIMOS_CACHE);
        // shaft with ladder up to a hatch on the surface
        for (int y = fy + 1; y <= G; y++) {
            placeBlock(level, s(Blocks.AIR), 7, y, 4, box);
            placeBlock(level, wall, 7, y, 3, box);
            DungeonBits.placeFacing(level, box, getWorldPos(7, y, 4), getWorldPos(7, y, 3), s(Blocks.LADDER), true);
        }
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), 7, G + 1, 4, box);
        // a marker beacon: a short mast with a reflector
        for (int y = G + 1; y <= G + 4; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 9, y, 3, box);
        }
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 9, G + 5, 3, box);
    }

    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        createChest(level, box, RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z)), x, y, z, loot);
    }
}
