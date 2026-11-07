package com.pizzaparker07.spacebreaker.content.belt.dungeon;

import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned ice mining outpost on Ceres. On the surface: a crew habitat and an ice-processing hall joined by a short
 * pressurised tube, a landing pad and a radio mast, half-buried in drifted regolith from decades of nearby impacts. A
 * head frame stands over a shaft down to the ice drift, where the crew cut a lens of buried water ice - and where a
 * colony of rock mites has moved in, drawn by the salts the drills left behind.
 */
public class MiningOutpostPiece extends StructurePiece {
    public static final int SIZE = 29;
    /** Depth of the ice drift floor below the surface. */
    public static final int DEPTH = 13;
    static final ResourceKey<LootTable> OFFICE = Dungeons.lootTable("ceres", "outpost_office");
    static final ResourceKey<LootTable> STORES = Dungeons.lootTable("ceres", "outpost_stores");
    static final ResourceKey<LootTable> CORE_STORE = Dungeons.lootTable("ceres", "outpost_core_store");
    private final long seed;

    public MiningOutpostPiece(long seed, int x, int y, int z, Direction facing) {
        super(BeltStructures.OUTPOST_PIECE.get(), 0, makeBoundingBox(x, y - DEPTH - 1, z, facing, SIZE, DEPTH + 14, SIZE));
        this.seed = seed;
        setOrientation(facing);
    }

    public MiningOutpostPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(BeltStructures.OUTPOST_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Local y of the surface (floor of the buildings). */
    static final int F = DEPTH + 1;

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        foundation(level, box);
        habitat(level, box, r);
        processing(level, box, r);
        pad(level, box);
        mast(level, box);
        headFrame(level, box);
        mine(level, box, r);
        drifts(level, box, r);
    }

    // ------------------------------------------------------------------ surface

    /** Levels the site: firm rock under the buildings, air above. */
    private void foundation(WorldGenLevel level, BoundingBox box) {
        BlockState rock = s(BeltModule.CEREAN_ROCK.base().get());
        BlockState soil = s(BeltModule.CERES_REGOLITH.get());
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 1; z < SIZE - 1; z++) {
                for (int y = F - 3; y < F; y++) {
                    placeBlock(level, y == F - 1 ? soil : rock, x, y, z, box);
                }
                for (int y = F; y < F + 12; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
            }
        }
    }

    private void module(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1, int h) {
        BlockState wall = s(Blocks.LIGHT_GRAY_CONCRETE);
        BlockState frame = s(Blocks.IRON_BLOCK);
        BlockState roof = s(Blocks.WHITE_CONCRETE);
        BlockState floor = s(Blocks.SMOOTH_STONE);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edgeX = x == x0 || x == x1;
                boolean edgeZ = z == z0 || z == z1;
                placeBlock(level, floor, x, F - 1, z, box);
                for (int y = F; y <= F + h; y++) {
                    if (y == F + h) {
                        // rounded roof: the outer ring one block lower reads as a curved hull
                        placeBlock(level, edgeX || edgeZ ? s(Blocks.AIR) : roof, x, y, z, box);
                    } else if (edgeX || edgeZ) {
                        boolean corner = edgeX && edgeZ;
                        placeBlock(level, corner ? s(Blocks.AIR) : wall, x, y, z, box);
                    } else {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    }
                }
                if (edgeX && edgeZ) {
                    for (int y = F; y < F + h - 1; y++) {
                        placeBlock(level, frame, x, y, z, box);
                    }
                }
                if ((edgeX || edgeZ) && !(edgeX && edgeZ)) {
                    placeBlock(level, roof, x, F + h - 1, z, box);
                }
            }
        }
        // iron ribs every third block
        for (int x = x0 + 2; x < x1; x += 3) {
            for (int y = F; y < F + h - 1; y++) {
                placeBlock(level, frame, x, y, z0, box);
                placeBlock(level, frame, x, y, z1, box);
            }
        }
        // windows: a band of glass panes in the long walls
        for (int x = x0 + 1; x < x1; x++) {
            if ((x - x0) % 3 != 2) {
                placeBlock(level, s(Blocks.GLASS_PANE), x, F + 2, z0, box);
                placeBlock(level, s(Blocks.GLASS_PANE), x, F + 2, z1, box);
            }
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        BlockState d = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), x, F, z, box);
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, F + 1, z, box);
    }

    private void habitat(WorldGenLevel level, BoundingBox box, RandomSource r) {
        module(level, box, 2, 3, 11, 10, 5);
        // tube to the processing hall
        for (int x = 12; x <= 15; x++) {
            for (int y = F; y <= F + 3; y++) {
                for (int z = 5; z <= 8; z++) {
                    boolean shell = y == F + 3 || z == 5 || z == 8;
                    placeBlock(level, shell ? s(Blocks.LIGHT_GRAY_CONCRETE) : s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, s(Blocks.SMOOTH_STONE), x, F - 1, 6, box);
                placeBlock(level, s(Blocks.SMOOTH_STONE), x, F - 1, 7, box);
            }
        }
        placeBlock(level, s(Blocks.IRON_BLOCK), 13, F + 3, 6, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 13, F + 3, 7, box);
        for (int z = 6; z <= 7; z++) {
            for (int y = F; y <= F + 1; y++) {
                placeBlock(level, s(Blocks.AIR), 11, y, z, box);
                placeBlock(level, s(Blocks.AIR), 16, y, z, box);
            }
        }
        // the airlock: an iron door on the south wall with a pressure plate
        door(level, box, 5, 10, Direction.SOUTH);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 5, F, 11, box);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 5, F, 9, box);
        // crew quarters: bunks, a desk with the logbook, the office chest
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 3, F, 4, box);
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 4, F, 4, box);
        placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), 3, F + 2, 4, box);
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 3, F + 3, 4, box);
        placeBlock(level, s(Blocks.LIGHT_GRAY_WOOL), 4, F + 3, 4, box);
        placeBlock(level, s(Blocks.LECTERN), 9, F, 4, box);
        placeBlock(level, s(Blocks.CRAFTING_TABLE), 10, F, 4, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 10, F, 5, box);
        createChest(level, box, RandomSource.create(seed + 1), 10, F, 9, OFFICE);
        placeBlock(level, s(Blocks.BARREL), 3, F, 9, box);
        placeBlock(level, s(Blocks.BARREL), 3, F, 8, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 6, F + 4, 6, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 6, F + 4, 7, box);
        // a cracked window: the reason the crew left
        placeBlock(level, s(Blocks.AIR), 7, F + 2, 3, box);
        placeBlock(level, s(BeltModule.CERES_REGOLITH.get()), 7, F, 4, box);
        placeBlock(level, s(BeltModule.CERES_REGOLITH.get()), 8, F, 4, box);
    }

    private void processing(WorldGenLevel level, BoundingBox box, RandomSource r) {
        module(level, box, 16, 2, 26, 11, 6);
        // ice melters: furnaces under copper hoppers, cauldrons for the meltwater, an ice bunker
        for (int z = 3; z <= 5; z++) {
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST), 25, F, z, box);
            placeBlock(level, s(Blocks.HOPPER), 25, F + 1, z, box);
            placeBlock(level, s(Blocks.WAXED_CUT_COPPER), 25, F + 2, z, box);
        }
        placeBlock(level, s(Blocks.CAULDRON), 23, F, 3, box);
        placeBlock(level, s(Blocks.CAULDRON), 22, F, 3, box);
        for (int x = 17; x <= 19; x++) {
            for (int y = F; y <= F + 1; y++) {
                placeBlock(level, s(BeltModule.CEREAN_ICE.get()), x, y, 3, box);
            }
            placeBlock(level, s(Blocks.IRON_BARS), x, F, 4, box);
        }
        placeBlock(level, s(BeltModule.CEREAN_ICE.get()), 17, F, 4, box);
        createChest(level, box, RandomSource.create(seed + 2), 25, F, 10, STORES);
        placeBlock(level, s(Blocks.BARREL), 24, F, 10, box);
        placeBlock(level, s(Blocks.BARREL), 25, F + 1, 10, box);
        placeBlock(level, s(Blocks.SMITHING_TABLE), 17, F, 10, box);
        placeBlock(level, s(Blocks.STONECUTTER), 18, F, 10, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 21, F + 5, 6, box);
        // trapdoor and ladder down the inner shaft to the drift
        for (int y = 2; y < F; y++) {
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 21, y, 10, box);
            placeBlock(level, s(BeltModule.CEREAN_ROCK.base().get()), 21, y, 9, box);
        }
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.IRON_TRAPDOOR.defaultBlockState(), 21, F - 1, 10, box);
        door(level, box, 21, 11, Direction.SOUTH);
    }

    private void pad(WorldGenLevel level, BoundingBox box) {
        int cx = 7;
        int cz = 21;
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= 5.3) {
                    boolean mark = d > 4.3 || (x == cx && Math.abs(z - cz) <= 2) || (z == cz && Math.abs(x - cx) <= 2);
                    placeBlock(level, mark ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.SMOOTH_STONE), x, F - 1, z, box);
                }
            }
        }
        // a lander that never left: legs, a squat body, a dish
        for (int[] l : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            placeBlock(level, s(Blocks.IRON_BARS), cx + l[0], F, cz + l[1], box);
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                placeBlock(level, s(Blocks.IRON_BLOCK), x, F + 1, z, box);
                placeBlock(level, (x == cx && z == cz) ? s(Blocks.OBSERVER) : s(Blocks.LIGHT_GRAY_CONCRETE), x, F + 2, z, box);
            }
        }
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), cx, F + 3, cz, box);
        placeBlock(level, s(Blocks.GOLD_BLOCK), cx + 1, F + 1, cz - 1, box);
    }

    private void mast(WorldGenLevel level, BoundingBox box) {
        for (int y = F; y <= F + 8; y++) {
            placeBlock(level, s(Blocks.IRON_BARS), 14, y, 14, box);
        }
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 14, F + 9, 14, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 13, F + 6, 14, box);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 14, F, 13, box);
    }

    private void headFrame(WorldGenLevel level, BoundingBox box) {
        int cx = 21;
        int cz = 21;
        BlockState rock = s(BeltModule.CEREAN_ROCK.base().get());
        for (int y = 1; y <= F; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    placeBlock(level, s(Blocks.AIR), cx + dx, y, cz + dz, box);
                }
            }
            for (int dx = -2; dx <= 2; dx++) {
                placeBlock(level, rock, cx + dx, y, cz - 2, box);
                placeBlock(level, rock, cx + dx, y, cz + 2, box);
            }
            placeBlock(level, rock, cx - 2, y, cz - 1, box);
            placeBlock(level, rock, cx - 2, y, cz + 1, box);
            placeBlock(level, rock, cx + 2, y, cz - 1, box);
            placeBlock(level, rock, cx + 2, y, cz + 1, box);
            placeBlock(level, rock, cx - 2, y, cz, box);
            placeBlock(level, rock, cx + 2, y, cz, box);
        }
        // collar and frame
        for (int dx = -2; dx <= 2; dx++) {
            placeBlock(level, s(Blocks.IRON_BLOCK), cx + dx, F - 1, cz - 2, box);
            placeBlock(level, s(Blocks.IRON_BLOCK), cx + dx, F - 1, cz + 2, box);
        }
        for (int y = F; y <= F + 6; y++) {
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                placeBlock(level, s(Blocks.IRON_BARS), cx + c[0], y, cz + c[1], box);
            }
        }
        for (int d = -2; d <= 2; d++) {
            placeBlock(level, s(Blocks.IRON_BLOCK), cx + d, F + 7, cz, box);
            placeBlock(level, s(Blocks.IRON_BLOCK), cx, F + 7, cz + d, box);
        }
        for (int y = F + 2; y <= F + 6; y++) {
            placeBlock(level, s(Blocks.CHAIN), cx, y, cz, box);
        }
        placeBlock(level, s(Blocks.ANVIL), cx, F + 1, cz, box);
        placeBlock(level, s(Blocks.REDSTONE_LAMP), cx, F + 8, cz, box);
    }

    // ------------------------------------------------------------------ underground

    private void mine(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState ice = s(BeltModule.CEREAN_ICE.get());
        BlockState rock = s(BeltModule.CEREAN_ROCK.base().get());
        BlockState clay = s(BeltModule.AMMONIATED_CLAY.get());
        int fy = 1;
        // the ice lens the crew came for
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 12; z < SIZE - 1; z++) {
                for (int y = 0; y <= 7; y++) {
                    double e = Math.pow((x - 10) / 9.5, 2) + Math.pow((z - 20) / 7.5, 2) + Math.pow((y - 3) / 4.0, 2);
                    if (e < 1) {
                        placeBlock(level, ice, x, y, z, box);
                    }
                }
            }
        }
        // drift from the shaft (21, 21) west to the cutting face, and north to the inner ladder (21, 10)
        tunnel(level, box, 3, fy, 20, 21, fy + 3, 22, rock);
        tunnel(level, box, 20, fy, 11, 22, fy + 3, 20, rock);
        // cutting face: raw ice with a stalled cutter
        for (int z = 20; z <= 22; z++) {
            for (int y = fy + 1; y <= fy + 2; y++) {
                placeBlock(level, ice, 2, y, z, box);
            }
        }
        placeBlock(level, s(Blocks.GRINDSTONE), 4, fy + 1, 21, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 4, fy + 1, 20, box);
        placeBlock(level, s(Blocks.IRON_BLOCK), 4, fy + 1, 22, box);
        for (int x = 6; x <= 19; x++) {
            placeBlock(level, s(Blocks.RAIL), x, fy + 1, 21, box);
        }
        // core sample store off the drift
        tunnel(level, box, 12, fy, 23, 16, fy + 3, 26, rock);
        createChest(level, box, RandomSource.create(seed + 3), 15, fy + 1, 25, CORE_STORE);
        placeBlock(level, s(Blocks.BARREL), 13, fy + 1, 25, box);
        placeBlock(level, s(Blocks.CHISELED_TUFF), 12, fy + 1, 26, box);
        placeBlock(level, s(Blocks.CHISELED_TUFF), 16, fy + 1, 26, box);
        // the mites' den: a burrowed pocket of salty clay with their nest
        for (int x = 5; x <= 10; x++) {
            for (int z = 14; z <= 18; z++) {
                for (int y = fy; y <= fy + 3; y++) {
                    double e = Math.pow((x - 7.5) / 3.2, 2) + Math.pow((z - 16) / 2.6, 2) + Math.pow((y - fy - 1.5) / 2.2, 2);
                    if (e < 1) {
                        placeBlock(level, y == fy ? clay : s(Blocks.AIR), x, y, z, box);
                    } else if (e < 1.5) {
                        placeBlock(level, r.nextInt(3) == 0 ? s(BeltModule.CARBONATE_CRUST.get()) : clay, x, y, z, box);
                    }
                }
            }
        }
        for (int y = fy + 1; y <= fy + 2; y++) {
            placeBlock(level, s(Blocks.AIR), 7, y, 19, box);
            placeBlock(level, s(Blocks.AIR), 7, y, 20, box);
        }
        placeBlock(level, s(Blocks.SPAWNER), 7, fy + 1, 16, box);
        BlockPos sp = getWorldPos(7, fy + 1, 16);
        if (box.isInside(sp) && level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(BeltEntities.ROCK_MITE.get(), RandomSource.create(seed + 7));
        }
        // lamps along the drift
        for (int[] l : new int[][]{{8, 21}, {15, 21}, {21, 13}, {21, 17}, {14, 24}}) {
            placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), l[0], fy + 3, l[1], box);
        }
        // ladder down the head-frame shaft, against its south wall
        for (int y = fy + 1; y < F; y++) {
            placeBlock(level, rock, 21, y, 23, box);
            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), 21, y, 22, box);
        }
        guard(level, box, BeltEntities.ROCK_MITE.get(), 9, fy + 1, 16);
        guard(level, box, BeltEntities.ROCK_MITE.get(), 6, fy + 1, 15);
        guard(level, box, MoonEntities.SURVEY_DRONE.get(), 21, fy + 2, 15);
    }

    /** A drift: air inside, rock walls where the ground is not ice, iron arches every fourth block. */
    private void tunnel(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall) {
        boolean alongX = x1 - x0 > z1 - z0;
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                for (int y = y0; y <= y1 + 1; y++) {
                    boolean inside = x >= x0 && x <= x1 && z >= z0 && z <= z1 && y > y0 && y <= y1;
                    boolean arch = (alongX ? x % 4 == 0 : z % 4 == 0) && !inside;
                    BlockState here = getBlock(level, x, y, z, box);
                    if (inside) {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    } else if (y == y0) {
                        placeBlock(level, s(Blocks.SMOOTH_STONE), x, y, z, box);
                    } else if (arch && y <= y1 + 1) {
                        placeBlock(level, s(Blocks.IRON_BLOCK), x, y, z, box);
                    } else if (here.isAir()) {
                        placeBlock(level, wall, x, y, z, box);
                    }
                }
            }
        }
    }

    /** Wind-blown regolith drifts against the buildings: mounds on the north side and over the old pad edge. */
    private void drifts(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState soil = s(BeltModule.CERES_REGOLITH.get());
        for (int[] m : new int[][]{{1, 2, 3}, {27, 6, 3}, {12, 1, 2}, {1, 18, 2}, {26, 25, 3}}) {
            for (int x = m[0] - m[2]; x <= m[0] + m[2]; x++) {
                for (int z = m[1] - m[2]; z <= m[1] + m[2]; z++) {
                    double d = Math.hypot(x - m[0], z - m[1]);
                    int h = (int) Math.round((m[2] - d) * 0.9 + r.nextFloat() * 0.6);
                    for (int y = 0; y < h; y++) {
                        BlockPos p = getWorldPos(x, F + y, z);
                        if (box.isInside(p) && level.getBlockState(p).isAir()) {
                            placeBlock(level, soil, x, F + y, z, box);
                        }
                    }
                }
            }
        }
    }

    private void guard(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob != null) {
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
