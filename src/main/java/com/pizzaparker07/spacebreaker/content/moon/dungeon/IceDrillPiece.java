package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An old automated ice-drilling station on the floor of a permanently shadowed polar crater. The drill derrick
 * still stands over its bore hole and an RTG in the control hut still runs, which is exactly what draws the heat
 * siphons that haunt the cold trap. Rewards: racks of ice core samples and the station's data (tech disks).
 * Built block by block like the outpost, layout fixed by the structure seed.
 */
public class IceDrillPiece extends StructurePiece {
    static final int SIZE = 17;
    /** Local height of the platform floor. */
    static final int F = 3;
    private final long seed;

    public IceDrillPiece(long seed, int x, int floorY, int z, Direction facing) {
        super(MoonStructures.ICE_DRILL_PIECE.get(), 0, new BoundingBox(x, floorY - F, z, x + SIZE - 1, floorY + 12, z + SIZE - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public IceDrillPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MoonStructures.ICE_DRILL_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    /** World position of a local block. */
    public BlockPos localToWorld(int x, int y, int z) {
        return PieceOrientation.worldPos(this, x, y, z);
    }

    @Override
    protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        PieceOrientation.place(level, this, state, x, y, z, box);
    }

    private static BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState bricks = s(MoonModule.SINTERED_REGOLITH_BRICKS.base().get());
        BlockState hutWall = s(MoonModule.ANORTHOSITE_BRICKS.base().get());
        BlockState ice = s(MoonModule.LUNAR_ICE.get());
        // clear the site and lay an icy footing under everything
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = F + 1; y <= F + 11; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                for (int y = 0; y < F; y++) {
                    if (getBlock(level, x, y, z, box).isAir()) {
                        placeBlock(level, ice, x, y, z, box);
                    }
                }
                placeBlock(level, r.nextInt(5) == 0 ? s(MoonModule.ICY_REGOLITH.get()) : ice, x, F, z, box);
            }
        }
        platform(level, box, bricks);
        derrick(level, box);
        hut(level, box, hutWall, bricks);
        coreRacks(level, box);
        // frost: icy regolith drifts on the platform edges
        for (int i = 0; i < 14; i++) {
            int x = 4 + r.nextInt(11);
            int z = 4 + r.nextInt(11);
            if (getBlock(level, x, F + 1, z, box).isAir() && (x == 4 || x == 14 || z == 4 || z == 14)) {
                placeBlock(level, s(MoonModule.ICY_REGOLITH.get()), x, F + 1, z, box);
            }
        }
        guard(level, box, 3, F + 1, 9);
        guard(level, box, 12, F + 1, 13);
    }

    /** The drilling platform: an 11 x 11 brick deck one block above the ice, with a raised rim. */
    private void platform(WorldGenLevel level, BoundingBox box, BlockState bricks) {
        for (int x = 4; x <= 14; x++) {
            for (int z = 4; z <= 14; z++) {
                placeBlock(level, bricks, x, F, z, box);
            }
        }
        BlockState slab = MoonModule.SINTERED_REGOLITH_BRICKS.slab().get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int i = 4; i <= 14; i++) {
            for (int[] c : new int[][]{{i, 4}, {i, 14}, {4, i}, {14, i}}) {
                if (!(c[0] == 9 && c[1] == 4)) { // gap where the walkway from the hut comes up
                    placeBlock(level, slab, c[0], F + 1, c[1], box);
                }
            }
        }
    }

    /** The derrick: four posts, a crown, the drill motor and its chain running down the bore hole into the ice. */
    private void derrick(WorldGenLevel level, BoundingBox box) {
        BlockState post = s(MoonModule.ANORTHOSITE_BRICKS.wall().get());
        int cx = 9;
        int cz = 9;
        for (int[] c : new int[][]{{cx - 2, cz - 2}, {cx + 2, cz - 2}, {cx - 2, cz + 2}, {cx + 2, cz + 2}}) {
            for (int y = F + 1; y <= F + 7; y++) {
                placeBlock(level, post, c[0], y, c[1], box);
            }
        }
        BlockState beam = s(Blocks.SMOOTH_STONE_SLAB);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    placeBlock(level, beam, cx + dx, F + 8, cz + dz, box);
                }
            }
        }
        placeBlock(level, s(Blocks.IRON_BLOCK), cx, F + 8, cz, box);       // drill motor
        placeBlock(level, s(Blocks.LIGHTNING_ROD), cx, F + 9, cz, box);    // antenna on the crown
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx + 2, F + 7, cz, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx - 2, F + 7, cz, box);
        BlockState chain = s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y);
        for (int y = F - 3; y <= F + 7; y++) {
            placeBlock(level, chain, cx, y, cz, box);
        }
        // the bore hole around the drill string, down into the ice
        for (int y = F - 3; y <= F; y++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                placeBlock(level, y == F ? s(Blocks.IRON_BARS) : s(Blocks.AIR), cx + d.getStepX(), y, cz + d.getStepZ(), box);
            }
        }
        placeBlock(level, s(MoonModule.LUNAR_ICE.get()), cx, F - 4, cz, box);
    }

    /** The control hut on the south-west corner, with the still-running RTG, a lamp and the station data. */
    private void hut(WorldGenLevel level, BoundingBox box, BlockState wall, BlockState roof) {
        int x0 = 0;
        int z0 = 0;
        int x1 = 5;
        int z1 = 5;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                placeBlock(level, s(Blocks.SMOOTH_STONE), x, F, z, box);
                for (int y = F + 1; y <= F + 3; y++) {
                    boolean window = y == F + 2 && edge && (x == 2 || z == 3) && !(x == x1 && z == 3);
                    placeBlock(level, edge ? (window ? s(Blocks.LIGHT_GRAY_STAINED_GLASS) : wall) : s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, roof, x, F + 4, z, box);
            }
        }
        // doorway toward the platform, and a short walkway
        placeBlock(level, s(Blocks.AIR), x1, F + 1, 3, box);
        placeBlock(level, s(Blocks.AIR), x1, F + 2, 3, box);
        for (int x = x1 + 1; x <= 9; x++) {
            placeBlock(level, s(MoonModule.SINTERED_REGOLITH_BRICKS.base().get()), x, F, x <= 8 ? 3 : 4, box);
        }
        // the RTG, still warm, with its pellet
        placeBlock(level, MachinesModule.RTG.block().get().defaultBlockState(), 1, F + 1, 1, box);
        BlockPos rtg = PieceOrientation.worldPos(this, 1, F + 1, 1);
        if (box.isInside(rtg) && level.getBlockEntity(rtg) instanceof MachineBlockEntity be) {
            be.items().setStackInSlot(0, new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        }
        placeBlock(level, s(Blocks.REDSTONE_LAMP).setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true), 3, F + 3, 3, box);
        placeBlock(level, s(Blocks.REDSTONE_BLOCK), 3, F + 4, 3, box);
        placeBlock(level, s(Blocks.CARTOGRAPHY_TABLE), 4, F + 1, 1, box);
        placeBlock(level, s(Blocks.LECTERN), 1, F + 1, 4, box);
        container(level, box, 2, F + 1, 1, MoonLoot.ICE_DRILL_CONTROL, true);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2, F + 3, 4, box);
    }

    /** Core racks beside the derrick: barrels of drilled ice cores. */
    private void coreRacks(WorldGenLevel level, BoundingBox box) {
        container(level, box, 12, F + 1, 9, MoonLoot.ICE_DRILL_CORES, false);
        container(level, box, 12, F + 1, 10, MoonLoot.ICE_DRILL_CORES, false);
        placeBlock(level, s(Blocks.BLUE_ICE), 12, F + 2, 9, box);
        placeBlock(level, s(Blocks.PACKED_ICE), 12, F + 1, 11, box);
    }

    private void container(WorldGenLevel level, BoundingBox box, int x, int y, int z, ResourceKey<LootTable> loot, boolean chest) {
        // the shared supply locker for every kind of store (it was a chest or a barrel); doors turned toward the middle
        RandomSource r = RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z));
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, x, y, z, inward(x, z, SIZE), loot, r.nextLong(), box);
    }

    /** The side of a square piece's floor plan facing its middle from cell (x, z), along the longer way. */
    static Direction inward(int x, int z, int size) {
        int dx = size / 2 - x;
        int dz = size / 2 - z;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void guard(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = PieceOrientation.worldPos(this, x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = MoonEntities.HEAT_SIPHON.get().create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
