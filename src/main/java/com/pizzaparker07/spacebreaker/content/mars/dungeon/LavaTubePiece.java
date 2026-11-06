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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.List;

/**
 * The tube itself: a wide, round-roofed tunnel lined with dark basalt, a glassy polished lining where the lava ran,
 * a flat floor of old lava with drifts of olivine sand and wind-blown dust, and a few collapsed skylights with rubble
 * cones under them. Several metres of rock overhead block most of the radiation: a ready-made shelter for a base.
 */
public class LavaTubePiece extends StructurePiece {
    public static final double HALF_WIDTH = 4.5;
    public static final double HALF_HEIGHT = 3.5;
    /** Every n-th node has a collapsed skylight. */
    static final int SKYLIGHT_EVERY = 14;
    /** Per node: x, y, z, surface y. */
    private final int[] nodes;
    private final long seed;

    public LavaTubePiece(long seed, List<int[]> path) {
        super(MarsStructures.LAVA_TUBE_PIECE.get(), 0, bounds(path));
        this.seed = seed;
        this.nodes = new int[path.size() * 4];
        for (int i = 0; i < path.size(); i++) {
            System.arraycopy(path.get(i), 0, nodes, i * 4, 4);
        }
    }

    public LavaTubePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.LAVA_TUBE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.nodes = tag.getIntArray("Nodes");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putIntArray("Nodes", nodes);
    }

    private static BoundingBox bounds(List<int[]> path) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        int r = (int) Math.ceil(HALF_WIDTH) + 3;
        for (int[] n : path) {
            x0 = Math.min(x0, n[0] - r);
            x1 = Math.max(x1, n[0] + r);
            y0 = Math.min(y0, n[1] - 6);
            y1 = Math.max(y1, n[3] + 2);
            z0 = Math.min(z0, n[2] - r);
            z1 = Math.max(z1, n[2] + r);
        }
        return new BoundingBox(x0, y0, z0, x1, y1, z1);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    public int nodeCount() {
        return nodes.length / 4;
    }

    /** Centre of node i (world coordinates). */
    public BlockPos node(int i) {
        return new BlockPos(nodes[i * 4], nodes[i * 4 + 1], nodes[i * 4 + 2]);
    }

    /** Floor height at node i: the first air block above the floor. */
    public int floor(int i) {
        return nodes[i * 4 + 1] - (int) Math.floor(HALF_HEIGHT) + 1;
    }

    public static boolean skylight(int i) {
        return i % SKYLIGHT_EVERY == 3;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int n = nodeCount();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState basalt = s(MarsModule.MARTIAN_BASALT.base().get());
        BlockState glaze = s(MarsModule.POLISHED_MARTIAN_BASALT.base().get());
        // pass 0: basalt shell, pass 1: glassy lining, pass 2: hollow
        double[] grow = {2.2, 1.0, 0};
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < n; i++) {
                int cx = nodes[i * 4];
                int cy = nodes[i * 4 + 1];
                int cz = nodes[i * 4 + 2];
                double hw = HALF_WIDTH + grow[pass];
                double hh = HALF_HEIGHT + grow[pass];
                int rw = (int) Math.ceil(hw);
                int rh = (int) Math.ceil(hh);
                for (int dx = -rw; dx <= rw; dx++) {
                    for (int dz = -rw; dz <= rw; dz++) {
                        for (int dy = -rh; dy <= rh; dy++) {
                            // round roof, flat floor at the lower third
                            if (dy < -HALF_HEIGHT + 1 - grow[pass]) {
                                continue;
                            }
                            double e = (dx * dx + dz * dz) / (hw * hw) + (dy > 0 ? (double) dy * dy / (hh * hh) : 0);
                            if (e > 1) {
                                continue;
                            }
                            p.set(cx + dx, cy + dy, cz + dz);
                            if (!box.isInside(p)) {
                                continue;
                            }
                            BlockState here = level.getBlockState(p);
                            if (pass == 2) {
                                level.setBlock(p, s(Blocks.AIR), Block.UPDATE_CLIENTS);
                            } else if (!here.isAir()) {
                                level.setBlock(p, pass == 0 ? basalt : glaze, Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
            }
        }
        RandomSource r = RandomSource.create(seed);
        // the floor: dark old lava with drifts of olivine sand and dust that blew in through the skylights
        for (int i = 0; i < n; i++) {
            int fy = floor(i) - 1;
            int rw = (int) HALF_WIDTH;
            for (int dx = -rw; dx <= rw; dx++) {
                for (int dz = -rw; dz <= rw; dz++) {
                    p.set(nodes[i * 4] + dx, fy, nodes[i * 4 + 2] + dz);
                    if (!box.isInside(p) || !level.getBlockState(p.above()).isAir() || level.getBlockState(p).isAir()) {
                        continue;
                    }
                    int k = Math.floorMod(p.getX() * 31 + p.getZ() * 17 + (int) (seed & 7), 11);
                    BlockState f = k == 0 ? s(MarsModule.OLIVINE_SAND.get()) : k == 1 ? s(MarsModule.MARTIAN_REGOLITH.get()) : basalt;
                    level.setBlock(p, f, Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (skylight(i)) {
                skylight(level, box, r, i);
            }
        }
    }

    /** A collapsed patch of roof: an open shaft to the surface and a cone of rubble and dust below it. */
    private void skylight(WorldGenLevel level, BoundingBox box, RandomSource r, int i) {
        int cx = nodes[i * 4];
        int cy = nodes[i * 4 + 1];
        int cz = nodes[i * 4 + 2];
        int top = nodes[i * 4 + 3];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = cy; y <= top + 2; y++) {
            double rad = 2.4 + 0.15 * (y - cy);
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz > rad * rad) {
                        continue;
                    }
                    p.set(cx + dx, y, cz + dz);
                    if (box.isInside(p)) {
                        level.setBlock(p, s(Blocks.AIR), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        int fy = floor(i);
        for (int layer = 0; layer < 3; layer++) {
            int rad = 3 - layer;
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz > rad * rad + 1) {
                        continue;
                    }
                    p.set(cx + dx, fy + layer, cz + dz);
                    if (box.isInside(p) && level.getBlockState(p).isAir()) {
                        level.setBlock(p, (dx + dz + layer) % 3 == 0 ? s(MarsModule.MARTIAN_BASALT.base().get()) : s(MarsModule.MARTIAN_REGOLITH.get()),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
