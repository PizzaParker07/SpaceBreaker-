package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.MiningOutpostPiece;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ObservatoryPiece;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ProbePiece;
import com.pizzaparker07.spacebreaker.content.saturn.dungeon.HangarPiece;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.BiFunction;

/** Every outer-system dungeon in all four orientations: ladders hang on a wall, door halves agree. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterDungeonOrientationTests {
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 400)
    public static void outerDungeonsFaceRightInEveryOrientation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-11000, 0, 11000);
        List<BiFunction<BlockPos, Direction, StructurePiece>> pieces = List.of(
                (p, d) -> new LanderPiece(3L, LanderPiece.Variant.PLUTO, p.getX(), 120, p.getZ(), d),
                (p, d) -> new MiningOutpostPiece(3L, p.getX(), 120, p.getZ(), d),
                (p, d) -> new ProbePiece(3L, p.getX(), 120, p.getZ(), d),
                (p, d) -> new ObservatoryPiece(3L, p.getX(), 120, p.getZ(), d),
                (p, d) -> new HangarPiece(3L, p.getX(), 120, p.getZ(), d),
                (p, d) -> new com.pizzaparker07.spacebreaker.content.neptune.dungeon.GeyserStationPiece(3L, p.getX(), 120, p.getZ(), d),
                (p, d) -> new com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece(3L, p.getX(), 120, p.getZ(), d));
        int checked = 0;
        for (int k = 0; k < pieces.size(); k++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos at = base.offset(k * 80, 0, d.get2DDataValue() * 80);
                StructurePiece piece = pieces.get(k).apply(at, d);
                BeltGameTests.place(level, piece);
                BoundingBox bb = piece.getBoundingBox();
                for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.maxZ() >= bb.minZ() ? bb.minZ() : bb.maxZ(), bb.maxX(), bb.maxY(),
                        bb.maxZ())) {
                    var st = level.getBlockState(p);
                    if (st.is(Blocks.LADDER)) {
                        helper.assertTrue(st.canSurvive(level, p), piece.getClass().getSimpleName() + " " + d + ": ladder hangs on nothing at " + p);
                        checked++;
                    }
                    if (st.getBlock() instanceof com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock
                            && st.getValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF)
                            == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                        var up = level.getBlockState(p.above());
                        helper.assertTrue(up.getBlock() instanceof com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock
                                        && up.getValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING)
                                        == st.getValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.FACING),
                                piece.getClass().getSimpleName() + " " + d + ": airlock door halves disagree at " + p);
                        checked++;
                    }
                    if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                        var up = level.getBlockState(p.above());
                        helper.assertTrue(up.getBlock() instanceof DoorBlock && up.getValue(DoorBlock.FACING) == st.getValue(DoorBlock.FACING),
                                piece.getClass().getSimpleName() + " " + d + ": door halves disagree at " + p);
                    }
                }
            }
        }
        helper.assertTrue(checked > 20, "ladders and doors were found and checked: " + checked);
        helper.succeed();
    }
}
