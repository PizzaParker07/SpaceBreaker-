package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned ice mine under the floor of a permanently shadowed polar crater on Mercury, where water ice has sat in
 * the dark for billions of years a few kilometres from ground that bakes at 430 °C. A head frame over the shaft, a
 * drift through the ice to the cutting face, a melt room, a store, and the mine's old maintenance drones.
 */
public class IceMinePiece extends StructurePiece {
    static final int SIZE = 25;
    private static final int G = 14;
    private final long seed;

    public IceMinePiece(long seed, int x, int y, int z) {
        super(MercuryStructures.ICE_MINE_PIECE.get(), 0, new BoundingBox(x, y - G, z, x + SIZE - 1, y + 10, z + SIZE - 1));
        this.seed = seed;
        setOrientation(Direction.NORTH);
    }

    public IceMinePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MercuryStructures.ICE_MINE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    public static final ResourceKey<LootTable> MINE_OFFICE = Dungeons.lootTable("mercury", "ice_mine_office");
    public static final ResourceKey<LootTable> MINE_STORES = Dungeons.lootTable("mercury", "ice_mine_stores");

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this, state, x, y, z, box);
    }

    private static BlockState base(net.neoforged.neoforge.registries.DeferredBlock<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    private static final BlockState RIB = s(Blocks.POLISHED_ANDESITE);

    private static double hash(int a, int b, long salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ salt;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState ice = s(MercuryModule.MERCURY_ICE.get());
        BlockState wall = s(MercuryModule.MERCURIAN_CRUST.get());
        int fy = 2; // mine level floor (local y)
        // the ice body: a lens of ice around the workings, with darker dust layers laid down between the ice ages
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 1; z < SIZE - 1; z++) {
                for (int y = 0; y <= 8; y++) {
                    double e = Math.pow((x - 12) / 11.5, 2) + Math.pow((z - 12) / 11.5, 2) + Math.pow((y - 4) / 5.0, 2);
                    if (e < 1) {
                        put(level, box, x, y, z, y % 3 == 0 ? s(MercuryModule.LOW_REFLECTANCE_ROCK.get()) : ice);
                    }
                }
            }
        }
        // drifts: an L-shaped tunnel; the melt room (west) and the store (east)
        room(level, box, 10, fy, 2, 14, fy + 3, 22, true);
        room(level, box, 3, fy, 15, 21, fy + 3, 19, false);
        room(level, box, 2, fy, 5, 8, fy + 4, 11, true);
        room(level, box, 16, fy, 5, 22, fy + 4, 11, true);
        door(level, box, 9, fy, 8);
        door(level, box, 15, fy, 8);
        // rails down both drifts for the ice carts
        for (int z = 7; z <= 18; z++) {
            if (z != 12) { // the trap plate
                put(level, box, 12, fy + 1, z, s(Blocks.RAIL));
            }
        }
        for (int x = 4; x <= 20; x++) {
            if (x != 12) {
                put(level, box, x, fy + 1, 17, Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE,
                        net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST));
            }
        }
        // puzzle: when the power failed the store was sealed with an ice plug; the sign says to cut through it
        put(level, box, 15, fy + 1, 8, ice);
        put(level, box, 15, fy + 2, 8, ice);
        BlockPos sign = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, Blocks.BIRCH_WALL_SIGN.defaultBlockState(),
                new BlockPos(13, fy + 2, 9), new BlockPos(14, fy + 2, 9), true, box);
        if (sign != null) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "ice_mine_store");
        }
        // trap: the old frost-protection system; a plate in the drift makes a dispenser in the floor throw powder snow
        put(level, box, 12, fy + 1, 12, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));
        BlockPos disp = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.place(level, this,
                Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.UP), 12, fy, 11, box);
        if (disp != null) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.load(level, disp,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET));
        }
        cutter(level, box, fy, ice);
        meltRoom(level, box, fy);
        store(level, box, fy, ice);
        headFrame(level, box, fy, wall);
        // the old maintenance drones still patrol the drift
        guard(level, box, 12, fy + 2, 10);
        guard(level, box, 8, fy + 2, 17);
    }

    /**
     * The cutting face at the end of the drift: clean ice exposed, the stalled cutter against it (a body, a toothed drum
     * and a conveyor back to a cart), and a stack of cut blocks waiting to be hauled.
     */
    private void cutter(WorldGenLevel level, BoundingBox box, int fy, BlockState ice) {
        for (int x = 10; x <= 14; x++) {
            for (int y = fy + 1; y <= fy + 3; y++) {
                put(level, box, x, y, 2, ice);
            }
        }
        // the drum: grinding wheels across the face
        for (int x = 11; x <= 13; x++) {
            put(level, box, x, fy + 2, 3, s(Blocks.GRINDSTONE));
        }
        // the body and the conveyor
        put(level, box, 11, fy + 1, 4, s(Blocks.IRON_BLOCK));
        put(level, box, 13, fy + 1, 4, s(Blocks.IRON_BLOCK));
        put(level, box, 12, fy + 1, 4, base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM));
        put(level, box, 12, fy + 2, 4, base(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR)
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        for (int z = 5; z <= 6; z++) {
            put(level, box, 12, fy + 1, z, base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM));
        }
        // cut blocks stacked by the wall
        put(level, box, 11, fy + 1, 6, ice);
        put(level, box, 11, fy + 1, 7, ice);
        put(level, box, 11, fy + 2, 6, ice);
    }

    /** The melt room: tanks and an electrolyser on a pipe run (ice to water to oxygen and hydrogen), the office locker. */
    private void meltRoom(WorldGenLevel level, BoundingBox box, int fy) {
        var tank = base(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.FLUID_TANK);
        for (int z = 6; z <= 7; z++) {
            put(level, box, 3, fy + 1, z, tank);
            put(level, box, 3, fy + 2, z, tank);
        }
        put(level, box, 3, fy + 1, 9, base(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.ELECTROLYZER.block())
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
        for (int z = 6; z <= 9; z++) {
            put(level, box, 3, fy + 3, z, base(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.FLUID_PIPE));
        }
        put(level, box, 4, fy + 1, 10, s(Blocks.WATER_CAULDRON));
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 8, fy + 1, 6, Direction.WEST, MINE_OFFICE, seed + 1, box);
        BlockPos log = com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, Blocks.BIRCH_WALL_SIGN.defaultBlockState(),
                new BlockPos(5, fy + 2, 6), new BlockPos(5, fy + 2, 5), true, box);
        if (log != null) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, log, "ice_mine_log");
        }
    }

    /** The store: lockers along the far wall and a pallet of cut ice that never went up. */
    private void store(WorldGenLevel level, BoundingBox box, int fy, BlockState ice) {
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 22, fy + 1, 6, Direction.WEST, MINE_STORES, seed + 2, box);
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, 22, fy + 1, 8, Direction.WEST, MINE_STORES, seed + 3, box);
        for (int x = 17; x <= 18; x++) {
            put(level, box, x, fy + 1, 10, base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM));
            put(level, box, x, fy + 2, 10, ice);
        }
    }

    /**
     * Above the shaft on the crater floor: a lattice head frame with its sheave wheel, a floodlight mast that shows the
     * pithead from the crater rim, and a small control hut.
     */
    private void headFrame(WorldGenLevel level, BoundingBox box, int fy, BlockState wall) {
        for (int y = fy + 1; y <= G + 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean centre = dx == 0 && dz == 0;
                    if (y > fy + 3 || centre) {
                        if (centre) {
                            // the ladder hangs on the shaft wall at local z 19, whatever the piece's orientation
                            com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation.placeToward(level, this, s(Blocks.LADDER),
                                    new BlockPos(12, y, 20), new BlockPos(12, y, 19), true, box);
                        } else {
                            put(level, box, 12 + dx, y, 20 + dz, dz == -1 ? wall : s(Blocks.AIR));
                        }
                    }
                }
            }
            put(level, box, 12, y, 19, wall);
        }
        // the lattice: four posts with braces every three blocks
        BlockState post = s(Blocks.SMOOTH_QUARTZ); // pale, so the frame reads against the dark crater floor
        for (int y = G + 1; y <= G + 8; y++) {
            for (int[] c : new int[][]{{10, 18}, {14, 18}, {10, 22}, {14, 22}}) {
                put(level, box, c[0], y, c[1], post);
            }
            if ((y - G) % 3 == 0) {
                for (int i = 11; i <= 13; i++) {
                    put(level, box, i, y, 18, s(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, true).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, true));
                    put(level, box, i, y, 22, s(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, true).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, true));
                    put(level, box, 10, y, i + 8, s(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, true).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, true));
                    put(level, box, 14, y, i + 8, s(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, true).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, true));
                }
            }
        }
        for (int x = 10; x <= 14; x++) {
            put(level, box, x, G + 9, 20, RIB);
        }
        put(level, box, 12, G + 10, 20, s(Blocks.GRINDSTONE)); // the sheave
        for (int y = G + 2; y <= G + 8; y++) {
            put(level, box, 12, y, 20, s(Blocks.CHAIN));
        }
        // the floodlight mast, its lamps turned down onto the pithead
        for (int y = G + 1; y <= G + 10; y++) {
            put(level, box, 17, y, 20, s(Blocks.IRON_BARS));
        }
        put(level, box, 17, G + 11, 20, RIB);
        for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            put(level, box, 17 + d[0], G + 11, 20 + d[1], base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT)
                    .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.DOWN));
        }
        put(level, box, 17, G + 12, 20, base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL)); // the beacon seen from the rim
        // the control hut
        var panel = base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base());
        for (int x = 18; x <= 21; x++) {
            for (int z = 21; z <= 23; z++) {
                for (int y = G + 1; y <= G + 3; y++) {
                    boolean shell = x == 18 || x == 21 || z == 21 || z == 23;
                    boolean doorway = x == 18 && z == 22 && y <= G + 2;
                    boolean window = y == G + 2 && z == 23 && x == 20;
                    put(level, box, x, y, z, !shell || doorway ? s(Blocks.AIR) : window ? s(Blocks.GLASS_PANE) : panel);
                }
                put(level, box, x, G + 4, z, s(Blocks.POLISHED_ANDESITE_SLAB));
            }
        }
        put(level, box, 20, G + 1, 22, base(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR)
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.WEST));
    }

    /**
     * A drift or room: walls cut into the ice where it is ice, crust elsewhere; steel arches every three blocks along it
     * (grey frames with brushed panels), a cable duct along the ceiling and floodlights, some still lit.
     */
    private void room(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, boolean alongZ) {
        BlockState wall = s(MercuryModule.MERCURIAN_CRUST.get());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int a = alongZ ? z : x;
                boolean arch = a % 3 == 0;
                for (int y = y0; y <= y1; y++) {
                    boolean side = alongZ ? (x == x0 || x == x1) : (z == z0 || z == z1);
                    boolean end = alongZ ? (z == z0 || z == z1) : (x == x0 || x == x1);
                    BlockState here = getBlock(level, x, y, z, box);
                    if (y == y0) {
                        put(level, box, x, y, z, s(Blocks.SMOOTH_STONE));
                    } else if (side || y == y1) {
                        if (arch) {
                            put(level, box, x, y, z, RIB);
                        } else if (y == y1 && (alongZ ? x == x0 + 1 : z == z0 + 1)) {
                            put(level, box, x, y, z, base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT));
                        } else if (y == y1 && (alongZ ? x == (x0 + x1) / 2 : z == (z0 + z1) / 2) && a % 6 == 3) {
                            put(level, box, x, y, z, hash(x, z, seed) < 0.5 ? s(Blocks.REDSTONE_LAMP)
                                    : base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL));
                        } else if (y == y1 || !(here.is(MercuryModule.MERCURY_ICE.get()) || here.is(MercuryModule.LOW_REFLECTANCE_ROCK.get()))) {
                            put(level, box, x, y, z, y == y1 ? base(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base()) : wall);
                        }
                    } else if (end) {
                        if (!(here.is(MercuryModule.MERCURY_ICE.get()) || here.is(MercuryModule.LOW_REFLECTANCE_ROCK.get()))) {
                            put(level, box, x, y, z, wall);
                        }
                    } else {
                        put(level, box, x, y, z, s(Blocks.AIR));
                    }
                }
            }
        }
    }

    /** A doorway through both walls between two rooms (x-1 .. x+1). */
    private void door(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            put(level, box, x + dx, y + 1, z, s(Blocks.AIR));
            put(level, box, x + dx, y + 2, z, s(Blocks.AIR));
        }
    }

    private void guard(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = MoonEntities.SURVEY_DRONE.get().create(level.getLevel());
        if (mob != null) {
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
