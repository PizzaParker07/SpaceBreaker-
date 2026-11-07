package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.LifeSupport;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Station modules (rocketry hull, windows, docking port) hold the air an oxygen distributor puts in them. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationAirGameTests {
    private StationAirGameTests() {
    }

    /** A 3 × 3 × 3 module of station hull with a window, a docking port and an airlock door in its walls. */
    private static OxygenDistributorBlockEntity module(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    boolean shell = x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5;
                    helper.setBlock(x, y, z, shell ? RocketryModule.STATION_HULL.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.setBlock(3, 3, 1, RocketryModule.STATION_WINDOW.get());
        helper.setBlock(1, 3, 3, RocketryModule.DOCKING_PORT.get());
        BlockState lower = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(5, 2, 3), lower);
        helper.setBlock(new BlockPos(5, 3, 3), lower.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos d = new BlockPos(3, 2, 3);
        helper.setBlock(d, SuitModule.OXYGEN_DISTRIBUTOR.get());
        OxygenDistributorBlockEntity be = helper.getBlockEntity(d);
        be.setStack(OxygenTankItem.filled(SuitModule.LARGE_OXYGEN_TANK.get()));
        return be;
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void stationModuleHoldsAirBehindItsAirlock(GameTestHelper helper) {
        OxygenDistributorBlockEntity be = module(helper);
        BlockPos inside = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.room().sealed() && be.room().volume() == 26,
                        "hull, window, docking port and closed airlock must seal 26 blocks, got " + be.room().state() + " " + be.room().volume()))
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside), "the module pressurises"))
                .thenExecute(() -> {
                    Player p = helper.makeMockPlayer(GameType.SURVIVAL);
                    p.setPos(inside.getX() + 0.5, inside.getY() - 1, inside.getZ() + 0.5);
                    helper.assertTrue(LifeSupport.Env.at(p, 0).breathable(), "without a helmet the air inside is breathable");
                    BlockPos door = helper.absolutePos(new BlockPos(5, 2, 3));
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), door, helper.getLevel().getBlockState(door), true);
                })
                .thenWaitUntil(() -> helper.assertTrue(!be.room().isScanning() && !be.room().sealed(), "opening the airlock lets the air out"))
                .thenExecute(() -> helper.assertFalse(SealedRooms.providesOxygen(helper.getLevel(), inside), "a vented module is not breathable"))
                .thenExecute(() -> {
                    BlockPos door = helper.absolutePos(new BlockPos(5, 2, 3));
                    ((AirlockDoorBlock) SuitModule.AIRLOCK_DOOR.get()).setOpen(helper.getLevel(), door, helper.getLevel().getBlockState(door), false);
                })
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.providesOxygen(helper.getLevel(), inside), "closed again, the module re-pressurises"))
                .thenSucceed();
    }
}
