package com.pizzaparker07.spacebreaker.testmod.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * Oxygen must come from somewhere: a distributor's room air cannot be pumped back into tanks for free on an
 * airless world, piped oxygen (ISRU: gas or liquid oxygen) refills tanks, and pressurising a room costs oxygen.
 * (The GameTest server has no Moon level, so Moon conditions come from the Moon's body data.)
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OxygenEconomyGameTests {
    private OxygenEconomyGameTests() {
    }

    /** A filler that is not placed in the world (so the overworld's natural air does not tick it). */
    private static OxygenFillerBlockEntity looseFiller() {
        OxygenFillerBlockEntity f = new OxygenFillerBlockEntity(BlockPos.ZERO, SuitModule.OXYGEN_FILLER.get().defaultBlockState());
        f.setStack(new ItemStack(SuitModule.OXYGEN_TANK.get()));
        f.getEnergy().receiveEnergy(16000, false);
        return f;
    }

    @GameTest(template = "empty")
    public static void moonAirIsNotFreeOxygen(GameTestHelper helper) {
        var moon = Bodies.get(helper.getLevel().registryAccess(), Bodies.MOON);
        var earth = Bodies.get(helper.getLevel().registryAccess(), Bodies.EARTH);
        helper.assertFalse(OxygenFillerBlockEntity.naturalAir(moon, false), "the Moon has no air to compress");
        helper.assertTrue(OxygenFillerBlockEntity.naturalAir(earth, false), "Earth's air is free to compress");
        helper.assertFalse(OxygenFillerBlockEntity.naturalAir(earth, true), "but not in orbit");
        helper.assertTrue(OxygenFillerBlockEntity.naturalAir(Optional.empty(), false), "dimensions without a body count as Earth-like");
        // a filler standing in a distributor's pressurised room on the Moon: no natural air, nothing piped in
        OxygenFillerBlockEntity f = looseFiller();
        for (int i = 0; i < 100; i++) {
            f.pumpTick(false);
        }
        helper.assertTrue(OxygenTankItem.getOxygen(f.getStack()) == 0, "no oxygen out of nothing, has " + OxygenTankItem.getOxygen(f.getStack()));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pipedOxygenFillsTanks(GameTestHelper helper) {
        OxygenFillerBlockEntity f = looseFiller();
        IFluidHandler in = f.getOxygenInput();
        // regolith heater / electrolyser output arrives through pipes as liquid oxygen or oxygen gas
        helper.assertTrue(in.fill(new FluidStack(FluidsModule.LIQUID_OXYGEN.source.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                "liquid oxygen is accepted");
        helper.assertTrue(in.fill(new FluidStack(FluidsModule.OXYGEN.source.get(), 50), IFluidHandler.FluidAction.EXECUTE) == 50,
                "oxygen gas is accepted");
        helper.assertTrue(in.fill(new FluidStack(FluidsModule.CRUDE_OIL.source.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                "other fluids are refused");
        int expected = 100 * GasMachineBlockEntity.TICKS_PER_MB_LIQUID + 50 * GasMachineBlockEntity.TICKS_PER_MB_GAS;
        helper.assertTrue(f.storedOxygen() == expected, "buffer holds " + f.storedOxygen());
        for (int i = 0; i < 200; i++) {
            f.pumpTick(false);
        }
        helper.assertTrue(OxygenTankItem.getOxygen(f.getStack()) == expected && f.storedOxygen() == 0,
                "everything piped in ends up in the tank, has " + OxygenTankItem.getOxygen(f.getStack()));
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pressurisingCostsOxygen(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    boolean shell = x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        BlockPos dp = new BlockPos(3, 2, 3);
        helper.setBlock(dp, SuitModule.OXYGEN_DISTRIBUTOR.get());
        OxygenDistributorBlockEntity d = helper.getBlockEntity(dp);
        int volume = 26;
        ItemStack tank = new ItemStack(SuitModule.LARGE_OXYGEN_TANK.get());
        tank.set(SuitModule.OXYGEN.get(), (volume - 1) * OxygenDistributorBlockEntity.PRESSURISE_PER_BLOCK);
        d.setStack(tank);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(d.room().sealed() && d.room().volume() == volume, "sealed " + d.room().volume()))
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertFalse(d.active(), "not enough oxygen to fill the room");
                    helper.assertTrue(d.supply() == 0 && d.pressurised() == volume - 1, "the oxygen went into the air, pressurised " + d.pressurised());
                    d.getOxygenInput().fill(new FluidStack(FluidsModule.OXYGEN.source.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenWaitUntil(() -> helper.assertTrue(d.active(), "piped oxygen gas finishes the job"))
                .thenExecute(() -> helper.setBlock(5, 3, 3, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(!d.room().isScanning() && !d.room().sealed() && d.pressurised() == 0,
                        "a leak vents the air"))
                .thenSucceed();
    }
}
