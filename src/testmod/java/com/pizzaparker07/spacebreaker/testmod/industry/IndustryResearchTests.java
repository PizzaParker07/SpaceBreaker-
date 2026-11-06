package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Machines listed in a technology's "unlocks" only run once their owner has researched it. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryResearchTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void liquefierWaitsForCryogenicsResearch(GameTestHelper h) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "researcher"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, MachinesModule.LIQUEFIER.block().get());
        MachineBlockEntity be = h.getBlockEntity(pos);
        h.assertFalse(be.techRequirement().isEmpty(), "the research tree should gate the liquefier");
        be.setOwner(player.getUUID());
        be.refreshLock(player);
        be.energy().generate(60_000);
        be.tank(0).fill(FluidsModule.OXYGEN.stack(2_000), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(100, () -> {
            h.assertTrue(be.isLocked(), "machine must be locked for a player without the technology");
            h.assertTrue(be.tank(1).isEmpty(), "a locked liquefier made LOX");
            Tech.setKnownSilently(player, new java.util.HashSet<>(be.techRequirement().techs()));
            be.refreshLock(player);
        });
        h.runAfterDelay(380, () -> {
            h.assertFalse(be.isLocked(), "machine stayed locked after research");
            h.assertTrue(be.tank(1).getFluidAmount() > 0, "no LOX after research");
            h.succeed();
        });
    }

    private IndustryResearchTests() {
    }
}
