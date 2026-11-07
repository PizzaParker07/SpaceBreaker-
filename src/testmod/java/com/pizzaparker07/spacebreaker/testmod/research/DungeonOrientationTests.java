package com.pizzaparker07.spacebreaker.testmod.research;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.PieceOrientation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * {@link PieceOrientation} in all four piece orientations: wall-mounted blocks stay on their wall, aimed blocks point at
 * their target and stairs face the way the piece says.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DungeonOrientationTests {
    private static final String EMPTY = "empty";

    /** A bare piece: only its orientation and bounding box matter. */
    private static final class TestPiece extends StructurePiece {
        TestPiece(BoundingBox box, Direction orientation) {
            super(StructurePieceType.JUNGLE_PYRAMID_PIECE, 0, box);
            setOrientation(orientation);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox box,
                                ChunkPos chunkPos, BlockPos pos) {
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dungeonBlocksFaceTheRightWayInEveryOrientation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction[] orientations = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < orientations.length; i++) {
            Direction o = orientations[i];
            BlockPos origin = helper.absolutePos(new BlockPos(1 + i * 7, 1, 1));
            BoundingBox box = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 4, origin.getY() + 4, origin.getZ() + 4);
            TestPiece piece = new TestPiece(box, o);
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }

            // a lever and a wall sign on a wall at piece +z: in piece space they face north (away from the wall)
            PieceOrientation.place(level, piece, Blocks.STONE.defaultBlockState(), 2, 1, 3, box);
            BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.NORTH);
            BlockPos leverPos = PieceOrientation.place(level, piece, lever, 2, 1, 2, box);
            helper.assertTrue(level.getBlockState(leverPos).canSurvive(level, leverPos), o + ": lever is on its wall");
            PieceOrientation.place(level, piece, Blocks.STONE.defaultBlockState(), 3, 1, 3, box);
            BlockPos signPos = PieceOrientation.place(level, piece, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH),
                    3, 1, 2, box);
            helper.assertTrue(level.getBlockState(signPos).canSurvive(level, signPos), o + ": wall sign is on its wall");

            // a dispenser aimed from (1,2,1) at (1,2,3)
            BlockPos disp = PieceOrientation.placeToward(level, piece, Blocks.DISPENSER.defaultBlockState(), new BlockPos(1, 2, 1),
                    new BlockPos(1, 2, 3), false, box);
            BlockPos target = PieceOrientation.worldPos(piece, 1, 2, 3);
            Direction aim = level.getBlockState(disp).getValue(DispenserBlock.FACING);
            helper.assertTrue(disp.relative(aim, 2).equals(target), o + ": dispenser aims at its target, faces " + aim);

            // stairs facing piece east climb toward the piece's +x neighbour
            BlockPos stair = PieceOrientation.place(level, piece, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST),
                    1, 1, 1, box);
            BlockPos east = PieceOrientation.worldPos(piece, 2, 1, 1);
            Direction facing = level.getBlockState(stair).getValue(StairBlock.FACING);
            helper.assertTrue(stair.relative(facing).equals(east), o + ": stairs face the piece's east, got " + facing);
            helper.assertTrue(PieceOrientation.direction(piece, Direction.EAST) == facing, o + ": direction() agrees with the placed state");
        }
        helper.succeed();
    }
}
