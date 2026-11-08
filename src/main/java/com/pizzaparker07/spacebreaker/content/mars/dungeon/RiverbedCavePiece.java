package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.entity.CanyonLurker;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
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

import java.util.ArrayList;
import java.util.List;

/**
 * An ancient buried riverbed: billions of years ago a river cut this channel; later it was buried under sediment
 * and wind erosion reopened it as a cave. Its walls show the river's layered mudstones and sandstones, its floor is
 * clay, gypsum veins mark where water once seeped, and old ice hides in the cold pockets. At the far end lies a wide
 * pool chamber with strange layered mounds and the crashed remains of a lost exploration probe. Canyon Lurkers lie in
 * wait among the rocks.
 * <p>
 * The path is a seeded walk computed once; each chunk carves its part (lining, then air) from the saved nodes.
 */
public class RiverbedCavePiece extends StructurePiece {
    /** Per node: x, y, z, half width ×10, half height ×10. */
    private final int[] nodes;
    private final int surface;
    private final long seed;

    public RiverbedCavePiece(long seed, int x, int surface, int z) {
        this(seed, walk(seed, x, surface, z), surface);
    }

    private RiverbedCavePiece(long seed, List<int[]> path, int surface) {
        super(MarsStructures.RIVERBED_PIECE.get(), 0, bounds(path, surface));
        this.seed = seed;
        this.surface = surface;
        this.nodes = new int[path.size() * 5];
        for (int i = 0; i < path.size(); i++) {
            System.arraycopy(path.get(i), 0, nodes, i * 5, 5);
        }
    }

    public RiverbedCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.RIVERBED_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.surface = tag.getInt("Surface");
        this.nodes = tag.getIntArray("Nodes");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putInt("Surface", surface);
        tag.putIntArray("Nodes", nodes);
    }

    static final int STEPS = 46;

    /** The channel: starts under a sinkhole, meanders with a gentle downstream slope, ends in a wide pool chamber. */
    static List<int[]> walk(long seed, int x, int surface, int z) {
        RandomSource r = RandomSource.create(seed);
        List<int[]> path = new ArrayList<>();
        double a = r.nextDouble() * Mth.TWO_PI;
        double px = x;
        double pz = z;
        double py = surface - 16;
        double turn = 0;
        for (int i = 0; i < STEPS; i++) {
            boolean last = i == STEPS - 1;
            int hw = last ? 75 : 30 + r.nextInt(16);
            int hh = last ? 50 : 22 + r.nextInt(8);
            path.add(new int[]{(int) Math.round(px), (int) Math.round(py), (int) Math.round(pz), hw, hh});
            turn = turn * 0.8 + (r.nextDouble() - 0.5) * 0.5;
            a += turn;
            px += Math.cos(a) * 2.6;
            pz += Math.sin(a) * 2.6;
            py -= 0.12;
        }
        return path;
    }

    private static BoundingBox bounds(List<int[]> path, int surface) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (int[] n : path) {
            int rh = n[3] / 10 + 3;
            int rv = n[4] / 10 + 3;
            x0 = Math.min(x0, n[0] - rh);
            x1 = Math.max(x1, n[0] + rh);
            y0 = Math.min(y0, n[1] - rv);
            y1 = Math.max(y1, n[1] + rv);
            z0 = Math.min(z0, n[2] - rh);
            z1 = Math.max(z1, n[2] + rh);
        }
        y1 = Math.max(y1, surface + 3);
        return new BoundingBox(x0, y0, z0, x1, y1, z1);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Sediment layer at height y: alternating mudstone and sandstone beds. */
    private static BlockState bed(int y, int x, int z) {
        int k = Math.floorMod(y + ((x * 3 + z * 5) >> 4 & 1), 5);
        return k < 2 ? s(MarsModule.MARTIAN_MUDSTONE.get()) : s(MarsModule.MARTIAN_SANDSTONE.base().get());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int n = nodes.length / 5;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // pass 1: line the channel with layered sediment, pass 2: carve, pass 3: floor and details
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                int cx = nodes[i * 5];
                int cy = nodes[i * 5 + 1];
                int cz = nodes[i * 5 + 2];
                double hw = nodes[i * 5 + 3] / 10.0 + (pass == 0 ? 1.5 : 0);
                double hh = nodes[i * 5 + 4] / 10.0 + (pass == 0 ? 1.5 : 0);
                for (int dx = (int) -Math.ceil(hw); dx <= Math.ceil(hw); dx++) {
                    for (int dz = (int) -Math.ceil(hw); dz <= Math.ceil(hw); dz++) {
                        for (int dy = (int) -Math.ceil(hh); dy <= Math.ceil(hh); dy++) {
                            double e = (dx * dx + dz * dz) / (hw * hw) + (dy > 0 ? dy * dy / (hh * hh) : dy * dy / (hh * hh * 0.45));
                            if (e > 1) {
                                continue;
                            }
                            p.set(cx + dx, cy + dy, cz + dz);
                            if (!box.isInside(p)) {
                                continue;
                            }
                            if (pass == 0) {
                                BlockState here = level.getBlockState(p);
                                if (!here.isAir()) {
                                    level.setBlock(p, bed(p.getY(), p.getX(), p.getZ()), Block.UPDATE_CLIENTS);
                                }
                            } else {
                                level.setBlock(p, s(Blocks.AIR), Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
            }
        }
        RandomSource r = RandomSource.create(seed);
        // clay floor along the old stream bed; gypsum veins and ice pockets in the walls
        for (int i = 0; i < n; i++) {
            int cx = nodes[i * 5];
            int cy = nodes[i * 5 + 1];
            int cz = nodes[i * 5 + 2];
            int hw = nodes[i * 5 + 3] / 10;
            int floorY = cy - (int) Math.floor(nodes[i * 5 + 4] / 10.0 * Math.sqrt(0.45));
            for (int dx = -hw; dx <= hw; dx++) {
                for (int dz = -hw; dz <= hw; dz++) {
                    p.set(cx + dx, floorY, cz + dz);
                    if (box.isInside(p) && level.getBlockState(p.above()).isAir() && !level.getBlockState(p).isAir()) {
                        level.setBlock(p, (dx * dx + dz * dz) % 7 == 0 ? s(MarsModule.HEMATITE_SOIL.get()) : s(MarsModule.MARTIAN_CLAY.get()),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (r.nextInt(3) == 0) {
                vein(level, box, r, cx + r.nextInt(7) - 3, cy + 1 + r.nextInt(2), cz + r.nextInt(7) - 3, s(MarsModule.GYPSUM_ORE.get()));
            }
            if (r.nextInt(6) == 0) {
                vein(level, box, r, cx + r.nextInt(9) - 4, cy + r.nextInt(3), cz + r.nextInt(9) - 4, s(MarsModule.MARTIAN_ICE.get()));
            }
        }
        sinkhole(level, box);
        poolChamber(level, box, r);
        // ambushers
        int[] mids = {n / 3, (2 * n) / 3};
        for (int m : mids) {
            lurker(level, box, nodes[m * 5], nodes[m * 5 + 1], nodes[m * 5 + 2]);
        }
    }

    /** A thin vein following the wall: replaces only sediment blocks. */
    private static void vein(WorldGenLevel level, BoundingBox box, RandomSource r, int x, int y, int z, BlockState ore) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < 6; i++) {
            if (box.isInside(p)) {
                BlockState here = level.getBlockState(p);
                if (here.is(MarsModule.MARTIAN_MUDSTONE.get()) || here.is(MarsModule.MARTIAN_SANDSTONE.base().get())) {
                    level.setBlock(p, ore, Block.UPDATE_CLIENTS);
                }
            }
            p.move(r.nextInt(3) - 1, r.nextInt(2) == 0 ? 0 : (r.nextBoolean() ? 1 : -1), r.nextInt(3) - 1);
        }
    }

    /** The way in: a collapsed sinkhole from the surface down to the first node, with a rubble slope. */
    private void sinkhole(WorldGenLevel level, BoundingBox box) {
        int cx = nodes[0];
        int cy = nodes[1];
        int cz = nodes[2];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = cy; y <= surface + 2; y++) {
            double t = (double) (y - cy) / Math.max(1, surface + 2 - cy);
            double rad = 2.2 + 2.6 * t;
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
        // a rubble ramp to climb out
        for (int i = 0; i <= surface - cy; i++) {
            for (int w = -1; w <= 1; w++) {
                p.set(cx + w, cy - 1 + i, cz - 2 + i / 3);
                if (box.isInside(p) && level.getBlockState(p).isAir()) {
                    level.setBlock(p, i % 2 == 0 ? s(MarsModule.MARTIAN_REGOLITH.get()) : s(MarsModule.MARTIAN_SAND.get()), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** The end chamber: layered sediment mounds and the probe wreck with its data vault. */
    private void poolChamber(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int i = nodes.length / 5 - 1;
        int cx = nodes[i * 5];
        int cy = nodes[i * 5 + 1];
        int cz = nodes[i * 5 + 2];
        int floor = cy - (int) Math.floor(nodes[i * 5 + 4] / 10.0 * Math.sqrt(0.45)) + 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // dome-shaped layered mounds, banded mudstone and clay (left by mats of... something?)
        int[][] mounds = {{-4, -3}, {3, -4}, {-5, 3}, {5, 2}};
        for (int[] m : mounds) {
            for (int dy = 0; dy < 3; dy++) {
                int rad = 2 - dy / 2;
                for (int dx = -rad; dx <= rad; dx++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        if (dx * dx + dz * dz > rad * rad + 1) {
                            continue;
                        }
                        p.set(cx + m[0] + dx, floor + dy, cz + m[1] + dz);
                        if (box.isInside(p)) {
                            level.setBlock(p, dy % 2 == 0 ? s(MarsModule.MARTIAN_MUDSTONE.get()) : s(MarsModule.MARTIAN_CLAY.get()), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        // frozen pool in the lowest part
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                p.set(cx + dx, floor - 1, cz + dz);
                if (box.isInside(p)) {
                    level.setBlock(p, s(MarsModule.MARTIAN_ICE.get()), Block.UPDATE_CLIENTS);
                }
            }
        }
        // the wreck: a crumpled lander body, a torn heat shield and the data vault
        int wx = cx + 1;
        int wz = cz + 5;
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                p.set(wx + dx, floor + dy, wz);
                if (box.isInside(p)) {
                    level.setBlock(p, dy == 1 && dx == 1 ? s(com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.GOLD_FOIL_CLADDING.get()) : s(Blocks.WHITE_CONCRETE), Block.UPDATE_CLIENTS);
                }
            }
        }
        p.set(wx - 1, floor, wz + 1);
        if (box.isInside(p)) {
            level.setBlock(p, s(Blocks.IRON_TRAPDOOR), Block.UPDATE_CLIENTS);
        }
        p.set(wx + 3, floor, wz);
        if (box.isInside(p)) {
            level.setBlock(p, s(Blocks.LIGHTNING_ROD), Block.UPDATE_CLIENTS);
        }
        chest(level, box, cx - 1, floor, cz + 5);
        chest(level, box, cx + 2, floor, cz - 5);
        // the survey team's lights, still on their tripods round the chamber, and the cable between them
        for (int[] l : new int[][]{{-4, -3}, {4, 2}, {-2, 6}}) {
            int lx = cx + l[0];
            int lz = cz + l[1];
            p.set(lx, floor, lz);
            if (box.isInside(p) && level.getBlockState(p).isAir()) {
                level.setBlock(p, s(Blocks.IRON_BARS), Block.UPDATE_CLIENTS);
                p.set(lx, floor + 1, lz);
                level.setBlock(p, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get())
                        .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.UP), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The probe's data vault: a supply locker, its doors to the open chamber. */
    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        level.setBlock(pos, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity locker) {
            locker.setLootTable(MarsLoot.RIVERBED_PROBE, seed ^ (x * 341873128712L + z));
        }
    }

    private void lurker(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y - 1, z);
        if (!box.isInside(pos)) {
            return;
        }
        CanyonLurker mob = MarsEntities.CANYON_LURKER.get().create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }

    /** First node (entrance) in world coordinates, for tests and the showcase. */
    public BlockPos entrance() {
        return new BlockPos(nodes[0], nodes[1], nodes[2]);
    }

    public BlockPos chamber() {
        int i = nodes.length / 5 - 1;
        return new BlockPos(nodes[i * 5], nodes[i * 5 + 1], nodes[i * 5 + 2]);
    }
}
