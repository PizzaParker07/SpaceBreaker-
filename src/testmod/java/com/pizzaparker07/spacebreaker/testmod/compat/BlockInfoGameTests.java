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

    /** Oxygen Filler: what is in the slot (tank fill, or nothing) and, from server data, the oxygen piped in. */
    @GameTest(template = "empty")
    public static void gasMachineShowsTankAndPipedOxygen(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_FILLER.get());
        com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity gas = helper.getBlockEntity(pos);
        helper.assertTrue(BlockInfoLines.wantsServerData(gas), "gas machines ask for server data");
        var empty = BlockInfoLines.lines(gas);
        helper.assertTrue(empty.size() == 1 && key(empty.get(0)).equals(BlockInfoLines.KEY_GAS_EMPTY), "empty slot line: " + empty);

        var tank = new net.minecraft.world.item.ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_TANK.get());
        int cap = com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.capacityOf(tank);
        com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.fill(tank, cap / 2);
        gas.setStack(tank);
        gas.getOxygenInput().fill(new net.neoforged.neoforge.fluids.FluidStack(
                com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.OXYGEN.get(), 600),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        BlockInfoLines.serverData(gas, data);
        helper.assertTrue(data.getInt(BlockInfoLines.DATA_PIPED_OXYGEN) == gas.storedOxygen() && gas.storedOxygen() > 0,
                "server data carries the piped oxygen: " + data);
        var lines = BlockInfoLines.lines(gas, data);
        helper.assertTrue(lines.size() == 2, "tank and piped lines, got " + lines);
        helper.assertTrue(key(lines.get(0)).equals(BlockInfoLines.KEY_GAS_TANK), "tank line: " + lines.get(0));
        Object[] args = ((TranslatableContents) lines.get(0).getContents()).getArgs();
        helper.assertTrue(Integer.valueOf(50).equals(args[2]), "half-full tank shows 50 %: " + java.util.Arrays.toString(args));
        helper.assertTrue(key(lines.get(1)).equals(BlockInfoLines.KEY_GAS_PIPED), "piped line: " + lines.get(1));
        helper.assertTrue(BlockInfoLines.lines(gas).size() == 1, "no piped line without server data");
        helper.succeed();
    }

    private static String key(net.minecraft.network.chat.Component line) {
        return line.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    /** Jade asserts (and fails resource loading) when a plugin component has no config name. */
    @GameTest(template = "empty")
    public static void jadeComponentHasAConfigName(GameTestHelper helper) throws java.io.IOException {
        for (String lang : new String[]{"en_us", "tr_tr"}) {
            try (var in = Spacebreaker.class.getResourceAsStream("/assets/spacebreaker/lang/" + lang + ".json")) {
                helper.assertTrue(in != null, "no " + lang);
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : new String[]{"config.jade.plugin_spacebreaker.info_lines", "config.jade.plugin_spacebreaker.gas_machine_fluid"}) {
                    helper.assertTrue(json.has(key), "Jade config name " + key + " missing in " + lang);
                }
            }
        }
        helper.succeed();
    }
}
