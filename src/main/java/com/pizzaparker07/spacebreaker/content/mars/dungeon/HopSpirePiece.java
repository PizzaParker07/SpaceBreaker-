package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        // the spire: the relay's own lattice mast, light metal that stands out from the dark rock: four corner posts,
        // braces every four blocks, a deck at the top; too smooth to climb on foot
        BlockPos sum = summit();
        BlockState post = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = sum.getX() + dx;
                int z = sum.getZ() + dz;
                boolean corner = dx != 0 && dz != 0;
                boolean face = (dx == 0) != (dz == 0);
                for (int y = ground(level, x, z) - 2; y < sum.getY(); y++) {
                    if (y == sum.getY() - 1 || y < ground(level, x, z)) {
                        set(level, box, x, y, z, post); // the deck, and footings under the dust
                    } else if (corner) {
                        set(level, box, x, y, z, post);
                    } else if (face && (y - baseY) % 4 == 0) {
                        set(level, box, x, y, z, s(Blocks.IRON_BARS)
                                .setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, dz != 0)
                                .setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, dz != 0)
                                .setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, dx != 0)
                                .setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, dx != 0));
                    } else {
                        set(level, box, x, y, z, s(Blocks.AIR));
                    }
                }
            }
        }
        // the relay on the summit: a mast carrying a wide dish turned to the sky and a red beacon over it
        int sy = sum.getY();
        int mx = sum.getX() - 1;
        int mz = sum.getZ() - 1;
        for (int dy = 0; dy <= 4; dy++) {
            set(level, box, mx, sy + dy, mz, dy % 2 == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.IRON_BARS));
        }
        int dishY = sy + 5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                set(level, box, mx + dx, dishY + (rim ? 1 : 0), mz + dz, dx == 0 && dz == 0 ? s(Blocks.DAYLIGHT_DETECTOR) : s(Blocks.SMOOTH_QUARTZ_SLAB));
            }
        }
        set(level, box, mx, dishY + 1, mz, s(Blocks.LIGHTNING_ROD));
        set(level, box, mx, dishY + 2, mz, s(Blocks.POLISHED_ANDESITE));
        set(level, box, mx, dishY + 3, mz, s(Blocks.REDSTONE_TORCH)); // the red beacon
        set(level, box, sum.getX() + 1, sy, sum.getZ() + 1, s(Blocks.SMOOTH_QUARTZ));
        set(level, box, sum.getX() + 1, sy + 1, sum.getZ() + 1, s(com.pizzaparker07.spacebreaker.content.belt.LanderBlocks.GOLD_FOIL_CLADDING.get()));
        BlockPos cache = new BlockPos(sum.getX(), sy, sum.getZ());
        if (box.isInside(cache)) {
            level.setBlock(cache, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                    .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(cache) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity be) {
                be.setLootTable(MarsLoot.DEIMOS_RELAY, seed);
            }
        }
        // the cable: down the needle's north face to a ground station at its foot
        int cz = sum.getZ() - 2;
        int gy = ground(level, sum.getX(), cz);
        for (int y = gy; y < sy; y++) {
            set(level, box, sum.getX(), y, cz, s(Blocks.CHAIN));
        }
        groundStation(level, box, sum.getX() - 2, cz - 4);
        // the plate at the foot of the first stack
        BlockPos first = stacks().get(0);
        int signX = first.getX() - 2;
        BlockPos sign = new BlockPos(signX, ground(level, signX, first.getZ()), first.getZ());
        if (box.isInside(sign)) {
            level.setBlock(sign, s(Blocks.OAK_SIGN).setValue(StandingSignBlock.ROTATION, 12), Block.UPDATE_CLIENTS);
            DungeonBits.signText(level, sign, "deimos_hop");
        }
    }

    /** The relay's ground station: a small panelled hut with a dish on its roof and a floodlight over the door. */
    private void groundStation(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        BlockState panel = s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
        int gy = ground(level, x0 + 2, z0 + 1);
        for (int x = x0; x <= x0 + 4; x++) {
            for (int z = z0; z <= z0 + 2; z++) {
                for (int y = ground(level, x, z); y < gy; y++) {
                    set(level, box, x, y, z, s(MarsModule.CARBONACEOUS_CHONDRITE.get()));
                }
                for (int y = gy; y <= gy + 2; y++) {
                    boolean shell = x == x0 || x == x0 + 4 || z == z0 || z == z0 + 2;
                    boolean door = z == z0 + 2 && x == x0 + 2 && y <= gy + 1;
                    set(level, box, x, y, z, shell && !door ? panel : s(Blocks.AIR));
                }
                set(level, box, x, gy + 3, z, s(Blocks.POLISHED_ANDESITE_SLAB));
            }
        }
        set(level, box, x0 + 2, gy, z0 + 1, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get()));
        set(level, box, x0 + 1, gy + 4, z0 + 1, s(Blocks.DAYLIGHT_DETECTOR));
        for (int y = gy + 4; y <= gy + 7; y++) {
            set(level, box, x0 + 3, y, z0 + 1, s(Blocks.IRON_BARS));
        }
        set(level, box, x0 + 3, gy + 8, z0 + 1, s(Blocks.POLISHED_ANDESITE));
        set(level, box, x0 + 3, gy + 9, z0 + 1, s(Blocks.REDSTONE_TORCH));
        set(level, box, x0 + 2, gy + 2, z0 + 3, s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get())
                .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.SOUTH));
    }
}
