package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;

/**
 * An abandoned automated lunar survey outpost: a central hub with a lab, a storage module and a drone bay
 * around it, an airlock to the outside, a solar field, a landing pad and a radio mast. Half-buried in dust,
 * windows cracked, still guarded by its survey drones. Built block by block (no template files), layout fixed
 * per structure seed so every chunk agrees.
 */
public class OutpostPiece extends StructurePiece {
    static final int SIZE = 31;
    static final int HEIGHT = 14;
    private final long seed;

    public OutpostPiece(long seed, int x, int y, int z, Direction facing) {
        super(MoonStructures.OUTPOST_PIECE.get(), 0, new BoundingBox(x, y - 4, z, x + SIZE - 1, y + HEIGHT, z + SIZE - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public OutpostPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MoonStructures.OUTPOST_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    /** World position of a local block (local +x = east, +z = south before orientation). */
    public BlockPos localToWorld(int x, int y, int z) {
        return PieceOrientation.worldPos(this, x, y, z);
    }

    /** World direction of a local direction, from how this piece maps local coordinates. */
    public Direction worldDirection(Direction local) {
        return PieceOrientation.direction(this, local);
    }

    /**
     * Places a block written in piece space through the shared dungeon helper: turned to match the piece's coordinate
     * mapping and, like world generation, without neighbour shape updates.
     */
    @Override
    protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        BlockPos pos = PieceOrientation.place(level, this, state, x, y, z, box);
        if (pos != null && (state.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock || state.getBlock() instanceof net.minecraft.world.level.block.WallBlock)) {
            level.getChunk(pos).markPosForPostprocessing(pos);
        }
    }

    // local y: 0..3 foundation, 4 = floor level of the pad
    private static final int F = 4;

    private BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        BlockState pad = s(MoonModule.SINTERED_REGOLITH.get());
        BlockState foundation = s(MoonModule.ANORTHOSITE.base().get());
        // clear the site and lay a sintered pad on a solid foundation
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = F + 1; y <= HEIGHT + 3; y++) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                }
                boolean edge = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                placeBlock(level, edge ? s(MoonModule.LUNAR_REGOLITH.get()) : pad, x, F, z, box);
                fillColumnDown(level, foundation, x, F - 1, z, box);
            }
        }
        BlockState wall = s(BaseBlocks.ALUMINUM_PANEL.base().get());
        BlockState frame = s(RocketryModule.STATION_HULL.get());
        // room variety: the east module is either a storage hall or a collapsed greenhouse
        boolean greenhouse = (seed & 1) == 1;
        // hub, lab (north), storage (east), drone bay (west), airlock (south)
        module(level, box, r, 10, 10, 20, 20, 6, wall, frame);
        module(level, box, r, 11, 1, 19, 8, 5, wall, frame);
        module(level, box, r, 22, 11, 29, 19, 5, wall, frame);
        module(level, box, r, 1, 11, 8, 19, 5, wall, frame);
        module(level, box, r, 13, 22, 17, 26, 4, wall, frame);
        module(level, box, r, 1, 1, 8, 8, 5, wall, frame);   // crew quarters (north-west)
        tunnelX(level, box, 5, 8, 11, frame);
        vault(level, box, frame);                             // sealed vault (north-east)
        tunnelX(level, box, 5, 19, 22, frame);
        // short pressurised tunnels between modules
        tunnelZ(level, box, 15, 8, 10, frame);
        tunnelX(level, box, 15, 20, 22, frame);
        tunnelX(level, box, 15, 8, 10, frame);
        tunnelZ(level, box, 15, 20, 22, frame);
        // doorways through the walls
        for (int[] d : new int[][]{{15, 8}, {15, 10}, {20, 15}, {22, 15}, {8, 15}, {10, 15}, {15, 20}, {15, 22}, {8, 5}, {11, 5}, {19, 5}}) {
            placeBlock(level, s(Blocks.AIR), d[0], F + 1, d[1], box);
            placeBlock(level, s(Blocks.AIR), d[0], F + 2, d[1], box);
        }
        for (int[] m : MODULES) {
            berm(level, box, m);
        }
        airlockDoors(level, box);
        furnishHub(level, box, r);
        furnishLab(level, box, r);
        if (greenhouse) {
            furnishGreenhouse(level, box);
        } else {
            furnishStorage(level, box, r);
        }
        furnishQuarters(level, box);
        vaultPuzzle(level, box);
        furnishDroneBay(level, box, r);
        exterior(level, box, r);
        decay(level, box, r);
    }

    /** Module footprints (x0, z0, x1, z1): hub, lab, storage, drone bay, airlock, crew quarters. */
    private static final int[][] MODULES = {{10, 10, 20, 20}, {11, 1, 19, 8}, {22, 11, 29, 19}, {1, 11, 8, 19}, {13, 22, 17, 26}, {1, 1, 8, 8}};
    /** Tunnel footprints and the vault, kept clear of the berms. */
    private static final int[][] KEEP_CLEAR = {{14, 8, 16, 10}, {20, 14, 22, 16}, {8, 14, 10, 16}, {14, 20, 16, 22}, {8, 4, 11, 6},
            {19, 4, 22, 6}, {22, 2, 28, 8}, {13, 27, 17, 28}};

    /**
     * Sintered regolith bags piled against a module's outer walls (shielding, as on the roof): a block high, a slab on
     * top here and there, never in front of a tunnel, a door or another module. Chosen by position only, so every chunk
     * agrees.
     */
    private void berm(WorldGenLevel level, BoundingBox box, int[] m) {
        for (int x = m[0] - 1; x <= m[2] + 1; x++) {
            for (int z = m[1] - 1; z <= m[3] + 1; z++) {
                boolean ring = x == m[0] - 1 || x == m[2] + 1 || z == m[1] - 1 || z == m[3] + 1;
                if (!ring || x < 1 || z < 1 || x > SIZE - 2 || z > SIZE - 2 || inside(MODULES, x, z) || inside(KEEP_CLEAR, x, z)) {
                    continue;
                }
                int h = (int) (Math.floorMod(x * 73856093L ^ z * 19349663L ^ seed, 7L));
                if (h == 0) {
                    continue; // a gap in the pile
                }
                placeBlock(level, h < 3 ? s(MoonModule.SINTERED_REGOLITH_BRICKS.base().get()) : s(MoonModule.SINTERED_REGOLITH.get()), x, F + 1, z, box);
                if (h >= 5) {
                    placeBlock(level, s(MoonModule.SINTERED_REGOLITH_BRICKS.slab().get()), x, F + 2, z, box);
                }
            }
        }
    }

    private static boolean inside(int[][] rects, int x, int z) {
        for (int[] q : rects) {
            if (x >= q[0] && x <= q[2] && z >= q[1] && z <= q[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * A box module: hull edges, aluminium wall panels with hull windows, a cable tray along the top of the walls, a
     * titanium deck, a ceiling light, and a regolith shield on the roof.
     */
    private void module(WorldGenLevel level, BoundingBox box, RandomSource r, int x0, int z0, int x1, int z1, int h,
                        BlockState wall, BlockState frame) {
        BlockState floor = s(BaseBlocks.TITANIUM_DECK.base().get());
        BlockState glass = s(RocketryModule.STATION_WINDOW.get());
        BlockState tray = s(BaseBlocks.CABLE_DUCT.get());
        BlockState skirting = s(Blocks.GRAY_CONCRETE);
        BlockState stripe = s(Blocks.BLUE_CONCRETE);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                placeBlock(level, ex || ez ? frame : floor, x, F, z, box);
                for (int y = F + 1; y < F + h; y++) {
                    BlockState st;
                    if (ex && ez) {
                        st = frame;
                    } else if (ex || ez) {
                        boolean window = y == F + 2 && ((ex ? z : x) % 3 == 0);
                        // dark skirting at the floor, a blue band at waist height with the windows in it, the cable tray on top
                        st = window ? glass : y == F + 1 ? skirting : y == F + 2 ? stripe : y == F + h - 1 ? tray : wall;
                    } else {
                        st = s(Blocks.AIR);
                    }
                    placeBlock(level, st, x, y, z, box);
                }
                placeBlock(level, ex || ez ? frame : wall, x, F + h, z, box);
                // regolith shielding piled on the roof (radiation protection)
                if (!ex && !ez) {
                    placeBlock(level, s(MoonModule.LUNAR_REGOLITH.get()), x, F + h + 1, z, box);
                }
            }
        }
        // light panels set into the ceiling every three blocks, and one in the middle
        for (int x = x0 + 2; x < x1; x += 3) {
            for (int z = z0 + 2; z < z1; z += 3) {
                placeBlock(level, s(BaseBlocks.LED_PANEL.get()), x, F + h, z, box);
            }
        }
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), (x0 + x1) / 2, F + h, (z0 + z1) / 2, box);
    }

    /** A pressurised tunnel along z between two module walls (exclusive), one block wide inside. */
    private void tunnelZ(WorldGenLevel level, BoundingBox box, int cx, int z0, int z1, BlockState frame) {
        for (int z = z0 + 1; z < z1; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                placeBlock(level, dx == 0 ? s(BaseBlocks.TITANIUM_DECK.base().get()) : frame, cx + dx, F, z, box);
                placeBlock(level, frame, cx + dx, F + 3, z, box);
                for (int y = F + 1; y <= F + 2; y++) {
                    placeBlock(level, dx == 0 ? s(Blocks.AIR) : frame, cx + dx, y, z, box);
                }
            }
        }
    }

    /** The same along x. */
    private void tunnelX(WorldGenLevel level, BoundingBox box, int cz, int x0, int x1, BlockState frame) {
        for (int x = x0 + 1; x < x1; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                placeBlock(level, dz == 0 ? s(BaseBlocks.TITANIUM_DECK.base().get()) : frame, x, F, cz + dz, box);
                placeBlock(level, frame, x, F + 3, cz + dz, box);
                for (int y = F + 1; y <= F + 2; y++) {
                    placeBlock(level, dz == 0 ? s(Blocks.AIR) : frame, x, y, cz + dz, box);
                }
            }
        }
    }

    private void airlockDoors(WorldGenLevel level, BoundingBox box) {
        // outer door in the south wall of the airlock, inner door toward the hub
        door(level, box, 15, 26, Direction.SOUTH);
        door(level, box, 15, 22, Direction.SOUTH);
        placeBlock(level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), 16, F + 2, 27, box);
        placeBlock(level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), 14, F + 2, 25, box);
        // inner door opens from pressure plates on both sides
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 15, F + 1, 21, box);
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 15, F + 1, 23, box);
        // pressure warning stripes on the airlock floor
        for (int x = 14; x <= 16; x++) {
            for (int z = 23; z <= 25; z++) {
                placeBlock(level, x == 15 ? s(BaseBlocks.GRATE.get()) : s(BaseBlocks.HAZARD_STRIPE.base().get()), x, F, z, box);
            }
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        BlockState lower = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        placeBlock(level, lower, x, F + 1, z, box);
        placeBlock(level, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, F + 2, z, box);
    }

    private void furnishHub(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // survey consoles along the north wall: control desks facing into the room, a seat at two of them
        for (int x = 11; x <= 19; x++) {
            if (x < 14 || x > 16) {
                placeBlock(level, MoonModule.CONTROL_DESK.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH), x, F + 1, 11, box);
            }
        }
        for (int x : new int[]{12, 18}) {
            placeBlock(level, BaseBlocks.ALUMINUM_PANEL.stairs().get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), x, F + 1, 12, box);
        }
        // the map table in the middle, a seat at it
        for (int x = 14; x <= 16; x++) {
            placeBlock(level, BaseBlocks.TITANIUM_DECK.slab().get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP), x, F + 1, 15, box);
        }
        placeBlock(level, BaseBlocks.ALUMINUM_PANEL.stairs().get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 15, F + 1, 16, box);
        // the power and water lines come in along the floor
        for (int z = 12; z <= 19; z++) {
            if (z >= 14 && z <= 16) {
                continue; // the east doorway
            }
            placeBlock(level, s(BaseBlocks.PIPE_DUCT.get()), 19, F + 1, z, box);
        }
        container(level, box, r, 11, F + 1, 19, MoonLoot.OUTPOST_COMMAND, true);
    }

    private void furnishLab(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // a long bench with instruments, a sample tank of pressure glass in the corner
        for (int x = 12; x <= 18; x++) {
            placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP), x, F + 1, 2, box);
        }
        placeBlock(level, s(BaseBlocks.CABLE_DUCT.get()), 13, F + 2, 2, box);
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), 15, F + 2, 2, box);
        placeBlock(level, s(BaseBlocks.PRESSURE_GLASS.get()), 18, F + 1, 3, box);
        placeBlock(level, s(BaseBlocks.PRESSURE_GLASS.get()), 18, F + 2, 3, box);
        placeBlock(level, s(MoonModule.LUNAR_ICE.get()), 12, F + 1, 7, box);
        placeBlock(level, s(MoonModule.KREEP_ORE.get()), 18, F + 1, 7, box);
        container(level, box, r, 12, F + 1, 4, MoonLoot.OUTPOST_LAB, true);
        container(level, box, r, 18, F + 1, 4, MoonLoot.OUTPOST_LAB, true);
    }

    private void furnishStorage(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int z = 12; z <= 18; z += 2) {
            container(level, box, r, 28, F + 1, z, MoonLoot.OUTPOST_STORAGE, false);
            if (r.nextBoolean()) {
                container(level, box, r, 28, F + 2, z, MoonLoot.OUTPOST_STORAGE, false);
            }
        }
        container(level, box, r, 23, F + 1, 18, MoonLoot.OUTPOST_STORAGE, true);
    }

    private void furnishDroneBay(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // charging pads and the drone dispenser (spawner) in a cage
        placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 4, F, 15, box);
        placeBlock(level, s(Blocks.SPAWNER), 4, F + 1, 15, box);
        BlockPos sp = getWorldPos(4, F + 1, 15);
        if (box.isInside(sp) && level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(MoonEntities.SURVEY_DRONE.get(), RandomSource.create(seed + 7));
        }
        for (int[] c : new int[][]{{3, 14}, {5, 14}, {3, 16}, {5, 16}}) {
            for (int y = F + 1; y <= F + 3; y++) {
                placeBlock(level, s(BaseBlocks.RAILING.get()), c[0], y, c[1], box);
            }
        }
        placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 3, F, 12, box);
        placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 3, F, 18, box);
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), 3, F + 1, 12, box);
        placeBlock(level, s(BaseBlocks.CABLE_DUCT.get()), 3, F + 1, 18, box);
        guard(level, box, MoonEntities.SURVEY_DRONE.get(), 3, F + 2, 12);
        guard(level, box, MoonEntities.SURVEY_DRONE.get(), 15, F + 3, 16);
    }

    private void exterior(WorldGenLevel level, BoundingBox box, RandomSource r) {
        // solar field: rows of panels on deck plates; some knocked flat into the dust, some frames left bare
        for (int x = 22; x <= 29; x += 2) {
            for (int z = 22; z <= 29; z += 2) {
                int roll = r.nextInt(6);
                if (roll == 0) {
                    placeBlock(level, s(BaseBlocks.GRATE.get()), x, F + 1, z, box);           // bare frame
                } else if (roll == 1) {
                    placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState(), x, F + 1, z, box); // fallen panel
                } else {
                    placeBlock(level, MachinesModule.SOLAR_PANEL.get().defaultBlockState(), x, F + 1, z, box);
                }
                placeBlock(level, s(BaseBlocks.TITANIUM_DECK.base().get()), x, F, z, box);
            }
        }
        // the field's power line runs to the storage module along the ground
        for (int z = 21; z <= 29; z++) {
            placeBlock(level, s(BaseBlocks.CABLE_DUCT.get()), 21, F, z, box);
        }
        // outside pipework: a riser up the gap between hub and storage, then along the hub roof edge
        for (int y = F + 1; y <= F + 6; y++) {
            placeBlock(level, s(BaseBlocks.PIPE_DUCT.get()), 21, y, 12, box);
        }
        for (int x = 11; x <= 21; x++) {
            placeBlock(level, s(BaseBlocks.PIPE_DUCT.get()), x, F + 7, 11, box);
        }
        // landing pad: polished disc, hazard ring, a cross for the touchdown point, lights flush at four points
        for (int x = 1; x <= 9; x++) {
            for (int z = 21; z <= 29; z++) {
                double d = Math.hypot(x - 5, z - 25);
                if (d > 4.6) {
                    continue;
                }
                boolean ring = d > 3.6;
                boolean cross = (x == 5 || z == 25) && d <= 2.1;
                boolean light = ring && (x == 5 || z == 25);
                BlockState st = light ? s(BaseBlocks.LED_PANEL.get())
                        : ring || cross ? s(BaseBlocks.HAZARD_STRIPE.base().get()) : s(MoonModule.POLISHED_ANORTHOSITE.base().get());
                placeBlock(level, st, x, F, z, box);
            }
        }
        // radio mast
        for (int y = F + 1; y <= F + 9; y++) {
            placeBlock(level, y == F + 9 ? s(Blocks.LIGHTNING_ROD) : s(Blocks.IRON_BARS), 20, y, 27, box);
        }
        placeBlock(level, s(RocketryModule.STATION_HULL.get()), 20, F, 27, box);
        placeBlock(level, s(Blocks.DAYLIGHT_DETECTOR), 21, F + 1, 27, box);
    }

    // ------------------------------------------------------------------ extra rooms, trap and puzzle

    /** Crew quarters: three bunks, lockers, a table; the crew left in a hurry. */
    private void furnishQuarters(WorldGenLevel level, BoundingBox box) {
        for (int z : new int[]{2, 4, 6}) {
            bunk(level, box, 2, z);
        }
        container(level, box, null, 7, F + 1, 2, MoonLoot.OUTPOST_QUARTERS, false);
        container(level, box, null, 7, F + 1, 3, MoonLoot.OUTPOST_QUARTERS, false);
        placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.TOP), 6, F + 1, 6, box);
        placeBlock(level, BaseBlocks.ALUMINUM_PANEL.stairs().get().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 5, F + 1, 6, box);
        placeBlock(level, s(Blocks.FLOWER_POT), 6, F + 2, 6, box);
    }

    /** A station bunk against the west wall, a cable tray shelf over it. */
    private void bunk(WorldGenLevel level, BoundingBox box, int x, int z) {
        placeBlock(level, RocketryModule.STATION_BUNK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), x, F + 1, z, box);
        placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.TOP), x, F + 3, z, box);
    }

    /**
     * A greenhouse that lost its air: a pressure-glass roof, two grow beds of dry soil with dead plants under light bars,
     * the irrigation line along them, the water tank frozen solid, and the seed store.
     */
    private void furnishGreenhouse(WorldGenLevel level, BoundingBox box) {
        for (int x = 23; x <= 28; x++) {
            for (int z = 12; z <= 18; z++) {
                placeBlock(level, s(BaseBlocks.PRESSURE_GLASS.get()), x, F + 5, z, box); // glass roof
                placeBlock(level, s(Blocks.AIR), x, F + 6, z, box);                      // no regolith shield on glass
            }
        }
        for (int z : new int[]{13, 17}) {
            for (int x = 23; x <= 28; x++) {
                if (x <= 27) {
                    // the bed: soil in a deck-edged trough, a dead plant in every other cell
                    placeBlock(level, s(Blocks.COARSE_DIRT), x, F + 1, z, box);
                    placeBlock(level, (x & 1) == 0 ? s(Blocks.DEAD_BUSH) : s(Blocks.AIR), x, F + 2, z, box);
                }
                // a light bar from wall to wall above the bed, a grow light every other block
                placeBlock(level, (x & 1) == 1 ? s(BaseBlocks.LED_PANEL.get()) : s(BaseBlocks.CABLE_DUCT.get()), x, F + 4, z, box);
            }
            // the irrigation line along the outer side of the bed
            int side = z == 13 ? 12 : 18;
            for (int x = 23; x <= 27; x++) {
                placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState(), x, F + 1, side, box);
            }
            placeBlock(level, s(BaseBlocks.PIPE_DUCT.get()), 28, F + 1, z, box);
        }
        // the water tank, frozen solid when the heating failed, piped into the beds
        placeBlock(level, s(MoonModule.LUNAR_ICE.get()), 28, F + 1, 15, box);
        placeBlock(level, s(MoonModule.LUNAR_ICE.get()), 28, F + 2, 15, box);
        placeBlock(level, s(BaseBlocks.PRESSURE_GLASS.get()), 28, F + 3, 15, box);
        for (int z : new int[]{14, 16}) {
            placeBlock(level, s(BaseBlocks.PIPE_DUCT.get()), 28, F + 1, z, box);
        }
        container(level, box, null, 28, F + 1, 18, MoonLoot.OUTPOST_GREENHOUSE, false);
        container(level, box, null, 23, F + 1, 15, MoonLoot.OUTPOST_GREENHOUSE, true);
    }

    /** The vault: reinforced walls, no windows, an iron door that only opens with power. */
    private void vault(WorldGenLevel level, BoundingBox box, BlockState frame) {
        BlockState armour = s(MoonModule.ANORTHOSITE_BRICKS.base().get());
        for (int x = 22; x <= 28; x++) {
            for (int z = 2; z <= 8; z++) {
                boolean edge = x == 22 || x == 28 || z == 2 || z == 8;
                placeBlock(level, armour, x, F, z, box);
                for (int y = F + 1; y <= F + 3; y++) {
                    placeBlock(level, edge ? armour : s(Blocks.AIR), x, y, z, box);
                }
                placeBlock(level, armour, x, F + 4, z, box);
            }
        }
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), 25, F + 4, 5, box);
        container(level, box, null, 27, F + 1, 4, MoonLoot.OUTPOST_VAULT, true);
        container(level, box, null, 27, F + 1, 6, MoonLoot.OUTPOST_VAULT, true);
    }

    /**
     * Puzzle and trap: the vault door lost its power. A power cell (block of redstone, kept in the command chest)
     * fits the socket above the door. The approach tunnel is guarded by an arrow trap on a pressure plate.
     */
    private void vaultPuzzle(WorldGenLevel level, BoundingBox box) {
        door(level, box, 22, 5, Direction.EAST);
        placeBlock(level, s(Blocks.AIR), 22, F + 3, 5, box); // empty socket above the door
        // hazard stripes frame the door and its socket
        for (int y = F + 1; y <= F + 3; y++) {
            placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 22, y, 4, box);
            placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 22, y, 6, box);
        }
        // guide lights: set into the lab floor from the hub tunnel to the east doorway, then the tunnel floor and
        // ceiling, so the way to the vault reads from the lab even in the dark
        for (int[] c : new int[][]{{15, 7}, {15, 5}, {17, 5}}) {
            placeBlock(level, s(BaseBlocks.LED_PANEL.get()), c[0], F, c[1], box);
        }
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), 20, F, 5, box);
        placeBlock(level, s(BaseBlocks.LED_PANEL.get()), 20, F + 3, 5, box);
        placeBlock(level, s(BaseBlocks.HAZARD_STRIPE.base().get()), 21, F, 5, box); // under the trap plate
        placeBlock(level, s(Blocks.REDSTONE_LAMP), 22, F + 4, 5, box);
        // warning sign on the tunnel wall
        BlockPos signPos = getWorldPos(21, F + 2, 5);
        BlockState sign = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, Direction.SOUTH);
        if (box.isInside(signPos)) {
            placeBlock(level, sign, 21, F + 2, 5, box);
            if (level.getBlockEntity(signPos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be) {
                net.minecraft.world.level.block.entity.SignText text = new net.minecraft.world.level.block.entity.SignText();
                for (int i = 0; i < 4; i++) {
                    text = text.setMessage(i, net.minecraft.network.chat.Component.translatable("sign.spacebreaker.outpost_vault." + i));
                }
                be.setText(text, true);
            }
        }
        // arrow trap: a dispenser hidden in the tunnel wall, fired by the pressure plate in front of the door
        placeBlock(level, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE), 21, F + 1, 5, box);
        BlockPos disp = getWorldPos(21, F + 1, 4);
        placeBlock(level, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.SOUTH),
                21, F + 1, 4, box);
        if (box.isInside(disp) && level.getBlockEntity(disp) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d) {
            d.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 16));
        }
    }

    /** Time and impacts: holes, dust drifts, broken windows. Fixed by the seed, like the layout. */
    private void decay(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            int y = F + 1 + r.nextInt(5);
            int roll = r.nextInt(6); // drawn every time, so the sequence is the same in every chunk
            BlockState here = getBlock(level, x, y, z, box);
            if (here.is(RocketryModule.STATION_WINDOW.get()) || here.is(BaseBlocks.PRESSURE_GLASS.get())) {
                placeBlock(level, s(Blocks.AIR), x, y, z, box);
            } else if (here.is(BaseBlocks.ALUMINUM_PANEL.base().get()) || here.is(BaseBlocks.CABLE_DUCT.get())) {
                // a broken panel: knocked out, bent half off, or the bare grate behind it
                if (roll < 2) {
                    placeBlock(level, s(Blocks.AIR), x, y, z, box);
                } else if (roll == 2) {
                    placeBlock(level, BaseBlocks.ALUMINUM_PANEL.slab().get().defaultBlockState(), x, y, z, box);
                } else if (roll == 3) {
                    placeBlock(level, s(BaseBlocks.GRATE.get()), x, y, z, box);
                }
            }
        }
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(SIZE);
            int z = r.nextInt(SIZE);
            if (getBlock(level, x, F + 1, z, box).isAir() && !getBlock(level, x, F, z, box).isAir()) {
                placeBlock(level, s(MoonModule.LUNAR_REGOLITH.get()), x, F + 1, z, box);
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox box, @javax.annotation.Nullable RandomSource layout, int x, int y, int z,
                           ResourceKey<LootTable> loot, boolean chest) {
        // loot seeds come from the position, so the shared layout random is consumed the same way in every chunk.
        // Every store is the shared supply locker (it was a chest or a barrel), doors turned toward the middle.
        RandomSource r = RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z));
        com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.place(level, this, x, y, z, IceDrillPiece.inward(x, z, SIZE), loot,
                r.nextLong(), box);
    }

    private void guard(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
