package com.pizzaparker07.spacebreaker.content.venus.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A derelict research aerostat drifting in the Venusian cloud deck. A long envelope of coated fabric in alternating
 * gores with grey frame rings (breathable air is a lifting gas here), cruciform tail fins and two engine pods on
 * pylons; under it, hung on cables, a ribbed gondola with an acid-proof gold band: crew berths at the back, the lab in
 * the middle, a glazed bridge at the bow, and a railed walkway out to the docking point. Abandoned when its acid seals
 * failed: the stern compartment is eaten through and sealed off, sulfur crust grows in the leaks, Acid Rays nest
 * around it.
 */
public class AerostatPiece extends StructurePiece {
    static final int LEN = 33;
    static final int WID = 17;
    static final int HEIGHT = 27;
    /** Gondola floor height, its centre line, first and last ring. */
    private static final int G = 2;
    private static final int CZ = WID / 2;
    private static final int GX0 = 8;
    private static final int GX1 = 22;
    /** The side door out to the walkway, on the +z side. */
    private static final int DOOR_X = 16;
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

    private void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        PieceOrientation.place(level, this, state, x, y, z, box);
    }

    private static double hash(int a, int b, long salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ salt;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        envelope(level, box);
        fins(level, box);
        engines(level, box);
        cables(level, box);
        gondola(level, box);
        walkway(level, box);
        valvePuzzle(level, box);
        rooms(level, box);
        corrosion(level, box);
        guards(level, box);
    }

    // ------------------------------------------------------------------ the envelope

    private static final double EX = 16;
    private static final double EY = 18.5;
    private static final double AX = 15.5;
    private static final double AYZ = 6.6;

    private static double ellipsoid(double x, double y, double z, double shrink) {
        double ax = AX - shrink;
        double ar = AYZ - shrink;
        return sq((x - EX) / ax) + sq((y - EY) / ar) + sq((z - CZ) / ar);
    }

    private static double sq(double v) {
        return v * v;
    }

    /** The envelope: twelve gores in two tones, a grey frame ring every four blocks, a solar film strip along the top. */
    private void envelope(WorldGenLevel level, BoundingBox box) {
        BlockState fabric = s(VenusModule.ENVELOPE_FABRIC.get());
        BlockState gold = s(Blocks.SMOOTH_SANDSTONE);
        BlockState ring = s(Blocks.POLISHED_ANDESITE);
        for (int x = 0; x < LEN; x++) {
            for (int y = 10; y < HEIGHT; y++) {
                for (int z = 0; z < WID; z++) {
                    if (ellipsoid(x, y, z, 0) > 1 || ellipsoid(x, y, z, 1.1) <= 1) {
                        continue;
                    }
                    double theta = Math.atan2(y - EY, z - CZ);
                    int gore = (int) Math.floor((theta + Math.PI) / (Math.PI / 6));
                    BlockState st = gore % 2 == 0 ? fabric : gold;
                    if (Math.floorMod(x - 4, 4) == 0 && x > 2 && x < LEN - 3) {
                        st = ring;
                    }
                    if (y >= EY + AYZ - 1.5 && Math.abs(z - CZ) <= 1 && x >= 9 && x <= 23 && Math.floorMod(x - 4, 4) != 0) {
                        st = s(Blocks.BLUE_TERRACOTTA); // thin-film solar strip
                    }
                    put(level, box, x, y, z, st);
                }
            }
        }
        // the nose cap and the vent valves on top
        put(level, box, LEN - 1, (int) EY, CZ, s(Blocks.POLISHED_ANDESITE));
        for (int x : new int[]{12, 20}) {
            put(level, box, x, (int) (EY + AYZ) + 1, CZ, s(Blocks.LIGHTNING_ROD));
        }
    }

    /** Cruciform tail fins: stepped plates with stair-cut trailing edges. */
    private void fins(WorldGenLevel level, BoundingBox box) {
        BlockState panel = s(VenusModule.COMPOSITE_PANEL.get());
        BlockState edge = s(Blocks.SMOOTH_QUARTZ_STAIRS);
        int cy = (int) Math.round(EY);
        for (int i = 0; i < 5; i++) {
            int x = 1 + i;
            int span = 6 - i;
            for (int k = 1; k <= span; k++) {
                boolean tip = k == span;
                // vertical fin above and below, horizontal fins port and starboard
                put(level, box, x, cy + 4 + k, CZ, tip ? edge.setValue(StairBlock.FACING, Direction.EAST) : panel);
                put(level, box, x, cy - 4 - k, CZ, tip ? edge.setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP) : panel);
                put(level, box, x, cy, CZ + 4 + k, tip ? s(Blocks.SMOOTH_QUARTZ_SLAB) : panel);
                put(level, box, x, cy, CZ - 4 - k, tip ? s(Blocks.SMOOTH_QUARTZ_SLAB) : panel);
            }
        }
    }

    /** Two engine pods on pylons, low on the envelope's flanks, each with a four-bladed fan at the back. */
    private void engines(WorldGenLevel level, BoundingBox box) {
        BlockState body = s(VenusModule.COMPOSITE_PANEL.get());
        for (int side : new int[]{-1, 1}) {
            int z = CZ + side * 7;
            int y = 13;
            for (int x = 11; x <= 14; x++) {
                put(level, box, x, y, z, x == 14 ? s(Blocks.SMOOTH_QUARTZ_SLAB) : body);
            }
            put(level, box, 11, y + 1, z, s(Blocks.POLISHED_ANDESITE_SLAB));
            // the fan: a hub with four blades
            put(level, box, 10, y, z, s(Blocks.IRON_BLOCK));
            put(level, box, 10, y + 1, z, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
            put(level, box, 10, y - 1, z, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
            put(level, box, 10, y, z + 1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
            put(level, box, 10, y, z - 1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
            // the pylon up into the envelope
            for (int k = 1; k <= 2; k++) {
                put(level, box, 13, y, z - side * k, s(Blocks.IRON_BARS).setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
            }
        }
    }

    /** Four cables from the envelope's keel down to the gondola roof. */
    private void cables(WorldGenLevel level, BoundingBox box) {
        for (int x : new int[]{GX0 + 2, GX1 - 2}) {
            for (int dz : new int[]{-1, 1}) {
                for (int y = G + 6; y <= 12; y++) {
                    if (ellipsoid(x, y, CZ + dz, 0) > 1) {
                        put(level, box, x, y, CZ + dz, s(Blocks.CHAIN));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ the gondola

    private static final BlockState HULL = s(Blocks.SMOOTH_QUARTZ);
    private static final BlockState HULL_STAIR = s(Blocks.SMOOTH_QUARTZ_STAIRS);
    private static final BlockState RIB = s(Blocks.POLISHED_ANDESITE);
    private static final BlockState RIB_STAIR = s(Blocks.POLISHED_ANDESITE_STAIRS);

    private static BlockState base(String what) {
        return switch (what) {
            case "panel" -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
            case "led" -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
            case "cable" -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
            case "pipe" -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get());
            case "grate" -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get());
            default -> s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get());
        };
    }

    /**
     * The gondola: an octagonal, ribbed tube along x (half width 2, walls 3 high), a gold acid-proof band low on the
     * walls, windows between the ribs, ducts under the ceiling, and at the bow a glazed bridge.
     */
    private void gondola(WorldGenLevel level, BoundingBox box) {
        BlockState glass = s(VenusModule.AEROGEL.get());
        for (int x = GX0; x <= GX1 + 2; x++) {
            boolean bow = x > GX1;
            boolean cap = x == GX0 || x == GX1 + 2;
            boolean rib = (x - GX0) % 4 == 0 || x == GX0;
            int hw = x == GX1 + 2 ? 1 : 2;
            for (int u = -(hw + 1); u <= hw + 1; u++) {
                int z = CZ + u;
                int au = Math.abs(u);
                Direction in = u < 0 ? Direction.SOUTH : Direction.NORTH;
                BlockState hull = rib && !bow ? RIB : HULL;
                BlockState stair = rib && !bow ? RIB_STAIR : HULL_STAIR;
                for (int v = 0; v <= 5; v++) {
                    int y = G + v;
                    boolean interior = (au <= hw && v >= 1 && v <= 3) || (au <= hw - 1 && v == 4);
                    BlockState st = null;
                    if (v == 0 && au <= hw) {
                        st = cap ? hull : s(Blocks.SMOOTH_STONE);
                    } else if (v == 0 && au == hw + 1) {
                        st = stair.setValue(StairBlock.FACING, in).setValue(StairBlock.HALF, Half.TOP);
                    } else if (interior) {
                        st = cap ? (bow ? glass : hull) : s(Blocks.AIR);
                    } else if (au == hw + 1 && v >= 1 && v <= 3) {
                        boolean window = bow ? v >= 2 : !rib && v == 2 && (x - GX0) % 2 == 0;
                        st = window ? glass : !rib && v == 1 ? s(Blocks.GOLD_BLOCK) : rib ? hull : base("panel");
                    } else if (au == hw + 1 && v == 4) {
                        st = bow ? glass : stair.setValue(StairBlock.FACING, in);
                    } else if (au == hw && v == 4) {
                        st = bow ? glass : hull;
                    } else if (au == hw && v == 5) {
                        st = stair.setValue(StairBlock.FACING, in);
                    } else if (au <= hw - 1 && v == 5) {
                        boolean light = !cap && u == 0 && (x - GX0) % 4 == 2;
                        st = light ? (hash(x, 0, seed) < 0.5 ? base("led") : s(Blocks.REDSTONE_LAMP)) : bow ? glass : hull;
                    }
                    if (st == null && !cap && !bow && v == 4 && au == 1) {
                        st = u < 0 ? base("cable") : base("pipe");
                    }
                    if (st != null) {
                        put(level, box, x, y, z, st);
                    }
                }
            }
        }
        // the stern compartment was sealed off when its seals failed: a bulkhead with an airlock door between them
        int bulk = GX0 + 3;
        for (int u = -2; u <= 2; u++) {
            for (int v = 1; v <= 4; v++) {
                if (v <= 3 || Math.abs(u) <= 1) {
                    put(level, box, bulk, G + v, CZ + u, RIB);
                }
            }
        }
        airlockDoor(level, box, bulk, CZ, Direction.EAST);
    }

    private void airlockDoor(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        BlockState lower = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING, facing)
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.LOWER);
        put(level, box, x, G + 1, z, lower);
        put(level, box, x, G + 2, z, lower.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * The walkway: out of the side door, a railed grating to the docking point. Trap: a plate at its end drops the
     * floor panel before it (an iron trapdoor); a cargo net of cobweb hangs under it, so a careless visitor is caught,
     * not killed, while the acid rays come round.
     */
    private void walkway(WorldGenLevel level, BoundingBox box) {
        int z0 = CZ + 4;
        for (int z = z0; z <= z0 + 4; z++) {
            for (int x = DOOR_X - 1; x <= DOOR_X + 1; x++) {
                if (x == DOOR_X) {
                    put(level, box, x, G, z, base("grate"));
                } else {
                    put(level, box, x, G, z, s(Blocks.SMOOTH_QUARTZ_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
                    put(level, box, x, G + 1, z, base("rail").setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
                }
            }
        }
        // the docking point: a bollard ring at the walkway's end
        put(level, box, DOOR_X, G, z0 + 5, s(Blocks.POLISHED_ANDESITE));
        put(level, box, DOOR_X, G + 1, z0 + 5, s(Blocks.CHAIN));
        int plate = z0 + 4;
        put(level, box, DOOR_X, G + 1, plate, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));
        put(level, box, DOOR_X, G, plate - 1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH));
        put(level, box, DOOR_X, G - 1, plate - 1, s(Blocks.COBWEB));
        put(level, box, DOOR_X, G - 2, plate - 1, s(VenusModule.ENVELOPE_FABRIC.get()));
        for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            put(level, box, DOOR_X + d[0], G - 1, plate - 1 + d[1], s(Blocks.IRON_BARS));
        }
    }

    /**
     * Puzzle: the side door is sealed. Three gas valves (levers) above it on the outside; only the one under the
     * sulfur-yellow mark is wired to the door. The sign tells which. The wrong ones do nothing.
     */
    private void valvePuzzle(WorldGenLevel level, BoundingBox box) {
        int wall = CZ + 3;
        airlockDoor(level, box, DOOR_X, wall, Direction.SOUTH);
        BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL);
        int[][] marks = {{DOOR_X - 2, 0}, {DOOR_X, 1}, {DOOR_X + 2, 2}};
        for (int[] m : marks) {
            put(level, box, m[0], G + 3, wall, HULL); // solid wall for the valve to sit on
            PieceOrientation.placeToward(level, this, lever, new BlockPos(m[0], G + 3, wall + 1), new BlockPos(m[0], G + 3, wall), true, box);
            put(level, box, m[0], G + 4, wall, m[1] == 1 ? s(VenusModule.SULFUR_CRUST.get()) : m[1] == 0 ? s(Blocks.CYAN_TERRACOTTA) : s(Blocks.RED_TERRACOTTA));
        }
        BlockPos sign = PieceOrientation.placeToward(level, this, Blocks.BIRCH_WALL_SIGN.defaultBlockState(),
                new BlockPos(DOOR_X - 1, G + 2, wall + 1), new BlockPos(DOOR_X - 1, G + 2, wall), true, box);
        if (sign != null) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, sign, "aerostat_valve");
        }
    }

    /** Berths at the back, the lab amidships, the bridge at the bow, lockers set into the walls. */
    private void rooms(WorldGenLevel level, BoundingBox box) {
        int n = CZ - 2;
        int s = CZ + 2;
        // crew berths, just forward of the sealed stern
        for (int x = GX0 + 4; x <= GX0 + 5; x++) {
            put(level, box, x, G + 1, n, s(Blocks.WHITE_WOOL));
            put(level, box, x, G + 2, n, s(Blocks.SMOOTH_QUARTZ_SLAB));
            put(level, box, x, G + 3, n, s(Blocks.LIGHT_GRAY_WOOL));
        }
        SupplyLocker.place(level, this, GX0 + 5, G + 1, s + 1, Direction.NORTH, VenusLoot.AEROSTAT_STORES, seed + 2, box);
        // the lab: the research bench, the atmosphere sampler, a sample shelf
        put(level, box, GX0 + 7, G + 1, n, s(com.pizzaparker07.spacebreaker.content.research.ResearchModule.RESEARCH_LAB.get())
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, GX0 + 9, G + 1, n, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.ATMOSPHERE_PROCESSOR.block().get())
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, GX0 + 8, G + 1, n, s(Blocks.POLISHED_ANDESITE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        put(level, box, GX0 + 8, G + 2, n, s(Blocks.BREWING_STAND));
        put(level, box, GX0 + 10, G + 1, n, s(Blocks.POLISHED_ANDESITE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        put(level, box, GX0 + 10, G + 2, n, s(VenusModule.SULFUR_CRUST.get())); // a sample of the crust that killed it
        SupplyLocker.place(level, this, GX0 + 8, G + 1, n - 1, Direction.SOUTH, VenusLoot.AEROSTAT_LAB, seed + 1, box);
        // the bridge: consoles under the bow glazing, the nav table, the last log
        for (int z = n; z <= s; z += 2) {
            put(level, box, GX1 + 1, G + 1, z, s(Blocks.POLISHED_ANDESITE));
            put(level, box, GX1 + 1, G + 2, z, z == CZ ? s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get())
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST) : s(Blocks.OBSERVER).setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.WEST));
        }
        put(level, box, GX1 - 1, G + 1, CZ, s(Blocks.CARTOGRAPHY_TABLE));
        BlockPos log = PieceOrientation.placeToward(level, this, Blocks.BIRCH_WALL_SIGN.defaultBlockState(),
                new BlockPos(GX1 - 1, G + 2, n), new BlockPos(GX1 - 1, G + 2, n - 1), true, box);
        if (log != null) {
            com.pizzaparker07.spacebreaker.content.mars.dungeon.DungeonBits.signText(level, log, "aerostat_log");
        }
    }

    /** The sealed stern: acid has eaten through the hull, sulfur crust grows in the leaks; corroded panels elsewhere. */
    private void corrosion(WorldGenLevel level, BoundingBox box) {
        BlockState crust = s(VenusModule.SULFUR_CRUST.get());
        for (int x = GX0; x < GX0 + 3; x++) {
            for (int u = -3; u <= 3; u++) {
                for (int v = 0; v <= 5; v++) {
                    double h = hash(x * 7 + u, v, seed + 9);
                    BlockPos p = PieceOrientation.worldPos(this, x, G + v, CZ + u);
                    if (!box.isInside(p) || level.getBlockState(p).isAir()) {
                        continue;
                    }
                    if (h < 0.22) {
                        put(level, box, x, G + v, CZ + u, s(Blocks.AIR));
                    } else if (h < 0.45) {
                        put(level, box, x, G + v, CZ + u, crust);
                    }
                }
            }
        }
        // a few corroded panels along the rest of the hull
        for (int i = 0; i < 4; i++) {
            int x = GX0 + 4 + (int) (hash(i, 1, seed) * (GX1 - GX0 - 4));
            int u = hash(i, 2, seed) < 0.5 ? -3 : 3;
            int v = 1 + (int) (hash(i, 3, seed) * 3);
            if (x == DOOR_X && u > 0) {
                continue;
            }
            put(level, box, x, G + v, CZ + u, crust);
        }
    }

    private void guards(WorldGenLevel level, BoundingBox box) {
        for (int[] g : new int[][]{{4, 4, 2}, {26, 5, 14}}) {
            BlockPos pos = PieceOrientation.worldPos(this, g[0], g[1], g[2]);
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
