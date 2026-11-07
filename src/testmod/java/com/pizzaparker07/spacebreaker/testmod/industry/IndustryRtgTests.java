package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.earth.EarthWorldgen;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.RtgFuelPelletItem;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.RtgBlockEntity;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** RTG power, pellet decay and dose, the uraninite fuel chain, and what the sun is worth on each world. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryRtgTests {
    private static MachineBlockEntity machine(GameTestHelper h, MachineEntry<?, ?> m, BlockPos pos, boolean charged) {
        h.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity be = h.getBlockEntity(pos);
        if (charged) {
            be.energy().generate(be.energy().getMaxEnergyStored());
        }
        return be;
    }

    /** Fresh pellet: full output under a roof (no sky needed); the battery next to it fills. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rtgPowersABatteryUnderARoof(GameTestHelper h) {
        MachineBlockEntity rtg = machine(h, MachinesModule.RTG, new BlockPos(2, 1, 2), false);
        rtg.items().setStackInSlot(0, new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        h.setBlock(new BlockPos(2, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        MachineBlockEntity battery = machine(h, MachinesModule.BATTERY_BANK, new BlockPos(3, 1, 2), false);
        h.succeedWhen(() -> {
            h.assertTrue(rtg.items().getStackInSlot(0).has(MachinesModule.DECAY_START.get()), "the pellet is time-stamped in the RTG");
            h.assertTrue(rtg.progress() >= 980, "fresh pellet shows (almost) full activity, got " + rtg.progress());
            h.assertTrue(battery.energy().getEnergyStored() >= RtgBlockEntity.OUTPUT * 40, "battery " + battery.energy().getEnergyStored());
        });
    }

    @GameTest(template = "empty")
    public static void pelletDecaysWithItsHalfLife(GameTestHelper h) {
        h.assertTrue(RtgBlockEntity.output(RtgFuelPelletItem.activity(0)) == RtgBlockEntity.OUTPUT, "fresh = full output");
        h.assertTrue(RtgBlockEntity.output(RtgFuelPelletItem.activity(RtgFuelPelletItem.HALF_LIFE)) == RtgBlockEntity.OUTPUT / 2, "one half-life = half");
        h.assertTrue(RtgFuelPelletItem.HALF_LIFE >= 24_000L * 7 && RtgFuelPelletItem.HALF_LIFE <= 24_000L * 60,
                "half-life should be a few in-game weeks");
        ItemStack pellet = new ItemStack(MachinesModule.RTG_FUEL_PELLET.get());
        pellet.set(MachinesModule.DECAY_START.get(), 1_000L);
        double a = RtgFuelPelletItem.activity(pellet, 1_000L + 2 * RtgFuelPelletItem.HALF_LIFE);
        h.assertTrue(Math.abs(a - 0.25) < 1e-9, "two half-lives = a quarter, got " + a);
        h.succeed();
    }

    /** Carried pellets add a small dose through the Environment hook; the shielded RTG adds none. */
    @GameTest(template = "empty")
    public static void carriedPelletsGiveASmallDose(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        long now = h.getLevel().getGameTime();
        h.assertTrue(RtgFuelPelletItem.inventoryDose(player, now) == 0, "empty inventory, no dose");
        player.getInventory().add(new ItemStack(MachinesModule.RTG_FUEL_PELLET.get(), 4));
        double dose = RtgFuelPelletItem.inventoryDose(player, now);
        h.assertTrue(Math.abs(dose - 4 * RtgFuelPelletItem.DOSE_PER_PELLET) < 1e-6, "4 fresh pellets, got " + dose);
        // small: one pellet is a few times the Moon's surface dose, far below the 500 mSv where symptoms start in a day
        h.assertTrue(RtgFuelPelletItem.DOSE_PER_PELLET < 50, "one pellet should be a small dose");
        BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(Environment.radiation(h.getLevel(), at) >= Environment.skyRadiation(h.getLevel(), at), "local sources only add");
        machine(h, MachinesModule.RTG, new BlockPos(1, 1, 1), false).items()
                .setStackInSlot(0, new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        h.assertTrue(RtgFuelPelletItem.carriedDose(h.getLevel(), at.above()) == 0, "a pellet inside the RTG is shielded");
        h.succeed();
    }

    /** Uraninite ore -> crusher -> uranium dust; dust + sulfuric acid -> yellowcake; 4 yellowcake -> sintering furnace -> pellet. */
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void uraniniteBecomesRtgFuel(GameTestHelper h) {
        h.assertTrue(EarthWorldgen.VEINS.stream().anyMatch(v -> v.metal() == EarthModule.URANIUM && v.maxY() <= 0),
                "uraninite must generate deep");
        h.assertTrue(EarthModule.URANIUM.oreItem.get().builtInRegistryHolder().is(net.neoforged.neoforge.common.Tags.Items.ORES), "#c:ores");
        MachineBlockEntity crusher = machine(h, MachinesModule.CRUSHER, new BlockPos(1, 1, 1), true);
        crusher.items().setStackInSlot(0, new ItemStack(EarthModule.URANIUM.oreItem.get(), 2));
        MachineBlockEntity reactor = machine(h, MachinesModule.CHEMICAL_REACTOR, new BlockPos(3, 1, 1), true);
        reactor.tank(0).fill(new FluidStack(FluidsModule.SULFURIC_ACID.get(), 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        MachineBlockEntity sinter = machine(h, MachinesModule.SINTERING_FURNACE, new BlockPos(5, 1, 1), true);
        h.succeedWhen(() -> {
            // uraninite -> crusher dust -> leached yellowcake -> sintered pellet, moving each step by hand
            for (int i = 1; i < crusher.items().getSlots(); i++) {
                ItemStack s = crusher.items().getStackInSlot(i);
                if (s.is(EarthModule.URANIUM.dust.get())) {
                    ItemStack in = reactor.items().getStackInSlot(0);
                    if (in.isEmpty() || in.is(s.getItem())) {
                        reactor.items().setStackInSlot(0, s.copyWithCount(in.getCount() + s.getCount()));
                        crusher.items().setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
            ItemStack cake = reactor.items().getStackInSlot(1);
            if (cake.is(MachinesModule.YELLOWCAKE.get()) && cake.getCount() >= 4 && sinter.items().getStackInSlot(0).isEmpty()) {
                sinter.items().setStackInSlot(0, cake.copyWithCount(4));
                reactor.items().setStackInSlot(1, cake.copyWithCount(cake.getCount() - 4));
            }
            h.assertTrue(sinter.items().getStackInSlot(1).is(MachinesModule.RTG_FUEL_PELLET.get()), "no pellet yet; yellowcake " + cake.getCount());
        });
    }

    /**
     * The nuclear chain: uranium dust is leached to yellowcake, yellowcake is sintered into RTG pellets or spun in
     * the gas centrifuge into enriched and depleted uranium, and only enriched uranium makes reactor fuel.
     */
    @GameTest(template = "empty")
    public static void nuclearChainRecipesFitTogether(GameTestHelper h) {
        var rm = h.getLevel().getRecipeManager();
        h.assertTrue(rm.getAllRecipesFor(MachinesModule.CHEMICAL_REACTION.type().get()).stream()
                .anyMatch(r -> r.value().results.stream().anyMatch(s -> s.is(MachinesModule.YELLOWCAKE.get()))), "a chemical reaction makes yellowcake");
        h.assertTrue(rm.getAllRecipesFor(MachinesModule.SINTERING.type().get()).stream()
                .anyMatch(r -> r.value().results.stream().anyMatch(s -> s.is(MachinesModule.RTG_FUEL_PELLET.get()))), "sintering makes RTG pellets");
        var spin = rm.getAllRecipesFor(MachinesModule.CENTRIFUGING.type().get());
        h.assertTrue(spin.stream().anyMatch(r -> r.value().results.stream().anyMatch(s -> s.is(MachinesModule.ENRICHED_URANIUM.get()))
                && r.value().results.stream().anyMatch(s -> s.is(MachinesModule.DEPLETED_URANIUM.get()))), "the centrifuge gives enriched and depleted uranium");
        var fuel = rm.byKey(Spacebreaker.id("arc_smelting/nuclear_fuel_element")).orElseThrow(() -> new AssertionError("fuel element recipe"));
        h.assertTrue(fuel.value().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(MachinesModule.ENRICHED_URANIUM.get()))), "fuel needs enriched uranium");
        h.assertTrue(fuel.value().getIngredients().stream().noneMatch(i -> i.test(new ItemStack(EarthModule.URANIUM.dust.get()))), "raw uranium dust is not enough");
        h.succeed();
    }

    /** One centrifuge alone is slow; the middle of a three-machine cascade runs twice as fast. */
    @GameTest(template = "empty", timeoutTicks = 700)
    public static void centrifugeCascadeIsFaster(GameTestHelper h) {
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.machine.GasCentrifugeBlockEntity.speed(0) == 1
                && com.pizzaparker07.spacebreaker.content.machines.machine.GasCentrifugeBlockEntity.speed(2) == 2, "half a stage per neighbour");
        MachineBlockEntity lone = machine(h, MachinesModule.GAS_CENTRIFUGE, new BlockPos(0, 1, 4), true);
        MachineBlockEntity[] row = new MachineBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            row[i] = machine(h, MachinesModule.GAS_CENTRIFUGE, new BlockPos(2 + i, 1, 1), true);
        }
        for (MachineBlockEntity m : new MachineBlockEntity[]{lone, row[0], row[1], row[2]}) {
            m.items().setStackInSlot(0, new ItemStack(MachinesModule.YELLOWCAKE.get(), 4));
        }
        h.succeedWhen(() -> {
            for (MachineBlockEntity m : new MachineBlockEntity[]{lone, row[0], row[1], row[2]}) {
                m.energy().generate(1_000);
            }
            h.assertTrue(row[1].items().getStackInSlot(1).is(MachinesModule.ENRICHED_URANIUM.get()), "the middle stage finishes first");
            h.assertTrue(row[1].items().getStackInSlot(2).getCount() == 3, "with three depleted uranium");
            h.assertTrue(lone.items().getStackInSlot(1).isEmpty(), "the lone centrifuge is still spinning (" + lone.progress() + ")");
        });
    }

    /** The panel tells the player how much sunlight there is: about 4 % of Earth's at Jupiter. */
    @GameTest(template = "empty")
    public static void solarPanelShowsSunlightHere(GameTestHelper h) {
        var bodies = h.getLevel().registryAccess().registryOrThrow(Bodies.REGISTRY);
        var europa = bodies.getOptional(Spacebreaker.id("europa")).orElseThrow(() -> new AssertionError("europa body missing"));
        double f = europa.solarIrradiance() / 1361.0;
        String pct = SolarPanelBlock.sunlightPercent(f);
        h.assertTrue(pct.equals("3.7"), "Europa sunlight should read 3.7 %, got " + pct);
        h.assertTrue(SolarPanelBlock.sunlightPercent(1.0).equals("100"), "Earth reads 100 %");
        h.assertTrue(SolarPanelBlock.sunlightPercent(0.0006).equals("0.1"), "Pluto still shows a value");
        h.succeed();
    }

    /**
     * Moon night: one battery bank carries a modest base through the dark. Numbers for the guide table:
     * night length from the planet clock, energy for a 10 FE/t load.
     */
    @GameTest(template = "empty")
    public static void oneBatteryBankCoversAMoonNightAt10FePerTick(GameTestHelper h) {
        var moon = Bodies.get(h.getLevel().registryAccess(), Bodies.MOON).orElseThrow(() -> new AssertionError("moon body missing"));
        long day = PlanetTime.dayTicks(moon.rotation().dayLengthHours());
        long night = day / 2;
        long need = night * 10L;
        h.assertTrue(need <= BatteryBankBlockEntity.CAPACITY, "Moon night " + night + " ticks needs " + need + " FE for 10 FE/t");
        h.assertTrue(need * 4 > BatteryBankBlockEntity.CAPACITY, "the night should still matter for a 40 FE/t base");
        h.succeed();
    }

    /** No air, no extra loss with the sun low: the Moon and stations keep more of the morning and evening. */
    @GameTest(template = "empty")
    public static void vacuumPanelsLoseNothingNearTheHorizon(GameTestHelper h) {
        float low = (float) Math.toRadians(75);
        float air = com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.irradiance(low, false);
        float vac = com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.irradiance(low, true);
        h.assertTrue(vac > air * 1.3F, "low sun: air " + air + " vacuum " + vac);
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.irradiance(0F, true) == 1F, "noon is full output");
        h.succeed();
    }

    /** The station array is three trackers on one tube: three times the output from one block of footprint. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void solarArrayMakesThreeTimesATracker(GameTestHelper h) {
        h.getLevel().setDayTime(6000);
        h.setBlock(new BlockPos(1, 1, 2), MachinesModule.SOLAR_PANEL.get());
        h.setBlock(new BlockPos(4, 1, 2), MachinesModule.SOLAR_ARRAY.get());
        h.succeedWhen(() -> {
            com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity panel = h.getBlockEntity(new BlockPos(1, 1, 2));
            com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity array = h.getBlockEntity(new BlockPos(4, 1, 2));
            h.assertTrue(panel.getExactOutput() > 0, "the tracker sees the sun");
            double ratio = array.getExactOutput() / panel.getExactOutput();
            h.assertTrue(Math.abs(ratio - 3) < 0.01, "array / tracker = " + ratio);
            h.assertTrue(array.getEnergy().getEnergyStored() > 0, "the array stores what it makes");
        });
    }

    /** A broken tank keeps its fluid on the item and puts it back when placed: fuel can be carried to a station. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void brokenTankKeepsItsFuel(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, MachinesModule.CRYOGENIC_TANK.get());
        com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity tank = h.getBlockEntity(at);
        tank.tank().fill(FluidsModule.LIQUID_HYDROGEN.stack(12_000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        h.getLevel().destroyBlock(h.absolutePos(at), true);
        h.runAfterDelay(2, () -> {
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(at)).inflate(2));
            h.assertTrue(drops.size() == 1, "one tank item, got " + drops.size());
            ItemStack item = drops.get(0).getItem();
            var content = item.get(FluidsModule.FLUID_CONTENT.get());
            h.assertTrue(content != null && content.copy().getAmount() == 12_000 && content.copy().is(FluidsModule.TAG_LH2), "contents on the item: " + content);
            BlockPos again = new BlockPos(4, 1, 2);
            h.setBlock(again, MachinesModule.CRYOGENIC_TANK.get());
            com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity placed = h.getBlockEntity(again);
            placed.applyComponentsFromItemStack(item);
            h.assertTrue(placed.tank().getFluidAmount() == 12_000, "placed tank holds " + placed.tank().getFluidAmount());
            h.succeed();
        });
    }

    /** A fluid pipe pulling from an endless water source feeds an electrolyser without anyone carrying buckets. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pipeDrawsFromAnEndlessWaterSource(GameTestHelper h) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 2; z++) {
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }
        h.setBlock(new BlockPos(1, 0, 2), net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 2), MachinesModule.FLUID_PIPE.get());
        MachineBlockEntity cell = machine(h, MachinesModule.ELECTROLYZER, new BlockPos(1, 1, 3), false);
        h.runAfterDelay(2, () -> {
            BlockPos pipe = h.absolutePos(new BlockPos(1, 1, 2));
            var state = h.getLevel().getBlockState(pipe);
            h.assertTrue(state.getValue(com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock.SIDES.get(Direction.NORTH)).connected(),
                    "pipe should connect to the water: " + state);
            h.getLevel().setBlock(pipe, state.setValue(com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock.SIDES.get(Direction.NORTH),
                    com.pizzaparker07.spacebreaker.content.machines.net.ConduitSide.EXTRACT), 3);
        });
        h.succeedWhen(() -> {
            h.assertTrue(cell.tank(0).getFluidAmount() >= 1000, "water in the electrolyser " + cell.tank(0).getFluidAmount());
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(new BlockPos(1, 1, 1))).isSource(), "the source is not used up");
        });
    }

    @GameTest(template = "empty", batch = "industry_power")
    public static void powerMonitorMaths(GameTestHelper h) {
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlockEntity.ticksLeft(500, 1000, 10) == 50, "fills in 50 ticks");
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlockEntity.ticksLeft(500, 1000, -25) == 20, "empties in 20 ticks");
        h.assertTrue(com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlockEntity.ticksLeft(500, 1000, 0) == -1, "steady: no estimate");
        h.succeed();
    }

    /** A monitor against a battery bank reads its charge and the net flow into it. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "industry_power")
    public static void powerMonitorReadsABatteryBank(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), MachinesModule.BATTERY_BANK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity bank = h.getBlockEntity(new BlockPos(2, 1, 2));
        bank.energy().generate(250_000);
        h.setBlock(new BlockPos(2, 1, 3), MachinesModule.POWER_MONITOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlock.FACING, Direction.SOUTH));
        com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlockEntity monitor = h.getBlockEntity(new BlockPos(2, 1, 3));
        h.succeedWhen(() -> {
            bank.energy().generate(100);
            h.assertTrue(monitor.found(), "monitor should find the battery bank behind it");
            h.assertTrue(monitor.capacity() == 1_000_000 && monitor.stored() >= 250_000, "stored " + monitor.stored() + " / " + monitor.capacity());
            h.assertTrue(monitor.net() > 80 && monitor.net() < 120, "net flow " + monitor.net());
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(2, 1, 3))).getAnalogOutputSignal(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 3))) >= 3,
                    "comparator follows the charge");
        });
    }

    /**
     * Station power through the orbit's shadow: the habitat kit's single solar array (with its own buffer) and the fuel
     * depot's buffer, a big stage docking right as the station enters shadow, every lap for three laps. The pumps
     * must never stall long; the array must spend about a third of each lap dark.
     */
    @GameTest(template = "empty", batch = "industry_power")
    public static void stationArrayCarriesTheDepotThroughShadow(GameTestHelper h) {
        var sys = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(h.getLevel().registryAccess());
        var earth = Spacebreaker.id("earth");
        int lap = (int) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.ORBIT_TICKS;
        double lit = 0;
        int firstDark = -1;
        for (int t = 0; t < lap; t++) {
            double s = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(sys, earth, t);
            lit += s;
            if (firstDark < 0 && s < 0.01) {
                firstDark = t;
            }
        }
        lit /= lap;
        h.assertTrue(lit > 0.5 && lit < 0.8, "sunlit share of a lap " + lit);
        h.assertTrue(firstDark >= 0, "a low orbit has a shadow");
        int arrayCap = com.pizzaparker07.spacebreaker.content.machines.SolarArrayBlock.PEAK_OUTPUT * 400;
        int depotCap = com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity.ENERGY_CAPACITY;
        int maxIn = com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity.MAX_INPUT;
        int pumpCost = com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity.PUMP_FE_PER_TICK;
        int mbPerTick = com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity.PUMP_MB
                / com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity.PUMP_INTERVAL;
        double array = 0, depot = 0;
        int stalled = 0;
        for (int t = 0; t < 4 * lap; t++) {
            double sun = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(sys, earth, t);
            array = Math.min(arrayCap, array + com.pizzaparker07.spacebreaker.content.machines.SolarArrayBlock.PEAK_OUTPUT * sun);
            double move = Math.min(Math.min(maxIn, array), depotCap - depot);
            array -= move;
            depot += move;
            // from the second lap on, a 100 000 mB stage docks as the station goes dark and is pumped full
            int inLap = t % lap;
            boolean pumping = t >= lap && inLap >= firstDark && inLap < firstDark + 100_000 / mbPerTick;
            if (pumping) {
                if (depot >= pumpCost) {
                    depot -= pumpCost;
                } else {
                    stalled++;
                }
            }
        }
        h.assertTrue(stalled == 0, "the depot ran dry for " + stalled + " ticks of pumping in the dark");
        h.succeed();
    }

    /**
     * Habitat roof: solar array on the roof, a ceiling-mounted battery bank in the roof under it (output facing down),
     * and whatever hangs below in the room gets power - here a second bank standing in for the room's machines.
     */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "industry_power")
    public static void roofBatteryChargesFromTheArrayAndPowersTheRoom(GameTestHelper h) {
        h.getLevel().setDayTime(6000);
        BlockPos room = new BlockPos(2, 1, 2);
        BlockPos roof = room.above();
        h.setBlock(room, MachinesModule.BATTERY_BANK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        h.setBlock(roof, MachinesModule.BATTERY_BANK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH)
                .setValue(com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlock.OUTPUT_DOWN, true));
        h.setBlock(roof.above(), MachinesModule.SOLAR_ARRAY.get());
        MachineBlockEntity roofBank = h.getBlockEntity(roof);
        MachineBlockEntity roomBank = h.getBlockEntity(room);
        roofBank.energy().generate(10_000);
        h.assertTrue(roofBank.energyCapability(Direction.UP).canReceive(), "the roof bank takes power from the array above");
        h.assertTrue(roofBank.energyCapability(Direction.DOWN).canExtract(), "and gives it out downwards");
        h.succeedWhen(() -> h.assertTrue(roomBank.energy().getEnergyStored() >= 5_000, "room got " + roomBank.energy().getEnergyStored()));
    }

    private static volatile BlockPos halfLight;
    private static boolean modifierAdded;

    /**
     * Sunlight modifiers (dust storms, the orbit's shadow) reach a panel exactly once, through Environment.solarFactor:
     * a test modifier that halves the light at one panel halves its output, not more.
     */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "industry_power")
    public static void sunlightModifiersApplyOnce(GameTestHelper h) {
        if (!modifierAdded) {
            modifierAdded = true;
            Environment.addSunlightModifier((level, pos) -> pos.equals(halfLight) ? 0.5 : 1.0);
        }
        h.getLevel().setDayTime(6000);
        BlockPos plain = new BlockPos(1, 1, 2);
        BlockPos dimmed = new BlockPos(4, 1, 2);
        halfLight = h.absolutePos(dimmed);
        h.setBlock(plain, MachinesModule.SOLAR_PANEL.get());
        h.setBlock(dimmed, MachinesModule.SOLAR_PANEL.get());
        h.succeedWhen(() -> {
            com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity a = h.getBlockEntity(plain);
            com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity b = h.getBlockEntity(dimmed);
            h.assertTrue(a.getExactOutput() > 0, "the plain panel sees the sun");
            double ratio = b.getExactOutput() / a.getExactOutput();
            h.assertTrue(Math.abs(ratio - 0.5) < 0.01, "half the light should give half the power, got " + ratio);
            halfLight = null;
        });
    }

    /** Dust on the cells cuts output to 60% when fully covered; wiping (or a dust devil) brings it back. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "industry_power")
    public static void dustyPanelLosesOutputUntilCleaned(GameTestHelper h) {
        h.getLevel().setDayTime(6000);
        BlockPos cleanAt = new BlockPos(1, 1, 2);
        BlockPos dustyAt = new BlockPos(4, 1, 2);
        h.setBlock(cleanAt, MachinesModule.SOLAR_PANEL.get());
        h.setBlock(dustyAt, MachinesModule.SOLAR_PANEL.get());
        com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity dusty = h.getBlockEntity(dustyAt);
        dusty.setDust(1F);
        h.assertTrue(com.pizzaparker07.spacebreaker.core.env.Environment.dustRate(h.getLevel(), h.absolutePos(dustyAt)) == 0,
                "no dust settles on Earth");
        var tag = dusty.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(Math.abs(tag.getFloat("dust") - 1F) < 1e-6, "dust is saved");
        h.runAfterDelay(3, () -> {
            com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity clean = h.getBlockEntity(cleanAt);
            h.assertTrue(clean.getExactOutput() > 0, "the clean panel sees the sun");
            double ratio = dusty.getExactOutput() / clean.getExactOutput();
            h.assertTrue(Math.abs(ratio - 0.6) < 0.01, "covered / clean = " + ratio);
            float removed = dusty.clean(0.5F);
            h.assertTrue(Math.abs(removed - 0.5F) < 1e-6 && Math.abs(dusty.dust() - 0.5F) < 1e-6, "half wiped: " + dusty.dust());
            dusty.clean(1F);
            h.runAfterDelay(2, () -> {
                double after = dusty.getExactOutput() / clean.getExactOutput();
                h.assertTrue(Math.abs(after - 1) < 0.01, "wiped / clean = " + after);
                h.succeed();
            });
        });
    }

    /** Reactor fuel for the nuclear thermal rocket: made in the arc furnace and listed in spacebreaker:reactor_fuel. */
    @GameTest(template = "empty", batch = "industry_power")
    public static void nuclearFuelElementIsMadeAndTagged(GameTestHelper h) {
        ItemStack element = new ItemStack(MachinesModule.NUCLEAR_FUEL_ELEMENT.get());
        h.assertTrue(element.is(com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.REACTOR_FUEL), "the element is reactor fuel");
        var recipe = h.getLevel().getRecipeManager().byKey(com.pizzaparker07.spacebreaker.Spacebreaker.id("arc_smelting/nuclear_fuel_element"));
        h.assertTrue(recipe.isPresent(), "arc furnace recipe for the fuel element");
        h.assertTrue(recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(MachinesModule.NUCLEAR_FUEL_ELEMENT.get()),
                "the recipe makes the element");
        h.succeed();
    }

    /** Depleted uranium from the centrifuge is pressed into plate that counts as radiation shielding, like tungsten. */
    @GameTest(template = "empty")
    public static void depletedUraniumIsRadiationShielding(GameTestHelper h) {
        var shielding = com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.RADIATION_SHIELDING;
        h.assertTrue(new ItemStack(MachinesModule.DEPLETED_URANIUM_PLATE.get()).is(shielding), "depleted uranium plate shields");
        h.assertTrue(new ItemStack(EarthModule.TUNGSTEN.plate.get()).is(shielding), "tungsten plate shields too");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(MachinesModule.PRESSING.type().get()).stream()
                .anyMatch(r -> r.value().results.stream().anyMatch(s -> s.is(MachinesModule.DEPLETED_URANIUM_PLATE.get()))
                        && r.value().ingredients.stream().anyMatch(i -> i.ingredient().test(new ItemStack(MachinesModule.DEPLETED_URANIUM.get())))),
                "the press makes the plate from depleted uranium");
        h.succeed();
    }

    private IndustryRtgTests() {
    }
}
