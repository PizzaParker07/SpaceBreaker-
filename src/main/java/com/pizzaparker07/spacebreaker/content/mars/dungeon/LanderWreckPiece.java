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
 * The wreck of an old lander: an octagonal bus on four splayed legs, one of them snapped so the body sags at that
 * corner, a solar wing torn off, its heat shield standing half buried a little way off, the orange-and-white parachute spread over the dust, and the data recorder still sealed inside. Everything that
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
        BlockState deck = s(Blocks.SMOOTH_STONE_SLAB).setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
        // skid marks: it bounced in from the east before it came to rest
        for (int i = 3; i <= 9; i++) {
            for (int side : new int[]{-1, 1}) {
                int gx = cx + i;
                int gz = cz + side * 2;
                set(level, box, gx, ground(level, gx, gz) - 1, gz, s(MarsModule.MARTIAN_SAND.get()));
            }
        }
        // the body: an octagonal bus on a deck, standing a block clear of the ground on its legs. The south-east leg gave
        // way, so that corner of the body sits a block lower.
        int b = y + 2;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                int t = dx + dz >= 2 ? -1 : 0;
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean diagonal = Math.abs(dx) + Math.abs(dz) == 3;
                set(level, box, cx + dx, b + t, cz + dz, grey);
                for (int dy = 1; dy <= 2; dy++) {
                    BlockState wall = diagonal ? white : (dy == 1 ? foil : white);
                    set(level, box, cx + dx, b + dy + t, cz + dz, edge ? wall : s(Blocks.AIR));
                }
                set(level, box, cx + dx, b + 3 + t, cz + dz, deck);
            }
        }
        // the torn side panel and the data recorder inside
        set(level, box, cx, b + 1, cz + 2, s(Blocks.AIR));
        set(level, box, cx, b + 2, cz + 2, s(Blocks.AIR));
        set(level, box, cx + 1, b + 1, cz + 2, s(Blocks.AIR));
        BlockPos chest = new BlockPos(cx, b + 1, cz);
        if (box.isInside(chest)) {
            level.setBlock(chest, s(Blocks.CHEST), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(MarsLoot.LANDER_DATA, seed);
            }
        }
        // legs: struts out to footpads at the four diagonals; the south-east one snapped and lies flat
        for (int[] leg : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            int fx = cx + leg[0] * 4;
            int fz = cz + leg[1] * 4;
            int fy = ground(level, fx, fz);
            if (leg[0] == 1 && leg[1] == 1) {
                for (int i = 0; i < 3; i++) {
                    set(level, box, cx + 3 + i, ground(level, cx + 3 + i, cz + 4), cz + 4, s(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, net.minecraft.core.Direction.Axis.X));
                }
                set(level, box, cx + 6, ground(level, cx + 6, cz + 4), cz + 4, s(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE));
                continue;
            }
            set(level, box, fx, fy, fz, s(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE));
            int sx = cx + leg[0] * 3;
            int sz = cz + leg[1] * 3;
            for (int dy = ground(level, sx, sz); dy <= b + 1; dy++) {
                set(level, box, sx, dy, sz, s(Blocks.IRON_BARS));
            }
        }
        // a solar wing still folded out to the west; the east wing broke off and lies in the dust
        for (int i = 3; i <= 6; i++) {
            set(level, box, cx - i, b + 2, cz, i == 3 ? s(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, net.minecraft.core.Direction.Axis.X)
                    : s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get()));
        }
        for (int i = 0; i < 3; i++) {
            int wx = cx + 5 + i;
            int wz = cz - 2;
            set(level, box, wx, ground(level, wx, wz), wz, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get()));
        }
        // on the top deck: the camera mast and the high-gain dish, still pointed at the sky
        set(level, box, cx - 1, b + 3, cz - 1, s(Blocks.LIGHT_GRAY_CONCRETE));
        set(level, box, cx - 1, b + 4, cz - 1, s(Blocks.LIGHTNING_ROD));
        set(level, box, cx - 1, b + 5, cz - 1, s(Blocks.OBSERVER));
        set(level, box, cx, b + 3, cz - 1, s(Blocks.IRON_BARS));
        set(level, box, cx, b + 4, cz - 1, s(Blocks.DAYLIGHT_DETECTOR));
        // dust drifted against the west side, up to the deck
        for (int dx = -5; dx <= -3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int top = y + (dx + 5) - (Math.abs(dz) == 2 ? 1 : 0);
                for (int dy = ground(level, cx + dx, cz + dz); dy <= top; dy++) {
                    if (level.getBlockState(new BlockPos(cx + dx, dy, cz + dz)).isAir()) {
                        set(level, box, cx + dx, dy, cz + dz, s(MarsModule.MARTIAN_REGOLITH.get()));
                    }
                }
            }
        }
        // the heat shield, dropped before landing: a dish standing on its edge, half buried, a little way north-east
        int hx = cx + 8;
        int hz = cz - 6;
        int hy = ground(level, hx, hz);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx * dx + dy * dy <= 5) {
                    set(level, box, hx + dx, hy + dy, hz, dx * dx + dy * dy <= 1 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.TERRACOTTA));
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
