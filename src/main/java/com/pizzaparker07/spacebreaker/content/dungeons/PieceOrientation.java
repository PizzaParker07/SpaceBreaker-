package com.pizzaparker07.spacebreaker.content.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jetbrains.annotations.Nullable;

/**
 * Correct block orientation for hand-built {@link StructurePiece}s, for every dungeon (Earth, Moon, inner and outer worlds).
 *
 * <p>Vanilla's {@code StructurePiece} maps piece coordinates to the world with {@code getWorldX/Z}: NORTH flips z, WEST and
 * EAST swap the axes. But {@code placeBlock} turns the block state with a different mirror/rotation pair (SOUTH and WEST
 * get {@code Mirror.LEFT_RIGHT}, NORTH none), so a lever, sign, door, stair, dispenser or observer placed with
 * {@code placeBlock} ends up facing the wrong way, or hanging in the air, in some orientations.
 *
 * <p>This class uses the transform that matches the coordinates:
 * <pre>
 *   orientation   coordinates                     state transform
 *   SOUTH / none  x→x,  z→z                        none
 *   NORTH         x→x,  z→−z                       Mirror.LEFT_RIGHT
 *   WEST          x→+z, z→−x                       Rotation.CLOCKWISE_90
 *   EAST          x→+z, z→+x                       Mirror.LEFT_RIGHT then Rotation.CLOCKWISE_90
 * </pre>
 * Use {@link #place} instead of {@code placeBlock} for anything with a direction (FACING, HORIZONTAL_FACING, AXIS, stair
 * shape, door hinge, rail shape, ...): write the state as it should look in piece space (a lever on the wall at +z is
 * {@code facing=north}) and it comes out right in every orientation. {@link #direction} turns a piece-space direction into
 * a world one, and {@link #placeToward} aims a block from one piece cell at another.
 * The GameTest {@code DungeonOrientationTests} checks all four orientations.
 */
public final class PieceOrientation {
    private PieceOrientation() {
    }

    /** The mirror that matches the piece's coordinate mapping. */
    public static Mirror mirror(StructurePiece piece) {
        Direction o = piece.getOrientation();
        return o == Direction.NORTH || o == Direction.EAST ? Mirror.LEFT_RIGHT : Mirror.NONE;
    }

    /** The rotation (applied after {@link #mirror}) that matches the piece's coordinate mapping. */
    public static Rotation rotation(StructurePiece piece) {
        Direction o = piece.getOrientation();
        return o == Direction.WEST || o == Direction.EAST ? Rotation.CLOCKWISE_90 : Rotation.NONE;
    }

    /** A piece-space state turned to face the right way in the world. */
    public static BlockState transform(StructurePiece piece, BlockState state) {
        Mirror m = mirror(piece);
        Rotation r = rotation(piece);
        if (m != Mirror.NONE) {
            state = state.mirror(m);
        }
        if (r != Rotation.NONE) {
            state = state.rotate(r);
        }
        return state;
    }

    /** A piece-space direction in the world. Up and down stay as they are. */
    public static Direction direction(StructurePiece piece, Direction local) {
        if (local.getAxis().isVertical()) {
            return local;
        }
        return rotation(piece).rotate(mirror(piece).mirror(local));
    }

    /** The world position of a piece cell (the same mapping as {@code StructurePiece.getWorldPos}). */
    public static BlockPos worldPos(StructurePiece piece, int x, int y, int z) {
        Direction o = piece.getOrientation();
        BoundingBox b = piece.getBoundingBox();
        if (o == null) {
            return new BlockPos(x, y, z);
        }
        return switch (o) {
            case NORTH -> new BlockPos(b.minX() + x, b.minY() + y, b.maxZ() - z);
            case WEST -> new BlockPos(b.maxX() - z, b.minY() + y, b.minZ() + x);
            case EAST -> new BlockPos(b.minX() + z, b.minY() + y, b.minZ() + x);
            default -> new BlockPos(b.minX() + x, b.minY() + y, b.minZ() + z);
        };
    }

    /**
     * Places a piece-space state at a piece cell, turned correctly. Only inside {@code box} (the chunk being generated), like
     * {@code placeBlock}. Returns the world position, or null when it is outside the box.
     */
    public static @Nullable BlockPos place(WorldGenLevel level, StructurePiece piece, BlockState state, int x, int y, int z, BoundingBox box) {
        BlockPos pos = worldPos(piece, x, y, z);
        if (!box.isInside(pos)) {
            return null;
        }
        // no shape updates, as in world generation: a wall sign or lever must not drop because its wall cell is
        // briefly air while a neighbouring chunk is built (matters when a piece is placed in a live world, e.g. tests)
        level.setBlock(pos, transform(piece, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return pos;
    }

    /**
     * Places a directional block at piece cell {@code from} facing piece cell {@code to} (a dispenser aimed at a target, an
     * observer watching a cell). With {@code away} it faces the other way: a lever or wall sign whose wall is at {@code to}.
     * The cells must be in a straight line (same row, column or height).
     */
    public static @Nullable BlockPos placeToward(WorldGenLevel level, StructurePiece piece, BlockState state, BlockPos from, BlockPos to,
                                                boolean away, BoundingBox box) {
        Direction local = Direction.fromDelta(Integer.signum(to.getX() - from.getX()), Integer.signum(to.getY() - from.getY()),
                Integer.signum(to.getZ() - from.getZ()));
        if (local == null) {
            throw new IllegalArgumentException("cells must be in a straight line: " + from + " -> " + to);
        }
        if (away) {
            local = local.getOpposite();
        }
        BlockState s = state;
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            s = s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, local);
        } else if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) && local.getAxis().isHorizontal()) {
            s = s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, local);
        }
        return place(level, piece, s, from.getX(), from.getY(), from.getZ(), box);
    }
}
