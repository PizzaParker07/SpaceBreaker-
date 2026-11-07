package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * An ancient lava tube: a long, winding tunnel left by a drained lava river, a few blocks under the mare.
 * Entered through a skylight where its roof collapsed. Deep inside lies the wreck of a robotic probe that
 * crashed through another skylight long ago; an Elder Regolith Tick nest guards it.
 * <p>
 * The path is a seeded random walk computed once when the structure starts; every chunk then carves its part
 * of the tunnel (lining first, then air) from the saved nodes, so the result is seamless.
 */
public class LavaTubePiece extends StructurePiece {
    /** x, y, z, horizontal radius ×10, vertical radius ×10 per node. */
    private final int[] nodes;
    private final int shaftTop;
    private final long seed;

    public LavaTubePiece(long seed, List<int[]> path, int shaftTop) {
        super(MoonStructures.LAVA_TUBE_PIECE.get(), 0, bounds(path, shaftTop));
        this.seed = seed;
        this.shaftTop = shaftTop;
        this.nodes = new int[path.size() * 5];
        for (int i = 0; i < path.size(); i++) {
            System.arraycopy(path.get(i), 0, nodes, i * 5, 5);
        }
    }

    public LavaTubePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MoonStructures.LAVA_TUBE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.shaftTop = tag.getInt("ShaftTop");
        this.nodes = tag.getIntArray("Nodes");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putInt("ShaftTop", shaftTop);
        tag.putIntArray("Nodes", nodes);
    }

    private static BoundingBox bounds(List<int[]> path, int shaftTop) {
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
        y1 = Math.max(y1, shaftTop + 2);
        return new BoundingBox(x0, y0, z0, x1, y1, z1);
    }

    /**
     * Builds the tunnel path: starts below the skylight at (x, z) and winds away with gentle turns,
     * staying 9-16 blocks under the local surface.
     */
    public static List<int[]> walk(RandomSource r, int x, int z, java.util.function.IntBinaryOperator surface) {
        List<int[]> path = new ArrayList<>();
        double heading = r.nextDouble() * Mth.TWO_PI;
        double px = x;
        double pz = z;
        int count = 22 + r.nextInt(10);
        double y = surface.applyAsInt(x, z) - 12;
        for (int i = 0; i < count; i++) {
            int top = surface.applyAsInt((int) px, (int) pz);
            double target = top - 11 - 3 * Math.sin(i * 0.4);
            y = Mth.clamp(Mth.lerp(0.35, y, target), top - 16, top - 9);
            boolean chamber = i == count * 2 / 3 || i == count - 1;
            double rh = chamber ? 6.5 : 3.6 + r.nextDouble() * 1.6;
            double rv = chamber ? 4.2 : 2.7 + r.nextDouble() * 0.9;
            path.add(new int[]{(int) px, (int) Math.round(y), (int) pz, (int) (rh * 10), (int) (rv * 10)});
            heading += (r.nextDouble() - 0.5) * 0.8;
            px += Math.cos(heading) * 5;
            pz += Math.sin(heading) * 5;
        }
        return path;
    }

    private int nodeCount() {
        return nodes.length / 5;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState lining = MoonModule.MARE_BASALT.get().defaultBlockState();
        BlockState ore = MoonModule.ILMENITE_ORE.get().defaultBlockState();
        BlockState floor = MoonModule.VOLCANIC_GLASS_SOIL.get().defaultBlockState();
        BlockState rubble = MoonModule.IMPACT_BRECCIA.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // two passes over the samples: solid lining first, then the hollow, so later samples never refill earlier air
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < nodeCount() - 1; i++) {
                int a = i * 5;
                int b = a + 5;
                double len = Math.sqrt(sq(nodes[b] - nodes[a]) + sq(nodes[b + 1] - nodes[a + 1]) + sq(nodes[b + 2] - nodes[a + 2]));
                int steps = Math.max(1, (int) Math.ceil(len));
                for (int s = 0; s <= steps; s++) {
                    double t = (double) s / steps;
                    double cx = Mth.lerp(t, nodes[a], nodes[b]);
                    double cy = Mth.lerp(t, nodes[a + 1], nodes[b + 1]);
                    double cz = Mth.lerp(t, nodes[a + 2], nodes[b + 2]);
                    double rh = Mth.lerp(t, nodes[a + 3], nodes[b + 3]) / 10.0;
                    double rv = Mth.lerp(t, nodes[a + 4], nodes[b + 4]) / 10.0;
                    carve(level, box, p, cx, cy, cz, rh, rv, pass, lining, ore, floor);
                }
            }
        }
        skylight(level, box, p, rubble, floor);
        RandomSource r = RandomSource.create(seed);
        // the probe wreck in the side chamber, the nest at the far end
        int wreck = (nodeCount() * 2 / 3) * 5;
        probe(level, box, r, nodes[wreck], nodes[wreck + 1], nodes[wreck + 2]);
        int nest = (nodeCount() - 1) * 5;
        nest(level, box, nodes[nest], nodes[nest + 1], nodes[nest + 2]);
    }

    private static double sq(double v) {
        return v * v;
    }

    private void carve(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos p, double cx, double cy, double cz, double rh,
                       double rv, int pass, BlockState lining, BlockState ore, BlockState floor) {
        int ih = (int) Math.ceil(rh * 1.3);
        int iv = (int) Math.ceil(rv * 1.3);
        for (int x = (int) Math.floor(cx) - ih; x <= (int) Math.floor(cx) + ih; x++) {
            for (int z = (int) Math.floor(cz) - ih; z <= (int) Math.floor(cz) + ih; z++) {
                if (!box.isInside(x, box.minY(), z)) {
                    continue;
                }
                for (int y = (int) Math.floor(cy) - iv; y <= (int) Math.floor(cy) + iv; y++) {
                    double dx = (x + 0.5 - cx) / rh;
                    double dz = (z + 0.5 - cz) / rh;
                    double dy = (y + 0.5 - cy) / rv;
                    // flattened floor: lava tubes have a level floor of solidified lava
                    if (dy < -0.62) {
                        dy = -0.62 + (dy + 0.62) * 2.5;
                    }
                    double d = dx * dx + dy * dy + dz * dz;
                    p.set(x, y, z);
                    if (pass == 0) {
                        if (d >= 1 && d < 1.55 && !level.getBlockState(p).isAir()) {
                            long h = mixPos(x, y, z);
                            level.setBlock(p, (h & 31) == 0 ? ore : lining, 2);
                        }
                    } else if (d < 1) {
                        BlockState below = level.getBlockState(p.below());
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        if (dy < -0.5 && !below.isAir() && (mixPos(x, y, z) & 3) != 0) {
                            level.setBlock(p.move(0, -1, 0), floor, 2);
                        }
                    }
                }
            }
        }
    }

    /** The collapsed roof at the entrance: a round shaft to the surface with a rubble cone below it. */
    private void skylight(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos p, BlockState rubble, BlockState floor) {
        int sx = nodes[0];
        int sy = nodes[1];
        int sz = nodes[2];
        for (int x = sx - 5; x <= sx + 5; x++) {
            for (int z = sz - 5; z <= sz + 5; z++) {
                if (!box.isInside(x, box.minY(), z)) {
                    continue;
                }
                double d = Math.sqrt(sq(x - sx) + sq(z - sz));
                double r = 3.6 + 0.6 * Math.sin((x * 3 + z * 7) * 0.7);
                if (d > r) {
                    continue;
                }
                for (int y = sy; y <= shaftTop + 2; y++) {
                    p.set(x, y, z);
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                // rubble cone from the fallen roof
                int cone = (int) Math.round((r - d) * 0.9);
                for (int y = sy - 3; y < sy - 3 + cone; y++) {
                    p.set(x, y, z);
                    level.setBlock(p, (mixPos(x, y, z) & 1) == 0 ? rubble : floor, 2);
                }
            }
        }
    }

    private void probe(WorldGenLevel level, BoundingBox box, RandomSource r, int x, int y, int z) {
        int fy = floorY(level, box, x, y, z);
        if (fy == Integer.MIN_VALUE) {
            return;
        }
        // crumpled bus, foil, a solar wing and a still-glowing beacon
        place(level, box, x, fy, z, Blocks.IRON_BLOCK.defaultBlockState());
        place(level, box, x + 1, fy, z, Blocks.WEATHERED_COPPER.defaultBlockState());
        place(level, box, x, fy + 1, z, Blocks.EXPOSED_CUT_COPPER.defaultBlockState());
        place(level, box, x - 1, fy, z + 1, Blocks.YELLOW_TERRACOTTA.defaultBlockState());
        place(level, box, x + 1, fy + 1, z, Blocks.SEA_LANTERN.defaultBlockState());
        place(level, box, x - 2, fy, z - 1, MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        place(level, box, x + 2, fy, z + 1, Blocks.IRON_BARS.defaultBlockState());
        place(level, box, x, fy, z - 2, Blocks.CHAIN.defaultBlockState());
        BlockPos chest = new BlockPos(x - 1, fy, z - 1);
        if (box.isInside(chest)) {
            // the probe's store: the shared supply locker
            level.setBlock(chest, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                    .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, net.minecraft.core.Direction.SOUTH), 2);
            if (level.getBlockEntity(chest) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity c) {
                c.setLootTable(MoonLoot.LAVA_TUBE_PROBE, seed ^ 0x9E3779B9L);
            }
        }
        guard(level, box, MoonEntities.REGOLITH_TICK.get(), x + 3, fy, z - 2);
        guard(level, box, MoonEntities.REGOLITH_TICK.get(), x - 3, fy, z + 2);
        guard(level, box, MoonEntities.GLIMMER_BEETLE.get(), x + 1, fy, z + 3);
        guard(level, box, MoonEntities.GLIMMER_BEETLE.get(), x - 2, fy, z - 3);
    }

    private void nest(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        int fy = floorY(level, box, x, y, z);
        if (fy == Integer.MIN_VALUE) {
            return;
        }
        BlockPos sp = new BlockPos(x, fy, z);
        if (box.isInside(sp)) {
            level.setBlock(sp, Blocks.SPAWNER.defaultBlockState(), 2);
            if (level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(MoonEntities.REGOLITH_TICK.get(), RandomSource.create(seed + 3));
            }
        }
        // the nest: a ring of breccia and KREEP-rich rock the old ticks feed on
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    long h = mixPos(x + dx, fy, z + dz);
                    place(level, box, x + dx, fy - 1, z + dz, (h & 3) == 0 ? MoonModule.KREEP_ORE.get().defaultBlockState()
                            : MoonModule.IMPACT_BRECCIA.get().defaultBlockState());
                }
            }
        }
        guard(level, box, MoonEntities.ELDER_REGOLITH_TICK.get(), x + 2, fy, z);
        guard(level, box, MoonEntities.ELDER_REGOLITH_TICK.get(), x - 2, fy, z + 1);
    }

    /** First air block above the tube floor near (x, y, z), or MIN_VALUE when outside this chunk. */
    private int floorY(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        if (!box.isInside(x, box.minY(), z)) {
            return Integer.MIN_VALUE;
        }
        for (int yy = y; yy > y - 8; yy--) {
            if (level.getBlockState(new BlockPos(x, yy, z)).isAir() && !level.getBlockState(new BlockPos(x, yy - 1, z)).isAir()) {
                return yy;
            }
        }
        return y - 2;
    }

    private void place(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private void guard(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(x + 0.5, y, z + 0.5, 0F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }

    private long mixPos(int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 32);
    }
}
