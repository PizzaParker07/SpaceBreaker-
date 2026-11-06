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
        helper.assertTrue(real > 9000 && real < 10000, "Earth to orbit ~9.4 km/s real");
        // a rocket without a heat shield can reach orbit but never comes home
        RocketDesign noShield = new RocketDesign(RocketryModule.MOON_ROCKET.parts().stream()
                .filter(id -> !id.getPath().equals("heat_shield")).toList());
        RocketLayout ns = RocketLayout.of(noShield, RocketParts.lookup(helper.getLevel().registryAccess()));
        helper.assertFalse(MissionPlanner.canReturnHome(ns, 0, ns.fullFuel(), Location.EARTH_ORBIT, SbConfig.Realism.BALANCED),
                "no heat shield, no way home");
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
            crew.stopRiding();
            helper.assertTrue(crew.getVehicle() == rocket, "nobody climbs out of a rocket in flight");
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
