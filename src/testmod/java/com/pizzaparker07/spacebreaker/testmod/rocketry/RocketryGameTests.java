package com.pizzaparker07.spacebreaker.testmod.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.SpentStageEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad;
import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketryGameTests {
    private static final String EMPTY = "empty";

    private static RocketLayout moonRocket(GameTestHelper helper) {
        return RocketLayout.of(RocketryModule.MOON_ROCKET, RocketParts.lookup(helper.getLevel().registryAccess()));
    }

    // ------------------------------------------------------------------ maths

    @GameTest(template = EMPTY)
    public static void rocketEquation(GameTestHelper helper) {
        // Tsiolkovsky by hand: Isp 300 s, mass ratio e -> exactly 300 * g0
        double dv = RocketMath.deltaV(300, Math.E * 1000, 1000);
        helper.assertTrue(Math.abs(dv - 300 * RocketPart.G0) < 1e-6, "Δv for mass ratio e must be Isp*g0, got " + dv);
        double prop = RocketMath.propellantFor(300, 10_000, dv);
        helper.assertTrue(Math.abs(prop - 10_000 * (1 - 1 / Math.E)) < 1e-6, "propellantFor must invert the rocket equation");
        helper.assertTrue(RocketMath.deltaV(300, 1000, 1000) == 0, "no propellant, no Δv");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void partsLoadWithRealValues(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        RocketPart engine = RocketParts.get(access, Spacebreaker.id("kerosene_engine")).orElseThrow(() -> new AssertionError("kerosene_engine.json missing"));
        RocketPart vac = RocketParts.get(access, Spacebreaker.id("vacuum_engine")).orElseThrow();
        helper.assertTrue(engine.type() == PartType.ENGINE && engine.ispSeaLevel() > 270 && engine.ispSeaLevel() < 300, "kerosene SL Isp ~280 s");
        helper.assertTrue(vac.ispVacuum() > 330 && vac.ispVacuum() < 360, "kerosene vacuum Isp ~340 s");
        helper.assertTrue(vac.ispSeaLevel() < engine.ispSeaLevel(), "a vacuum nozzle is worse at sea level");
        for (var item : RocketryModule.PARTS) {
            helper.assertTrue(RocketParts.forStack(access, new ItemStack(item.get())).isPresent(), "no part data for " + item.getId());
        }
        RocketPart tank = RocketParts.get(access, Spacebreaker.id("fuel_tank_large")).orElseThrow();
        double kg = tank.rp1Millibuckets() * RocketPart.RP1_DENSITY + tank.loxMillibuckets() * RocketPart.LOX_DENSITY;
        helper.assertTrue(Math.abs(kg - tank.propellantKg()) < 5, "RP-1 + LOX litres must add up to the tank's propellant mass");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void stageDeltaVAndTwr(GameTestHelper helper) {
        RocketLayout l = moonRocket(helper);
        helper.assertTrue(l.stages().size() == 3, "moon rocket has 3 stages, got " + l.stages().size());
        helper.assertTrue(l.payload().seats() == 1 && l.payload().parachute() && l.payload().heatShield() == 2, "payload: capsule, chute, lunar shield");
        double[] fuel = l.fullFuel();
        // stage 3 by hand: m0 = payload + stage 3 full, mf = payload + stage 3 dry
        RocketLayout.Stage s3 = l.stages().get(2);
        double m0 = l.payload().massKg() + s3.dryKg() + s3.propellantKg();
        double mf = l.payload().massKg() + s3.dryKg();
        double expected = s3.ispVacuum() * RocketPart.G0 * Math.log(m0 / mf);
        double got = RocketMath.stageDeltaV(l, 2, fuel, 0);
        helper.assertTrue(Math.abs(got - expected) < 1e-6, "stage 3 Δv " + got + " != " + expected);
        double total = RocketMath.totalDeltaV(l, 0, fuel, RocketMath.ASCENT_ATMOSPHERE);
        helper.assertTrue(total > 7000 && total < 9000, "moon rocket total Δv ~8 km/s, got " + total);
        double twr = RocketMath.twr(l, 0, fuel, 9.80665);
        helper.assertTrue(twr > 1.2 && twr < 2.0, "launch TWR ~1.5, got " + twr);
        double moonTwr = RocketMath.twr(l, 2, fuel, 1.62);
        helper.assertTrue(moonTwr > 2, "the lander lifts off the Moon easily: " + moonTwr);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void burnStagesWhenDry(GameTestHelper helper) {
        RocketLayout l = moonRocket(helper);
        double[] fuel = l.fullFuel();
        RocketMath.Burn small = RocketMath.burn(l, 0, fuel, 500, 0);
        helper.assertTrue(small.complete() && small.separated().isEmpty() && small.activeStage() == 0, "a small burn stays on stage 1");
        helper.assertTrue(small.fuel()[0] < fuel[0] && fuel[0] == l.stages().get(0).propellantKg(), "burn must not mutate the input");
        RocketMath.Burn big = RocketMath.burn(l, 0, fuel, 3000, RocketMath.ASCENT_ATMOSPHERE);
        helper.assertTrue(big.complete() && big.separated().equals(List.of(0)) && big.activeStage() == 1, "3 km/s needs staging once: " + big.separated());
        RocketMath.Burn huge = RocketMath.burn(l, 0, fuel, 50_000, 0);
        helper.assertFalse(huge.complete(), "50 km/s is impossible");
        helper.assertTrue(huge.separated().equals(List.of(0, 1, 2)), "every stage runs dry in order");
        helper.assertTrue(Math.abs(huge.delivered() - RocketMath.totalDeltaV(l, 0, fuel, 0)) < 1, "delivered = total Δv");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void moonMissionBudget(GameTestHelper helper) {
        RocketLayout l = moonRocket(helper);
        List<MissionPlanner.Milestone> ms = MissionPlanner.milestones(l, SbConfig.Realism.BALANCED);
        for (MissionPlanner.Milestone m : ms) {
            helper.assertTrue(m.reached(), "moon rocket must reach " + m.location() + " on BALANCED");
        }
        helper.assertFalse(MissionPlanner.milestones(l, SbConfig.Realism.REALISTIC).get(1).reached(),
                "with real distances a kerosene 3-stager cannot reach the Moon");
        double real = Transfers.model().deltaV(Location.EARTH_SURFACE, Location.EARTH_ORBIT).orElseThrow();
        helper.assertTrue(real > 8500 && real < 10000, "Earth to orbit ~9.2 km/s real, got " + real);
        helper.assertTrue(Transfers.model() instanceof com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel,
                "the astrophysics library drives the Δv costs");
        double tli = Transfers.model().deltaV(Location.EARTH_ORBIT, Location.MOON_ORBIT).orElseThrow();
        helper.assertTrue(tli > 3500 && tli < 4500, "LEO to low lunar orbit ~3.95 km/s, got " + tli);
        // a rocket without a heat shield can reach orbit but never comes home
        RocketDesign noShield = new RocketDesign(RocketryModule.MOON_ROCKET.parts().stream()
                .filter(id -> !id.getPath().equals("heat_shield")).toList());
        RocketLayout ns = RocketLayout.of(noShield, RocketParts.lookup(helper.getLevel().registryAccess()));
        helper.assertFalse(MissionPlanner.canReturnHome(ns, 0, ns.fullFuel(), Location.EARTH_ORBIT, SbConfig.Realism.BALANCED),
                "no heat shield, no way home");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void realisticModeUsesRealDeltaV(GameTestHelper helper) {
        for (Location from : com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.all()) {
            for (Location to : Transfers.destinations(from)) {
                double real = Transfers.real(from, to).orElseThrow();
                double realistic = Transfers.cost(from, to, SbConfig.Realism.REALISTIC).orElseThrow();
                double balanced = Transfers.cost(from, to, SbConfig.Realism.BALANCED).orElseThrow();
                helper.assertTrue(Math.abs(realistic - real) < 1e-6, "REALISTIC must use the real Δv for " + from + "->" + to);
                helper.assertTrue(Math.abs(balanced - real * Transfers.scale(SbConfig.Realism.BALANCED)) < 1e-6, "BALANCED = real x scale");
            }
        }
        // the real numbers are the textbook ones (docs/ASTRO.md Δv map)
        helper.assertTrue(within(Transfers.real(Location.EARTH_ORBIT, Location.MOON_ORBIT).orElseThrow(), 3950, 250), "LEO -> LLO ~3.95 km/s");
        helper.assertTrue(within(Transfers.real(Location.MOON_ORBIT, Location.MOON_SURFACE).orElseThrow(), 1900, 300), "lunar landing ~2.0 km/s");
        helper.assertTrue(within(Transfers.real(Location.MOON_SURFACE, Location.MOON_ORBIT).orElseThrow(), 1900, 300), "lunar ascent ~2.0 km/s");
        helper.assertTrue(within(Transfers.real(Location.MOON_ORBIT, Location.EARTH_SURFACE).orElseThrow(), 850, 200), "trans-Earth injection ~0.8 km/s");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void innerPlanetsAreDestinations(GameTestHelper helper) {
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false);
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true);
        var venus = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("venus"), false);
        var phobos = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("phobos"), false);
        helper.assertTrue(mars.isPresent() && marsOrbit.isPresent() && venus.isPresent() && phobos.isPresent(), "Mars, Venus and Phobos come from the body data");
        helper.assertTrue(marsOrbit.get().dimension().location().equals(Spacebreaker.id("mars_orbit")),
                "Mars has its own orbit dimension (orbit_dimension in its body data)");
        var phobosOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("phobos"), true);
        helper.assertTrue(phobosOrbit.isPresent() && phobosOrbit.get().dimension().equals(Location.Orbits.SHARED),
                "bodies without an orbit dimension share one");
        helper.assertTrue(mars.get().atmosphere() && !phobos.get().atmosphere(), "Mars has air, Phobos does not");
        helper.assertTrue(Transfers.destinations(Location.EARTH_ORBIT).contains(marsOrbit.get()), "Mars orbit is a destination from Earth orbit");
        double toMars = Transfers.real(Location.EARTH_ORBIT, marsOrbit.get()).orElseThrow();
        helper.assertTrue(toMars > 3000 && toMars < 6500, "LEO to low Mars orbit 3.6-5.7 km/s (aerobraking), got " + toMars);
        double landMars = Transfers.real(marsOrbit.get(), mars.get()).orElseThrow();
        helper.assertTrue(landMars < 1000, "Mars landing uses the air and parachutes, got " + landMars);
        helper.assertTrue(Transfers.reentryRating(marsOrbit.get(), mars.get()) == 1, "Mars entry from orbit needs a shield");
        var venusOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("venus"), true).orElseThrow();
        helper.assertTrue(Transfers.reentryRating(venusOrbit, venus.get()) == 2, "Venus' dense air needs the lunar-grade shield");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity.launchTarget(mars.get()).equals(marsOrbit.get()),
                "launching from Mars goes to Mars orbit");
        helper.assertTrue(Location.parse(marsOrbit.get().id()).orElseThrow().equals(marsOrbit.get()), "location ids round-trip");
        var home = MissionPlanner.homeRoute(phobos.get());
        helper.assertTrue(home.size() == 3 && home.get(2).equals(Location.EARTH_SURFACE), "home from Phobos: orbit, then Earth");
        helper.succeed();
    }

    private static boolean within(double v, double target, double tol) {
        return Math.abs(v - target) <= tol;
    }

    @GameTest(template = EMPTY)
    public static void advancedPartsNeedResearch(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack vac = new ItemStack(RocketryModule.VACUUM_ENGINE.get());
        net.minecraft.resources.ResourceLocation tech = com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.techFor(player, vac);
        helper.assertTrue(tech != null, "the vacuum engine is gated by a technology");
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.unlocked(player, vac), "locked before research");
        com.pizzaparker07.spacebreaker.core.tech.Tech.learn(player, tech);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.unlocked(player, vac), "unlocked after research");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void assemblerBuildsTheMoonRocket(GameTestHelper helper) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        slots.put(AssemblerSlots.stageSlot(0, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.KEROSENE_ENGINE.get()));
        for (int t = AssemblerSlots.TANK_A; t <= AssemblerSlots.TANK_C; t++) {
            slots.put(AssemblerSlots.stageSlot(0, t), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        }
        slots.put(AssemblerSlots.stageSlot(0, AssemblerSlots.RADIAL), new ItemStack(RocketryModule.FINS.get()));
        slots.put(AssemblerSlots.stageSlot(0, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.INTERSTAGE.get()));
        slots.put(AssemblerSlots.stageSlot(1, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        slots.put(AssemblerSlots.stageSlot(1, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        slots.put(AssemblerSlots.stageSlot(1, AssemblerSlots.TANK_B), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        slots.put(AssemblerSlots.stageSlot(1, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.DECOUPLER.get()));
        slots.put(AssemblerSlots.stageSlot(2, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        slots.put(AssemblerSlots.stageSlot(2, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_MEDIUM.get()));
        slots.put(AssemblerSlots.stageSlot(2, AssemblerSlots.RADIAL), new ItemStack(RocketryModule.LANDING_LEGS.get()));
        slots.put(AssemblerSlots.HEAT_SHIELD, new ItemStack(RocketryModule.HEAT_SHIELD.get()));
        slots.put(AssemblerSlots.CAPSULE, new ItemStack(RocketryModule.CREW_CAPSULE.get()));
        slots.put(AssemblerSlots.TOP, new ItemStack(RocketryModule.PARACHUTE.get()));
        AssemblerSlots.Build build = AssemblerSlots.build(i -> slots.getOrDefault(i, ItemStack.EMPTY), helper.getLevel().registryAccess());
        helper.assertTrue(build.valid(), "valid build expected, problems: " + build.problems());
        helper.assertTrue(build.design().equals(RocketryModule.MOON_ROCKET), "assembler order differs: " + build.design().parts());
        // removing the interstage leaves two stages unconnected
        slots.remove(AssemblerSlots.stageSlot(0, AssemblerSlots.SEPARATOR));
        AssemblerSlots.Build broken = AssemblerSlots.build(i -> slots.getOrDefault(i, ItemStack.EMPTY), helper.getLevel().registryAccess());
        helper.assertTrue(broken.problems().contains("no_separator"), "missing separator must be reported");
        helper.assertFalse(AssemblerSlots.accepts(AssemblerSlots.CAPSULE, PartType.TANK), "tanks do not go in the capsule slot");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void stackGeometry(GameTestHelper helper) {
        RocketLayout l = moonRocket(helper);
        RocketGeometry g = RocketGeometry.ofActive(l, 0);
        // engine 20 + 3 x 32 tanks + 28 interstage (the vacuum engine sits inside it) + 2 x 32 + 3 + 28 + 24 + 3 + 26 + 6
        helper.assertTrue(Math.abs(g.height() - 298) < 1e-6, "stack height px, got " + g.height());
        RocketGeometry upper = RocketGeometry.ofActive(l, 1);
        helper.assertTrue(upper.height() < g.height(), "dropping a stage makes the rocket shorter");
        long fins = g.placements().stream().filter(p -> l.parts().get(p.part()).type() == PartType.FINS).count();
        helper.assertTrue(fins == 4, "fins are drawn four times around the stage");
        helper.assertTrue(g.capsuleBottom() > 200, "capsule sits on top");
        helper.succeed();
    }

    // ------------------------------------------------------------------ launch facility

    static BlockPos buildPad(GameTestHelper helper, boolean tower) {
        BlockPos c = new BlockPos(3, 1, 3);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                helper.setBlock(c.offset(dx, 0, dz), inner ? RocketryModule.FLAME_TRENCH.get() : RocketryModule.LAUNCH_PAD.get());
            }
        }
        if (tower) {
            for (int y = 0; y < 6; y++) {
                helper.setBlock(c.offset(-3, 1 + y, 0), RocketryModule.SERVICE_TOWER.get());
            }
            helper.setBlock(c.offset(-2, 5, 0), RocketryModule.CREW_ARM.get());
            helper.setBlock(c.offset(-2, 2, 0), RocketryModule.FUEL_UMBILICAL.get());
        }
        return helper.absolutePos(c);
    }

    @GameTest(template = EMPTY)
    public static void launchSiteBlueprintIsThePad(GameTestHelper helper) {
        var def = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(helper.getLevel().registryAccess(), LaunchPad.SITE).orElseThrow();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        for (net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.place(helper.getLevel(), def, center, facing);
            helper.assertTrue(com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.validate(helper.getLevel(), def, center, facing).complete(),
                    "the placed blueprint validates facing " + facing);
            LaunchPad.Result r = LaunchPad.check(helper.getLevel(), center);
            helper.assertTrue(r.formed() && r.hasTower(), "the blueprint is a formed pad with a tower, facing " + facing);
            helper.assertTrue(r.arms().size() == 1 && r.umbilicals().size() == 1, "arm and umbilical of the blueprint are found, facing " + facing);
            net.minecraft.world.level.block.state.BlockState arm = helper.getLevel().getBlockState(r.arms().get(0));
            helper.assertTrue(arm.getValue(com.pizzaparker07.spacebreaker.content.rocketry.pad.TowerAttachmentBlock.FACING) == facing.getOpposite(),
                    "the arm faces the pad, facing " + facing);
            BlockPos plate = center.offset(2, 0, 2);
            helper.getLevel().setBlock(plate, Blocks.STONE.defaultBlockState(), 3);
            LaunchPad.Result broken = LaunchPad.check(helper.getLevel(), center);
            helper.assertTrue(!broken.formed() && broken.missingPad().equals(List.of(plate)), "the missing corner plate is reported");
            var v = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.validate(helper.getLevel(), def, center, facing);
            helper.assertTrue(v.problems().size() == 1 && v.problems().get(0).pos().equals(plate), "the hologram points at the same block");
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy < def.height(); dy++) {
                        helper.getLevel().setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void launchPadFormation(GameTestHelper helper) {
        BlockPos center = buildPad(helper, true);
        LaunchPad.Result r = LaunchPad.check(helper.getLevel(), center);
        helper.assertTrue(r.formed(), "complete pad must form");
        helper.assertTrue(r.hasTower() && r.towerHeight() == 6, "tower of 6 found, got " + r.towerHeight());
        helper.assertTrue(r.arms().size() == 1 && r.umbilicals().size() == 1, "arm and umbilical found on the tower");
        helper.assertTrue(LaunchPad.centerNear(helper.getLevel(), center.east()).equals(center), "any grate finds the centre");
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        LaunchPad.Result broken = LaunchPad.check(helper.getLevel(), center);
        helper.assertFalse(broken.formed(), "a missing plate breaks the pad");
        helper.assertTrue(broken.missingPad().size() == 1 && broken.missingPad().get(0).equals(helper.absolutePos(new BlockPos(1, 1, 1))),
                "the missing block is reported");
        helper.succeed();
    }

    // ------------------------------------------------------------------ flight

    @GameTest(template = EMPTY)
    public static void outerWorldsAreReachable(GameTestHelper helper) {
        for (String name : new String[]{"ceres", "vesta", "io", "europa", "ganymede", "callisto"}) {
            var surface = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id(name), false);
            var orbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id(name), true);
            helper.assertTrue(surface.isPresent() && orbit.isPresent(), name + ": surface and orbit come from the body data");
            helper.assertTrue(Transfers.destinations(orbit.get()).contains(surface.get()), name + ": landing is offered from orbit");
            helper.assertTrue(Transfers.destinations(surface.get()).contains(orbit.get()), name + ": ascent is offered from the surface");
            double land = Transfers.real(orbit.get(), surface.get()).orElseThrow();
            helper.assertTrue(land > 0 && land < 2500, name + ": landing from low orbit costs " + land);
            helper.assertTrue(Transfers.real(Location.EARTH_ORBIT, orbit.get()).isPresent(), name + ": reachable from Earth orbit");
        }
        helper.succeed();
    }

    private static RocketDesign marsReturnRocket(String sl, String vac) {
        return new RocketDesign(List.of(Spacebreaker.id(sl), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"), Spacebreaker.id(vac), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("fuel_tank_medium"), Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));
    }

    @GameTest(template = EMPTY)
    public static void methaloxFliesHomeFromMars(GameTestHelper helper) {
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout methalox = RocketLayout.of(marsReturnRocket("methalox_engine", "methalox_vacuum_engine"), lookup);
        RocketLayout kerolox = RocketLayout.of(marsReturnRocket("kerosene_engine", "vacuum_engine"), lookup);
        helper.assertTrue(methalox.problems().isEmpty(), "valid design: " + methalox.problems());
        for (RocketLayout.Stage s : methalox.stages()) {
            helper.assertTrue(s.propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX,
                    "tanks of a methalox stage hold methalox, stage " + s.index());
        }
        RocketLayout.Stage upper = methalox.stages().get(1);
        helper.assertTrue(within(upper.ispVacuum(), 380, 1) && within(methalox.stages().get(0).ispSeaLevel(), 330, 1), "methalox Isp 330 s SL / 380 s vacuum");
        double dvCh4 = RocketMath.totalDeltaV(methalox, 0, methalox.fullFuel(), 0);
        double dvRp1 = RocketMath.totalDeltaV(kerolox, 0, kerolox.fullFuel(), 0);
        helper.assertTrue(dvCh4 > dvRp1, "methalox beats kerolox on Δv: " + dvCh4 + " vs " + dvRp1);
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
        List<Location> home = com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.homeRoute(mars);
        helper.assertTrue(home.size() == 3 && home.get(1).orbit() && home.get(2).equals(Location.EARTH_SURFACE), "Mars surface -> Mars orbit -> Earth");
        for (SbConfig.Realism realism : SbConfig.Realism.values()) {
            var plan = com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(methalox, 0, methalox.fullFuel(), home, realism);
            double need = 0;
            for (int i = 0; i + 1 < home.size(); i++) {
                need += Transfers.cost(home.get(i), home.get(i + 1), realism).orElseThrow();
            }
            helper.assertTrue(plan.possible(), realism + ": a methalox rocket fuelled on Mars flies home (needs " + (int) need
                    + " m/s, has " + (int) dvCh4 + " m/s): " + plan);
        }
        helper.succeed();
    }

    /** Hydrolox first stage, nuclear thermal (or hydrolox vacuum) upper stage, crew capsule. */
    private static RocketDesign nuclearRocket(String upperEngine) {
        return new RocketDesign(List.of(Spacebreaker.id("hydrolox_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"), Spacebreaker.id(upperEngine), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));
    }

    @GameTest(template = EMPTY)
    public static void nuclearThermalEngine(GameTestHelper helper) {
        var NUCLEAR = com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR;
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout ntr = RocketLayout.of(nuclearRocket("nuclear_thermal_engine"), lookup);
        RocketLayout chem = RocketLayout.of(nuclearRocket("hydrolox_vacuum_engine"), lookup);
        helper.assertTrue(ntr.problems().isEmpty(), "valid design: " + ntr.problems());
        RocketLayout.Stage upper = ntr.stages().get(1);
        helper.assertTrue(upper.propellant() == NUCLEAR, "the upper stage burns hydrogen through the reactor");
        helper.assertTrue(within(upper.ispVacuum(), 880, 1), "Isp 880 s in vacuum, got " + upper.ispVacuum());
        helper.assertTrue(upper.ispVacuum() > 1.8 * chem.stages().get(1).ispVacuum(), "close to twice any chemical engine");
        double dvNtr = RocketMath.stageDeltaV(ntr, 1, ntr.fullFuel(), 0);
        double dvChem = RocketMath.stageDeltaV(chem, 1, chem.fullFuel(), 0);
        helper.assertTrue(dvNtr > 1.3 * dvChem, "the same tanks give far more Δv: " + (int) dvNtr + " vs " + (int) dvChem + " m/s");
        // weak thrust: it cannot lift its own fully fuelled stage off the Earth
        double m0 = upper.dryKg() + upper.propellantKg() + ntr.payload().massKg();
        helper.assertTrue(upper.thrustKn() * 1000 / (m0 * RocketPart.G0) < 1, "TWR below 1 on Earth: not a launch engine");
        // the assembler warns: a nuclear upper stage is radioactive, a nuclear first stage is a mistake
        helper.assertTrue("nuclear".equals(com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.designWarning(ntr)),
                "upper-stage warning");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.designWarning(chem) == null, "no warning without one");
        RocketLayout bottom = RocketLayout.of(new RocketDesign(List.of(Spacebreaker.id("nuclear_thermal_engine"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("crew_capsule"))), lookup);
        helper.assertTrue("nuclear_first".equals(com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.designWarning(bottom)),
                "first-stage warning");
        // hydrogen alone fills it: no oxygen for the nuclear stage
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), nuclearRocket("nuclear_thermal_engine"),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)), 0F);
        helper.getLevel().addFreshEntity(rocket);
        var HYDROLOX = com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.HYDROLOX;
        int h2Room = rocket.roomFor(HYDROLOX);
        int loxRoom = rocket.roomFor((com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant) null);
        RocketLayout.Stage first = ntr.stages().get(0);
        helper.assertTrue(within(h2Room, HYDROLOX.fuelMillibuckets(first.propellantKg()) + NUCLEAR.fuelMillibuckets(upper.propellantKg()), 3),
                "hydrogen room covers both stages: " + h2Room);
        helper.assertTrue(within(loxRoom, HYDROLOX.loxMillibuckets(first.propellantKg()), 3), "oxygen room only for the hydrolox stage: " + loxRoom);
        var handler = new com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler(rocket);
        var exec = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        handler.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_HYDROGEN.stack(h2Room), exec);
        helper.assertTrue(within(rocket.fuel()[1], upper.propellantKg(), 1), "hydrogen alone fills the nuclear stage");
        handler.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN.stack(loxRoom), exec);
        helper.assertTrue(rocket.fullyFuelled(), "fully fuelled: " + java.util.Arrays.toString(rocket.fuel()));
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void cryogenicBoilOff(GameTestHelper helper) {
        var KEROLOX = com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.KEROLOX;
        var HYDROLOX = com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.HYDROLOX;
        var NUCLEAR = com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.NUCLEAR;
        helper.assertTrue(HYDROLOX.boilOffPerDay() == 0.001 && NUCLEAR.boilOffPerDay() == 0.001, "liquid hydrogen boils off 0.1 %/day");
        helper.assertTrue(KEROLOX.boilOffPerDay() < HYDROLOX.boilOffPerDay() / 4, "kerolox only loses a little oxygen");
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout h2 = RocketLayout.of(nuclearRocket("hydrolox_vacuum_engine"), lookup);
        double[] fuel = h2.fullFuel();
        double before = fuel[1];
        double lost = com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff.apply(h2, 1, fuel, 100, SbConfig.Realism.REALISTIC);
        helper.assertTrue(within(fuel[1], before * Math.pow(0.999, 100), 1) && within(lost, before - fuel[1], 1),
                "100 days cost a hydrolox stage about 9.5 %: " + (int) lost + " kg");
        // multi-layer insulation on the upper stage's tanks stops three quarters of it
        RocketLayout wrapped = RocketLayout.of(new RocketDesign(List.of(Spacebreaker.id("hydrolox_engine"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("interstage"), Spacebreaker.id("hydrolox_vacuum_engine"), Spacebreaker.id("fuel_tank_large_insulated"),
                Spacebreaker.id("fuel_tank_large_insulated"), Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"),
                Spacebreaker.id("parachute"))), lookup);
        helper.assertTrue(wrapped.problems().isEmpty() && wrapped.stages().get(1).insulated() == 1.0, "the upper stage is fully insulated");
        helper.assertTrue(wrapped.stages().get(1).dryKg() > h2.stages().get(1).dryKg(), "insulation adds a little dry mass");
        double[] wf = wrapped.fullFuel();
        double wBefore = wf[1];
        com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff.apply(wrapped, 1, wf, 100, SbConfig.Realism.REALISTIC);
        helper.assertTrue(within(wf[1], wBefore * Math.pow(1 - 0.001 * 0.25, 100), 1), "insulated, 100 days cost only about 2.5 %");
        // the planner counts it on a long coast: Earth orbit to Mars orbit
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true).orElseThrow();
        double days = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.coastSeconds(Location.EARTH_ORBIT, marsOrbit) / 86400;
        helper.assertTrue(days > 100, "the coast to Mars takes months: " + (int) days + " days");
        var plan = com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(h2, 1, h2.fullFuel(),
                List.of(Location.EARTH_ORBIT, marsOrbit), SbConfig.Realism.BALANCED);
        helper.assertTrue(plan.boilOffKg() > 0, "the planner counts boil-off on the way to Mars");
        var moonPlan = com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(h2, 1, h2.fullFuel(),
                List.of(Location.EARTH_ORBIT, Location.MOON_ORBIT), SbConfig.Realism.BALANCED);
        helper.assertTrue(moonPlan.boilOffKg() < plan.boilOffKg() / 10, "a few days to the Moon cost far less");
        // on the pad nothing boils off; waiting in orbit it does
        RocketEntity pad = RocketEntity.create(helper.getLevel(), nuclearRocket("hydrolox_vacuum_engine"),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)), 0F);
        RocketEntity orbit = RocketEntity.create(helper.getLevel(), nuclearRocket("hydrolox_vacuum_engine"),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5, 2, 6.5)), 0F);
        for (RocketEntity r : List.of(pad, orbit)) {
            r.fillCompletely();
            r.setNoGravity(true);
            helper.getLevel().addFreshEntity(r);
        }
        orbit.stageScene(FlightState.ORBIT, 0, 0, false);
        double padFuel = pad.fuel()[0], orbitFuel = orbit.fuel()[0];
        helper.runAfterDelay(RocketEntity.BOIL_OFF_TICKS + 20, () -> {
            helper.assertTrue(pad.fuel()[0] == padFuel, "nothing boils off on the pad");
            helper.assertTrue(orbit.fuel()[0] < orbitFuel, "waiting in orbit, hydrogen boils off");
            pad.discard();
            orbit.discard();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void cometRendezvousRoutes(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.ready(), "the rendezvous table is built");
        var halley = Spacebreaker.id("halley");
        var route = com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.route(halley).orElseThrow();
        helper.assertTrue(route.totalDv() > 15_000 && route.totalDv() < 25_000, "Halley is retrograde: a rendezvous costs " + (int) route.totalDv() + " m/s");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.needsNuclear(halley), "the map hints at nuclear for Halley");
        var halleyOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(halley, true).orElseThrow();
        helper.assertTrue(within(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.real(Location.EARTH_ORBIT, halleyOrbit).orElse(0),
                route.totalDv(), 1), "flights use the rendezvous Δv");
        helper.assertTrue(within(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.coastSeconds(Location.EARTH_ORBIT, halleyOrbit),
                route.flightSeconds(), 1), "and its flight time");
        for (String id : List.of("churyumov_gerasimenko", "encke", "tempel_1")) {
            var c = Spacebreaker.id(id);
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.route(c).isPresent()
                    && !com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.needsNuclear(c), id + " stays within chemical reach");
        }
        // home: straight into Earth's air, with a braking burn where it would come in too fast (Halley)
        var home = com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.of(halley).orElseThrow().home().orElseThrow();
        helper.assertTrue(home.brakeDv() > 0 && home.entrySpeed() <= com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.MAX_ENTRY_SPEED + 1,
                "Halley's return brakes to a safe entry: " + (int) home.burnDv() + " + " + (int) home.brakeDv() + " m/s");
        helper.assertTrue(within(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.real(halleyOrbit, Location.EARTH_SURFACE).orElse(0),
                home.totalDv(), 1), "the flight home uses the return search");
        for (String id : List.of("churyumov_gerasimenko", "encke", "tempel_1")) {
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.of(Spacebreaker.id(id)).orElseThrow().home().isPresent(),
                    id + " has a flight home");
        }
        var haleBopp = Spacebreaker.id("hale_bopp");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.CometRoutes.observationOnly(haleBopp), "Hale-Bopp is for watching");
        com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(haleBopp, true).ifPresent(o ->
                helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers.real(Location.EARTH_ORBIT, o).isEmpty(), "no flight to Hale-Bopp"));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void nuclearEngineRadiation(GameTestHelper helper) {
        var level = helper.getLevel();
        RocketEntity rocket = RocketEntity.create(level, nuclearRocket("nuclear_thermal_engine"),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5)), 0F);
        rocket.setNoGravity(true);
        level.addFreshEntity(rocket);
        BlockPos base = BlockPos.containing(rocket.position());
        BlockPos side = base.east(3), above = base.above(12), far = base.east(40);
        java.util.function.Function<BlockPos, Double> dose = p ->
                com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.doseMsvDay(level, p);
        helper.assertTrue(dose.apply(side) == 0, "a cold reactor is safe");
        rocket.stageScene(FlightState.COUNTDOWN, 0, 0, true);
        helper.assertTrue(dose.apply(side) == 0, "the chemical first stage burning gives no dose");
        rocket.stageScene(FlightState.COUNTDOWN, 0, 1, true);
        double beside = dose.apply(side);
        helper.assertTrue(beside > 10_000, "standing next to a burning reactor is dangerous: " + (int) beside + " mSv/day");
        helper.assertTrue(dose.apply(far) < beside / 100, "it falls off with distance");
        double crew = dose.apply(above);
        helper.assertTrue(crew < 50, "the shadow shield protects the crew above: " + crew + " mSv/day");
        helper.assertTrue(com.pizzaparker07.spacebreaker.core.env.Environment.radiation(level, side) >= beside,
                "the suit's radiation model sees it");
        rocket.stageScene(FlightState.COUNTDOWN, 0, 1, false);
        helper.assertTrue(dose.apply(side) == 0, "shut down, the dose stops");
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fuelWithMethaneAndOxygen(GameTestHelper helper) {
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), marsReturnRocket("methalox_engine", "methalox_vacuum_engine"),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)), 0F);
        helper.getLevel().addFreshEntity(rocket);
        var ch4 = com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_METHANE;
        var lox = com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN;
        var rp1 = com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.RP1;
        var handler = new com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler(rocket);
        var exec = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        helper.assertTrue(handler.fill(rp1.stack(1000), exec) == 0, "a methalox rocket takes no RP-1");
        int ch4Room = rocket.roomFor(com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX);
        int loxRoom = rocket.roomFor((com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant) null);
        RocketLayout l = rocket.layout();
        double cap = l.stages().stream().mapToDouble(RocketLayout.Stage::propellantKg).sum();
        helper.assertTrue(within(ch4Room, com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX.fuelMillibuckets(cap), 3),
                "methane room matches the O/F 3.6 mixture: " + ch4Room);
        helper.assertTrue(handler.fill(ch4.stack(ch4Room), exec) == ch4Room, "liquid methane goes in");
        helper.assertTrue(rocket.deltaV() == 0, "methane alone does not burn");
        helper.assertTrue(handler.fill(lox.stack(loxRoom), exec) == loxRoom, "liquid oxygen goes in");
        helper.assertTrue(rocket.fullyFuelled(), "methane + LOX fill every stage, fuel " + java.util.Arrays.toString(rocket.fuel()));
        helper.assertTrue(rocket.roomFor(com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX) == 0
                && rocket.roomFor((com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant) null) == 0, "no room left");
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void transitViewTiming(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.kind(Location.EARTH_SURFACE, Location.EARTH_ORBIT)
                == com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.ASCENT, "launch climbs out");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.kind(Location.EARTH_ORBIT, Location.MOON_ORBIT)
                == com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.TRANSFER, "orbit to orbit is a transfer");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.kind(Location.MOON_ORBIT, Location.MOON_SURFACE)
                == com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.LANDING, "orbit to ground is a landing");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.kind(Location.MOON_ORBIT, Location.EARTH_SURFACE)
                == com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.LANDING, "coming home is a landing");
        float prev = -1;
        for (float p = 0; p <= 1.0001F; p += 0.05F) {
            float l = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(FlightState.ASCENT, 0, p);
            helper.assertTrue(l >= prev && l >= 0 && l <= 1, "ascent lead-in grows from 0 to 1");
            prev = l;
        }
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(FlightState.ASCENT, 0, 0.5F) == 0, "low in the climb the world stays visible");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(FlightState.ASCENT, 0, 1F) == 1, "fully covered before the jump to orbit");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(FlightState.TRANSFER, 0, 0) == 0
                && com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(FlightState.TRANSFER,
                com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.TRANSFER_FADE_TICKS, 0) == 1, "transfer fades in, then covers");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.TRANSFER_FADE_TICKS < RocketEntity.TRANSFER_TICKS,
                "the view is complete well before the dimension changes");
        for (FlightState st : new FlightState[]{FlightState.IDLE, FlightState.ORBIT, FlightState.DESCENT, FlightState.PARACHUTE}) {
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.leadIn(st, 50, 1F) == 0, "no view in " + st);
        }
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.fadeOut(0) == 1
                && com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.fadeOut(com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.FADE_OUT_SECONDS) == 0,
                "fades out into the new world");
        float a = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.progress(2.5F, 5F);
        float b = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.progress(10F, 5F);
        float c = com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.progress(60F, 5F);
        helper.assertTrue(within(a, 0.5, 1e-4) && b > 0.9F && c > b && c <= 1F, "a slow load keeps the animation moving: " + a + " " + b + " " + c);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void starMapTrips(GameTestHelper helper) {
        RocketLayout l = moonRocket(helper);
        double[] full = l.fullFuel();
        double dv = RocketMath.totalDeltaV(l, 0, full, 0);
        var realism = SbConfig.Realism.BALANCED;
        var toMoon = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 0, full, dv, Location.EARTH_ORBIT, Location.MOON_SURFACE, realism, true, true);
        helper.assertTrue(toMoon.route().equals(List.of(Location.EARTH_ORBIT, Location.MOON_ORBIT, Location.MOON_SURFACE)), "route shows each stop: " + toMoon.route());
        helper.assertTrue(toMoon.enough() && toMoon.canGo() && toMoon.needed() > 0 && toMoon.shortBy() == 0, "the Moon rocket can land on the Moon: " + toMoon);
        helper.assertTrue(toMoon.next().equals(Location.MOON_SURFACE) || toMoon.next().equals(Location.MOON_ORBIT), "Go flies the first leg");
        var fromPad = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 0, full, dv, Location.EARTH_SURFACE, Location.MOON_ORBIT, realism, false, true);
        helper.assertTrue(fromPad.block() == com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Block.NOT_IN_ORBIT
                && fromPad.route().get(1).equals(Location.EARTH_ORBIT), "on the pad: climb to orbit first");
        double[] empty = new double[full.length];
        var dry = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 0, empty, 0, Location.EARTH_ORBIT, Location.MOON_ORBIT, realism, true, true);
        helper.assertTrue(!dry.enough() && dry.shortBy() > 0 && dry.block() == com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Block.NOT_ENOUGH_FUEL,
                "an empty rocket is told how much it is short");
        var unbuilt = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 0, full, dv, Location.EARTH_ORBIT, Location.MOON_SURFACE, realism, true, false);
        helper.assertTrue(unbuilt.block() == com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Block.NOT_BUILT, "missing worlds are 'coming soon'");
        // dangers in plain words
        java.util.function.Function<String, java.util.Set<com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard>> hz =
                n -> com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.hazards(
                        com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.body(Spacebreaker.id(n)).orElseThrow());
        helper.assertTrue(hz.apply("earth").isEmpty(), "Earth is safe: " + hz.apply("earth"));
        helper.assertTrue(hz.apply("moon").contains(com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard.NO_AIR), "the Moon has no air");
        helper.assertTrue(hz.apply("venus").containsAll(List.of(com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard.CRUSHING_AIR,
                com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard.HOT)), "Venus crushes and burns: " + hz.apply("venus"));
        helper.assertTrue(hz.apply("mars").containsAll(List.of(com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard.THIN_AIR,
                com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Hazard.COLD)), "Mars is thin and cold: " + hz.apply("mars"));
        // the map's layout data
        var planets = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.planets();
        helper.assertTrue(planets.indexOf(Spacebreaker.id("mercury")) == 0 && planets.indexOf(Location.EARTH) < planets.indexOf(Spacebreaker.id("mars")),
                "planets in order from the Sun: " + planets);
        helper.assertFalse(planets.contains(Location.MOON), "moons are not planets");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.moons(Spacebreaker.id("mars"))
                .containsAll(List.of(Spacebreaker.id("phobos"), Spacebreaker.id("deimos"))), "Mars has its two moons");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.system(Location.MOON).equals(Location.EARTH), "the Moon is in Earth's system");
        helper.succeed();
    }

    private static com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls.Snapshot snap(FlightState s, int timer, int stage, boolean back) {
        return new com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls.Snapshot(s, timer, stage, back);
    }

    @GameTest(template = EMPTY)
    public static void missionControlCalls(GameTestHelper helper) {
        java.util.function.BiFunction<com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls.Snapshot,
                com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls.Snapshot, com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line> call =
                com.pizzaparker07.spacebreaker.content.rocketry.entity.MissionCalls::call;
        // walk a whole countdown tick by tick and collect what is said
        List<com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line> said = new java.util.ArrayList<>();
        var prev = snap(FlightState.IDLE, 0, 0, true);
        for (int t = RocketEntity.COUNTDOWN_TICKS; t >= 1; t--) {
            var now = snap(FlightState.COUNTDOWN, t, 0, true);
            var line = call.apply(prev, now);
            if (line != null) {
                said.add(line);
            }
            prev = now;
        }
        var liftoff = call.apply(prev, snap(FlightState.ASCENT, 0, 0, true));
        helper.assertTrue(said.contains(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_10)
                && said.contains(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_1), "counts down from 10 to 1: " + said);
        helper.assertTrue(said.stream().filter(l -> l == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.IGNITION).count() == 1,
                "ignition is called once: " + said);
        helper.assertFalse(said.contains(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_3), "T-3 is the ignition call");
        helper.assertTrue(said.indexOf(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_4)
                < said.indexOf(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.IGNITION)
                && said.indexOf(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.IGNITION)
                < said.indexOf(com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_2), "4, ignition, 2, 1");
        helper.assertTrue(said.size() == 10, "one call per second from T-10: " + said);
        helper.assertTrue(liftoff == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.LIFTOFF, "liftoff");
        helper.assertTrue(call.apply(snap(FlightState.COUNTDOWN, 90, 0, true), snap(FlightState.IDLE, 0, 0, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.COUNTDOWN_HOLD, "abort holds the countdown");
        helper.assertTrue(call.apply(snap(FlightState.ASCENT, 300, 0, true), snap(FlightState.ASCENT, 301, 1, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.STAGE_SEPARATION, "staging");
        helper.assertTrue(call.apply(snap(FlightState.ASCENT, 900, 1, true), snap(FlightState.ORBIT, 0, 1, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.ORBIT_REACHED, "orbit after the climb");
        helper.assertTrue(call.apply(snap(FlightState.TRANSFER, 119, 1, true), snap(FlightState.ORBIT, 0, 1, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.ORBIT_REACHED, "orbit after a transfer");
        helper.assertTrue(call.apply(snap(FlightState.TRANSFER, 119, 3, true), snap(FlightState.REENTRY, 0, 3, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.ENTRY_INTERFACE, "entry interface");
        helper.assertTrue(call.apply(snap(FlightState.REENTRY, 150, 3, true), snap(FlightState.PARACHUTE, 0, 3, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.PARACHUTES, "parachutes");
        helper.assertTrue(call.apply(snap(FlightState.PARACHUTE, 200, 3, true), snap(FlightState.IDLE, 0, 3, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.TOUCHDOWN, "touchdown under chutes");
        helper.assertTrue(call.apply(snap(FlightState.DESCENT, 200, 2, true), snap(FlightState.IDLE, 0, 2, true))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.TOUCHDOWN, "powered touchdown");
        helper.assertTrue(call.apply(snap(FlightState.ORBIT, 10, 1, true), snap(FlightState.ORBIT, 11, 1, false))
                == com.pizzaparker07.spacebreaker.content.audio.SbSounds.Line.FUEL_LOW, "fuel low when home is out of reach");
        helper.assertTrue(call.apply(snap(FlightState.ORBIT, 10, 1, true), snap(FlightState.ORBIT, 11, 1, true)) == null, "quiet in a steady orbit");
        helper.assertTrue(call.apply(null, snap(FlightState.ORBIT, 0, 1, true)) == null, "nothing on boarding a parked rocket");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void launchCostFollowsThrustToWeight(GameTestHelper helper) {
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout heavy = moonRocket(helper);
        RocketLayout light = RocketLayout.of(new RocketDesign(List.of(Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_small"),
                Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute"))), lookup);
        double g = 9.80665;
        double twrHeavy = RocketMath.twr(heavy, 0, heavy.fullFuel(), g), twrLight = RocketMath.twr(light, 0, light.fullFuel(), g);
        double costHeavy = Transfers.cost(Location.EARTH_SURFACE, Location.EARTH_ORBIT, heavy, 0, SbConfig.Realism.REALISTIC).orElseThrow();
        double costLight = Transfers.cost(Location.EARTH_SURFACE, Location.EARTH_ORBIT, light, 0, SbConfig.Realism.REALISTIC).orElseThrow();
        helper.assertTrue(twrLight > twrHeavy, "the light rocket is punchier: " + twrLight + " vs " + twrHeavy);
        helper.assertTrue(costLight < costHeavy, "less gravity loss with more thrust: " + costLight + " vs " + costHeavy);
        helper.assertTrue(costLight > 7800 && costHeavy < 11500, "Earth ascent stays in the real 8-11 km/s range: " + costLight + " / " + costHeavy);
        helper.succeed();
    }

    /** Biggest three-stage rocket the assembler can build with these engines, with landing legs and a capsule for home. */
    static RocketDesign threeStage(String first, String upper) {
        java.util.List<net.minecraft.resources.ResourceLocation> p = new java.util.ArrayList<>();
        for (int stage = 0; stage < 3; stage++) {
            p.add(Spacebreaker.id(stage == 0 ? first : upper));
            for (int t = 0; t < 3; t++) {
                p.add(Spacebreaker.id("fuel_tank_large"));
            }
            if (stage == 2) {
                p.add(Spacebreaker.id("landing_legs"));
            }
            if (stage < 2) {
                p.add(Spacebreaker.id("interstage"));
            }
        }
        p.addAll(List.of(Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));
        return new RocketDesign(p);
    }

    @GameTest(template = EMPTY)
    public static void hydroloxReachesMercuryOrbit(GameTestHelper helper) {
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout h2 = RocketLayout.of(threeStage("hydrolox_engine", "hydrolox_vacuum_engine"), lookup);
        RocketLayout ch4 = RocketLayout.of(threeStage("methalox_engine", "methalox_vacuum_engine"), lookup);
        helper.assertTrue(h2.problems().isEmpty(), "valid: " + h2.problems());
        helper.assertTrue(h2.stages().stream().allMatch(s -> s.propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.HYDROLOX),
                "every stage burns hydrogen");
        var mercury = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mercury"), false).orElseThrow();
        var mercuryOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mercury"), true).orElseThrow();
        List<Location> round = new java.util.ArrayList<>(List.of(Location.EARTH_SURFACE, Location.EARTH_ORBIT, mercuryOrbit, mercury));
        List<Location> home = com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.homeRoute(mercury);
        round.addAll(home.subList(1, home.size()));
        var realism = SbConfig.Realism.BALANCED;
        double need = 0;
        StringBuilder legs = new StringBuilder();
        for (int i = 0; i + 1 < round.size(); i++) {
            double c = Transfers.cost(round.get(i), round.get(i + 1), realism).orElseThrow();
            need += c;
            legs.append(round.get(i).id()).append("->").append(round.get(i + 1).id()).append('=').append((int) c).append(' ');
        }
        double dvH2 = RocketMath.totalDeltaV(h2, 0, h2.fullFuel(), 0), dvCh4 = RocketMath.totalDeltaV(ch4, 0, ch4.fullFuel(), 0);
        helper.assertTrue(dvH2 > dvCh4 * 1.15, "hydrogen gives the most Δv: " + (int) dvH2 + " vs " + (int) dvCh4);
        // with hydrogen, Mercury orbit and back is possible
        List<Location> orbitTrip = new java.util.ArrayList<>(List.of(Location.EARTH_SURFACE, Location.EARTH_ORBIT, mercuryOrbit, Location.EARTH_SURFACE));
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(h2, 0, h2.fullFuel(), orbitTrip, realism).possible(),
                "a hydrolox rocket flies to Mercury orbit and home");
        // the map picks the cheaper of a direct transfer and a flyby tour (Mercury via Venus)
        var sys = com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.system(helper.getLevel().registryAccess()).orElseThrow();
        double direct = com.pizzaparker07.spacebreaker.core.astro.TransferPlanner.plan(sys, "spacebreaker:earth", "spacebreaker:mercury", false).total();
        double used = Transfers.real(Location.EARTH_ORBIT, mercuryOrbit).orElseThrow();
        helper.assertTrue(used <= direct + 1, "flybys never cost more than going direct: " + (int) used + " vs " + (int) direct);
        // and it lands on Mercury, where propellant can be made for the way home
        List<Location> oneWay = List.of(Location.EARTH_SURFACE, Location.EARTH_ORBIT, mercuryOrbit, mercury);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(h2, 0, h2.fullFuel(), oneWay, realism).possible(),
                "a hydrolox rocket lands on Mercury");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.canRefuel(Spacebreaker.id("mercury")), "and can refuel there");
        Spacebreaker.LOGGER.info("[rocketry] Mercury surface round trip needs {} m/s, best hydrolox rocket has {} ({})", (int) need, (int) dvH2, legs);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void refuelAwayFromHome(GameTestHelper helper) {
        for (String w : new String[]{"earth", "moon", "mars", "mercury"}) {
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.canRefuel(Spacebreaker.id(w)), w + " can make propellant");
        }
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.canRefuel(Spacebreaker.id("jupiter")), "no fuel on a gas giant");
        // a rocket that can reach Mars but not come back is told it can refuel there
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        RocketLayout l = RocketLayout.of(marsReturnRocket("methalox_engine", "methalox_vacuum_engine"), lookup);
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true).orElseThrow();
        double[] fuel = l.fullFuel();
        var realism = SbConfig.Realism.REALISTIC;
        // drain until the way there still works but the way back does not
        double[] f = fuel.clone();
        com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.Trip trip = null;
        for (int i = 0; i < 400; i++) {
            trip = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 1, f, RocketMath.totalDeltaV(l, 1, f, 0),
                    Location.EARTH_ORBIT, mars, realism, true, true);
            if (trip.enough() && !trip.returnHome()) {
                break;
            }
            f[1] *= 0.97;
            f[0] = 0;
        }
        helper.assertTrue(trip != null && trip.enough() && !trip.returnHome() && trip.refuelThere(),
                "Mars: reachable, no fuel for home, but propellant can be made there: " + trip);
        var jupiterTrip = com.pizzaparker07.spacebreaker.content.rocketry.nav.StarMapData.trip(l, 1, f, 0, Location.EARTH_ORBIT,
                com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("jupiter"), true).orElseThrow(), realism, true, true);
        helper.assertFalse(jupiterTrip.refuelThere(), "nothing to refuel with at Jupiter");
        // a pad with an umbilical works the same anywhere: fill a methalox rocket through it (pipes feed the umbilical)
        BlockPos center = buildPad(helper, true);
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), marsReturnRocket("methalox_engine", "methalox_vacuum_engine"),
                net.minecraft.world.phys.Vec3.atBottomCenterOf(center.above()), 0F);
        helper.getLevel().addFreshEntity(rocket);
        BlockPos umbilical = helper.absolutePos(new BlockPos(1, 3, 3));
        var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, umbilical, null);
        helper.assertTrue(handler != null, "the umbilical takes fluids");
        var exec = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        int ch4 = rocket.roomFor(com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX);
        int lox = rocket.roomFor((com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant) null);
        helper.assertTrue(handler.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_METHANE.stack(ch4), exec) == ch4
                && handler.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN.stack(lox), exec) == lox,
                "methane and LOX go through the umbilical");
        helper.assertTrue(rocket.fullyFuelled(), "fuelled on the pad");
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void nearbyStarsForTheMap(GameTestHelper helper) {
        var stars = com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.forServer(helper.getLevel().getServer());
        helper.assertTrue(!stars.isEmpty() && stars.size() <= com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.MAX_STARS, "stars found: " + stars.size());
        helper.assertTrue(stars.stream().allMatch(s -> s.distanceLy() <= com.pizzaparker07.spacebreaker.content.rocketry.nav.NearbyStars.RADIUS_LY + 0.01),
                "all within 25 light years");
        var alpha = stars.stream().filter(s -> s.nameEn().equals("Alpha Centauri")).findFirst();
        helper.assertTrue(alpha.isPresent() && within(alpha.get().distanceLy(), 4.37, 0.1), "Alpha Centauri at 4.37 ly");
        helper.assertTrue(stars.stream().anyMatch(s -> s.nameEn().startsWith("Sirius")), "Sirius is on the map");
        for (int i = 1; i < stars.size(); i++) {
            helper.assertTrue(stars.get(i).magnitude() >= stars.get(i - 1).magnitude(), "brightest first");
        }
        helper.assertTrue(stars.stream().anyMatch(s -> !s.nameTr().isEmpty()), "names in Turkish too");
        helper.assertTrue(stars.stream().allMatch(s -> s.knownPlanets() >= 0 && s.knownPlanets() <= s.planets()), "known planets are a part of all planets");
        var proxima = stars.stream().filter(s -> s.nameEn().startsWith("Proxima")).findFirst();
        helper.assertTrue(proxima.isEmpty() || proxima.get().knownPlanets() == 2, "Proxima Centauri has two confirmed planets");
        // the payload carries the list unchanged
        var buf = io.netty.buffer.Unpooled.buffer();
        var payload = new com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload(stars);
        com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.STREAM_CODEC.encode(buf, payload);
        var back = com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.stars().equals(stars), "the list survives the network");
        helper.succeed();
    }

    private static RocketDesign uncrewed(String payload) {
        return new RocketDesign(List.of(Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"),
                Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_medium"), Spacebreaker.id(payload), Spacebreaker.id("nose_cone")));
    }

    @GameTest(template = EMPTY)
    public static void uncrewedCraftToWorldsWithAir(GameTestHelper helper) {
        var lookup = RocketParts.lookup(helper.getLevel().registryAccess());
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true).orElseThrow();
        java.util.function.BiFunction<RocketLayout, Location, StarMapData.Block> block = (layout, to) -> StarMapData.trip(layout, 0, layout.fullFuel(), 0,
                Location.EARTH_ORBIT, to, SbConfig.Realism.BALANCED, true, true).block();
        // a probe carries its own aeroshell
        RocketLayout probe = RocketLayout.of(uncrewed("space_probe"), lookup);
        helper.assertTrue(probe.payload().heatShield() >= 2, "the probe has an aeroshell");
        helper.assertTrue(block.apply(probe, mars) != StarMapData.Block.NEEDS_HEAT_SHIELD, "a probe may enter Mars' air");
        // a bare satellite would burn up in aerocapture: the map says so instead of blaming the fuel
        RocketLayout bare = RocketLayout.of(uncrewed("comm_satellite"), lookup);
        helper.assertTrue(block.apply(bare, marsOrbit) == StarMapData.Block.NEEDS_HEAT_SHIELD, "the map asks for a heat shield: " + block.apply(bare, marsOrbit));
        // a heat shield fits under a satellite
        List<net.minecraft.resources.ResourceLocation> parts = new java.util.ArrayList<>(uncrewed("comm_satellite").parts());
        parts.add(parts.indexOf(Spacebreaker.id("comm_satellite")), Spacebreaker.id("heat_shield"));
        RocketLayout shielded = RocketLayout.of(new RocketDesign(parts), lookup);
        helper.assertTrue(shielded.problems().isEmpty() && shielded.payload().heatShield() >= 2, "satellite with a shield: " + shielded.problems());
        helper.assertTrue(block.apply(shielded, marsOrbit) != StarMapData.Block.NEEDS_HEAT_SHIELD, "with a shield the satellite may aerocapture");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void satellitesAndProbes(GameTestHelper helper) {
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots.accepts(
                com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots.CAPSULE,
                com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.SATELLITE), "satellites go in the capsule slot");
        // mission decisions
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true).orElseThrow();
        var sat = com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.SATELLITE;
        var probe = com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.PROBE;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(sat, Location.EARTH_ORBIT, null)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.DEPLOY, "no target: release in Earth orbit");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(sat, Location.EARTH_ORBIT, marsOrbit)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.CONTINUE, "carry on to Mars orbit");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(sat, marsOrbit, marsOrbit)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.DEPLOY, "release at the target");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(probe, marsOrbit, mars)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.CONTINUE, "a probe heads down to the surface");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(probe, mars, mars)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.NOTHING, "landing is handled at touchdown");
        // a real release: the rocket becomes a satellite and the world remembers it
        var server = helper.getLevel().getServer();
        var data = com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteData.get(server);
        int before = data.crafts().size();
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        RocketEntity relay = RocketEntity.create(helper.getLevel(), uncrewed("comm_satellite"), helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)), 0F);
        helper.getLevel().addFreshEntity(relay);
        helper.assertTrue(relay.isUncrewed() && relay.layout().payload().seats() == 0, "a satellite rocket has no crew");
        relay.setOwner(owner.getUUID());
        relay.setMissionTarget(marsOrbit);
        helper.assertTrue(relay.missionTarget().orElseThrow().equals(marsOrbit), "the console sets a mission target");
        com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.deploy(relay, marsOrbit);
        helper.assertTrue(relay.isRemoved(), "the rocket is gone, the satellite is out");
        helper.assertTrue(data.crafts().size() == before + 1 && data.crafts().get(before).owner().equals(owner.getUUID()), "the world remembers the satellite");
        helper.assertTrue(data.relaySystems().contains(Spacebreaker.id("mars")), "Mars now has a relay");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.covered(data.relaySystems(), Spacebreaker.id("phobos")),
                "the relay covers Mars's moons too");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.Coverage.covered(data.relaySystems(), Location.MOON), "Earth's system is always covered");
        helper.assertFalse(helper.getLevel().getEntitiesOfClass(com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity.class,
                new net.minecraft.world.phys.AABB(relay.blockPosition()).inflate(8)).isEmpty(), "the satellite is visible where it was released");
        // a probe lands and sends samples home
        RocketEntity lander = RocketEntity.create(helper.getLevel(), uncrewed("space_probe"), helper.absoluteVec(new net.minecraft.world.phys.Vec3(5.5, 2, 5.5)), 0F);
        helper.getLevel().addFreshEntity(lander);
        lander.setOwner(owner.getUUID());
        com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.landed(lander, mars);
        int samples = 0, records = 0;
        for (var stack : owner.getInventory().items) {
            if (stack.is(com.pizzaparker07.spacebreaker.content.research.ResearchModule.FIELD_SAMPLE.get())) {
                samples += stack.getCount();
            }
            if (stack.is(com.pizzaparker07.spacebreaker.content.research.ResearchModule.EXPERIMENT_DATA.get())) {
                records += stack.getCount();
            }
        }
        helper.assertTrue(samples == 2 && records == 1, "Mars samples and probe data arrive: " + samples + " / " + records);
        helper.assertTrue(data.observed().contains(Spacebreaker.id("mars")), "a probe landing maps the world");
        // an offline owner gets them on the next login
        java.util.UUID away = java.util.UUID.randomUUID();
        data.deliver(server, away, com.pizzaparker07.spacebreaker.content.research.Research.sample(Spacebreaker.id("mars")));
        helper.assertTrue(data.save(new net.minecraft.nbt.CompoundTag(), server.registryAccess()).getCompound("pending").contains(away.toString()),
                "kept for an offline player");
        lander.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void threeSeatCapsule(GameTestHelper helper) {
        RocketDesign d = new RocketDesign(List.of(Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule_large"), Spacebreaker.id("parachute")));
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), d, helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2, 3.5)), 0F);
        helper.getLevel().addFreshEntity(rocket);
        helper.assertTrue(rocket.layout().payload().seats() == 3, "three seats");
        List<ServerPlayer> crew = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ServerPlayer p = helper.makeMockServerPlayerInLevel();
            p.moveTo(rocket.position());
            crew.add(p);
        }
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(crew.get(i).startRiding(rocket, false), "crew member " + (i + 1) + " boards");
        }
        helper.assertFalse(crew.get(3).startRiding(rocket, false), "a fourth does not fit");
        helper.runAfterDelay(3, () -> {
            var seats = crew.subList(0, 3).stream().map(net.minecraft.world.entity.Entity::position).toList();
            helper.assertTrue(seats.get(0).distanceTo(seats.get(1)) > 0.4 && seats.get(1).distanceTo(seats.get(2)) > 0.4, "they sit side by side: " + seats);
            rocket.ejectPassengers();
            rocket.discard();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void stationModulesAndDocking(GameTestHelper helper) {
        var level = helper.getLevel();
        // the kit unfolds a 5x5 floor in empty space, and refuses when something is in the way
        BlockPos floor = helper.absolutePos(new BlockPos(4, 6, 4));
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.StationKitItem.unfold(level, floor), "the kit unfolds");
        int hull = 0;
        for (BlockPos p : BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 0, 2))) {
            if (level.getBlockState(p).is(RocketryModule.STATION_HULL.get())) {
                hull++;
            }
        }
        helper.assertTrue(hull == 25, "a 5x5 hull floor: " + hull);
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.rocketry.station.StationKitItem.unfold(level, floor), "not over existing blocks");
        // hull, windows and the port keep the air in (the suit area's oxygen distributor)
        for (var block : List.of(RocketryModule.STATION_HULL.get(), RocketryModule.STATION_WINDOW.get(), RocketryModule.DOCKING_PORT.get())) {
            BlockPos p = floor.above(2);
            level.setBlock(p, block.defaultBlockState(), 3);
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(level, p, level.getBlockState(p)),
                    block + " is airtight");
        }
        // a rocket parked in orbit next to the port is docked: the crew may step out, and the port fills its tanks
        BlockPos port = floor.above(2);
        RocketEntity rocket = RocketEntity.create(level, marsReturnRocket("methalox_engine", "methalox_vacuum_engine"),
                net.minecraft.world.phys.Vec3.atBottomCenterOf(port.above(3)), 0F);
        level.addFreshEntity(rocket);
        rocket.stageScene(FlightState.ORBIT, 0, 0, false);
        rocket.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        helper.assertTrue(rocket.docked() && rocket.canDismount(), "docked at the station");
        var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, port, null);
        int lox = rocket.roomFor((com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant) null);
        helper.assertTrue(handler != null && lox > 0 && handler.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN.stack(1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "LOX goes through the port into the rocket");
        level.removeBlock(port, false);
        helper.runAfterDelay(1, () -> {
            helper.assertFalse(rocket.docked() || rocket.canDismount(), "without a port, nobody steps out into space");
            rocket.discard();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "rocketry_habitat")
    public static void habitatModuleHoldsAir(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(4, 2, 4));
        Direction forward = Direction.SOUTH;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(level, centre, forward), "the module unfolds");
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(level, centre, forward), "not over existing blocks");
        BlockPos dist = com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.distributor(centre);
        helper.assertTrue(level.getBlockState(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.port(centre, forward))
                .is(RocketryModule.DOCKING_PORT.get()), "a docking port on the far wall");
        helper.assertTrue(level.getBlockState(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.bunk(centre, forward))
                .is(RocketryModule.STATION_BUNK.get()), "a bunk inside");
        helper.assertTrue(level.getBlockState(centre.above(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.HEIGHT))
                .is(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_ARRAY.get()), "a solar array on the roof");
        helper.assertTrue(level.getBlockState(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.battery(centre))
                .is(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.BATTERY_BANK.block().get()), "a battery bank under the array, for the night");
        helper.assertTrue(level.getBlockState(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.battery(centre))
                .getValue(com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlock.OUTPUT_DOWN), "the battery gives power down into the room");
        helper.assertTrue(level.getBlockState(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.battery(centre).below())
                .is(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.COPPER_CABLE.get()), "a cable end under the battery");
        // the distributor finds a closed room: hull, windows, port and the shut airlock keep the air in
        helper.succeedWhen(() -> {
            var be = (com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity) level.getBlockEntity(dist);
            helper.assertTrue(be != null && be.room().sealed(), "the room is sealed");
            int v = be.room().volume();
            helper.assertTrue(v >= 70 && v <= 75, "a 5x3x5 room inside: " + v);
        });
    }

    @GameTest(template = EMPTY)
    public static void stationBunkSetsRespawn(GameTestHelper helper) {
        BlockPos bunk = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(bunk, RocketryModule.STATION_BUNK.get().defaultBlockState(), 3);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.pizzaparker07.spacebreaker.content.rocketry.station.StationBunkBlock.setSpawn(player, bunk);
        helper.assertTrue(bunk.above().equals(player.getRespawnPosition()) && player.isRespawnForced()
                && player.getRespawnDimension().equals(helper.getLevel().dimension()), "respawn on top of the bunk, no bed needed");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void fuelDepotFillsDockedRockets(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos depotPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos port = helper.absolutePos(new BlockPos(3, 2, 1));
        level.setBlock(depotPos, RocketryModule.FUEL_DEPOT.get().defaultBlockState(), 3);
        level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState(), 3);
        RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, net.minecraft.world.phys.Vec3.atBottomCenterOf(port.above()).add(2, 0, 0), 0F);
        level.addFreshEntity(rocket);
        var fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, depotPos, null);
        var energy = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, depotPos, null);
        helper.assertTrue(fluids != null && energy != null, "the depot takes pipes and power");
        var execute = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        helper.assertTrue(fluids.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN.stack(10_000), execute) == 10_000, "LOX in");
        helper.assertTrue(fluids.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), execute) == 0,
                "water is refused");
        int lox = rocket.roomFor(true);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(rocket.roomFor(true) == lox, "without power the pump stands still");
            for (int i = 0; i < 10; i++) {
                energy.receiveEnergy(1000, false);
            }
            helper.assertTrue(energy.getEnergyStored() == 4000, "power in, 400 FE at a time: " + energy.getEnergyStored());
            helper.runAfterDelay(40, () -> {
                var depot = (com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity) level.getBlockEntity(depotPos);
                int moved = lox - rocket.roomFor(true);
                helper.assertTrue(moved > 0 && depot != null && depot.stored(1) == 10_000 - moved,
                        "the depot pumps LOX into the docked rocket: " + moved);
                helper.assertTrue(energy.getEnergyStored() < 4000, "pumping uses power");
                rocket.discard();
                helper.succeed();
            });
        });
    }

    // own batch: the rocket flies in from outside its port, and a neighbouring test clearing its area must not catch it
    @GameTest(template = EMPTY, timeoutTicks = 1200, batch = "rocketry_docking")
    public static void dockingApproach(GameTestHelper helper) {
        var level = helper.getLevel();
        // a sideways port puts the capsule level with the hatch
        BlockPos side = helper.absolutePos(new BlockPos(1, 30, 2));
        net.minecraft.world.phys.Vec3 beside = RocketEntity.dockPoint(side, Direction.EAST, 20, 1.5);
        helper.assertTrue(beside.y + 20 > side.getY() && beside.y < side.getY() && beside.x > side.getX() + 1, "beside the hatch, capsule level with it");
        // the approach itself onto an upward port, flying straight down its column (stays in the test's chunk)
        BlockPos port = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock.FACING, Direction.UP), 3);
        var ports = com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.get(level);
        helper.assertTrue(ports.nearest(port.getCenter(), p -> true).filter(p -> p.pos().equals(port) && p.facing() == Direction.UP).isPresent(),
                "the port registers itself for arriving rockets");
        // keep the chunk ticking and let the rocket settle in before the approach starts
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(port);
        level.setChunkForced(chunk.x, chunk.z, true);
        RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, port.getCenter().add(0, 1, 0), 0F);
        level.addFreshEntity(rocket);
        rocket.stageScene(FlightState.ORBIT, 0, 0, false);
        net.minecraft.world.phys.Vec3 end = RocketEntity.dockPoint(port, Direction.UP, rocket.getBbHeight(), rocket.getBbWidth());
        // start once the rocket itself is ticking (its chunk is live), whenever that happens
        boolean[] started = {false};
        helper.onEachTick(() -> {
            if (!started[0] && rocket.isAlive() && rocket.tickCount >= 5) {
                started[0] = true;
                rocket.approachPort(port, Direction.UP, 6);
                helper.assertTrue(rocket.docking() && !rocket.docked() && !rocket.canDismount(), "still flying in: nobody steps out");
                helper.assertTrue(rocket.position().distanceTo(end) > 5.9, "starts out from the port");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0], "waiting for the rocket to tick (alive " + rocket.isAlive() + ", ticks " + rocket.tickCount + ")");
            helper.assertFalse(rocket.docking(), "approach finished: " + String.format(java.util.Locale.ROOT, "%.3f blocks to go, alive %s, ticks %d, state %s",
                    rocket.position().distanceTo(end), rocket.isAlive(), rocket.tickCount, rocket.state()));
            helper.assertTrue(rocket.position().distanceTo(end) < 0.01 && rocket.docked() && rocket.canDismount(), "latched on");
            level.removeBlock(port, false);
            helper.assertTrue(ports.nearest(port.getCenter(), p -> p.pos().equals(port)).isEmpty(), "a broken port leaves the list");
            rocket.discard();
            level.setChunkForced(chunk.x, chunk.z, false);
        });
    }

    @GameTest(template = EMPTY)
    public static void stationsOnTheStarMap(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(a, RocketryModule.DOCKING_PORT.get().defaultBlockState(), 3);
        var ports = com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.get(level);
        ports.setOwner(a, "PizzaParker07");
        helper.assertTrue(ports.nearest(a.getCenter(), p -> p.pos().equals(a)).map(p -> p.owner().equals("PizzaParker07")).orElse(false),
                "the port knows who built it");
        // ports near each other make one station; a far one is another; only orbits count
        List<com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.Port> list = List.of(
                new com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.Port(new BlockPos(0, 160, 0), Direction.UP, ""),
                new com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.Port(new BlockPos(12, 160, 4), Direction.EAST, "PizzaParker07"),
                new com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.Port(new BlockPos(900, 160, 0), Direction.UP, ""));
        var stations = com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.group(list, p -> java.util.Optional.of(Location.EARTH_ORBIT));
        helper.assertTrue(stations.size() == 2, "two stations: " + stations);
        var big = stations.stream().filter(st -> st.ports() == 2).findFirst();
        helper.assertTrue(big.isPresent() && big.get().owner().equals("PizzaParker07") && big.get().body().equals(Location.EARTH_ORBIT.body()),
                "the first station: 2 ports, built by its owner, around Earth");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.group(list, p -> java.util.Optional.of(Location.EARTH_SURFACE)).isEmpty(),
                "ports on the ground are no space station");
        // the list survives the network
        var buf = io.netty.buffer.Unpooled.buffer();
        var payload = new com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload(stations);
        com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload.STREAM_CODEC.encode(buf, payload);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload.STREAM_CODEC.decode(buf).equals(payload), "sent intact");
        level.removeBlock(a, false);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dayAndNightInOrbit(GameTestHelper helper) {
        var sys = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.system(helper.getLevel().registryAccess());
        helper.assertTrue(sys != null, "the body system is there");
        float orbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.ORBIT_TICKS;
        for (var body : List.of(Location.EARTH, Location.MOON, Spacebreaker.id("mars"))) {
            java.util.function.DoubleUnaryOperator lit = t -> com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(sys, body, t);
            helper.assertTrue(lit.applyAsDouble(0) > 0.99, body + ": full sun under the sun");
            helper.assertTrue(lit.applyAsDouble(orbit * 0.5) < 0.01, body + ": dark behind the planet");
            int dark = 0, n = 400;
            double last = lit.applyAsDouble(0);
            double biggestJump = 0;
            for (int i = 1; i <= n; i++) {
                double v = lit.applyAsDouble(orbit * i / n);
                dark += v < 0.5 ? 1 : 0;
                biggestJump = Math.max(biggestJump, Math.abs(v - last));
                last = v;
            }
            double share = dark / (double) n;
            helper.assertTrue(share > 0.25 && share < 0.5, body + ": a low orbit spends about a third in shadow: " + share);
            helper.assertTrue(biggestJump < 0.6, body + ": dusk through the penumbra, no flicker: " + biggestJump);
        }
        // the same time gives the same light everywhere (server panels, every client)
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(sys, Location.EARTH, 4321)
                == com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(sys, Location.EARTH, 4321), "deterministic");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)) == 1.0,
                "on the ground the dimension keeps its own day");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pressurisedCabin(GameTestHelper helper) {
        var level = helper.getLevel();
        RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)), 0F);
        level.addFreshEntity(rocket);
        helper.assertTrue(rocket.hasCabin(), "a crew capsule is a cabin");
        helper.assertFalse(RocketEntity.create(level, uncrewed("comm_satellite"), rocket.position(), 0F).hasCabin(), "a satellite has none");
        ServerPlayer crew = helper.makeMockServerPlayerInLevel();
        crew.moveTo(rocket.position());
        helper.assertTrue(crew.startRiding(rocket, false), "boards");
        rocket.stageScene(FlightState.ORBIT, 0, 0, false);
        helper.runAfterDelay(3, () -> {
            BlockPos eyes = BlockPos.containing(crew.getEyePosition());
            helper.assertTrue(RocketEntity.cabinAir(level, eyes), "seated in the capsule, the crew breathes cabin air (no suit)");
            helper.assertTrue(com.pizzaparker07.spacebreaker.core.env.Environment.canBreathe(level, eyes), "the environment knows it");
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.suit.SuitEvents.pressurisedAt(crew), "the suit HUD sees a pressurised cabin");
            rocket.ejectPassengers();
            crew.moveTo(rocket.position().add(4, 0, 0));
            helper.runAfterDelay(1, () -> {
                helper.assertFalse(RocketEntity.cabinAir(level, BlockPos.containing(crew.getEyePosition())), "outside the capsule there is no cabin air");
                rocket.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void lunarShuttle(GameTestHelper helper) {
        var level = helper.getLevel();
        RocketLayout shuttle = RocketLayout.of(RocketryModule.LUNAR_SHUTTLE, RocketParts.lookup(level.registryAccess()));
        helper.assertTrue(shuttle.problems().isEmpty(), "a valid design: " + shuttle.problems());
        helper.assertTrue(shuttle.hasLegsFrom(0) && shuttle.payload().seats() > 0, "legs and a crew seat");
        var moonOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Location.MOON, true).orElseThrow();
        // one tank flies down from the station to the base and back up again
        List<Location> roundTrip = List.of(moonOrbit, Location.MOON_SURFACE, moonOrbit);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(shuttle, 0, shuttle.fullFuel(), roundTrip,
                SbConfig.Realism.BALANCED).possible(), "Moon orbit -> surface -> orbit on one tank");
        // the map times landings and launches with the astro library's surface legs (Apollo-like)
        if (Transfers.model() instanceof com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel astro) {
            double down = astro.transferSeconds(moonOrbit, Location.MOON_SURFACE).orElse(0);
            double up = astro.transferSeconds(Location.MOON_SURFACE, moonOrbit).orElse(0);
            helper.assertTrue(down > 40 * 60 && down < 90 * 60, "a Moon landing takes about an hour: " + (int) (down / 60) + " min");
            helper.assertTrue(up > 3 * 60 && up < 15 * 60, "a Moon launch takes minutes: " + (int) (up / 60) + " min");
        }
        // a descent comes down on the nearest landing beacon instead of wherever the orbit happens to be
        BlockPos beacon = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(beacon, RocketryModule.LANDING_BEACON.get().defaultBlockState(), 3);
        var beacons = com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(level);
        var fallback = new net.minecraft.world.phys.Vec3(beacon.getX() + 300, 360, beacon.getZ() - 200);
        var spot = RocketEntity.landingSpot(level, fallback);
        helper.assertTrue(Math.abs(spot.x - (beacon.getX() + 0.5)) < 1e-6 && Math.abs(spot.z - (beacon.getZ() + 0.5)) < 1e-6 && spot.y == 360,
                "the descent starts over the beacon: " + spot);
        beacons.setOwner(beacon, "PizzaParker07");
        // beacons make a base on the map; only on the ground
        var bases = com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.bases(beacons.all(), p -> java.util.Optional.of(Location.MOON_SURFACE));
        helper.assertTrue(bases.size() == 1 && bases.get(0).surface() && bases.get(0).owner().equals("PizzaParker07")
                && bases.get(0).body().equals(Location.MOON), "a base on the Moon, built by its owner: " + bases);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.bases(beacons.all(),
                p -> java.util.Optional.of(Location.EARTH_ORBIT)).isEmpty(), "no bases in orbit");
        level.removeBlock(beacon, false);
        helper.assertTrue(beacons.nearest(fallback).filter(beacon::equals).isEmpty(), "a broken beacon leaves the list");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void marsLander(GameTestHelper helper) {
        var level = helper.getLevel();
        RocketLayout lander = RocketLayout.of(RocketryModule.MARS_LANDER, RocketParts.lookup(level.registryAccess()));
        helper.assertTrue(lander.problems().isEmpty(), "a valid design: " + lander.problems());
        helper.assertTrue(lander.hasLegsFrom(0) && lander.payload().seats() > 0 && lander.payload().parachute(), "legs, a seat and a parachute");
        helper.assertTrue(lander.stages().stream().allMatch(s -> s.propellant() == com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant.METHALOX),
                "burns methane, which is made on Mars");
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true).orElseThrow();
        helper.assertTrue(Transfers.reentryRating(marsOrbit, mars) <= lander.payload().heatShield(), "its shield takes the entry");
        var balanced = SbConfig.Realism.BALANCED;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(lander, 0, lander.fullFuel(), List.of(marsOrbit, mars), balanced).possible(),
                "down from Mars orbit");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(lander, 0, lander.fullFuel(), List.of(mars, marsOrbit), balanced).possible(),
                "and, refuelled on Mars, back up to orbit");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cometsOnTheMap(GameTestHelper helper) {
        var planets = StarMapData.planets();
        helper.assertTrue(!planets.isEmpty() && planets.get(planets.size() - 1).equals(StarMapData.COMETS), "one comet node, last: " + planets);
        var comets = StarMapData.moons(StarMapData.COMETS);
        for (String n : List.of("halley", "encke", "churyumov_gerasimenko")) {
            var id = Spacebreaker.id(n);
            helper.assertTrue(comets.contains(id) && !planets.contains(id), n + " is in the comet group");
            helper.assertTrue(StarMapData.system(id).equals(StarMapData.COMETS), n + " belongs to the group");
            // still a destination: a rocket can park in a comet's orbit, as Rosetta did at 67P
            var orbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(id, true);
            helper.assertTrue(orbit.isPresent() && Transfers.real(Location.EARTH_ORBIT, orbit.get()).isPresent(), n + ": its orbit can be reached");
        }
        // the card at J2000
        var halley = StarMapData.cometInfo(Spacebreaker.id("halley"), 0).orElseThrow();
        helper.assertTrue(halley.nextPerihelion().getYear() == 2061, "Halley comes back in 2061: " + halley.nextPerihelion());
        helper.assertTrue(halley.sunAu() > 10 && halley.level() == com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.DORMANT,
                "far out in 2000 and asleep: " + halley.sunAu() + " AU");
        var encke = StarMapData.cometInfo(Spacebreaker.id("encke"), 0).orElseThrow();
        helper.assertTrue(encke.periodYears() > 3 && encke.periodYears() < 3.6, "Encke: 3.3 years: " + encke.periodYears());
        // 67P at its 2015 perihelion was active
        double perihelion2015 = (java.time.LocalDate.of(2015, 8, 13).toEpochDay() - java.time.LocalDate.of(2000, 1, 1).toEpochDay()) * 86400.0;
        var p67 = StarMapData.cometInfo(Spacebreaker.id("churyumov_gerasimenko"), perihelion2015).orElseThrow();
        helper.assertTrue(p67.sunAu() < 1.4 && p67.level() != com.pizzaparker07.spacebreaker.core.astro.smallbody.CometActivity.Level.DORMANT,
                "67P near the Sun in August 2015, active: " + p67.sunAu() + " AU, " + p67.level());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void nearEarthAsteroids(GameTestHelper helper) {
        var planets = StarMapData.planets();
        helper.assertTrue(planets.contains(StarMapData.NEAR_EARTH_ASTEROIDS), "one group for the near-Earth asteroids: " + planets);
        helper.assertTrue(planets.indexOf(StarMapData.NEAR_EARTH_ASTEROIDS) == planets.indexOf(Location.EARTH) + 1, "right after Earth");
        var group = StarMapData.moons(StarMapData.NEAR_EARTH_ASTEROIDS);
        for (String n : List.of("bennu", "ryugu", "itokawa", "eros")) {
            var id = Spacebreaker.id(n);
            helper.assertTrue(group.contains(id) && !planets.contains(id), n + " is in the group, not on its own");
            helper.assertTrue(StarMapData.system(id).equals(StarMapData.NEAR_EARTH_ASTEROIDS), n + " belongs to the group");
            var orbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(id, true).orElseThrow();
            var surface = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(id, false).orElseThrow();
            double real = Transfers.real(Location.EARTH_ORBIT, orbit).orElse(-1);
            helper.assertTrue(real > 3000 && real < 8000, n + ": a few km/s from Earth orbit (rendezvous): " + (int) real);
            double land = Transfers.cost(orbit, surface, SbConfig.Realism.BALANCED).orElse(-1);
            helper.assertTrue(land >= 0 && land < 10, n + ": landing takes almost nothing: " + land);
            if (Transfers.model() instanceof com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel astro) {
                double days = astro.transferSeconds(Location.EARTH_ORBIT, orbit).orElse(0) / 86400;
                helper.assertTrue(days > 60 && days < 600, n + ": months to get there: " + (int) days + " days");
            }
        }
        helper.assertFalse(StarMapData.nearEarthAsteroid(Spacebreaker.id("vesta")), "Vesta stays in the main belt");
        // even where the climb to orbit is a nudge, the rocket is seen to lift off
        helper.assertFalse(RocketEntity.ascentDone(0.05, 0.02, 1), "not done on the first tick");
        helper.assertTrue(RocketEntity.ascentDone(0.05, 0.02, RocketEntity.MIN_ASCENT_TICKS), "done after a short climb");
        helper.assertFalse(RocketEntity.ascentDone(100, 1000, 500), "not before the burn is delivered");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cargoRocket(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = RocketParts.lookup(level.registryAccess());
        RocketLayout layout = RocketLayout.of(uncrewed("cargo_module"), lookup);
        helper.assertTrue(layout.problems().isEmpty(), "a valid cargo rocket: " + layout.problems());
        var cargoType = com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.CARGO;
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(cargoType, Location.EARTH_ORBIT, Location.EARTH_ORBIT)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.NOTHING, "cargo is not released in orbit: it docks");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.onArrival(cargoType, Location.EARTH_ORBIT, Location.MOON_SURFACE)
                == com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.Next.CONTINUE, "and flies on to a base");
        // the hold
        BlockPos port = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos chest = port.east(2);
        level.setBlock(port, RocketryModule.DOCKING_PORT.get().defaultBlockState(), 3);
        level.setBlock(chest, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
        // a machine with an inventory right next to the port gets nothing (a furnace stands in for one)
        BlockPos furnace = port.north();
        level.setBlock(furnace, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState(), 3);
        RocketEntity rocket = RocketEntity.create(level, uncrewed("cargo_module"), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5)), 0F);
        level.addFreshEntity(rocket);
        helper.assertTrue(rocket.hasCargoHold() && rocket.cargo().getContainerSize() == 27, "a 27-slot hold");
        rocket.cargo().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
        rocket.cargo().setItem(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 20));
        // the hold survives saving (and so dimension changes)
        var tag = rocket.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        RocketEntity copy = RocketEntity.create(level, uncrewed("cargo_module"), rocket.position(), 0F);
        copy.load(tag);
        helper.assertTrue(copy.cargo().getItem(0).getCount() == 64 && copy.cargo().getItem(5).is(net.minecraft.world.item.Items.BREAD), "the hold is saved");
        // docked at the port: the hold empties into the chest next to it
        int moved = com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.unloadCargo(rocket, port, Location.EARTH_ORBIT);
        helper.assertTrue(moved == 84 && rocket.cargo().isEmpty(), "84 items unloaded: " + moved);
        var box = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chest);
        int iron = 0;
        for (int i = 0; i < box.getContainerSize(); i++) {
            iron += box.getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT) ? box.getItem(i).getCount() : 0;
        }
        helper.assertTrue(iron == 64, "the iron is in the chest: " + iron);
        var oven = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) level.getBlockEntity(furnace);
        helper.assertTrue(oven.isEmpty(), "nothing went into the machine next to the port");
        // the automatic route: between two places away from Earth's surface
        var moonOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Location.MOON, true).orElseThrow();
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeAllowed(Location.EARTH_SURFACE, Location.EARTH_ORBIT),
                "no route from Earth's surface (no beacon landing there)");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeAllowed(Location.MOON_SURFACE, moonOrbit), "Moon base <-> station");
        rocket.startRoute(Location.MOON_SURFACE, moonOrbit);
        helper.assertTrue(rocket.routeOn(), "the route runs");
        // at the station: a barrel by the port holds what goes down to the base
        BlockPos barrel = port.south(2);
        level.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState(), 3);
        var out = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(barrel);
        out.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 10));
        com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeArrived(rocket, port, moonOrbit);
        helper.assertTrue(rocket.missionTarget().filter(Location.MOON_SURFACE::equals).isPresent(), "next stop: the base");
        rocket.tick();
        for (int i = 0; i < 20; i++) {
            rocket.tick();
        }
        helper.assertTrue(rocket.routeStatus() == RocketEntity.RouteStatus.LOADING && rocket.holdFilled() == 1,
                "the console sees the route: " + rocket.routeStatus() + ", " + rocket.holdFilled() + " slots");
        helper.assertTrue(out.isEmpty() && rocket.cargo().countItem(net.minecraft.world.item.Items.COBBLESTONE) == 10, "the barrel's load came aboard");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.storageNear(level, port).stream().noneMatch(barrel::equals),
                "barrels are the outbox: deliveries go to chests");
        // a crewed rocket has no hold
        helper.assertFalse(RocketEntity.create(level, RocketryModule.MOON_ROCKET, rocket.position(), 0F).hasCargoHold(), "a crew rocket has no hold");
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void warningLight(GameTestHelper helper) {
        var level = helper.getLevel();
        var LIT = com.pizzaparker07.spacebreaker.content.rocketry.pad.WarningLightBlock.LIT;
        BlockPos light = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(light, RocketryModule.WARNING_LIGHT.get().defaultBlockState(), 3);
        helper.assertFalse(level.getBlockState(light).getValue(LIT), "dark at first");
        // redstone turns it on and gives real light
        level.setBlock(light.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(light).getValue(LIT) && level.getBlockState(light).getLightEmission() >= 12, "redstone lights it");
        level.removeBlock(light.east(), false);
        helper.assertFalse(level.getBlockState(light).getValue(LIT), "and turns it off");
        // a rocket being fuelled nearby turns it on by itself
        RocketEntity rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5)), 0F);
        level.addFreshEntity(rocket);
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(level.getBlockState(light).getValue(LIT), "an idle rocket does not");
            rocket.fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.LIQUID_OXYGEN.stack(1000), false);
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(level.getBlockState(light).getValue(LIT), "fuelling nearby: it spins");
                helper.runAfterDelay(60, () -> {
                    helper.assertFalse(level.getBlockState(light).getValue(LIT), "and stops once the fuelling is over");
                    rocket.discard();
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void baseBlocksHoldAir(GameTestHelper helper) {
        var level = helper.getLevel();
        java.util.function.Function<net.minecraft.world.level.block.Block, Boolean> airtight = block -> {
            BlockPos p = helper.absolutePos(new BlockPos(1, 2, 1));
            level.setBlock(p, block.defaultBlockState(), 3);
            return com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(level, p, level.getBlockState(p));
        };
        for (var block : List.of(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PRESSURE_GLASS.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PORTHOLE.get())) {
            helper.assertTrue(airtight.apply(block), block + " holds air");
        }
        helper.assertFalse(airtight.apply(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get()), "the grate lets air through");
        helper.assertFalse(airtight.apply(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.slab().get()), "a slab leaves a gap");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void slidingDoorAirlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get();
        var OPEN = com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN;
        // an inner and an outer door three blocks apart along z: a small airlock
        BlockPos inner = helper.absolutePos(new BlockPos(2, 2, 1)), outer = inner.south(3);
        com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(level, inner, block.defaultBlockState());
        com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(level, outer, block.defaultBlockState());
        helper.assertTrue(block.partners(level, inner, level.getBlockState(inner)).contains(outer), "the two doors pair up");
        block.request(level, inner, level.getBlockState(inner), true);
        helper.assertTrue(level.getBlockState(inner).getValue(OPEN), "with the outer door shut, the inner one opens at once");
        // asking the outer door to open shuts the inner one first and holds the outer one for the cycle
        block.request(level, outer, level.getBlockState(outer), true);
        helper.assertFalse(level.getBlockState(inner).getValue(OPEN), "the inner door shuts");
        helper.assertFalse(level.getBlockState(outer).getValue(OPEN), "the outer door waits for the cycle");
        helper.runAtTickTime(com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.CYCLE_TICKS / 2, () ->
                helper.assertFalse(level.getBlockState(outer).getValue(OPEN), "still cycling"));
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockState(outer).getValue(OPEN), "after the cycle the outer door opens");
            helper.assertFalse(level.getBlockState(inner).getValue(OPEN), "and never both at once");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void slidingDoorWideAirlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get();
        var OPEN = com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN;
        // a two-door-wide airlock: inner pair on one wall, outer pair three blocks further along z
        BlockPos innerL = helper.absolutePos(new BlockPos(2, 2, 1)), innerR = innerL.east();
        BlockPos outerL = innerL.south(3), outerR = innerR.south(3);
        for (BlockPos d : List.of(innerL, innerR, outerL, outerR)) {
            com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(level, d, block.defaultBlockState());
        }
        java.util.function.Predicate<BlockPos> open = d -> level.getBlockState(d).getValue(OPEN);
        helper.assertTrue(block.group(level, innerL, level.getBlockState(innerL)).containsAll(List.of(innerL, innerR)), "side by side they are one door");
        block.request(level, innerL, level.getBlockState(innerL), true);
        helper.assertTrue(open.test(innerL) && open.test(innerR), "both inner leaves open together");
        // asking one outer leaf to open shuts both inner leaves, not just the one in line with it
        block.request(level, outerL, level.getBlockState(outerL), true);
        helper.assertFalse(open.test(innerL) || open.test(innerR), "every inner leaf shuts before the outer side opens");
        helper.assertFalse(open.test(outerL) || open.test(outerR), "the outer side waits for the cycle");
        helper.succeedWhen(() -> {
            helper.assertTrue(open.test(outerL) && open.test(outerR), "after the cycle the whole outer door opens");
            helper.assertFalse(open.test(innerL) || open.test(innerR), "and the inner side stays shut");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void slidingDoorCycleCancels(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get();
        var OPEN = com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN;
        int cycle = com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.CYCLE_TICKS;
        // two airlocks side by side: one cancelled by a second press, one by its redstone signal dropping
        BlockPos innerA = helper.absolutePos(new BlockPos(1, 2, 1)), outerA = innerA.south(3);
        BlockPos innerB = helper.absolutePos(new BlockPos(5, 2, 1)), outerB = innerB.south(3);
        for (BlockPos d : List.of(innerA, outerA, innerB, outerB)) {
            com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(level, d, block.defaultBlockState());
        }
        block.request(level, innerA, level.getBlockState(innerA), true);
        block.request(level, innerB, level.getBlockState(innerB), true);
        // A: ask the outer door, then press it again while it cycles
        block.request(level, outerA, level.getBlockState(outerA), true);
        helper.assertTrue(block.cycling(level, outerA), "the outer door is cycling");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        level.getBlockState(outerA).useWithoutItem(level, player, new net.minecraft.world.phys.BlockHitResult(outerA.getCenter(), Direction.NORTH, outerA, false));
        helper.assertFalse(block.cycling(level, outerA), "a second press calls it off");
        // B: a redstone signal asks, then drops during the cycle
        level.setBlock(outerB.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(block.cycling(level, outerB), "redstone starts the cycle");
        level.removeBlock(outerB.west(), false);
        helper.assertFalse(block.cycling(level, outerB), "the signal dropping calls it off");
        helper.runAfterDelay(cycle + 20, () -> {
            helper.assertFalse(level.getBlockState(outerA).getValue(OPEN), "cancelled by hand: the outer door stays shut");
            helper.assertFalse(level.getBlockState(outerB).getValue(OPEN), "cancelled by redstone: the outer door stays shut");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void baseFittings(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos p = helper.absolutePos(new BlockPos(1, 2, 1));
        java.util.function.Function<net.minecraft.world.level.block.state.BlockState, Boolean> airtight = state -> {
            level.setBlock(p, state, 3);
            return com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(level, p, level.getBlockState(p));
        };
        // stripes and duct covers are full walls; grate platform, railing and ladder let air through
        for (var block : List.of(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get())) {
            helper.assertTrue(airtight.apply(block.defaultBlockState()), block + " holds air");
        }
        for (var block : List.of(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get())) {
            helper.assertFalse(airtight.apply(block.defaultBlockState()), block + " lets air through");
        }
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER.get().defaultBlockState()
                .is(net.minecraft.tags.BlockTags.CLIMBABLE), "the ladder is climbable");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get().defaultBlockState().getLightEmission() == 15
                && com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get().defaultBlockState().getLightEmission() == 15,
                "the lamps light the base");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void slidingDoor(GameTestHelper helper) {
        var level = helper.getLevel();
        var door = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get();
        var OPEN = com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN;
        BlockPos lower = helper.absolutePos(new BlockPos(2, 2, 2));
        com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(level, lower, door.defaultBlockState());
        java.util.function.Predicate<BlockPos> airtight = p -> com.pizzaparker07.spacebreaker.content.suit.block.SealedRoom.airtight(level, p, level.getBlockState(p));
        helper.assertTrue(level.getBlockState(lower.above()).is(door), "two blocks tall");
        helper.assertTrue(airtight.test(lower) && airtight.test(lower.above()), "shut, it holds air");
        // a hand opens it, both halves together
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        level.getBlockState(lower.above()).useWithoutItem(level, player, new net.minecraft.world.phys.BlockHitResult(lower.above().getCenter(), Direction.NORTH, lower.above(), false));
        helper.assertTrue(level.getBlockState(lower).getValue(OPEN) && level.getBlockState(lower.above()).getValue(OPEN), "opens as one");
        helper.assertFalse(airtight.test(lower) || airtight.test(lower.above()), "open, air flows through");
        helper.assertTrue(level.getBlockState(lower).getCollisionShape(level, lower).toAabbs().stream().noneMatch(a -> a.contains(0.5, 0.5, 0.5)),
                "and you can walk through the middle");
        // redstone opens and shuts it
        level.getBlockState(lower).useWithoutItem(level, player, new net.minecraft.world.phys.BlockHitResult(lower.getCenter(), Direction.NORTH, lower, false));
        helper.assertFalse(level.getBlockState(lower).getValue(OPEN), "shut again");
        level.setBlock(lower.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(lower.above()).getValue(OPEN), "redstone opens it");
        level.removeBlock(lower.west(), false);
        helper.assertFalse(level.getBlockState(lower).getValue(OPEN), "and closes it when the signal goes");
        // breaking one half takes the other
        level.destroyBlock(lower.above(), false);
        helper.assertFalse(level.getBlockState(lower).is(door), "no half door left behind");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fuelFromFluidContainers(GameTestHelper helper) {
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), RocketryModule.MOON_ROCKET, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)), 0F);
        helper.getLevel().addFreshEntity(rocket);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int rp1 = rocket.roomFor(false);
        int lox = rocket.roomFor(true);
        net.minecraft.world.item.Item rp1Bucket = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Spacebreaker.id("rp1_bucket"));
        helper.assertTrue(rp1Bucket != net.minecraft.world.item.Items.AIR, "RP-1 bucket exists");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(rp1Bucket));
        rocket.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(rocket.roomFor(false) == rp1 - 1000, "a bucket loads 1000 mB of RP-1: " + rp1 + " -> " + rocket.roomFor(false));
        helper.assertTrue(rocket.roomFor(true) == lox, "RP-1 does not go into the oxidiser tank");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        rocket.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(rocket.roomFor(false) == rp1 - 1000 && rocket.roomFor(true) == lox, "water is refused");
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1200, batch = "rocketry_launch")
    public static void countdownLiftoffAndStaging(GameTestHelper helper) {
        BlockPos center = buildPad(helper, false);
        LaunchPad.Result pad = LaunchPad.check(helper.getLevel(), center);
        // short first stage so staging happens early in the ascent (the test level only ticks the test area)
        RocketDesign quick = new RocketDesign(List.of(Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_small"),
                Spacebreaker.id("interstage"), Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_large"),
                Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));
        RocketEntity rocket = RocketEntity.create(helper.getLevel(), quick, pad.rocketPos(), 0F);
        helper.getLevel().addFreshEntity(rocket);
        helper.assertTrue(rocket.launchProblem() != null, "an empty rocket cannot launch");
        helper.assertTrue(rocket.deltaV() == 0, "no propellant, no Δv");
        rocket.fillCompletely();
        helper.assertTrue(rocket.fullyFuelled(), "cartridge fills every stage");
        ServerPlayer crew = helper.makeMockServerPlayerInLevel();
        crew.moveTo(pad.rocketPos());
        helper.assertTrue(crew.startRiding(rocket, true), "crew boards");
        helper.assertTrue(rocket.launchProblem() == null, "ready rocket on a pad: " + rocket.launchProblem());
        helper.assertTrue(rocket.tryStartCountdown(null), "countdown starts");
        helper.assertTrue(rocket.state() == FlightState.COUNTDOWN, "countdown state");
        rocket.abortCountdown(null);
        helper.assertTrue(rocket.state() == FlightState.IDLE, "abort returns to the pad");
        rocket.tryStartCountdown(null);
        helper.runAfterDelay(RocketEntity.COUNTDOWN_TICKS + 5, () -> {
            helper.assertTrue(rocket.state() == FlightState.ASCENT, "liftoff after T-0, state " + rocket.state());
            helper.assertTrue(rocket.getDeltaMovement().y > 0, "climbing");
            helper.assertFalse(rocket.canDismount(), "nobody climbs out of a rocket in flight (creative players excepted)");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.activeStage() >= 1, "waiting for staging: state " + rocket.state() + " t=" + rocket.timer() + " fuel0=" + (int) rocket.fuel()[0] + " crew=" + rocket.getPassengers() + " y=" + (int) rocket.getY());
            AABB around = new AABB(rocket.blockPosition()).inflate(64);
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(SpentStageEntity.class, around).isEmpty(), "spent stage spawned");
            helper.assertTrue(rocket.fuel()[0] <= 1e-6, "stage 1 burned out");
            rocket.discard();
            crew.discard();
        });
    }
}
