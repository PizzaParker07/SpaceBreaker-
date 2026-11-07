package com.pizzaparker07.spacebreaker.content.mars.dungeon;

import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned Mars research base. A domed hub (command deck, bunks) with four pressurised tubes: a hydroponic
 * greenhouse to the north, the laboratory and ISRU control to the east (the only section still on reserve power, so its
 * lights still burn), the rover garage to the west and the airlock to the south. Outside: a solar array whose cables run
 * to the hub, a pipe bridge to the propellant tanks, a landing pad with a toppled cargo sled and a core-drill rig.
 * <p>
 * Each module sits on the ground where it stands: regolith berms under it, no levelled pad. A dust storm from the west
 * broke into the garage long ago; dust has drifted against every west wall and half fills the garage.
 */
public class ResearchStationPiece extends StructurePiece {
    static final int SIZE = 41;
    static final int HEIGHT = 18;
    /** Local height of the module floors (the piece is placed this far below its floor). */
    private static final int F = 7;
    private static final int C = SIZE / 2;
    /** Hub dome: radius and the rise of the dome above the walls. */
    private static final double HUB_R = 6.2;
    private static final double HUB_RISE = 4.2;
    private static final int HUB_WALL = 3;
    private final long seed;
    /** Local height of the ground (top solid block) per column of the chunk being built; MIN_VALUE outside it. */
    private int[][] ground;

    public ResearchStationPiece(long seed, int x, int y, int z, Direction facing) {
        super(MarsStructures.STATION_PIECE.get(), 0, new BoundingBox(x, y - F, z, x + SIZE - 1, y - F + HEIGHT + F, z + SIZE - 1));
        this.seed = seed;
        setOrientation(facing);
    }

    public ResearchStationPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(MarsStructures.STATION_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    // ------------------------------------------------------------------ layout (pure geometry, the same in every chunk)

    /** A pressurised tube: along Z (x = c + u) or along X (z = c + u), from a0 to a1, half width hw, wall height wh. */
    private record Tube(boolean alongZ, int a0, int a1, int c, int hw, int wh) {
        int x(int a, int u) {
            return alongZ ? c + u : a;
        }

        int z(int a, int u) {
            return alongZ ? a : c + u;
        }

        boolean covers(int x, int z) {
            int a = alongZ ? z : x;
            int u = (alongZ ? x : z) - c;
            return a >= a0 && a <= a1 && Math.abs(u) <= hw + 1;
        }
    }

    private static final Tube GREENHOUSE = new Tube(true, 3, 11, C, 2, 3);
    private static final Tube LAB = new Tube(false, 29, 37, C, 2, 3);
    private static final Tube GARAGE = new Tube(false, 3, 11, C, 2, 3);
    private static final Tube AIRLOCK = new Tube(true, 29, 34, C, 2, 3);
    private static final Tube[] ARMS = {GREENHOUSE, LAB, GARAGE, AIRLOCK};
    private static final Tube[] LINKS = {
            new Tube(true, 12, 13, C, 1, 2), new Tube(false, 27, 28, C, 1, 2),
            new Tube(false, 12, 13, C, 1, 2), new Tube(true, 27, 28, C, 1, 2)};

    private static double hubDist(int x, int z) {
        return Math.sqrt((x - C) * (x - C) + (z - C) * (z - C));
    }

    /** Whether a module stands on this column. */
    static boolean footprint(int x, int z) {
        if (hubDist(x, z) <= HUB_R) {
            return true;
        }
        for (Tube t : ARMS) {
            if (t.covers(x, z)) {
                return true;
            }
        }
        for (Tube t : LINKS) {
            if (t.covers(x, z)) {
                return true;
            }
        }
        return false;
    }

    private static double hash(int x, int z, long salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    // ------------------------------------------------------------------ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground = new int[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos w = PieceOrientation.worldPos(this, x, 0, z);
                ground[x][z] = box.isInside(w.getX(), box.minY(), w.getZ())
                        ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, w.getX(), w.getZ()) - 1 - boundingBox.minY()
                        : Integer.MIN_VALUE;
            }
        }
        RandomSource r = RandomSource.create(seed);
        berms(level, box);
        hub(level, box);
        tube(level, box, GREENHOUSE, true, false);
        tube(level, box, LAB, true, true);
        tube(level, box, GARAGE, true, false);
        tube(level, box, AIRLOCK, true, true);
        for (Tube link : LINKS) {
            tube(level, box, link, false, false);
        }
        for (Tube link : LINKS) {
            carveLink(level, box, link);
        }
        airlock(level, box);
        command(level, box, r);
        greenhouse(level, box, r);
        lab(level, box, r);
        garage(level, box, r);
        solarArray(level, box, r);
        propellantPlant(level, box);
        landingPad(level, box);
        drillRig(level, box);
        drifts(level, box);
    }

    private void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        PieceOrientation.place(level, this, state, x, y, z, box);
    }

    private BlockState at(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos p = PieceOrientation.worldPos(this, x, y, z);
        return box.isInside(p) ? level.getBlockState(p) : s(Blocks.VOID_AIR);
    }

    private boolean known(int x, int z) {
        return x >= 0 && z >= 0 && x < SIZE && z < SIZE && ground[x][z] != Integer.MIN_VALUE;
    }

    /** Where the ground lies more than this far below a floor, the module stands on legs instead of a berm. */
    private static final int BERM_MAX = 3;

    /**
     * What holds the modules up: a low regolith berm where the ground is close below the floor, sloping away one block
     * per block; where the ground falls away, grey legs every four blocks down to the rock.
     */
    private void berms(WorldGenLevel level, BoundingBox box) {
        BlockState regolith = s(MarsModule.MARTIAN_REGOLITH.get());
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (!known(x, z)) {
                    continue;
                }
                int g = ground[x][z];
                if (footprint(x, z)) {
                    if (g >= F - 1 - BERM_MAX) {
                        for (int y = g + 1; y <= F - 1; y++) {
                            put(level, box, x, y, z, regolith);
                        }
                    } else if (Math.floorMod(x - C, 4) == 0 && Math.floorMod(z - C, 4) == 0) {
                        for (int y = Math.max(0, g + 1); y <= F - 1; y++) {
                            put(level, box, x, y, z, RIB);
                        }
                    }
                    continue;
                }
                int d = 4;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (footprint(x + dx, z + dz)) {
                            d = Math.min(d, Math.max(Math.abs(dx), Math.abs(dz)));
                        }
                    }
                }
                int top = F - 1 - d;
                if (d < 4 && g >= F - 1 - BERM_MAX - d) {
                    for (int y = g + 1; y <= top; y++) {
                        put(level, box, x, y, z, regolith);
                    }
                }
            }
        }
    }

    // hull: smooth off-white panels; ribs: grey frames every fourth ring; a thin orange band low on the walls
    private static final BlockState HULL = s(Blocks.SMOOTH_QUARTZ);
    private static final BlockState HULL_STAIR = s(Blocks.SMOOTH_QUARTZ_STAIRS);
    private static final BlockState RIB = s(Blocks.POLISHED_ANDESITE);
    private static final BlockState RIB_STAIR = s(Blocks.POLISHED_ANDESITE_STAIRS);
    private static final BlockState FLOOR = s(Blocks.SMOOTH_STONE);
    /** Inside: brushed aluminium wall panels, ducts along the ceiling, LED panels (and dead lamps). */
    private static BlockState panel() {
        return s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get());
    }

    private static BlockState led() {
        return s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get());
    }

    private static BlockState cableDuct() {
        return s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get());
    }

    private static BlockState pipeDuct() {
        return s(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get());
    }

    private static BlockState pane(boolean alongZ) {
        BlockState p = s(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
        return alongZ ? p.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                : p.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
    }

    /**
     * Builds a tube with an octagonal cross-section (stair-cut shoulders and keel), ribbed every four blocks, windows
     * between the ribs, and ceiling lights. The greenhouse gets a glass vault; {@code lit}: lights still on.
     */
    private void tube(WorldGenLevel level, BoundingBox box, Tube t, boolean caps, boolean lit) {
        boolean greenhouse = t == GREENHOUSE;
        int hw = t.hw();
        int wh = t.wh();
        for (int a = t.a0(); a <= t.a1(); a++) {
            boolean cap = caps && (a == t.a0() || a == t.a1());
            boolean rib = (a - t.a0()) % 4 == 0 || cap;
            BlockState hull = rib ? RIB : HULL;
            BlockState stair = rib ? RIB_STAIR : HULL_STAIR;
            for (int u = -(hw + 1); u <= hw + 1; u++) {
                Direction in = t.alongZ() ? (u < 0 ? Direction.EAST : Direction.WEST) : (u < 0 ? Direction.SOUTH : Direction.NORTH);
                int x = t.x(a, u);
                int z = t.z(a, u);
                int au = Math.abs(u);
                for (int v = 0; v <= wh + 2; v++) {
                    int y = F + v;
                    boolean interior = (au <= hw && v >= 1 && v <= wh) || (au <= hw - 1 && v == wh + 1);
                    BlockState st = null;
                    if (v == 0 && au <= hw) {
                        st = cap ? hull : FLOOR;
                    } else if (v == 0 && au == hw + 1) {
                        st = stair.setValue(StairBlock.FACING, in).setValue(StairBlock.HALF, Half.TOP);
                    } else if (interior) {
                        st = cap ? hull : s(Blocks.AIR);
                    } else if (au == hw + 1 && v >= 1 && v <= wh) {
                        boolean window = !rib && v == 2 && (a - t.a0()) % 2 == 0;
                        st = window ? pane(t.alongZ()) : (!rib && v == 1 && hw > 1 ? s(Blocks.ORANGE_TERRACOTTA) : rib ? hull : panel());
                    } else if (au == hw + 1 && v == wh + 1) {
                        st = greenhouse && !rib ? s(Blocks.GLASS) : stair.setValue(StairBlock.FACING, in);
                    } else if (au == hw && v == wh + 1) {
                        st = greenhouse && !rib ? s(Blocks.GLASS) : hull;
                    } else if (au == hw && v == wh + 2) {
                        st = greenhouse && !rib ? s(Blocks.GLASS) : stair.setValue(StairBlock.FACING, in);
                    } else if (au <= hw - 1 && v == wh + 2) {
                        boolean light = !cap && u == 0 && (a - t.a0()) % 4 == 2 && hw > 1;
                        if (light) {
                            st = lit || hash(a, t.c(), seed) < 0.25 ? led() : s(Blocks.REDSTONE_LAMP);
                        } else {
                            st = greenhouse && !rib ? s(Blocks.GLASS) : hull;
                        }
                    }
                    if (st == null && !cap && !greenhouse && hw > 1 && v == wh + 1 && au == 1) {
                        st = u < 0 ? cableDuct() : pipeDuct(); // the duct runs along the ceiling, cables left, pipes right
                    }
                    if (st != null) {
                        put(level, box, x, y, z, st);
                    }
                }
            }
        }
    }

    /** Opens a link tube through the hub wall and the arm's end cap: a two-high doorway. */
    private void carveLink(WorldGenLevel level, BoundingBox box, Tube t) {
        for (int a = t.a0() - 2; a <= t.a1() + 2; a++) {
            for (int u = -1; u <= 1; u++) {
                for (int v = 1; v <= 2; v++) {
                    put(level, box, t.x(a, u), F + v, t.z(a, u), s(Blocks.AIR));
                }
                put(level, box, t.x(a, u), F, t.z(a, u), FLOOR);
            }
        }
    }

    /** The hub: a ribbed dome on a low drum, windows round the drum, a ring of lights under the crown. */
    private void hub(WorldGenLevel level, BoundingBox box) {
        int r = (int) Math.ceil(HUB_R);
        for (int x = C - r; x <= C + r; x++) {
            for (int z = C - r; z <= C + r; z++) {
                double d = hubDist(x, z);
                if (d > HUB_R) {
                    continue;
                }
                boolean meridian = x == C || z == C || Math.abs(x - C) == Math.abs(z - C);
                put(level, box, x, F, z, d > HUB_R - 1 ? RIB : FLOOR);
                for (int y = F + 1; y <= F + HUB_WALL + HUB_RISE + 1; y++) {
                    boolean solid = solidDome(d, y, HUB_R, HUB_RISE);
                    boolean inside = y > F && solidDome(d, y, HUB_R - 1, HUB_RISE - 1);
                    if (inside) {
                        put(level, box, x, y, z, s(Blocks.AIR));
                    } else if (solid) {
                        BlockState st = meridian || y == F + HUB_WALL ? RIB : HULL;
                        if (y == F + 2 && !meridian && (x + z) % 2 == 0 && d > HUB_R - 1.2) {
                            st = s(Blocks.LIGHT_GRAY_STAINED_GLASS);
                        }
                        put(level, box, x, y, z, st);
                    }
                }
            }
        }
        // lights under the crown: two of the four still burn
        int top = F + HUB_WALL + (int) HUB_RISE - 1;
        put(level, box, C + 2, top, C, led());
        put(level, box, C - 2, top, C, s(Blocks.REDSTONE_LAMP));
        put(level, box, C, top, C + 2, led());
        put(level, box, C, top, C - 2, s(Blocks.REDSTONE_LAMP));
        // the antenna mast on the crown, its dish turned to the sky
        int crown = F + HUB_WALL + (int) Math.round(HUB_RISE) + 1;
        for (int y = crown; y <= crown + 4; y++) {
            put(level, box, C, y, C, s(Blocks.IRON_BARS));
        }
        put(level, box, C, crown + 5, C, s(Blocks.DAYLIGHT_DETECTOR));
        for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            put(level, box, C + d[0], crown + 5, C + d[1], s(Blocks.SMOOTH_QUARTZ_SLAB));
        }
        put(level, box, C, crown + 6, C, s(Blocks.LIGHTNING_ROD));
    }

    private static boolean solidDome(double d, int y, double radius, double rise) {
        double h = y - F - HUB_WALL;
        if (h <= 0) {
            return d <= radius;
        }
        return (d * d) / (radius * radius) + (h * h) / (rise * rise) <= 1;
    }

    // ------------------------------------------------------------------ rooms

    private void airlock(WorldGenLevel level, BoundingBox box) {
        // the inner door closes the link into the airlock; the outer door is in the far end cap
        int inner = AIRLOCK.a0();
        int outer = AIRLOCK.a1();
        for (int u = -1; u <= 1; u++) {
            for (int v = 1; v <= 3; v++) {
                if (u != 0 || v == 3) {
                    put(level, box, C + u, F + v, inner, RIB);
                }
            }
        }
        put(level, box, C, F + 1, outer, s(Blocks.AIR));
        put(level, box, C, F + 2, outer, s(Blocks.AIR));
        door(level, box, C, inner);
        door(level, box, C, outer);
        PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON), new BlockPos(C + 1, F + 2, inner - 1), new BlockPos(C + 1, F + 2, inner), true, box);
        PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON), new BlockPos(C - 1, F + 2, inner + 1), new BlockPos(C - 1, F + 2, inner), true, box);
        PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON), new BlockPos(C - 1, F + 2, outer - 1), new BlockPos(C - 1, F + 2, outer), true, box);
        PieceOrientation.placeToward(level, this, s(Blocks.STONE_BUTTON), new BlockPos(C + 1, F + 2, outer + 1), new BlockPos(C + 1, F + 2, outer), true, box);
        // hazard stripes on the airlock floor; suit lockers on both sides
        for (int z = inner + 1; z < outer; z++) {
            put(level, box, C, F, z, (z % 2 == 0) ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.BLACK_TERRACOTTA));
            for (int u : new int[]{-2, 2}) {
                if (z == inner + 2 || z == inner + 3) {
                    put(level, box, C + u, F + 1, z, s(Blocks.IRON_BLOCK));
                    put(level, box, C + u, F + 2, z, Blocks.IRON_TRAPDOOR.defaultBlockState()
                            .setValue(TrapDoorBlock.FACING, u < 0 ? Direction.EAST : Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
                }
            }
        }
        // the steps down to the ground outside
        int g = known(C, outer + 1) ? ground[C][outer + 1] : F - 1;
        for (int z = outer + 1, y = F; z < SIZE && y > g; z++, y--) {
            for (int u = -1; u <= 1; u++) {
                put(level, box, C + u, y, z, s(Blocks.POLISHED_ANDESITE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
                put(level, box, C + u, y + 1, z, s(Blocks.AIR));
                put(level, box, C + u, y + 2, z, s(Blocks.AIR));
            }
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockState lower = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING, Direction.SOUTH)
                .setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.LOWER);
        PieceOrientation.place(level, this, lower, x, F + 1, z, box);
        PieceOrientation.place(level, this, lower.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER), x, F + 2, z, box);
    }

    /** Hub floor: the survey table at the centre, consoles on the north-east, bunks on the south-west, the last log. */
    private void command(WorldGenLevel level, BoundingBox box, RandomSource r) {
        put(level, box, C, F + 1, C, s(Blocks.CARTOGRAPHY_TABLE));
        // consoles: a quarter ring between the lab and greenhouse doors
        for (int[] p : new int[][]{{C + 2, C - 4}, {C + 3, C - 3}, {C + 4, C - 2}}) {
            put(level, box, p[0], F + 1, p[1], s(Blocks.POLISHED_ANDESITE));
            put(level, box, p[0], F + 2, p[1], (p[0] + p[1]) % 2 == 0 ? s(Blocks.OBSERVER) : s(Blocks.CRAFTER));
        }
        put(level, box, C + 2, F + 1, C - 2, s(Blocks.LECTERN));
        // bunks: two-high berths against the south-west wall
        for (int[] p : new int[][]{{C - 4, C + 2}, {C - 3, C + 3}, {C - 2, C + 4}}) {
            put(level, box, p[0], F + 1, p[1], s(Blocks.WHITE_WOOL));
            put(level, box, p[0], F + 2, p[1], s(Blocks.SMOOTH_QUARTZ_SLAB));
            put(level, box, p[0], F + 3, p[1], s(Blocks.LIGHT_GRAY_WOOL));
        }
        container(level, box, C + 4, F + 1, C + 4, MarsLoot.STATION_COMMAND, true); // a locker set into the drum wall
        put(level, box, C - 3, F + 1, C - 3, s(Blocks.WATER_CAULDRON));
        put(level, box, C - 4, F + 1, C - 2, s(com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_DISTRIBUTOR.get()));
        // the last log, on a stand by the table
        BlockPos sign = PieceOrientation.place(level, this, s(Blocks.BIRCH_SIGN).setValue(StandingSignBlock.ROTATION, 0), C, F + 1, C + 2, box);
        if (sign != null) {
            DungeonBits.signText(level, sign, "station_log");
        }
        guard(level, box, com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities.SURVEY_DRONE.get(), C - 2, F + 3, C - 2);
    }

    /** The greenhouse: planter beds with the dead crop on both sides, grow lights dark, a water tank at the end. */
    private void greenhouse(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int z = GREENHOUSE.a0() + 1; z < GREENHOUSE.a1(); z++) {
            for (int u : new int[]{-2, 2}) {
                put(level, box, C + u, F, z, s(Blocks.COARSE_DIRT));
                if (hash(u, z, seed) < 0.7) {
                    put(level, box, C + u, F + 1, z, s(Blocks.DEAD_BUSH));
                }
                // the grow-light rack above the bed
                put(level, box, C + u, F + 3, z, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP));
            }
        }
        container(level, box, C + 3, F + 1, GREENHOUSE.a0() + 2, MarsLoot.STATION_LAB, true);
        put(level, box, C - 2, F + 1, GREENHOUSE.a0() + 1, s(Blocks.COMPOSTER));
        put(level, box, C, F + 1, GREENHOUSE.a0() + 1, s(Blocks.CAULDRON));
    }

    /** The laboratory, still on reserve power: benches, a sample analyser, core racks; ISRU control at the far end. */
    private void lab(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int a = LAB.a0();
        for (int x = a + 1; x <= a + 5; x++) {
            put(level, box, x, F + 1, C - 2, s(Blocks.POLISHED_ANDESITE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        put(level, box, a + 1, F + 2, C - 2, s(Blocks.BREWING_STAND));
        put(level, box, a + 3, F + 2, C - 2, s(Blocks.DECORATED_POT));
        put(level, box, a + 5, F + 2, C - 2, s(Blocks.FLOWER_POT));
        // the research lab bench and the power monitor still showing the reserve draining
        put(level, box, a + 2, F + 1, C - 2, s(com.pizzaparker07.spacebreaker.content.research.ResearchModule.RESEARCH_LAB.get())
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, a + 2, F + 2, C - 2, s(Blocks.AIR));
        put(level, box, a + 4, F + 2, C - 3, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get())
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        // core racks: drilled cores of the ground below, layer by layer
        BlockState[] cores = {s(MarsModule.MARTIAN_BASALT.base().get()), s(MarsModule.MARTIAN_SANDSTONE.base().get()), s(MarsModule.MARTIAN_CLAY.get())};
        for (int i = 0; i < 3; i++) {
            put(level, box, a + 1 + i, F + 1, C + 2, cores[i]);
            put(level, box, a + 1 + i, F + 2, C + 2, cores[2 - i]);
            put(level, box, a + 1 + i, F + 3, C + 2, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP));
        }
        container(level, box, a + 4, F + 1, C + 3, MarsLoot.STATION_LAB, true);
        put(level, box, a + 5, F + 1, C + 2, s(MarsModule.MARTIAN_ICE.get()));
        put(level, box, a + 5, F + 2, C + 2, s(MarsModule.DRY_ICE.get()));
        // ISRU control: the Sabatier reactor, electrolysis cell, the propellant log and stores
        int e = LAB.a1() - 1;
        put(level, box, e, F + 1, C - 2, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SABATIER_REACTOR.block().get())
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, e - 2, F + 1, C - 2, s(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.ATMOSPHERE_PROCESSOR.block().get())
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, e - 1, F + 1, C - 2, s(Blocks.CAULDRON));
        container(level, box, e, F + 1, C + 3, MarsLoot.STATION_ISRU, true);
        container(level, box, e - 1, F + 1, C + 3, MarsLoot.STATION_STORAGE, true);
        container(level, box, e - 3, F + 1, C + 3, MarsLoot.STATION_STORAGE, true);
    }

    /** The garage: the storm broke in through the far end; dust half fills it, rust beetles nest in the rover wreck. */
    private void garage(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int a = GARAGE.a0();
        // the breach: a torn end cap and a split in the north wall
        for (int u = -2; u <= 2; u++) {
            for (int v = 1; v <= 4; v++) {
                if (Math.abs(u) + v <= 4 || hash(u, v, seed + 3) < 0.4) {
                    put(level, box, a, F + v, C + u, s(Blocks.AIR));
                }
            }
        }
        for (int x = a + 1; x <= a + 2; x++) {
            put(level, box, x, F + 2, C - 3, s(Blocks.AIR));
            put(level, box, x, F + 3, C - 3, s(Blocks.AIR));
        }
        // the dust that came in: a slope from the breach
        for (int x = a; x <= a + 3; x++) {
            int depth = Math.max(0, 3 - (x - a));
            for (int u = -2; u <= 2; u++) {
                int dd = depth - (hash(x, u, seed + 5) < 0.3 ? 1 : 0);
                for (int v = 1; v <= dd; v++) {
                    put(level, box, x, F + v, C + u, s(MarsModule.MARTIAN_REGOLITH.get()));
                }
            }
        }
        // the rover, parked against the north wall, its front wheels in the dust; rust beetles nest under it
        for (int x = a + 2; x <= a + 5; x++) {
            for (int u = -2; u <= -1; u++) {
                put(level, box, x, F + 2, C + u, s(Blocks.LIGHT_GRAY_CONCRETE));
            }
        }
        for (int x : new int[]{a + 2, a + 5}) {
            put(level, box, x, F + 1, C - 2, s(Blocks.BLACK_CONCRETE));
            put(level, box, x, F + 1, C - 1, s(Blocks.BLACK_CONCRETE));
        }
        put(level, box, a + 3, F + 3, C - 1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        put(level, box, a + 5, F + 3, C - 2, s(MachinesModule.SOLAR_PANEL.get()));
        put(level, box, a + 4, F + 1, C - 1, s(Blocks.SPAWNER));
        BlockPos sp = PieceOrientation.worldPos(this, a + 4, F + 1, C - 1);
        if (box.isInside(sp) && level.getBlockEntity(sp) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(MarsEntities.RUST_BEETLE.get(), RandomSource.create(seed + 7));
        }
        container(level, box, GARAGE.a1() - 1, F + 1, C + 3, MarsLoot.STATION_STORAGE, true);
        // trap: the perimeter defence still has power; a plate in the garage link fires a dispenser in the link wall
        int link = GARAGE.a1() + 2;
        put(level, box, link, F + 1, C, s(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));
        BlockPos disp = PieceOrientation.placeToward(level, this, s(Blocks.DISPENSER),
                new BlockPos(link, F + 1, C - 2), new BlockPos(link, F + 1, C - 1), false, box);
        if (disp != null) {
            DungeonBits.load(level, disp, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 12));
        }
        guard(level, box, com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities.SURVEY_DRONE.get(), a + 7, F + 2, C);
    }

    // ------------------------------------------------------------------ outside

    private int groundAt(int x, int z) {
        return known(x, z) ? ground[x][z] : F - 1;
    }

    /** Rows of tilted panels south-east of the hub; dust has buried the windward ends; a cable runs to the hub. */
    private void solarArray(WorldGenLevel level, BoundingBox box, RandomSource r) {
        for (int z = 29; z <= 38; z += 3) {
            for (int x = 27; x <= 39; x++) {
                if (!known(x, z)) {
                    continue;
                }
                int g = ground[x][z];
                if (Math.abs(g - (F - 1)) > 3) {
                    continue;
                }
                put(level, box, x, g + 1, z, s(Blocks.IRON_BARS));
                boolean buried = x <= 28 + (z - 29) / 3 && hash(x, z, seed + 11) < 0.8;
                put(level, box, x, g + 2, z, buried ? s(MarsModule.MARTIAN_REGOLITH.get()) : s(MachinesModule.SOLAR_PANEL.get()));
                if (buried) {
                    put(level, box, x, g + 1, z, s(MarsModule.MARTIAN_REGOLITH.get()));
                }
            }
        }
        // the cable: along the array's edge and up to the hub wall
        for (int z = 39; z >= 26; z--) {
            if (known(26, z)) {
                put(level, box, 26, ground[26][z] + 1, z, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
            }
        }
    }

    /** Propellant tanks north-east of the lab, fed by a pipe bridge from the ISRU plant. */
    private void propellantPlant(WorldGenLevel level, BoundingBox box) {
        int[][] tanks = {{31, 7}, {35, 7}};
        for (int i = 0; i < tanks.length; i++) {
            int tx = tanks[i][0];
            int tz = tanks[i][1];
            int base = Math.max(groundAt(tx, tz), F - 1);
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    int g = groundAt(tx + dx, tz + dz);
                    BlockState support = base - g > BERM_MAX ? s(Blocks.IRON_BARS) : s(MarsModule.POLISHED_MARTIAN_BASALT.base().get());
                    for (int y = Math.max(0, g + 1); y <= base; y++) {
                        put(level, box, tx + dx, y, tz + dz, support);
                    }
                    for (int y = base + 1; y <= base + 5; y++) {
                        put(level, box, tx + dx, y, tz + dz, i == 0 ? s(Blocks.IRON_BLOCK) : s(Blocks.WAXED_COPPER_BLOCK));
                    }
                    put(level, box, tx + dx, base + 6, tz + dz, s(Blocks.SMOOTH_STONE_SLAB));
                }
            }
        }
        // the pipe bridge: from the lab roof north to the tanks, on lattice posts
        int py = F + 6;
        int px = 33;
        for (int z = LAB.c() - 3; z >= 8; z--) {
            put(level, box, px, py, z, s(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.NORTH));
            if (z % 4 == 0 && z < LAB.c() - 3) {
                for (int y = groundAt(px, z) + 1; y < py; y++) {
                    put(level, box, px, y, z, s(Blocks.IRON_BARS));
                }
            }
        }
        for (int x = 31; x <= 36; x++) {
            if (x != px) {
                put(level, box, x, py, 8, s(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.EAST));
            }
        }
    }

    /** A landing pad north-west of the hub, levelled into the slope, and the cargo sled that tipped over beside it. */
    private void landingPad(WorldGenLevel level, BoundingBox box) {
        int cx = 8;
        int cz = 8;
        int padY = F - 1;
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (d > 5.3 || !known(x, z)) {
                    continue;
                }
                int g = ground[x][z];
                boolean leg = Math.floorMod(x - cx, 3) == 0 && Math.floorMod(z - cz, 3) == 0;
                if (g >= padY - 2 || leg) {
                    for (int y = Math.max(0, g + 1); y < padY; y++) {
                        put(level, box, x, y, z, g >= padY - 2 ? s(MarsModule.MARTIAN_REGOLITH.get()) : RIB);
                    }
                }
                boolean ring = d >= 3.3 && d < 4.3;
                put(level, box, x, padY, z, ring ? ((x + z) % 2 == 0 ? s(Blocks.ORANGE_TERRACOTTA) : s(Blocks.WHITE_CONCRETE))
                        : s(MarsModule.POLISHED_MARTIAN_BASALT.base().get()));
                for (int y = padY + 1; y <= padY + 6; y++) {
                    put(level, box, x, y, z, s(Blocks.AIR));
                }
            }
        }
        // the sled on its side: a frame and a spilled crate
        int sx = 14;
        int sz = 3;
        int g = groundAt(sx, sz);
        for (int x = sx; x <= sx + 2; x++) {
            put(level, box, x, g + 1, sz, s(Blocks.LIGHT_GRAY_CONCRETE));
        }
        put(level, box, sx + 1, g + 2, sz, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        put(level, box, sx + 3, groundAt(sx + 3, sz + 1) + 1, sz + 1, s(Blocks.BARREL));
    }

    /** The core-drill rig south-west of the hub: a lattice tower over a cased hole. */
    private void drillRig(WorldGenLevel level, BoundingBox box) {
        int cx = 6;
        int cz = 33;
        int g = groundAt(cx, cz);
        for (int[] o : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            int gx = groundAt(cx + o[0], cz + o[1]);
            for (int y = gx + 1; y <= g + 10; y++) {
                put(level, box, cx + o[0], y, cz + o[1], s(Blocks.IRON_BARS));
            }
        }
        put(level, box, cx, g + 10, cz, s(Blocks.ANVIL));
        for (int y = g + 2; y <= g + 9; y++) {
            put(level, box, cx, y, cz, s(Blocks.CHAIN));
        }
        put(level, box, cx, g + 1, cz, s(Blocks.HOPPER));
        put(level, box, cx, g, cz, s(Blocks.IRON_BLOCK));
        for (int y = Math.max(0, g - 6); y < g; y++) {
            put(level, box, cx, y, cz, s(MarsModule.MARTIAN_CLAY.get()));
        }
    }

    /** Dust drifted against every west wall by the storms (the prevailing wind blows from the west). */
    private void drifts(WorldGenLevel level, BoundingBox box) {
        BlockState dust = s(MarsModule.MARTIAN_REGOLITH.get());
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (!known(x, z) || footprint(x, z)) {
                    continue;
                }
                int d = 0;
                for (int k = 1; k <= 5; k++) {
                    if (footprint(x + k, z)) {
                        d = k;
                        break;
                    }
                }
                if (d == 0) {
                    continue;
                }
                int top = F - 1 + (int) Math.floor((6 - d) * 0.75 + hash(x, z, seed + 17) * 0.9);
                if (top - ground[x][z] > 4) {
                    continue; // over a drop the wind keeps no drift
                }
                for (int y = ground[x][z] + 1; y <= top; y++) {
                    if (at(level, box, x, y, z).isAir()) {
                        put(level, box, x, y, z, dust);
                    }
                }
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox box, int x, int y, int z, ResourceKey<LootTable> loot, boolean chest) {
        RandomSource r = RandomSource.create(seed ^ (x * 341873128712L + y * 132897987541L + z));
        if (chest) {
            createChest(level, box, r, x, y, z, loot);
            return;
        }
        BlockPos pos = PieceOrientation.place(level, this, s(Blocks.BARREL), x, y, z, box);
        if (pos != null && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            barrel.setLootTable(loot, r.nextLong());
        }
    }

    private void guard(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, int x, int y, int z) {
        BlockPos pos = PieceOrientation.worldPos(this, x, y, z);
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
