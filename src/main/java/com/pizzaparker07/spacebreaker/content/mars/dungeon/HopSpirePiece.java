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
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The relay spire of Deimos: a tall rock needle with an old communications relay on top and a ring of rock stacks
 * spiralling up around it. Each stack stands {@link #RISE} blocks higher and several blocks farther than the last:
 * an impossible staircase on Earth, an easy one in Deimos' gravity, where a jump lasts for many seconds. A plate at
 * the foot explains; the relay's data cache waits at the top.
 */
public class HopSpirePiece extends StructurePiece {
    public static final int SIZE = 27;
    /** Height gained per stack: more than an Earth jump (1.25), less than a Deimos jump in any realism setting. */
    public static final int RISE = 2;
    public static final int STACKS = 12;
    /** Distance of the stacks from the spire's axis. */
    public static final int RADIUS = 12;
    private static final int C = SIZE / 2;
    private final long seed;
    private final int baseY;

    public HopSpirePiece(long seed, int x, int y, int z) {
        super(MarsStructures.HOP_SPIRE_PIECE.get(), 0, new BoundingBox(x, y - 6, z, x + SIZE - 1, y + RISE * STACKS + 12, z + SIZE - 1));
        this.seed = seed;
        this.baseY = y;
    }

    public HopSpirePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.HOP_SPIRE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
        this.baseY = tag.getInt("BaseY");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putInt("BaseY", baseY);
    }

    /** Top standing block of each stack, lowest first (world positions; the stack is 2x2 from this corner). */
    public List<BlockPos> stacks() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < STACKS; i++) {
            double a = Math.toRadians(i * 360.0 / STACKS);
            int x = boundingBox.minX() + C + (int) Math.round(Math.cos(a) * RADIUS) - (Math.cos(a) < -0.1 ? 1 : 0);
            int z = boundingBox.minZ() + C + (int) Math.round(Math.sin(a) * RADIUS) - (Math.sin(a) < -0.1 ? 1 : 0);
            out.add(new BlockPos(x, baseY + RISE * (i + 1), z));
        }
        return out;
    }

    /** Top block of the spire (its 3x3 summit is centred here). */
    public BlockPos summit() {
        return new BlockPos(boundingBox.minX() + C, baseY + RISE * (STACKS + 1), boundingBox.minZ() + C);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
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

    /** A rock column from below the ground up to {@code top}, banded dark and dusty. */
    private void column(WorldGenLevel level, BoundingBox box, int x, int z, int top, RandomSource r) {
        BlockState rock = s(MarsModule.CARBONACEOUS_CHONDRITE.get());
        BlockState dust = s(MarsModule.DEIMOS_REGOLITH.get());
        for (int y = Math.min(ground(level, x, z), top) - 2; y <= top; y++) {
            set(level, box, x, y, z, y == top || (y % 5 == 0 && r.nextInt(3) == 0) ? dust : rock);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        // the stacks, 2x2 each
        for (BlockPos top : stacks()) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    column(level, box, top.getX() + dx, top.getZ() + dz, top.getY() - 1, r);
                }
            }
        }
        // the spire: a bare 3x3 needle, too high to climb on foot
        BlockPos sum = summit();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                column(level, box, sum.getX() + dx, sum.getZ() + dz, sum.getY() - 1, r);
            }
        }
        // the relay on the summit: a mast with a dish and a sealed data cache
        int sy = sum.getY();
        set(level, box, sum.getX() - 1, sy, sum.getZ() - 1, s(Blocks.IRON_BLOCK));
        for (int dy = 1; dy <= 3; dy++) {
            set(level, box, sum.getX() - 1, sy + dy, sum.getZ() - 1, s(Blocks.LIGHTNING_ROD));
        }
        set(level, box, sum.getX() - 1, sy + 4, sum.getZ() - 1, s(Blocks.DAYLIGHT_DETECTOR));
        set(level, box, sum.getX() + 1, sy, sum.getZ() + 1, s(Blocks.WHITE_CONCRETE));
        set(level, box, sum.getX() + 1, sy + 1, sum.getZ() + 1, s(Blocks.GOLD_BLOCK));
        BlockPos chest = new BlockPos(sum.getX(), sy, sum.getZ());
        if (box.isInside(chest)) {
            level.setBlock(chest, s(Blocks.CHEST), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(MarsLoot.DEIMOS_RELAY, seed);
            }
        }
        // the plate at the foot of the first stack
        BlockPos first = stacks().get(0);
        int signX = first.getX() - 2;
        BlockPos sign = new BlockPos(signX, ground(level, signX, first.getZ()), first.getZ());
        if (box.isInside(sign)) {
            level.setBlock(sign, s(Blocks.OAK_SIGN).setValue(StandingSignBlock.ROTATION, 12), Block.UPDATE_CLIENTS);
            DungeonBits.signText(level, sign, "deimos_hop");
        }
    }
}
