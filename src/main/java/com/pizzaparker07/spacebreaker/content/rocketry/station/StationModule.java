package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * The station modules and how they join. Every module is built of station hull around a standard mouth: a 3 × 3
 * opening whose bottom row is at walking height (the mouth's middle is two blocks above the module's floor block).
 * Unused mouths are closed by nine {@link StationHatchBlock}s; attaching a module at a mouth builds the new module
 * against it and opens both walls into a short walk-through passage, so air flows through.
 * <p>
 * Module frame: {@code wall} is the middle of the new module's mouth wall, {@code forward} points from the old module
 * into the new one; local (right, up, forward) offsets are measured from {@code wall}.
 */
public enum StationModule {
    /** 7 × 7 × 7: the console, an oxygen distributor, lights, a docking port on the roof, four mouths. */
    CORE("station_core_module", 3, 2, 4, 6),
    /** 5 × 5 × 7 tunnel with a docking port at the far end. */
    DOCKING("docking_module", 2, 2, 2, 6),
    /** 7 × 5 × 9 living quarters: three bunks, a locker, a window band, a mouth at the far end to carry on. */
    HABITATION("habitation_module", 3, 2, 2, 8),
    /** 5 × 5 × 5 airlock: a door into the station, a door to space, a small chamber between (one person at a time). */
    AIRLOCK("airlock_module", 2, 2, 2, 4),
    /**
     * Solar wings, outside the hull: a cable truss out from the mouth carrying three solar arrays, feeding a battery
     * bank set into the mouth's middle (its output into the station). The mouth stays sealed.
     */
    SOLAR_WINGS("solar_wings", 1, 1, 2, 6),
    /**
     * Shipyard: an open frame 15 x 15 x 31 outside the hull, a berth for one spacecraft (hull up to 13 x 13 x 29). The
     * mouth's middle hatch becomes the berth's docking port; a fuel depot stands at the root, floodlights hang from
     * the top beams.
     */
    SHIPYARD("shipyard_module", 7, 7, 7, 30);

    private final String part;
    private final int halfWidth, down, up, length;

    StationModule(String part, int halfWidth, int down, int up, int length) {
        this.part = part;
        this.halfWidth = halfWidth;
        this.down = down;
        this.up = up;
        this.length = length;
    }

    public String part() {
        return part;
    }

    public static Optional<StationModule> byPart(ResourceLocation id) {
        for (StationModule m : values()) {
            if (m.part.equals(id.getPath())) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    private static BlockPos at(BlockPos wall, Direction forward, int r, int u, int f) {
        return wall.relative(forward.getClockWise(), r).above(u).relative(forward, f);
    }

    /** Blocks of rails, fins and antennas outside the hull (sides and top), part of the room a module needs. */
    private int margin() {
        return this == SOLAR_WINGS || this == SHIPYARD ? 0 : 1;
    }

    /** The blocks the module would take (for the clearance check and the preview outline). */
    public AABB bounds(BlockPos wall, Direction forward) {
        int m = margin();
        BlockPos a = at(wall, forward, -halfWidth - m, -down, 0), b = at(wall, forward, halfWidth + m, up + 2 * m, length);
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Whether nothing is in the way of building the module here. */
    public boolean fits(Level level, BlockPos wall, Direction forward) {
        int m = margin();
        for (int r = -halfWidth - m; r <= halfWidth + m; r++) {
            for (int u = -down; u <= up + 2 * m; u++) {
                for (int f = 0; f <= length; f++) {
                    // the details stay off the end walls, so modules can sit end to end
                    boolean detail = Math.abs(r) > halfWidth || u > up;
                    if (detail && (f == 0 || f == length)) {
                        continue;
                    }
                    if (!level.getBlockState(at(wall, forward, r, u, f)).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The wall middle and direction of a module attached at the mouth whose middle hatch is {@code mouth}. */
    public static BlockPos wallAt(BlockPos mouth, Direction facing) {
        return mouth.relative(facing);
    }

    /**
     * Attaches the module at a mouth (its middle hatch), opening the passage. False, and nothing changed, if the
     * mouth is not whole or something is in the way.
     */
    public boolean attach(ServerLevel level, BlockPos mouth, String owner) {
        BlockState s = level.getBlockState(mouth);
        if (!(s.getBlock() instanceof StationHatchBlock) || !s.getValue(StationHatchBlock.CENTER)) {
            return false;
        }
        Direction facing = s.getValue(StationHatchBlock.FACING);
        if (facing.getAxis().isVertical()) {
            return false; // vertical mouths come with later modules
        }
        BlockPos wall = wallAt(mouth, facing);
        if (!fits(level, wall, facing)) {
            return false;
        }
        StationParts.get(level).add(wall, this);
        if (this == SOLAR_WINGS) {
            buildWings(level, mouth, facing);
            return true; // outside the hull: the mouth stays sealed
        }
        if (this == SHIPYARD) {
            buildShipyard(level, mouth, facing, owner);
            return true;
        }
        build(level, wall, facing, true, owner);
        // open the old wall too: a two-block passage
        for (int r = -1; r <= 1; r++) {
            for (int u = -1; u <= 1; u++) {
                BlockPos p = at(mouth, facing, r, u, 0);
                if (level.getBlockState(p).getBlock() instanceof StationHatchBlock) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return true;
    }

    /**
     * Builds a station core on its own (the first module of a station), its floor centred on {@code floorCentre}.
     * False, and nothing placed, if something is in the way.
     */
    public static boolean buildCore(ServerLevel level, BlockPos floorCentre, Direction forward, String owner) {
        BlockPos wall = floorCentre.relative(forward.getOpposite(), CORE.length / 2).above(CORE.down);
        if (!CORE.fits(level, wall, forward)) {
            return false;
        }
        CORE.build(level, wall, forward, false, owner);
        StationParts.get(level).add(wall, CORE);
        return true;
    }

    /** Where the core's roof docking port is, for a core built with {@link #buildCore}. */
    public static BlockPos corePort(BlockPos floorCentre) {
        return floorCentre.above(CORE.down + CORE.up);
    }

    // materials: white hull panels, grey aluminium ribs, flanges and trim, a grey titanium deck, hazard stripes
    private static BlockState hull() {
        return RocketryModule.STATION_HULL.get().defaultBlockState();
    }

    private static BlockState trim() {
        return com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get().defaultBlockState();
    }

    private static BlockState trimStair(Direction facing, net.minecraft.world.level.block.state.properties.Half half) {
        return com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.stairs().get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.StairBlock.HALF, half);
    }

    private static BlockState deck() {
        return com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get().defaultBlockState();
    }

    private static BlockState stripe() {
        return com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get().defaultBlockState();
    }

    /** A supply locker (the shared one from the dungeons kit), its doors facing {@code facing}. */
    private static BlockState locker(Direction facing) {
        Direction f = facing.getAxis().isVertical() ? Direction.NORTH : facing;
        return com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.FACING, f);
    }

    private static BlockState light() {
        return com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get().defaultBlockState();
    }

    private void build(ServerLevel level, BlockPos wall, Direction forward, boolean openBack, String owner) {
        Direction right = forward.getClockWise();
        if (this == CORE) {
            node(level, wall, forward, right);
        } else {
            tube(level, wall, forward, right);
        }
        BlockState window = RocketryModule.STATION_WINDOW.get().defaultBlockState();
        // the back mouth: open onto the module it joins, or sealed with hatches (the airlock keeps a wall with a door)
        if (this != AIRLOCK) {
            mouth(level, wall, forward.getOpposite(), right, openBack);
        }
        DockingPorts ports = DockingPorts.get(level);
        if (this == CORE) {
            int mid = length / 2;
            mouth(level, at(wall, forward, 0, 0, length), forward, right, false);
            mouth(level, at(wall, forward, halfWidth, 0, mid), right, forward, false);
            mouth(level, at(wall, forward, -halfWidth, 0, mid), right.getOpposite(), forward, false);
            BlockPos port = at(wall, forward, 0, up, mid);
            level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState().setValue(DockingPortBlock.FACING, Direction.UP), 3);
            ports.setOwner(port, owner);
            // the console with a power monitor beside it, the oxygen distributor across, lockers in the corners
            BlockPos console = at(wall, forward, 2, -down + 1, 1);
            level.setBlock(console, RocketryModule.STATION_CORE.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, forward), 3);
            if (level.getBlockEntity(console) instanceof StationCoreBlockEntity core) {
                core.setOwner(owner);
            }
            level.setBlock(at(wall, forward, 1, -down + 1, 1), com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get()
                    .defaultBlockState(), 3);
            level.setBlock(at(wall, forward, -2, -down + 1, 1), SuitModule.OXYGEN_DISTRIBUTOR.get().defaultBlockState(), 3);
            for (int[] c : new int[][]{{2, length - 1}, {-2, length - 1}}) {
                level.setBlock(at(wall, forward, c[0], -down + 1, c[1]), locker(forward.getOpposite()), 3);
            }
        } else if (this == HABITATION) {
            // three sleeping pods along the left wall: a bunk in an alcove between grey partitions, a light above
            for (int f : new int[]{2, 4, 6}) {
                level.setBlock(at(wall, forward, -2, -down + 1, f), RocketryModule.STATION_BUNK.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, forward), 3);
                for (int side : new int[]{-1, 1}) {
                    for (int u = -down + 1; u <= 0; u++) {
                        level.setBlock(at(wall, forward, -2, u, f + side), trim(), 3);
                    }
                }
                level.setBlock(at(wall, forward, -halfWidth, 0, f), light(), 3);
            }
            level.setBlock(at(wall, forward, 2, -down + 1, length - 2), locker(right.getOpposite()), 3);
            level.setBlock(at(wall, forward, 2, -down + 1, length - 3), locker(right.getOpposite()), 3);
            level.setBlock(at(wall, forward, 2, -down + 1, 2), com.pizzaparker07.spacebreaker.content.machines.MachinesModule.POWER_MONITOR.get()
                    .defaultBlockState(), 3);
            mouth(level, at(wall, forward, 0, 0, length), forward, right, false);
        } else if (this == AIRLOCK) {
            // the inner wall stays, with a door into the station; the outer wall has the door to space
            BlockState door = SuitModule.AIRLOCK_DOOR.get().defaultBlockState();
            for (int f : new int[]{0, length}) {
                BlockState d = door.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING,
                        f == 0 ? forward.getOpposite() : forward);
                level.setBlock(at(wall, forward, 0, -1, f), d.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
                level.setBlock(at(wall, forward, 0, 0, f), d.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
                // hazard stripes framing each door
                for (int u = -1; u <= 1; u++) {
                    level.setBlock(at(wall, forward, -1, u, f), stripe(), 3);
                    level.setBlock(at(wall, forward, 1, u, f), stripe(), 3);
                }
                level.setBlock(at(wall, forward, 0, 1, f), stripe(), 3);
            }
        } else {
            BlockPos port = at(wall, forward, 0, 0, length);
            level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState().setValue(DockingPortBlock.FACING, forward), 3);
            ports.setOwner(port, owner);
        }
    }

    /**
     * A tube module: an octagonal hull (bevelled corners) between two square end flanges, grey ribs every third ring,
     * a window band, a grey deck, a cable and a pipe duct along the ceiling, lights in the roof; outside, handrails
     * along both sides, radiator fins and an antenna on top, a green and a red position light at the far end.
     */
    private void tube(ServerLevel level, BlockPos wall, Direction forward, Direction right) {
        BlockState window = RocketryModule.STATION_WINDOW.get().defaultBlockState();
        for (int f = 0; f <= length; f++) {
            boolean end = f == 0 || f == length;
            boolean rib = !end && (f % 3 == 0 || f == 1 || f == length - 1);
            for (int r = -halfWidth; r <= halfWidth; r++) {
                for (int u = -down; u <= up; u++) {
                    int ar = Math.abs(r);
                    boolean side = ar == halfWidth, floor = u == -down, roof = u == up;
                    BlockState st;
                    if (end) {
                        st = side || floor || roof ? trim() : hull();       // a square flange round the mouth
                    } else if (side && (floor || roof)) {
                        // bevelled corner: the hull's section is an octagon
                        Direction in = r > 0 ? right.getOpposite() : right;
                        st = trimStair(in, roof ? net.minecraft.world.level.block.state.properties.Half.BOTTOM
                                : net.minecraft.world.level.block.state.properties.Half.TOP);
                    } else if (floor) {
                        st = deck();
                    } else if (roof) {
                        st = rib ? trim() : r == 0 && f % 3 == 2 ? light() : hull();
                    } else if (side) {
                        boolean windowBand = u == 0 && !rib && f >= 2 && f <= length - 2 && this != AIRLOCK;
                        st = rib ? trim() : windowBand ? window : u < 0 ? trim() : hull();
                    } else {
                        st = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(at(wall, forward, r, u, f), st, 3);
                }
            }
            if (end) {
                continue;
            }
            // ducts along the ceiling: cables on the left, pipes on the right
            if (halfWidth >= 2 && this != AIRLOCK) {
                level.setBlock(at(wall, forward, -(halfWidth - 1), up - 1, f),
                        com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get().defaultBlockState(), 3);
                level.setBlock(at(wall, forward, halfWidth - 1, up - 1, f),
                        com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get().defaultBlockState(), 3);
            }
            // outside: handrails along both sides at shoulder height
            if (!rib) {
                for (int sgn : new int[]{-1, 1}) {
                    BlockPos rail = at(wall, forward, sgn * (halfWidth + 1), 1, f);
                    level.setBlock(rail, Block.updateFromNeighbourShapes(
                            com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get().defaultBlockState(), level, rail), 3);
                }
            }
        }
        // radiator fins on the roof, an antenna, position lights (green on the right, red on the left) near the far end
        BlockState fin = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get().defaultBlockState();
        for (int f = 2; f <= length - 2; f += 2) {
            level.setBlock(at(wall, forward, 0, up + 1, f), fin, 3);
        }
        level.setBlock(at(wall, forward, halfWidth - 1, up + 1, length - 1), Blocks.END_ROD.defaultBlockState(), 3);
        level.setBlock(at(wall, forward, halfWidth - 1, up + 2, length - 1), Blocks.END_ROD.defaultBlockState(), 3);
        level.setBlock(at(wall, forward, halfWidth + 1, -1, length - 1), Blocks.VERDANT_FROGLIGHT.defaultBlockState(), 3);
        level.setBlock(at(wall, forward, -halfWidth - 1, -1, length - 1), Blocks.SHROOMLIGHT.defaultBlockState(), 3);
        // a hazard band round the hull next to the far flange
        for (int r = -halfWidth + 1; r <= halfWidth - 1; r++) {
            level.setBlock(at(wall, forward, r, up, length - 1), stripe(), 3);
        }
        for (int u = -down + 1; u <= up - 1; u++) {
            level.setBlock(at(wall, forward, halfWidth, u, length - 1), stripe(), 3);
            level.setBlock(at(wall, forward, -halfWidth, u, length - 1), stripe(), 3);
        }
        refreshRails(level, wall, forward);
    }

    /**
     * The core node: a 7 x 7 x 7 hub with bevelled roof and floor edges, a grey frame round each of its mouths, a
     * striped collar round the roof port, radiator fins and antennas on the roof; inside a grey deck, a dado of grey
     * panels, cable and pipe ducts under the roof and a ring of lights.
     */
    private void node(ServerLevel level, BlockPos wall, Direction forward, Direction right) {
        int mid = length / 2;
        for (int f = 0; f <= length; f++) {
            for (int r = -halfWidth; r <= halfWidth; r++) {
                for (int u = -down; u <= up; u++) {
                    boolean sideR = Math.abs(r) == halfWidth, sideF = f == 0 || f == length;
                    boolean floor = u == -down, roof = u == up;
                    BlockState st;
                    // under a mouth the floor edge stays a full block: once the mouth opens it borders the passage
                    boolean underMouth = floor && (sideF && Math.abs(r) <= 1 || sideR && Math.abs(f - mid) <= 1);
                    if ((sideR || sideF) && (floor || roof) && !(sideR && sideF) && !underMouth) {
                        Direction in = sideR ? (r > 0 ? right.getOpposite() : right) : (f == 0 ? forward : forward.getOpposite());
                        st = trimStair(in, roof ? net.minecraft.world.level.block.state.properties.Half.BOTTOM
                                : net.minecraft.world.level.block.state.properties.Half.TOP);
                    } else if (sideR && sideF) {
                        st = trim();                                          // vertical edges: grey posts
                    } else if (floor) {
                        st = deck();
                    } else if (roof) {
                        int dr = Math.abs(r), df = Math.abs(f - mid);
                        st = dr <= 1 && df <= 1 ? stripe() : (dr == 2 && df == 2) ? light() : hull();
                    } else if (sideR || sideF) {
                        // a grey frame round the mouth in the middle of each wall, a grey dado low down
                        int across = sideR ? Math.abs(f - mid) : Math.abs(r);
                        boolean frame = across <= 2 && u >= -1 && u <= 1 && across == 2 || across <= 2 && u == 2;
                        st = frame || u == -1 && across > 2 ? trim() : hull();
                        if (u == 3 && across <= 1) {
                            st = RocketryModule.STATION_WINDOW.get().defaultBlockState();  // a window above each mouth
                        }
                    } else {
                        st = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(at(wall, forward, r, u, f), st, 3);
                }
            }
        }
        // ducts under the roof along two walls, lights between them
        for (int f = 1; f <= length - 1; f++) {
            level.setBlock(at(wall, forward, -(halfWidth - 1), up - 1, f),
                    com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get().defaultBlockState(), 3);
            level.setBlock(at(wall, forward, halfWidth - 1, up - 1, f),
                    com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get().defaultBlockState(), 3);
        }
        // roof: radiator fins at two corners, two antennas
        BlockState fin = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get().defaultBlockState();
        for (int[] c : new int[][]{{-2, 1}, {-2, 2}, {2, length - 1}, {2, length - 2}}) {
            level.setBlock(at(wall, forward, c[0], up + 1, c[1]), fin, 3);
        }
        for (int h = 1; h <= 2; h++) {
            level.setBlock(at(wall, forward, 2, up + h, 1), Blocks.END_ROD.defaultBlockState(), 3);
            level.setBlock(at(wall, forward, -2, up + h, length - 1), Blocks.END_ROD.defaultBlockState(), 3);
        }
    }

    /** Railings join up with their neighbours once all of them are placed. */
    private void refreshRails(ServerLevel level, BlockPos wall, Direction forward) {
        for (int f = 1; f < length; f++) {
            for (int sgn : new int[]{-1, 1}) {
                BlockPos p = at(wall, forward, sgn * (halfWidth + 1), 1, f);
                BlockState st = level.getBlockState(p);
                if (!st.isAir()) {
                    level.setBlock(p, Block.updateFromNeighbourShapes(st, level, p), 3);
                }
            }
        }
    }

    /**
     * Solar wings at a mouth: a battery bank in the mouth's middle (output inwards), a grey rotary joint collar, then a
     * lattice boom (grate above and below a copper cable) with two solar arrays on each side.
     */
    private static void buildWings(ServerLevel level, BlockPos mouth, Direction facing) {
        level.setBlock(mouth, com.pizzaparker07.spacebreaker.content.machines.MachinesModule.BATTERY_BANK.block().get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock.FACING, facing.getOpposite()), 3);
        BlockState cable = com.pizzaparker07.spacebreaker.content.machines.MachinesModule.COPPER_CABLE.get().defaultBlockState();
        BlockState array = com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_ARRAY.get().defaultBlockState();
        BlockState lattice = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get().defaultBlockState();
        Direction across = facing.getClockWise();
        for (int k = 1; k <= SOLAR_WINGS.length; k++) {
            BlockPos p = mouth.relative(facing, k);
            level.setBlock(p, cable, 3);
            if (k == 1) {
                // the rotary joint: a grey collar round the cable
                for (BlockPos q : new BlockPos[]{p.above(), p.below(), p.relative(across), p.relative(across.getOpposite())}) {
                    level.setBlock(q, trim(), 3);
                }
                continue;
            }
            level.setBlock(p.above(), lattice, 3);
            level.setBlock(p.below(), lattice, 3);
            if (k == 3 || k == 5) {
                level.setBlock(p.relative(across), array, 3);
                level.setBlock(p.relative(across.getOpposite()), array, 3);
            }
        }
        level.setBlock(mouth.relative(facing, SOLAR_WINGS.length + 0).above(2), Blocks.END_ROD.defaultBlockState(), 3);
        for (int k = 1; k <= SOLAR_WINGS.length; k++) {
            BlockPos p = mouth.relative(facing, k);
            level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), 3);
        }
    }

    /** The shipyard frame at a mouth, its berth registered (see {@link Berths}). */
    private static void buildShipyard(ServerLevel level, BlockPos mouth, Direction facing, String owner) {
        BlockPos wall = wallAt(mouth, facing);
        int h = SHIPYARD.halfWidth, len = SHIPYARD.length;
        BlockState beam = trim();
        BlockState lattice = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get().defaultBlockState();
        for (int f = 0; f <= len; f++) {
            boolean ring = f % 6 == 0;
            for (int r = -h; r <= h; r++) {
                for (int u = -h; u <= h; u++) {
                    boolean edgeR = Math.abs(r) == h, edgeU = Math.abs(u) == h;
                    if (edgeR && edgeU) {
                        level.setBlock(at(wall, facing, r, u, f), ring ? stripe() : beam, 3);   // the four long beams
                    } else if (ring && (edgeR || edgeU)) {
                        level.setBlock(at(wall, facing, r, u, f), lattice, 3);                  // cross frames
                    }
                }
            }
            if (ring && f > 0) {
                // floodlights under the top beams, shining down into the berth
                for (int sgn : new int[]{-1, 1}) {
                    level.setBlock(at(wall, facing, sgn * (h - 1), h - 1, f), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get()
                            .defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.DOWN), 3);
                }
            }
        }
        // the berth's docking port in the mouth, a fuel depot beside it
        level.setBlock(mouth, RocketryModule.DOCKING_PORT.get().defaultBlockState().setValue(DockingPortBlock.FACING, facing), 3);
        DockingPorts.get(level).setOwner(mouth, owner);
        level.setBlock(at(wall, facing, 2, -1, 0), RocketryModule.FUEL_DEPOT.get().defaultBlockState(), 3);
        BlockPos a = at(wall, facing, -(h - 1), -(h - 1), 1), b = at(wall, facing, h - 1, h - 1, len - 1);
        AABB berth = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
        StationCoreBlockEntity core = StationCoreBlockEntity.near(level, mouth);
        Berths.get(level).add(new Berths.Berth(berth, mouth.immutable(), facing, core != null ? core.stationId() : java.util.UUID.randomUUID()));
    }

    /** A 3 × 3 mouth in a wall: air (open) or nine hatches facing {@code out}, the middle one marked. */
    private static void mouth(ServerLevel level, BlockPos middle, Direction out, Direction across, boolean open) {
        BlockState hatch = RocketryModule.STATION_HATCH.get().defaultBlockState().setValue(StationHatchBlock.FACING, out);
        for (int a = -1; a <= 1; a++) {
            for (int u = -1; u <= 1; u++) {
                BlockPos p = middle.relative(across, a).above(u);
                level.setBlock(p, open ? Blocks.AIR.defaultBlockState() : hatch.setValue(StationHatchBlock.CENTER, a == 0 && u == 0), 3);
            }
        }
    }
}
