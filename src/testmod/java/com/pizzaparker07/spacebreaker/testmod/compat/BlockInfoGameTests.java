package com.pizzaparker07.spacebreaker.testmod.compat;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.info.BlockInfoLines;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Lines for the block info overlays (Jade, WTHIT), checked without either installed. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockInfoGameTests {
    @GameTest(template = "empty")
    public static void dustySolarPanelShowsDustAndOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, MachinesModule.SOLAR_PANEL.get());
        SolarPanelBlockEntity panel = helper.getBlockEntity(pos);
        panel.setDust(0.5F);
        var lines = BlockInfoLines.lines(panel);
        helper.assertTrue(lines.size() == 1, "one line for a solar panel, got " + lines.size());
        var contents = lines.get(0).getContents();
        helper.assertTrue(contents instanceof TranslatableContents t && t.getKey().equals(BlockInfoLines.KEY_SOLAR_DUST),
                "dust line key: " + contents);
        Object[] args = ((TranslatableContents) contents).getArgs();
        int expectedOutput = Math.round(SolarPanelBlockEntity.dustFactor(0.5F) * 100F);
        helper.assertTrue(args.length == 2 && Integer.valueOf(50).equals(args[0]) && Integer.valueOf(expectedOutput).equals(args[1]),
                "dust 50 %, output " + expectedOutput + " %: " + java.util.Arrays.toString(args));
        helper.succeed();
    }
}
