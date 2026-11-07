package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The wreck of an old lander: a hexagonal white body slumped on a broken leg, its heat shield half buried a little way
 * off, the orange-and-white parachute spread over the dust, and the data recorder still sealed inside. Everything that
 * lies on the ground follows the terrain; drifting dust has partly covered it.
 */
public class LanderWreckPiece extends StructurePiece {
    public static final int SIZE = 21;
    private final long seed;
    private final int baseY;

    public LanderWreckPiece(long seed, int x, int y, int z) {
        super(MarsStructures.LANDER_PIECE.get(), 0, new BoundingBox(x, y - 4, z, x + SIZE - 1, y + 9, z + SIZE - 1));
        this.seed = seed;
        this.baseY = y;
    }

    public LanderWreckPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.LANDER_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.baseY = tag.getInt("BaseY");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putInt("BaseY", baseY);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private int x0() {
        return boundingBox.minX();
    }

    private int z0() {
        return boundingBox.minZ();
    }

    /** World position of the lander's centre at its base. */
    public BlockPos body() {
        return new BlockPos(x0() + 7, baseY, z0() + 7);
    }

    /** Sets a block at a world position if it is inside the chunk being generated. */
    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    /** The first air block above the ground at a column (world coordinates). */
    private int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockPos c = body();
        int cx = c.getX();
        int cz = c.getZ();
        // sit on the actual ground at the body (the terrain may differ a little from the structure's start height)
        int y = ground(level, cx, cz) - 1;
        BlockState white = s(Blocks.WHITE_CONCRETE);
        BlockState grey = s(Blocks.LIGHT_GRAY_CONCRETE);
        BlockState foil = s(Blocks.GOLD_BLOCK);
        // a shallow scuffed patch where it came down hard
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 17) {
                    int gy = ground(level, cx + dx, cz + dz);
                    set(level, box, cx + dx, gy - 1, cz + dz, (dx + dz) % 3 == 0 ? s(MarsModule.MARTIAN_SAND.get()) : s(MarsModule.MARTIAN_REGOLITH.get()));
                }
            }
        }
        // the body: a squat hexagon (3 layers), tilted down toward its broken leg
        for (int layer = 0; layer < 3; layer++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 3) {
                        continue;
                    }
                    int tilt = dx >= 1 ? 0 : 1; // the west side sits a block higher
                    boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2 || Math.abs(dx) + Math.abs(dz) == 3 || layer != 1;
                    BlockState st = shell ? (layer == 1 && (dx + dz) % 2 == 0 ? foil : layer == 2 ? grey : white) : s(Blocks.AIR);
                    set(level, box, cx + dx, y + layer + tilt, cz + dz, st);
                }
            }
        }
        // the data recorder inside, and a torn panel to reach it
        set(level, box, cx, y + 2, cz, s(Blocks.AIR));
        set(level, box, cx + 2, y + 1, cz, s(Blocks.AIR));
        set(level, box, cx + 2, y + 2, cz, s(Blocks.AIR));
        BlockPos chest = new BlockPos(cx, y + 1, cz);
        if (box.isInside(chest)) {
            level.setBlock(chest, s(Blocks.CHEST), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(MarsLoot.LANDER_DATA, seed);
            }
        }
        // legs: two still standing, one snapped off and lying in the dust
        for (int[] leg : new int[][]{{-3, -2}, {-3, 2}}) {
            int ly = ground(level, cx + leg[0], cz + leg[1]);
            for (int dy = ly; dy <= y + 2; dy++) {
                set(level, box, cx + leg[0], dy, cz + leg[1], s(Blocks.IRON_BARS));
            }
        }
        for (int i = 0; i < 3; i++) {
            set(level, box, cx + 4 + i, ground(level, cx + 4 + i, cz + 1), cz + 1, s(Blocks.CHAIN));
        }
        // mast and a bent high-gain antenna
        for (int dy = 4; dy <= 5; dy++) {
            set(level, box, cx - 1, y + dy, cz, s(Blocks.LIGHTNING_ROD));
        }
        set(level, box, cx - 1, y + 6, cz, s(Blocks.DAYLIGHT_DETECTOR));
        // the heat shield, half buried a little way north-east
        int hx = cx + 8;
        int hz = cz - 5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz <= 5) {
                    int gy = ground(level, hx + dx, hz + dz);
                    set(level, box, hx + dx, gy - (dx < 0 ? 1 : 0), hz + dz, dx * dx + dz * dz <= 1 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.TERRACOTTA));
                }
            }
        }
        // the parachute spread over the dust to the south-west: orange and white gores, partly buried by drift
        int px = cx - 4;
        int pz = cz + 7;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                double e = dx * dx / 18.0 + dz * dz / 7.0;
                if (e > 1) {
                    continue;
                }
                int gy = ground(level, px + dx, pz + dz);
                boolean buried = r.nextInt(6) == 0; // drifted over: the dust hides that gore
                double ang = Math.atan2(dz, dx);
                boolean orange = ((int) Math.floor((ang + Math.PI) / (Math.PI / 4))) % 2 == 0;
                if (!buried) {
                    set(level, box, px + dx, gy, pz + dz, orange ? s(Blocks.ORANGE_CARPET) : s(Blocks.WHITE_CARPET));
                }
            }
        }
        // shroud lines back to the body
        for (int i = 1; i <= 3; i++) {
            set(level, box, cx - 1 - i, ground(level, cx - 1 - i, cz + 2 + i), cz + 2 + i, s(Blocks.CHAIN));
        }
    }
}
