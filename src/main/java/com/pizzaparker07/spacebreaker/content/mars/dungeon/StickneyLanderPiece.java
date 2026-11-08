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
    public static final int SIZE = 21;
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
        BlockState foil = s(com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.GOLD_FOIL_CLADDING.get());
        // the body: an octagonal bus seven blocks across, wrapped in gold foil (white only on the cut corners), under a
        // slab deck; it lists a little: the east side rides a block high on its stretched tether
        for (int layer = 0; layer < 4; layer++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ax = Math.abs(dx);
                    int az = Math.abs(dz);
                    if (ax + az > 4) {
                        continue;
                    }
                    boolean edge = ax == 3 || az == 3 || ax + az == 4;
                    boolean corner = ax == 2 && az == 2; // only the cut diagonals are white
                    int tilt = dx >= 2 ? 1 : 0;
                    BlockState st;
                    if (layer == 0) {
                        st = trim;
                    } else if (layer == 3) {
                        st = s(Blocks.SMOOTH_STONE_SLAB);
                    } else {
                        st = edge ? (corner ? hull : foil) : s(Blocks.AIR);
                    }
                    set(level, box, cx + dx, y + layer + tilt, cz + dz, st);
                }
            }
        }
        // the survey recorder inside, reached through the open hatch on the south side
        BlockPos rec = new BlockPos(cx, y + 1, cz);
        if (box.isInside(rec)) {
            level.setBlock(rec, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                    .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(rec) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity be) {
                be.setLootTable(MarsLoot.STICKNEY_LANDER, seed);
            }
        }
        set(level, box, cx, y + 1, cz + 3, s(Blocks.AIR));
        set(level, box, cx, y + 2, cz + 3, s(Blocks.AIR));
        // four legs splayed well out: a strut down from each corner of the body to a footpad
        for (int[] leg : new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            int top = y + (leg[0] == 1 ? 1 : 0);
            for (int k = 0; k <= 2; k++) {
                int sx = cx + leg[0] * (3 + k);
                int sz = cz + leg[1] * (3 + k);
                int sy = top - k;
                set(level, box, sx, Math.max(sy, ground(level, sx, sz)), sz, s(Blocks.ANDESITE_WALL));
            }
            int fx = cx + leg[0] * 6;
            int fz = cz + leg[1] * 6;
            set(level, box, fx, ground(level, fx, fz), fz, s(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE));
        }
        // one small solar wing folded out to the north
        set(level, box, cx, y + 2, cz - 4, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        set(level, box, cx, y + 2, cz - 5, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get()));
        set(level, box, cx, y + 2, cz - 6, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get()));
        // the high-gain dish on the deck: three blocks across, turned to the sky
        int dy = y + 4;
        set(level, box, cx - 1, dy, cz, s(Blocks.POLISHED_ANDESITE));
        set(level, box, cx - 1, dy + 1, cz, s(Blocks.DAYLIGHT_DETECTOR));
        for (int ddx = -1; ddx <= 1; ddx++) {
            for (int ddz = -1; ddz <= 1; ddz++) {
                if (ddx != 0 || ddz != 0) {
                    set(level, box, cx - 1 + ddx, dy + 1, cz + ddz, s(Blocks.SMOOTH_QUARTZ_SLAB));
                }
            }
        }
        set(level, box, cx - 1, dy + 2, cz, s(Blocks.LIGHTNING_ROD));
        // three harpoon tethers: chains slanting out from the base to anchors driven into the regolith
        int[][] anchors = {{9, 0}, {-5, -8}, {-5, 8}};
        for (int[] a : anchors) {
            int ax = cx + a[0];
            int az = cz + a[1];
            int ay = ground(level, ax, az) - 1;
            int steps = Math.max(Math.abs(a[0]), Math.abs(a[1]));
            for (int i = 1; i < steps; i++) {
                int px = cx + Math.round(a[0] * (float) i / steps);
                int pz = cz + Math.round(a[1] * (float) i / steps);
                if (Math.abs(px - cx) + Math.abs(pz - cz) <= 4 && Math.abs(px - cx) <= 3 && Math.abs(pz - cz) <= 3) {
                    continue; // still under the body
                }
                int py = Math.round(y + (ay + 1 - y) * (float) i / steps);
                Direction.Axis axis = Math.abs(a[0]) >= Math.abs(a[1]) ? Direction.Axis.X : Direction.Axis.Z;
                int ty = Math.max(py, ground(level, px, pz));
                set(level, box, px, ty, pz, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, axis));
                // the tether is a braided pair: a second strand beside the first
                int ox = axis == Direction.Axis.X ? 0 : 1;
                int oz = axis == Direction.Axis.X ? 1 : 0;
                set(level, box, px + ox, Math.max(ty, ground(level, px + ox, pz + oz)), pz + oz, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, axis));
            }
            // the harpoon: a barbed spike buried in the dust, its winch ring showing
            set(level, box, ax, ay, az, s(Blocks.IRON_BLOCK));
            set(level, box, ax, ay - 1, az, s(Blocks.POINTED_DRIPSTONE));
            set(level, box, ax, ay + 1, az, s(Blocks.POLISHED_ANDESITE)); // the winch housing on the harpoon head
            set(level, box, ax, ay + 2, az, s(Blocks.LIGHTNING_ROD));
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
