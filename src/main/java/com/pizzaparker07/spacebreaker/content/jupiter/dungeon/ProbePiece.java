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
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A research probe that landed on Europa long ago and sent a cryobot melting down through the ice. On the surface: the
 * lander's bent legs, a toppled antenna and the mouth of the melt shaft, its walls refrozen into stained ice. At the
 * bottom of the shaft the cryobot broke into a cave in the ice where a pocket of salty water never froze - and something
 * in that water glows.
 */
public class ProbePiece extends StructurePiece {
    public static final int SIZE = 21;
    /** Depth of the cave floor below the surface. */
    public static final int DEPTH = 24;
    static final ResourceKey<LootTable> INSTRUMENTS = Dungeons.lootTable("europa", "probe_instruments");
    static final ResourceKey<LootTable> SAMPLES = Dungeons.lootTable("europa", "probe_samples");
    private final long seed;

    public ProbePiece(long seed, int x, int y, int z, Direction facing) {
        super(JupiterStructures.PROBE_PIECE.get(), 0, makeBoundingBox(x, y - DEPTH, z, facing, SIZE, DEPTH + 10, SIZE));
        this.seed = seed;
        setOrientation(facing);
    }

    public ProbePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(JupiterStructures.PROBE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Local y of the surface. */
    static final int F = DEPTH;
    /** Local y of the cave floor. */
    static final int CAVE = 2;
    static final int SHAFT_X = 10;
    static final int SHAFT_Z = 10;

    /** World position of the cave centre (for showcases and tests). */
    public BlockPos caveCentre() {
        return getWorldPos(10, CAVE + 1, 10);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState ice = s(JupiterModule.EUROPAN_ICE.get());
        BlockState stained = s(JupiterModule.STAINED_ICE.get());
        BlockState brine = s(JupiterModule.REFROZEN_BRINE.get());
        // the cave: a flattened ellipsoid melted out of the shell, its floor a frozen brine crust
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y <= CAVE + 8; y++) {
                    double e = Math.pow((x - 10) / 9.0, 2) + Math.pow((z - 10) / 8.0, 2) + Math.pow((y - CAVE - 2.5) / 4.5, 2);
                    if (e < 1 && y > CAVE) {
                        placeBlock(level, s(Blocks.AIR), x, y, z, box);
                    } else if (e < 1.35) {
                        placeBlock(level, y <= CAVE ? brine : (r.nextInt(5) == 0 ? stained : ice), x, y, z, box);
                    }
                }
            }
        }
        // the brine pool, a little lower, lit from below by the serpents' glow
        for (int x = 3; x <= 9; x++) {
            for (int z = 8; z <= 15; z++) {
                double e = Math.pow((x - 6) / 3.4, 2) + Math.pow((z - 11.5) / 3.8, 2);
                if (e < 1) {
                    placeBlock(level, s(Blocks.WATER), x, CAVE, z, box);
                    placeBlock(level, s(Blocks.WATER), x, CAVE - 1, z, box);
                    placeBlock(level, e < 0.3 ? s(Blocks.SEA_LANTERN) : brine, x, CAVE - 2, z, box);
                } else if (e < 1.4) {
                    placeBlock(level, brine, x, CAVE - 1, z, box);
                }
            }
        }
        // icicles hanging from the roof
        for (int[] c : new int[][]{{13, 6}, {15, 12}, {8, 5}, {12, 15}, {5, 7}}) {
            int top = CAVE + 6;
            int len = 1 + r.nextInt(3);
            for (int i = 0; i < len; i++) {
                placeBlock(level, ice, c[0], top - i, c[1], box);
            }
        }
        // the cryobot: a copper cylinder lying where it broke through, drill tip into the pool, tether up the shaft
        for (int y = CAVE + 1; y <= CAVE + 3; y++) {
            placeBlock(level, s(Blocks.WAXED_CUT_COPPER), SHAFT_X, y, SHAFT_Z, box);
        }
        placeBlock(level, s(Blocks.WAXED_COPPER_BULB), SHAFT_X, CAVE + 4, SHAFT_Z, box);
        placeBlock(level, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.WEST), SHAFT_X - 1, CAVE + 1, SHAFT_Z, box);
        placeBlock(level, s(Blocks.WAXED_CUT_COPPER_SLAB), SHAFT_X + 1, CAVE + 1, SHAFT_Z, box);
        // shaft: melted straight down, walls refrozen and stained, a ladder bolted in later and the tether beside it
        for (int y = CAVE + 5; y <= F; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean wall = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    if (wall) {
                        placeBlock(level, r.nextInt(3) == 0 ? brine : stained, SHAFT_X + dx, y, SHAFT_Z + dz, box);
                    } else {
                        placeBlock(level, s(Blocks.AIR), SHAFT_X, y, SHAFT_Z, box);
                    }
                }
            }
            placeBlock(level, s(Blocks.CHAIN), SHAFT_X, y, SHAFT_Z, box);
        }
        for (int y = CAVE + 1; y <= F; y++) {
            placeBlock(level, s(Blocks.AIR), SHAFT_X, y, SHAFT_Z + 1, box);
            placeBlock(level, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), SHAFT_X, y, SHAFT_Z + 1, box);
            placeBlock(level, stained, SHAFT_X, y, SHAFT_Z + 2, box);
        }
        // instrument racks by the cave wall
        createChest(level, box, RandomSource.create(seed + 1), 15, CAVE + 1, 8, INSTRUMENTS);
        createChest(level, box, RandomSource.create(seed + 2), 15, CAVE + 1, 12, SAMPLES);
        placeBlock(level, s(Blocks.IRON_BLOCK), 15, CAVE + 1, 10, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 15, CAVE + 2, 10, box);
        placeBlock(level, s(Blocks.BREWING_STAND), 14, CAVE + 1, 10, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 13, CAVE + 5, 10, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, CAVE + 5, 12, box);
        surface(level, box, r);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = getWorldPos(5 + i, CAVE - 1, 10 + i);
            if (box.isInside(pos)) {
                Mob m = JupiterEntities.GLOW_SERPENT.get().create(level.getLevel());
                if (m != null) {
                    m.moveTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(m);
                }
            }
        }
    }

    /** The lander on the surface: four bent legs, the body tipped on its side, a toppled antenna, the shaft collar. */
    private void surface(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState frost = s(JupiterModule.EUROPAN_FROST.get());
        for (int x = 4; x <= 16; x++) {
            for (int z = 4; z <= 16; z++) {
                for (int y = F + 1; y <= F + 6; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                if (Math.hypot(x - SHAFT_X, z - SHAFT_Z) > 1.6) {
                    placeBlock(level, frost, x, F, z, box);
                }
            }
        }
        for (int[] l : new int[][]{{6, 6}, {14, 6}, {6, 14}, {14, 14}}) {
            placeBlock(level, s(Blocks.IRON_BARS), l[0], F + 1, l[1], box);
            placeBlock(level, s(Blocks.IRON_BARS), l[0] + Integer.signum(SHAFT_X - l[0]), F + 2, l[1] + Integer.signum(SHAFT_Z - l[1]), box);
        }
        for (int x = 7; x <= 9; x++) {
            for (int z = 6; z <= 7; z++) {
                placeBlock(level, s(Blocks.WHITE_CONCRETE), x, F + 1, z, box);
            }
            placeBlock(level, s(Blocks.WAXED_CUT_COPPER), x, F + 2, 6, box);
        }
        placeBlock(level, s(Blocks.GOLD_BLOCK), 9, F + 2, 7, box);
        // the high-gain antenna lies where it fell
        for (int x = 11; x <= 15; x++) {
            placeBlock(level, s(Blocks.IRON_BARS), x, F + 1, 15, box);
        }
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 16, F + 1, 15, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 16, F + 1, 16, box);
        // shaft collar of refrozen, stained ice
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 1.6 && d < 2.9) {
                    placeBlock(level, s(JupiterModule.STAINED_ICE.get()), SHAFT_X + dx, F, SHAFT_Z + dz, box);
                }
            }
        }
    }
}
