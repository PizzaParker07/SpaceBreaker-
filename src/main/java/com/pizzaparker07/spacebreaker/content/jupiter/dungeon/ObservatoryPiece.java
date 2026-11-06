package com.pizzaparker07.spacebreaker.content.jupiter.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned volcano observatory on Io: a squat bunker of basalt bricks with a thick roof against the fallout of the
 * plumes, a telescope turret and a seismometer vault dug into the rock below. Sulfur crabs have moved into the warm
 * ruins.
 */
public class ObservatoryPiece extends StructurePiece {
    public static final int SIZE = 17;
    static final ResourceKey<LootTable> LOGS = Dungeons.lootTable("io", "observatory_logs");
    static final ResourceKey<LootTable> VAULT = Dungeons.lootTable("io", "observatory_vault");
    private final long seed;
    static final int F = 5;

    public ObservatoryPiece(long seed, int x, int y, int z, Direction facing) {
        super(JupiterStructures.OBSERVATORY_PIECE.get(), 0, makeBoundingBox(x, y - F, z, facing, SIZE, F + 12, SIZE));
        this.seed = seed;
        setOrientation(facing);
    }

    public ObservatoryPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(JupiterStructures.OBSERVATORY_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState basalt = s(JupiterModule.IONIAN_BASALT.base().get());
        BlockState slab = s(JupiterModule.IONIAN_BASALT.slab().get());
        BlockState wall = s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        BlockState roof = s(Blocks.POLISHED_BLACKSTONE);
        // footing and clearing
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < F; y++) {
                    placeBlock(level, basalt, x, y, z, box);
                }
                for (int y = F; y < F + 12; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, s(JupiterModule.SULFUR_SAND.get()), x, F - 1, z, box);
            }
        }
        // the bunker: 11 x 11, thick walls, low ceiling, small slit windows
        for (int x = 3; x <= 13; x++) {
            for (int z = 3; z <= 13; z++) {
                boolean edge = x == 3 || x == 13 || z == 3 || z == 13;
                placeBlock(level, s(Blocks.POLISHED_BASALT), x, F - 1, z, box);
                for (int y = F; y <= F + 4; y++) {
                    if (y == F + 4) {
                        placeBlock(level, roof, x, y, z, box);
                    } else if (edge) {
                        boolean slit = y == F + 2 && ((x == 3 || x == 13) && z % 3 == 1 || (z == 3 || z == 13) && x % 3 == 1);
                        placeBlock(level, slit ? s(Blocks.TINTED_GLASS) : wall, x, y, z, box);
                    }
                }
            }
        }
        // corner buttresses of basalt stairs
        for (int[] c : new int[][]{{2, 2}, {14, 2}, {2, 14}, {14, 14}}) {
            for (int y = F; y <= F + 3; y++) {
                placeBlock(level, basalt, c[0], y, c[1], box);
            }
            placeBlock(level, slab, c[0], F + 4, c[1], box);
        }
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 8, F, 13, box);
        placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 8, F + 1, 13, box);
        // telescope turret on the roof
        for (int x = 6; x <= 10; x++) {
            for (int z = 6; z <= 10; z++) {
                boolean ring = x == 6 || x == 10 || z == 6 || z == 10;
                if (ring) {
                    placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL), x, F + 5, z, box);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            placeBlock(level, s(Blocks.IRON_BLOCK), 8, F + 5 + (i > 1 ? 1 : 0), 8 - i, box);
        }
        placeBlock(level, s(Blocks.TINTED_GLASS), 8, F + 6, 4, box);
        placeBlock(level, s(Blocks.LIGHTNING_ROD), 10, F + 6, 10, box);
        // inside: logbook desk, seismograph, the logs chest
        placeBlock(level, s(Blocks.LECTERN), 5, F, 5, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 6, F, 5, box);
        placeBlock(level, s(Blocks.CALIBRATED_SCULK_SENSOR), 11, F, 5, box);
        placeBlock(level, s(Blocks.SCULK_SENSOR), 11, F, 6, box);
        createChest(level, box, RandomSource.create(seed + 1), 4, F, 11, LOGS);
        placeBlock(level, s(Blocks.BARREL), 5, F, 12, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 8, F + 3, 8, box);
        // stairs down to the vault cut into the rock
        for (int i = 0; i < 4; i++) {
            placeBlock(level, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 11, F - 1 - i, 9 - i, box);
            for (int y = F - i; y <= F + 1 - i + 2; y++) {
                placeBlock(level, s(Blocks.AIR), 11, y, 9 - i, box);
            }
        }
        for (int x = 9; x <= 13; x++) {
            for (int z = 3; z <= 6; z++) {
                for (int y = 1; y <= 3; y++) {
                    placeBlock(level, x == 9 || x == 13 || z == 3 || y == 3 ? wall : s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, s(Blocks.POLISHED_BASALT), x, 0, z, box);
            }
        }
        for (int y = 1; y <= 2; y++) {
            placeBlock(level, s(Blocks.AIR), 11, y, 6, box);
        }
        createChest(level, box, RandomSource.create(seed + 2), 12, 1, 4, VAULT);
        placeBlock(level, s(Blocks.SEA_LANTERN), 10, 1, 4, box);
        // fallout drifted against the walls
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean nearWall = (x == 2 || x == 14 || z == 2 || z == 14) && x > 1 && x < 15 && z > 1 && z < 15;
                if (nearWall && r.nextInt(3) == 0) {
                    placeBlock(level, s(r.nextBoolean() ? JupiterModule.RED_SULFUR.get() : JupiterModule.SO2_FROST.get()), x, F, z, box);
                }
            }
        }
        for (int[] g : new int[][]{{8, 15}, {1, 8}}) {
            BlockPos pos = getWorldPos(g[0], F, g[1]);
            if (box.isInside(pos)) {
                Mob m = JupiterEntities.SULFUR_CRAB.get().create(level.getLevel());
                if (m != null) {
                    m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(m);
                }
            }
        }
    }
}
