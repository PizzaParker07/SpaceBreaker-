package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned ice mine under the floor of a permanently shadowed polar crater on Mercury, where water ice has sat in
 * the dark for billions of years a few kilometres from ground that bakes at 430 °C. A head frame over the shaft, a
 * drift through the ice to the cutting face, a melt room, a store, and the mine's old maintenance drones.
 */
public class IceMinePiece extends StructurePiece {
    static final int SIZE = 25;
    private static final int G = 14;
    private final long seed;

    public IceMinePiece(long seed, int x, int y, int z) {
        super(MercuryStructures.ICE_MINE_PIECE.get(), 0, new BoundingBox(x, y - G, z, x + SIZE - 1, y + 10, z + SIZE - 1));
        this.seed = seed;
        setOrientation(Direction.NORTH);
    }

    public IceMinePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MercuryStructures.ICE_MINE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    public static final ResourceKey<LootTable> MINE_OFFICE = Dungeons.lootTable("mercury", "ice_mine_office");
    public static final ResourceKey<LootTable> MINE_STORES = Dungeons.lootTable("mercury", "ice_mine_stores");

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState ice = s(MercuryModule.MERCURY_ICE.get());
        BlockState wall = s(MercuryModule.MERCURIAN_CRUST.get());
        int fy = 2; // mine level floor (local y)
        // the ice body: a lens of ice around the workings
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 1; z < SIZE - 1; z++) {
                for (int y = 0; y <= 8; y++) {
                    double e = Math.pow((x - 12) / 11.5, 2) + Math.pow((z - 12) / 11.5, 2) + Math.pow((y - 4) / 5.0, 2);
                    if (e < 1) {
                        placeBlock(level, ice, x, y, z, box);
                    }
                }
            }
        }
        // drifts: an L-shaped tunnel with timber-free steel arches
        room(level, box, 10, fy, 2, 14, fy + 3, 22, wall);
        room(level, box, 3, fy, 15, 21, fy + 3, 19, wall);
        // melt room (west) and store (east)
        room(level, box, 2, fy, 5, 8, fy + 4, 11, wall);
        room(level, box, 16, fy, 5, 22, fy + 4, 11, wall);
        door(level, box, 9, fy, 8);
        door(level, box, 15, fy, 8);
        // puzzle: when the power failed the store was sealed with an ice plug; the sign says to cut through it
        placeBlock(level, ice, 15, fy + 1, 8, box);
        placeBlock(level, ice, 15, fy + 2, 8, box);
        com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.placeFacing(level, box, getWorldPos(13, fy + 2, 9),
                getWorldPos(14, fy + 2, 9), Blocks.OAK_WALL_SIGN.defaultBlockState(), true);
        BlockPos sign = getWorldPos(13, fy + 2, 9);
        if (box.isInside(sign)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "ice_mine_store");
        }
        // trap: the old frost-protection system; a plate in the drift makes a dispenser in the floor throw powder snow
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 12, fy + 1, 12, box);
        placeBlock(level, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.UP),
                12, fy, 11, box);
        BlockPos disp = getWorldPos(12, fy, 11);
        if (box.isInside(disp)) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.load(level, disp,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET));
        }
        // the cutting face: raw ice exposed at the end of the drift, with the stalled cutter
        placeBlock(level, s(Blocks.ANVIL), 12, fy + 1, 3, box);
        placeBlock(level, s(Blocks.GRINDSTONE), 11, fy + 1, 3, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 13, fy + 1, 3, box);
        for (int x = 10; x <= 14; x++) {
            placeBlock(level, ice, x, fy + 1, 2, box);
            placeBlock(level, ice, x, fy + 2, 2, box);
        }
        // melt room: furnaces and tanks
        placeBlock(level, s(Blocks.BLAST_FURNACE), 3, fy + 1, 6, box);
        placeBlock(level, s(Blocks.FURNACE), 3, fy + 1, 7, box);
        placeBlock(level, s(Blocks.CAULDRON), 3, fy + 1, 9, box);
        placeBlock(level, s(Blocks.WATER_CAULDRON), 4, fy + 1, 10, box);
        createChest(level, box, RandomSource.create(seed + 1), 7, fy + 1, 6, MINE_OFFICE);
        // store
        createChest(level, box, RandomSource.create(seed + 2), 21, fy + 1, 6, MINE_STORES);
        placeBlock(level, s(Blocks.BARREL), 21, fy + 1, 7, box);
        placeBlock(level, s(Blocks.BARREL), 21, fy + 2, 7, box);
        placeBlock(level, ice, 17, fy + 1, 10, box);
        placeBlock(level, ice, 18, fy + 1, 10, box);
        // lamps
        for (int[] l : new int[][]{{12, 6}, {12, 13}, {6, 17}, {18, 17}, {5, 8}, {19, 8}}) {
            placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), l[0], fy + 3, l[1], box);
        }
        // shaft up to the crater floor with a head frame
        for (int y = fy + 1; y <= G + 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean centre = dx == 0 && dz == 0;
                    if (y > fy + 3 || centre) {
                        placeBlock(level, centre ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH)
                                : (dz == -1 && dx == 0 ? wall : (dz == -1 ? wall : s(Blocks.AIR))), 12 + dx, y, 20 + dz, box);
                    }
                }
            }
            placeBlock(level, wall, 12, y, 19, box);
        }
        for (int y = G + 1; y <= G + 7; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 10, y, 18, box);
            placeBlock(level, s(Blocks.IRON_BARS), 14, y, 18, box);
            placeBlock(level, s(Blocks.IRON_BARS), 10, y, 22, box);
            placeBlock(level, s(Blocks.IRON_BARS), 14, y, 22, box);
        }
        for (int x = 10; x <= 14; x++) {
            placeBlock(level, s(Blocks.IRON_BLOCK), x, G + 8, 20, box);
        }
        placeBlock(level, s(Blocks.CHAIN), 12, G + 7, 20, box);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 12, G + 9, 20, box);
        // the old maintenance drones still patrol the drift
        guard(level, box, 12, fy + 2, 10);
        guard(level, box, 8, fy + 2, 17);
    }

    private void room(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    boolean floor = y == y0;
                    boolean arch = (x == x0 || x == x1 || y == y1) && z % 4 == 0;
                    BlockState here = getBlock(level, x, y, z, box);
                    if (floor) {
                        placeBlock(level, wall, x, y, z, box);
                    } else if (arch) {
                        placeBlock(level, s(Blocks.IRON_BLOCK), x, y, z, box);
                    } else if (x == x0 || x == x1 || y == y1 || z == z0 || z == z1) {
                        if (here.isAir() || !here.is(Blocks.IRON_BLOCK)) {
                            placeBlock(level, here.is(MercuryModule.MERCURY_ICE.get()) ? here : wall, x, y, z, box);
                        }
                    } else {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    }
                }
            }
        }
    }

    /** A doorway through both walls between two rooms (x-1 .. x+1). */
    private void door(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            placeBlock(level, s(Blocks.AIR), x + dx, y + 1, z, box);
            placeBlock(level, s(Blocks.AIR), x + dx, y + 2, z, box);
        }
    }

    private void guard(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = MoonEntities.SURVEY_DRONE.get().create(level.getLevel());
        if (mob != null) {
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
