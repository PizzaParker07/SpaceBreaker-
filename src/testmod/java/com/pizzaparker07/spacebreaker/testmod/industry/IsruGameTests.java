package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.AtmosphereProcessorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Off-world industry: atmosphere processing, Sabatier, regolith oxygen, ice melting and the methane fuel chain. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IsruGameTests {
    private static MachineBlockEntity machine(GameTestHelper h, MachineEntry<?, ?> m, BlockPos pos, Direction facing) {
        h.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, facing));
        MachineBlockEntity be = h.getBlockEntity(pos);
        be.energy().generate(be.energy().getMaxEnergyStored());
        return be;
    }

    private static Item first(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag).flatMap(s -> s.stream().findFirst()).orElseThrow(() -> new AssertionError("tag empty: " + tag)).value();
    }

    /** A regolith that is not also solar-wind regolith (those give hydrogen and helium-3 first). */
    private static Item plainRegolith() {
        return BuiltInRegistries.ITEM.getTag(IsruRecipes.REGOLITHS).flatMap(s -> s.stream()
                        .filter(h -> !h.is(IsruRecipes.SOLAR_WIND_REGOLITHS)).findFirst())
                .orElseThrow(() -> new AssertionError("no plain regolith in " + IsruRecipes.REGOLITHS)).value();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void atmosphereProcessorSeparatesOxygenFromEarthAir(GameTestHelper h) {
        var y = AtmosphereProcessorBlockEntity.yieldAt(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
        h.assertTrue(y != null && y.gas() == FluidsModule.OXYGEN, "Earth air should give oxygen, got " + (y == null ? null : y.gas().name));
        MachineBlockEntity be = machine(h, MachinesModule.ATMOSPHERE_PROCESSOR, new BlockPos(2, 1, 2), Direction.NORTH);
        h.succeedWhen(() -> h.assertTrue(be.tank(0).getFluid().is(FluidsModule.TAG_OXYGEN), "no oxygen collected"));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void sabatierMakesMethaneAndWater(GameTestHelper h) {
        MachineBlockEntity be = machine(h, MachinesModule.SABATIER_REACTOR, new BlockPos(2, 1, 2), Direction.NORTH);
        be.fluidCapability(null).fill(FluidsModule.HYDROGEN.stack(4_000), IFluidHandler.FluidAction.EXECUTE);
        be.fluidCapability(null).fill(FluidsModule.CARBON_DIOXIDE.stack(1_000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(be.tank(0).getFluidAmount() > 0 && be.tank(1).getFluidAmount() > 0, "both gases need their own input tank");
        h.succeedWhen(() -> {
            h.assertTrue(be.tank(2).getFluid().is(FluidsModule.TAG_METHANE) && be.tank(2).getFluidAmount() == 1_000, "methane " + be.tank(2).getFluidAmount());
            h.assertTrue(be.tank(3).getFluid().is(Fluids.WATER) && be.tank(3).getFluidAmount() == 500, "water " + be.tank(3).getFluidAmount());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void regolithHeaterReleasesOxygen(GameTestHelper h) {
        MachineBlockEntity be = machine(h, MachinesModule.REGOLITH_HEATER, new BlockPos(2, 1, 2), Direction.NORTH);
        be.items().setStackInSlot(0, new ItemStack(plainRegolith(), 2));
        h.succeedWhen(() -> h.assertTrue(be.tank(0).getFluid().is(FluidsModule.TAG_OXYGEN) && be.tank(0).getFluidAmount() >= 500,
                "oxygen " + be.tank(0).getFluidAmount()));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void iceMelterMeltsIceAndSublimatesDryIce(GameTestHelper h) {
        MachineBlockEntity water = machine(h, MachinesModule.ICE_MELTER, new BlockPos(1, 1, 2), Direction.NORTH);
        water.items().setStackInSlot(0, new ItemStack(Items.ICE, 2));
        MachineBlockEntity co2 = machine(h, MachinesModule.ICE_MELTER, new BlockPos(4, 1, 2), Direction.NORTH);
        co2.items().setStackInSlot(0, new ItemStack(first(IsruRecipes.DRY_ICES)));
        h.succeedWhen(() -> {
            h.assertTrue(water.tank(0).getFluidAmount() == 2_000, "water " + water.tank(0).getFluidAmount());
            h.assertTrue(co2.tank(0).getFluid().is(FluidsModule.TAG_CO2), "no carbon dioxide");
        });
    }

    /**
     * Mars fuel plant without player hand-offs: ice -> melter -> electrolyzer; hydrogen (left outlet) and
     * dry-ice CO2 -> Sabatier -> methane -> liquefier (liquid methane); oxygen (right outlet) -> liquefier (LOX);
     * Sabatier water flows back into the electrolyzer.
     */
    @GameTest(template = "empty", timeoutTicks = 2400)
    public static void marsFuelPlantMakesLiquidMethaneAndLox(GameTestHelper h) {
        MachineBlockEntity iceMelter = machine(h, MachinesModule.ICE_MELTER, new BlockPos(2, 1, 0), Direction.NORTH);
        iceMelter.items().setStackInSlot(0, new ItemStack(Items.ICE, 16));
        MachineBlockEntity dryIceMelter = machine(h, MachinesModule.ICE_MELTER, new BlockPos(3, 1, 0), Direction.NORTH);
        dryIceMelter.items().setStackInSlot(0, new ItemStack(first(IsruRecipes.DRY_ICES), 8));
        machine(h, MachinesModule.ELECTROLYZER, new BlockPos(2, 1, 1), Direction.NORTH);
        machine(h, MachinesModule.SABATIER_REACTOR, new BlockPos(3, 1, 1), Direction.NORTH);
        MachineBlockEntity loxPlant = machine(h, MachinesModule.LIQUEFIER, new BlockPos(1, 1, 1), Direction.NORTH);
        MachineBlockEntity methanePlant = machine(h, MachinesModule.LIQUEFIER, new BlockPos(4, 1, 1), Direction.NORTH);
        h.succeedWhen(() -> {
            h.assertTrue(methanePlant.tank(1).getFluid().is(FluidsModule.TAG_LCH4) && methanePlant.tank(1).getFluidAmount() >= 250,
                    "liquid methane " + methanePlant.tank(1).getFluidAmount());
            h.assertTrue(loxPlant.tank(1).getFluid().is(FluidsModule.TAG_LOX) && loxPlant.tank(1).getFluidAmount() >= 250,
                    "LOX " + loxPlant.tank(1).getFluidAmount());
        });
    }

    @GameTest(template = "empty")
    public static void solarPanelsFollowTheSunDistance(GameTestHelper h) {
        double earth = com.pizzaparker07.spacebreaker.core.env.Environment.solarFactor(h.getLevel());
        h.assertTrue(Math.abs(earth - 1.0) < 0.01, "Earth sunlight factor should be 1, got " + earth);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "overworld_rain")
    public static void moonPanelsIgnoreOverworldRain(GameTestHelper h) {
        var moon = com.pizzaparker07.spacebreaker.core.body.Bodies.get(h.getLevel().registryAccess(), com.pizzaparker07.spacebreaker.core.body.Bodies.MOON)
                .orElseThrow(() -> new AssertionError("moon body missing"));
        h.assertTrue(moon.atmosphere().isVacuum(), "the Moon is airless");
        // other dimensions see the Overworld's rain flag; in vacuum (and where no rain falls) panels keep full output
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.weatherFactor(true, true, true) == 1F,
                "a vacuum world must ignore the shared Overworld storm");
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.weatherFactor(false, false, true) == 1F,
                "no rain falling here means no penalty");
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.weatherFactor(false, true, false) < 1F,
                "rain still dims panels where it falls");
        // and the real Overworld path: raining world, open sky in a rainy biome -> dimmed
        var overworld = h.getLevel();
        overworld.setWeatherParameters(0, 6000, true, false);
        BlockPos here = h.absolutePos(new BlockPos(2, 1, 2));
        boolean falls = overworld.isRainingAt(here.above());
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.weatherFactor(overworld, here) == (falls ? 0.45F : 1F),
                "Overworld factor must follow rain falling at the panel");
        overworld.setWeatherParameters(6000, 0, false, false);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void planetFeedstockIsTagged(GameTestHelper h) {
        String[][] expected = {
                {"regoliths", "lunar_regolith", "martian_regolith", "phobos_regolith", "deimos_regolith"},
                {"ices", "lunar_ice", "martian_ice", "icy_regolith"},
                {"dry_ices", "dry_ice"},
                {"perchlorate_soils", "perchlorate_soil"}};
        for (String[] row : expected) {
            TagKey<Item> tag = com.pizzaparker07.spacebreaker.core.data.SbTagKeys.commonItem(row[0]);
            for (int i = 1; i < row.length; i++) {
                Item item = BuiltInRegistries.ITEM.get(Spacebreaker.id(row[i]));
                h.assertTrue(item != Items.AIR, "spacebreaker:" + row[i] + " is not registered");
                h.assertTrue(item.builtInRegistryHolder().is(tag), "spacebreaker:" + row[i] + " is missing from #c:" + row[0]);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void planetProcessFluidsRouteCorrectly(GameTestHelper h) {
        h.assertTrue(FluidsModule.SULFURIC_ACID.get().is(FluidsModule.TAG_SULFURIC_ACID), "c:sulfuric_acid");
        h.assertTrue(FluidsModule.HELIUM_3.get().is(FluidsModule.TAG_HELIUM_3), "c:helium_3");
        h.assertTrue(FluidsModule.SULFURIC_ACID.block == null && FluidsModule.SULFURIC_ACID.bucket == null, "acid stays in tanks and pipes");
        var fluid = com.pizzaparker07.spacebreaker.content.machines.net.ConduitType.FLUID_PIPE;
        var gas = com.pizzaparker07.spacebreaker.content.machines.net.ConduitType.GAS_PIPE;
        h.assertTrue(fluid.accepts(FluidsModule.SULFURIC_ACID.stack(100)) && !gas.accepts(FluidsModule.SULFURIC_ACID.stack(100)), "acid is a liquid");
        h.assertTrue(gas.accepts(FluidsModule.HELIUM_3.stack(100)) && !fluid.accepts(FluidsModule.HELIUM_3.stack(100)), "helium-3 is a gas");
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.REGOLITH_PROCESSING.type().get());
        h.assertTrue(recipes.stream().anyMatch(r -> r.id().getPath().endsWith("perchlorate_salt")), "perchlorate salt recipe missing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chemicalReactorTakesTwoFluidsAndMakesAcid(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.CHEMICAL_REACTION.type().get());
        var acid = recipes.stream().filter(r -> r.id().getPath().endsWith("sulfuric_acid")).findFirst()
                .orElseThrow(() -> new AssertionError("sulfuric acid recipe missing")).value();
        h.assertTrue(acid.fluidInput.isPresent() && acid.fluidInput2.isPresent(), "acid needs water and oxygen");
        h.assertTrue(acid.fluidResults.get(0).is(FluidsModule.TAG_SULFURIC_ACID), "acid output");
        MachineBlockEntity be = machine(h, MachinesModule.CHEMICAL_REACTOR, new BlockPos(2, 1, 2), Direction.NORTH);
        be.fluidCapability(null).fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        be.fluidCapability(null).fill(FluidsModule.OXYGEN.stack(3_000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(be.tank(0).getFluid().is(Fluids.WATER) && be.tank(1).getFluid().is(FluidsModule.TAG_OXYGEN), "each fluid gets its own input tank");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void solarWindRecipeWinsOverPlainRegolith(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.REGOLITH_PROCESSING.type().get()).stream()
                .filter(r -> r.id().getPath().endsWith("solar_wind_volatiles")).findFirst()
                .orElseThrow(() -> new AssertionError("solar wind recipe missing"));
        h.assertTrue(holder.value().priority == 1, "solar wind volatiles must have priority 1 (loaded " + holder.value().priority + ")");
        var plain = h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.REGOLITH_PROCESSING.type().get()).stream()
                .filter(r -> r.id().getPath().endsWith("/regolith")).findFirst().orElseThrow();
        h.assertTrue(plain.value().priority == 0, "plain regolith keeps the default priority");
        h.succeed();
    }

    /**
     * Life support from rock: regolith -> regolith heater -> two gas pipes -> the suit area's oxygen filler (its
     * piped buffer) -> an oxygen tank. The Overworld air would fill tanks for free, so the test drives the filler
     * from its buffer explicitly.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void regolithOxygenFillsSuitTanksThroughPipes(GameTestHelper h) {
        Item regolith = BuiltInRegistries.ITEM.getTag(IsruRecipes.REGOLITHS).flatMap(set -> set.stream()
                        .filter(hd -> !hd.is(IsruRecipes.SOLAR_WIND_REGOLITHS)).findFirst())
                .orElseThrow(() -> new AssertionError("no plain regolith")).value();
        MachineBlockEntity heater = machine(h, MachinesModule.REGOLITH_HEATER, new BlockPos(1, 1, 2), Direction.NORTH);
        heater.items().setStackInSlot(0, new ItemStack(regolith, 16));
        h.setBlock(new BlockPos(2, 1, 2), MachinesModule.GAS_PIPE.get());
        h.setBlock(new BlockPos(3, 1, 2), MachinesModule.GAS_PIPE.get());
        BlockPos fillerPos = new BlockPos(4, 1, 2);
        h.setBlock(fillerPos, com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_FILLER.get());
        var filler = (com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity) h.getBlockEntity(fillerPos);
        filler.getEnergy().receiveEnergy(filler.getEnergy().getMaxEnergyStored(), false);
        h.succeedWhen(() -> {
            h.assertTrue(heater.tank(0).getFluidAmount() < 250 || filler.storedOxygen() > 0, "heater oxygen is not leaving through the pipes");
            h.assertTrue(filler.storedOxygen() >= 1_000, "filler buffer " + filler.storedOxygen());
            ItemStack tank = new ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_TANK.get());
            filler.setStack(tank);
            int before = filler.storedOxygen();
            for (int i = 0; i < 20; i++) {
                filler.pumpTick(false);
            }
            int got = com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem.getOxygen(filler.getStack());
            h.assertTrue(got > 0, "the tank got no oxygen from the piped buffer");
            h.assertTrue(filler.storedOxygen() == before - got, "oxygen must come out of the piped buffer");
        });
    }

    private IsruGameTests() {
    }
}
