package com.pizzaparker07.spacebreaker.content.venus.dungeon;

import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.venus.entity.AcidRay;
import com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A derelict research aerostat drifting in the Venusian cloud deck: a long envelope of coated fabric (breathable air
 * is a lifting gas here), a composite gondola hanging beneath on cables, with a lab, an observation deck behind aerogel
 * windows, propeller pods and a docking walkway. Abandoned when its acid seals failed; Acid Rays now nest around it.
 */
public class AerostatPiece extends StructurePiece {
    static final int LEN = 27;
    static final int WID = 15;
    static final int HEIGHT = 24;
    private final long seed;

    public AerostatPiece(long seed, int x, int y, int z, Direction facing) {
        super(VenusStructures.AEROSTAT_PIECE.get(), 0, new BoundingBox(x, y, z, x + LEN - 1, y + HEIGHT - 1, z + WID - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public AerostatPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(VenusStructures.AEROSTAT_PIECE.get(), tag);
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
        BlockState fabric = s(VenusModule.ENVELOPE_FABRIC.get());
        BlockState seam = s(Blocks.LIGHT_GRAY_WOOL);
        // the envelope: a hollow ellipsoid, seams every few blocks, a few torn panels
        double cx = 13, cy = 16.5, cz = 7;
        double ax = 12.5, ay = 6, az = 6.5;
        for (int x = 0; x < LEN; x++) {
            for (int y = 9; y < HEIGHT; y++) {
                for (int z = 0; z < WID; z++) {
                    double e = sq((x - cx) / ax) + sq((y - cy) / ay) + sq((z - cz) / az);
                    double inner = sq((x - cx) / (ax - 1.1)) + sq((y - cy) / (ay - 1.1)) + sq((z - cz) / (az - 1.1));
                    if (e <= 1 && inner > 1) {
                        boolean torn = r.nextInt(40) == 0 && y > cy;
                        placeBlock(level, torn ? s(Blocks.AIR) : (x % 5 == 0 ? seam : fabric), x, y, z, box);
                    }
                }
            }
        }
        // tail fins
        for (int i = 0; i < 4; i++) {
            placeBlock(level, s(VenusModule.COMPOSITE_PANEL.get()), 0 + i, 16 + 6 - i, 7, box);
            placeBlock(level, s(VenusModule.COMPOSITE_PANEL.get()), 0 + i, 16 - 6 + i, 7, box);
            placeBlock(level, s(VenusModule.COMPOSITE_PANEL.get()), 0 + i, 16, 7 + 6 - i, box);
            placeBlock(level, s(VenusModule.COMPOSITE_PANEL.get()), 0 + i, 16, 7 - 6 + i, box);
        }
        // cables from the envelope to the gondola
        for (int[] c : new int[][]{{9, 5}, {9, 9}, {17, 5}, {17, 9}}) {
            for (int y = 8; y <= 11; y++) {
                placeBlock(level, s(Blocks.CHAIN), c[0], y, c[1], box);
            }
        }
        gondola(level, box, r);
        guards(level, box);
    }

    private static double sq(double v) {
        return v * v;
    }

    private void gondola(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState panel = s(VenusModule.COMPOSITE_PANEL.get());
        BlockState window = s(VenusModule.AEROGEL.get());
        int x0 = 7, x1 = 19, z0 = 4, z1 = 10, y0 = 2, y1 = 7;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    boolean shell = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1;
                    boolean win = shell && y == y0 + 2 && x != x0 && x != x1 && z != z0 && z != z1 && x % 2 == 1;
                    boolean winEnd = shell && (x == x0 || x == x1) && y >= y0 + 2 && y <= y0 + 3 && z > z0 + 1 && z < z1 - 1;
                    placeBlock(level, shell ? (win || winEnd ? window : panel) : s(Blocks.AIR), x, y, z, box);
                }
            }
        }
        // door to the docking walkway on the south side
        placeBlock(level, s(Blocks.AIR), 13, y0 + 1, z1, box);
        placeBlock(level, s(Blocks.AIR), 13, y0 + 2, z1, box);
        for (int z = z1 + 1; z <= z1 + 4; z++) {
            for (int x = 12; x <= 14; x++) {
                placeBlock(level, x == 13 ? panel : s(Blocks.IRON_BARS), x, x == 13 ? y0 : y0 + 1, z, box);
            }
            placeBlock(level, panel, 12, y0, z, box);
            placeBlock(level, panel, 14, y0, z, box);
        }
        // propeller pods
        for (int x : new int[]{6, 20}) {
            placeBlock(level, s(Blocks.IRON_BLOCK), x, y0 + 3, 7, box);
            placeBlock(level, s(Blocks.IRON_BARS), x + (x < 10 ? -1 : 1), y0 + 3, 7, box);
            placeBlock(level, s(Blocks.IRON_BARS), x + (x < 10 ? -1 : 1), y0 + 4, 7, box);
            placeBlock(level, s(Blocks.IRON_BARS), x + (x < 10 ? -1 : 1), y0 + 2, 7, box);
        }
        // the lab: benches, a sample rack, a lectern with the last log, lamps
        for (int x = 9; x <= 12; x++) {
            placeBlock(level, s(Blocks.SMOOTH_QUARTZ_SLAB), x, y0 + 1, z0 + 1, box);
        }
        placeBlock(level, s(Blocks.BREWING_STAND), 10, y0 + 2, z0 + 1, box);
        placeBlock(level, s(Blocks.LECTERN), 17, y0 + 1, z0 + 1, box);
        placeBlock(level, s(VenusModule.SULFUR_CRUST.get()), 15, y0 + 1, z0 + 1, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 10, y1 - 1, 7, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 16, y1 - 1, 7, box);
        createChest(level, box, RandomSource.create(seed + 1), 8, y0 + 1, z1 - 1, VenusLoot.AEROSTAT_LAB);
        createChest(level, box, RandomSource.create(seed + 2), 18, y0 + 1, z1 - 1, VenusLoot.AEROSTAT_STORES);
        // corrosion: holes eaten into the panels
        for (int i = 0; i < 10; i++) {
            int x = x0 + r.nextInt(x1 - x0 + 1);
            int z = r.nextBoolean() ? z0 : z1;
            int y = y0 + 1 + r.nextInt(4);
            if (getBlock(level, x, y, z, box).is(VenusModule.COMPOSITE_PANEL.get()) && !(x == 13 && z == z1)) {
                placeBlock(level, s(Blocks.AIR), x, y, z, box);
            }
        }
    }

    private void guards(WorldGenLevel level, BoundingBox box) {
        for (int[] g : new int[][]{{4, 4, 2}, {22, 5, 12}}) {
            BlockPos pos = getWorldPos(g[0], g[1], g[2]);
            if (!box.isInside(pos)) {
                continue;
            }
            AcidRay ray = VenusEntities.ACID_RAY.get().create(level.getLevel());
            if (ray != null) {
                ray.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
                ray.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                ray.setGuardian(true);
                level.addFreshEntityWithPassengers(ray);
            }
        }
    }
}
