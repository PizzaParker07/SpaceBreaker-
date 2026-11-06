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
        helper.assertTrue(total > 6000 && total < 7000, "moon rocket total Δv ~6.4 km/s, got " + total);
        double twr = RocketMath.twr(l, 0, fuel, 9.80665);
        helper.assertTrue(twr > 1.2 && twr < 1.5, "launch TWR ~1.3, got " + twr);
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
        slots.put(AssemblerSlots.stageSlot(1, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.DECOUPLER.get()));
        slots.put(AssemblerSlots.stageSlot(2, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        slots.put(AssemblerSlots.stageSlot(2, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_SMALL.get()));
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
        // engine 20 + 3 x 32 tanks + 28 interstage (the vacuum engine sits inside it) + 32 + 3 + 28 + 16 + 3 + 26 + 6
        helper.assertTrue(Math.abs(g.height() - 258) < 1e-6, "stack height px, got " + g.height());
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
