package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The anchored lander on the floor of Stickney, Phobos' giant crater. Gravity here is about a two-thousandth of
 * Earth's: a lander that simply touches down bounces straight back off. This one fired three harpoons into the
 * regolith and winched itself down; the tethers still hold it, one leg folded under the tilted body. Its survey
 * recorder is inside, and a plate on the ground explains why it had to be tied down.
 */
public class StickneyLanderPiece extends StructurePiece {
    public static final int SIZE = 17;
    private final long seed;

    public StickneyLanderPiece(long seed, int x, int y, int z) {
        super(MarsStructures.STICKNEY_PIECE.get(), 0, new BoundingBox(x, y - 6, z, x + SIZE - 1, y + 10, z + SIZE - 1));
        this.seed = seed;
    }

    public StickneyLanderPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.STICKNEY_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** World position of the body's centre column. */
    public BlockPos centre() {
        return new BlockPos(boundingBox.minX() + SIZE / 2, boundingBox.minY() + 6, boundingBox.minZ() + SIZE / 2);
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    private static int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos c = centre();
        int cx = c.getX();
        int cz = c.getZ();
        int gy = ground(level, cx, cz);
        int y = gy + 1; // the body hangs a block above the dust on its legs
        BlockState hull = s(Blocks.WHITE_CONCRETE);
        BlockState trim = s(Blocks.LIGHT_GRAY_CONCRETE);
        BlockState foil = s(Blocks.GOLD_BLOCK);
        // the body: a 5x5 box, three layers, with gold foil on the middle band and a solar deck on top
        for (int layer = 0; layer < 3; layer++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    if (corner) {
                        continue;
                    }
                    boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    int tilt = dx == 2 ? 1 : 0; // the east side rides a block high on its stretched tether
                    BlockState st;
                    if (layer == 0) {
                        st = trim;
                    } else if (layer == 2) {
                        st = edge ? trim : s(Blocks.DAYLIGHT_DETECTOR);
                    } else {
                        st = edge ? ((dx + dz) % 2 == 0 ? foil : hull) : s(Blocks.AIR);
                    }
                    set(level, box, cx + dx, y + layer + tilt, cz + dz, st);
                }
            }
        }
        // the survey recorder inside, reached through the open hatch on the south side
        BlockPos chest = new BlockPos(cx, y + 1, cz);
        if (box.isInside(chest)) {
            level.setBlock(chest, s(Blocks.CHEST), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(MarsLoot.STICKNEY_LANDER, seed);
            }
        }
        set(level, box, cx, y + 1, cz + 2, s(Blocks.AIR));
        // four legs; the south-west one folded under the body
        for (int[] leg : new int[][]{{-2, -2}, {2, -2}, {2, 2}}) {
            int lx = cx + leg[0];
            int lz = cz + leg[1];
            for (int dy = ground(level, lx, lz); dy < y + (leg[0] == 2 ? 1 : 0); dy++) {
                set(level, box, lx, dy, lz, s(Blocks.IRON_BARS));
            }
        }
        set(level, box, cx - 3, ground(level, cx - 3, cz + 3), cz + 3, s(Blocks.IRON_BARS));
        // high-gain antenna
        set(level, box, cx - 1, y + 3, cz - 1, s(Blocks.LIGHTNING_ROD));
        set(level, box, cx - 1, y + 4, cz - 1, s(Blocks.DAYLIGHT_DETECTOR));
        // three harpoon tethers: chains slanting out from the base to anchors driven into the regolith
        int[][] anchors = {{6, 0}, {-3, -6}, {-3, 6}};
        for (int[] a : anchors) {
            int ax = cx + a[0];
            int az = cz + a[1];
            int ay = ground(level, ax, az) - 1;
            int steps = Math.max(Math.abs(a[0]), Math.abs(a[1]));
            for (int i = 1; i < steps; i++) {
                int px = cx + Math.round(a[0] * (float) i / steps);
                int pz = cz + Math.round(a[1] * (float) i / steps);
                int py = Math.round(y + (ay + 1 - y) * (float) i / steps);
                Direction.Axis axis = Math.abs(a[0]) >= Math.abs(a[1]) ? Direction.Axis.X : Direction.Axis.Z;
                set(level, box, px, Math.max(py, ground(level, px, pz)), pz, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, axis));
            }
            // the harpoon: a barbed spike buried in the dust, its winch ring showing
            set(level, box, ax, ay, az, s(Blocks.IRON_BLOCK));
            set(level, box, ax, ay - 1, az, s(Blocks.POINTED_DRIPSTONE));
            set(level, box, ax, ay + 1, az, s(Blocks.CHAIN));
        }
        // the plate that explains it, facing the hatch
        BlockPos sign = new BlockPos(cx, ground(level, cx, cz + 4), cz + 4);
        if (box.isInside(sign)) {
            level.setBlock(sign, s(Blocks.OAK_SIGN).setValue(StandingSignBlock.ROTATION, 0), Block.UPDATE_CLIENTS);
            DungeonBits.signText(level, sign, "stickney_lander");
        }
        // bounce marks: the shallow dents of the first touchdown before the harpoons fired
        RandomSource r = RandomSource.create(seed);
        for (int i = 0; i < 3; i++) {
            int bx = cx - 5 - 2 * i + r.nextInt(2);
            int bz = cz - 2 + r.nextInt(5);
            set(level, box, bx, ground(level, bx, bz) - 1, bz, s(Blocks.AIR));
        }
    }
}
