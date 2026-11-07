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
    SOLAR_WINGS("solar_wings", 1, 1, 2, 6);

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

    /** The blocks the module would take (for the clearance check and the preview outline). */
    public AABB bounds(BlockPos wall, Direction forward) {
        BlockPos a = at(wall, forward, -halfWidth, -down, 0), b = at(wall, forward, halfWidth, up, length);
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Whether nothing is in the way of building the module here. */
    public boolean fits(Level level, BlockPos wall, Direction forward) {
        for (int r = -halfWidth; r <= halfWidth; r++) {
            for (int u = -down; u <= up; u++) {
                for (int f = 0; f <= length; f++) {
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
        if (this == SOLAR_WINGS) {
            buildWings(level, mouth, facing);
            return true; // outside the hull: the mouth stays sealed
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
        return true;
    }

    /** Where the core's roof docking port is, for a core built with {@link #buildCore}. */
    public static BlockPos corePort(BlockPos floorCentre) {
        return floorCentre.above(CORE.down + CORE.up);
    }

    private void build(ServerLevel level, BlockPos wall, Direction forward, boolean openBack, String owner) {
        BlockState hull = RocketryModule.STATION_HULL.get().defaultBlockState();
        BlockState window = RocketryModule.STATION_WINDOW.get().defaultBlockState();
        BlockState light = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get().defaultBlockState();
        for (int r = -halfWidth; r <= halfWidth; r++) {
            for (int u = -down; u <= up; u++) {
                for (int f = 0; f <= length; f++) {
                    boolean shell = Math.abs(r) == halfWidth || u == -down || u == up || f == 0 || f == length;
                    level.setBlock(at(wall, forward, r, u, f), shell ? hull : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        Direction right = forward.getClockWise();
        // the back mouth: open onto the module it joins, or sealed with hatches
        if (this != AIRLOCK) {
            mouth(level, wall, forward.getOpposite(), right, openBack);
        }
        DockingPorts ports = DockingPorts.get(level);
        if (this == CORE) {
            int mid = length / 2;
            mouth(level, at(wall, forward, 0, 0, length), forward, right, false);
            mouth(level, at(wall, forward, halfWidth, 0, mid), right, forward, false);
            mouth(level, at(wall, forward, -halfWidth, 0, mid), right.getOpposite(), forward, false);
            // a window above each side mouth
            for (int a = -1; a <= 1; a++) {
                level.setBlock(at(wall, forward, halfWidth, 2, mid + a), window, 3);
                level.setBlock(at(wall, forward, -halfWidth, 2, mid + a), window, 3);
            }
            // lights in the roof corners, the docking port in the middle of the roof
            for (int[] c : new int[][]{{-2, 1}, {2, 1}, {-2, length - 1}, {2, length - 1}}) {
                level.setBlock(at(wall, forward, c[0], up, c[1]), light, 3);
            }
            BlockPos port = at(wall, forward, 0, up, mid);
            level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState().setValue(DockingPortBlock.FACING, Direction.UP), 3);
            ports.setOwner(port, owner);
            // the console and the oxygen distributor stand in the near corners, clear of the mouths
            BlockPos console = at(wall, forward, 2, -down + 1, 1);
            level.setBlock(console, RocketryModule.STATION_CORE.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, forward), 3);
            if (level.getBlockEntity(console) instanceof StationCoreBlockEntity core) {
                core.setOwner(owner);
            }
            level.setBlock(at(wall, forward, -2, -down + 1, 1), SuitModule.OXYGEN_DISTRIBUTOR.get().defaultBlockState(), 3);
        } else if (this == HABITATION) {
            for (int f = 2; f <= length - 2; f++) {
                level.setBlock(at(wall, forward, halfWidth, 0, f), window, 3);
                level.setBlock(at(wall, forward, -halfWidth, 0, f), window, 3);
            }
            for (int f : new int[]{2, 4, 6}) {
                level.setBlock(at(wall, forward, -2, -down + 1, f), RocketryModule.STATION_BUNK.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, forward), 3);
                level.setBlock(at(wall, forward, 0, up, f), light, 3);
            }
            level.setBlock(at(wall, forward, 2, -down + 1, length - 2), Blocks.BARREL.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BarrelBlock.FACING, right.getOpposite()), 3);
            mouth(level, at(wall, forward, 0, 0, length), forward, right, false);
        } else if (this == AIRLOCK) {
            // the inner wall stays, with a door into the station; the outer wall has the door to space
            BlockState door = SuitModule.AIRLOCK_DOOR.get().defaultBlockState();
            for (int f : new int[]{0, length}) {
                BlockState d = door.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING,
                        f == 0 ? forward.getOpposite() : forward);
                level.setBlock(at(wall, forward, 0, -1, f), hull, 3);
                level.setBlock(at(wall, forward, 0, -1, f), d.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
                level.setBlock(at(wall, forward, 0, 0, f), d.setValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
            }
            level.setBlock(at(wall, forward, halfWidth, 0, length / 2), window, 3);
            level.setBlock(at(wall, forward, -halfWidth, 0, length / 2), window, 3);
            level.setBlock(at(wall, forward, 0, up, length / 2), light, 3);
        } else {
            for (int f = 2; f <= length - 2; f++) {
                level.setBlock(at(wall, forward, halfWidth, 0, f), window, 3);
                level.setBlock(at(wall, forward, -halfWidth, 0, f), window, 3);
            }
            level.setBlock(at(wall, forward, 0, up, length / 2), light, 3);
            BlockPos port = at(wall, forward, 0, 0, length);
            level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState().setValue(DockingPortBlock.FACING, forward), 3);
            ports.setOwner(port, owner);
        }
    }

    /** Solar wings at a mouth: battery in the mouth's middle (output inwards), a cable truss and three arrays. */
    private static void buildWings(ServerLevel level, BlockPos mouth, Direction facing) {
        level.setBlock(mouth, com.pizzaparker07.spacebreaker.content.machines.MachinesModule.BATTERY_BANK.block().get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock.FACING, facing.getOpposite()), 3);
        BlockState cable = com.pizzaparker07.spacebreaker.content.machines.MachinesModule.COPPER_CABLE.get().defaultBlockState();
        BlockState array = com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_ARRAY.get().defaultBlockState();
        for (int k = 1; k <= SOLAR_WINGS.length; k++) {
            BlockPos p = mouth.relative(facing, k);
            level.setBlock(p, Block.updateFromNeighbourShapes(cable, level, p), 3);
            if (k % 2 == 0) {
                level.setBlock(p.above(), array, 3);
            }
        }
        for (int k = 1; k <= SOLAR_WINGS.length; k++) {
            BlockPos p = mouth.relative(facing, k);
            level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), 3);
        }
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
