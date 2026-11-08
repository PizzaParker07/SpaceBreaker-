package com.pizzaparker07.spacebreaker.testmod.vehicles;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Rover driving rules (pure functions), battery use, charging, upgrades, saving and wheel tracks. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RoverTests {
    private static final double MOON = 0.1654;

    private static int ticksToReach(double target, double g) {
        double v = 0;
        for (int t = 1; t < 2000; t++) {
            v = RoverPhysics.speed(v, 1, true, RoverPhysics.traction(g));
            if (v >= target) {
                return t;
            }
        }
        return Integer.MAX_VALUE;
    }

    @GameTest(template = "empty")
    public static void drivingRulesFeelRightOnEarthAndTheMoon(GameTestHelper h) {
        int earth = ticksToReach(RoverPhysics.MAX_SPEED * 0.8, 1.0);
        int moon = ticksToReach(RoverPhysics.MAX_SPEED * 0.8, MOON);
        h.assertTrue(earth < 80, "Earth: 80 % of top speed within 4 s, took " + earth);
        h.assertTrue(moon > earth * 1.8 && moon < 400, "Moon: less grip, slower to speed up; earth " + earth + " moon " + moon);
        double v = RoverPhysics.MAX_SPEED;
        for (int i = 0; i < 2000; i++) {
            v = RoverPhysics.speed(v, 1, true, 1);
        }
        h.assertTrue(v <= RoverPhysics.MAX_SPEED + 1e-9, "top speed is capped");
        // braking (opposite throttle) stops much faster than coasting
        int brake = 0;
        for (double s = 0.4; s > 0; brake++) {
            s = RoverPhysics.speed(s, -1, true, 1);
        }
        int coast = 0;
        for (double s = 0.4; s > 0; coast++) {
            s = RoverPhysics.speed(s, 0, true, 1);
        }
        h.assertTrue(brake < coast, "brake " + brake + " coast " + coast);
        h.assertTrue(RoverPhysics.speed(0.2, 1, false, 1) < 0.2, "airborne wheels do not push");
        h.assertTrue(RoverPhysics.yawChange(0, 1F) == 0F, "no turning on the spot");
        h.assertTrue(RoverPhysics.yawChange(0.3, 1F) < 0 && RoverPhysics.yawChange(-0.1, 1F) > 0, "steering reverses when backing up");
        h.succeed();
    }

    /** Blocks rolled from top speed until stopped, with the given throttle and hand brake. */
    private static double stoppingDistance(double g, double throttle, boolean handBrake) {
        double push = RoverPhysics.traction(g), roll = RoverPhysics.rollingDrag(g);
        double s = RoverPhysics.MAX_SPEED, d = 0;
        for (int t = 0; s > 0.01 && t < 10_000; t++) {
            s = RoverPhysics.speed(s, throttle, handBrake, true, push, roll, RoverPhysics.MAX_SPEED, RoverPhysics.ACCEL);
            d += Math.max(s, 0);
        }
        return d;
    }

    /** Letting go of the throttle on the Moon: the rover rolls on about 8 blocks, not 18; the hand brake stops it short. */
    @GameTest(template = "empty")
    public static void moonCoastingAndTheHandBrake(GameTestHelper h) {
        double earthCoast = stoppingDistance(1.0, 0, false);
        double moonCoast = stoppingDistance(MOON, 0, false);
        h.assertTrue(earthCoast > 6 && earthCoast < 8, "Earth coasting " + earthCoast);
        h.assertTrue(moonCoast > 7 && moonCoast < 9, "Moon coasting " + moonCoast);
        double moonReverse = stoppingDistance(MOON, -1, false);
        double moonHand = stoppingDistance(MOON, 0, true);
        double earthHand = stoppingDistance(1.0, 0, true);
        h.assertTrue(moonHand < moonReverse * 0.75, "Moon hand brake " + moonHand + " vs reverse " + moonReverse);
        h.assertTrue(moonHand > earthHand * 1.5 && moonHand < 5, "Moon hand brake " + moonHand + " Earth " + earthHand);
        // the hand brake holds against the throttle and stops a reversing rover too
        h.assertTrue(RoverPhysics.speed(0.2, 1, true, true, 1, 1, RoverPhysics.MAX_SPEED, RoverPhysics.ACCEL) < 0.2, "brake beats throttle");
        h.assertTrue(RoverPhysics.speed(-0.1, 0, true, true, 1, 1, RoverPhysics.MAX_SPEED, RoverPhysics.ACCEL) > -0.1, "brakes backwards");
        h.assertTrue(RoverPhysics.speed(0.2, 0, true, false, 1, 1, RoverPhysics.MAX_SPEED, RoverPhysics.ACCEL) > 0.19, "airborne wheels do not brake");
        h.succeed();
    }

    /** Largest angle (degrees) between heading and velocity while turning at full lock from top speed, and the
     * distance from the start after one full circle of heading. */
    private static double[] fullLockTurn(double g) {
        double speed = RoverPhysics.MAX_SPEED, yaw = 0, vx = 0, vz = speed, x = 0, z = 0, drift = 0;
        double k = RoverPhysics.lateralGrip(1.0, g);
        int ticks = Math.round(360F / RoverPhysics.TURN_RATE);
        for (int t = 0; t < ticks; t++) {
            yaw += Math.toRadians(RoverPhysics.yawChange(speed, 1F));
            double[] v = RoverPhysics.tyres(vx, vz, -Math.sin(yaw), Math.cos(yaw), speed, 1.0, k);
            vx = v[0];
            vz = v[1];
            x += vx;
            z += vz;
            double heading = Math.atan2(-Math.sin(yaw), Math.cos(yaw)), moving = Math.atan2(vx, vz);
            drift = Math.max(drift, Math.abs(Mth.wrapDegrees(Math.toDegrees(heading - moving))));
        }
        return new double[]{drift, Math.hypot(x, z)};
    }

    /** A hard turn at top speed: on Earth the rover goes where it points, on the Moon it drifts a little wide. */
    @GameTest(template = "empty")
    public static void lowGravityTurnsDriftALittle(GameTestHelper h) {
        double[] earth = fullLockTurn(1.0);
        double[] moon = fullLockTurn(MOON);
        h.assertTrue(earth[0] < 0.5, "Earth drift " + earth[0] + " deg");
        h.assertTrue(moon[0] > 3 && moon[0] < 12, "Moon drift " + moon[0] + " deg");
        // still under control: after a full circle of heading the rover is back near where it started
        h.assertTrue(moon[1] < 4, "Moon circle closes within " + moon[1] + " blocks");
        // the along-heading part is untouched: driving straight, the tyres still pull the rover to its speed at once
        double[] straight = RoverPhysics.tyres(0, 0.1, 0, 1, 0.3, 1.0, RoverPhysics.lateralGrip(1.0, MOON));
        h.assertTrue(Math.abs(straight[1] - 0.3) < 1e-9 && straight[0] == 0, "straight ahead " + straight[1]);
        // a parked rover (full grip) shoved sideways on the Moon stops at once instead of sliding away
        double[] shoved = RoverPhysics.tyres(0.2, 0, 0, 1, 0, 1.0, 1.0);
        h.assertTrue(shoved[0] == 0 && shoved[1] == 0, "parked rover held: " + shoved[0]);
        // equal grips are the plain slide
        double[] a = RoverPhysics.tyres(0.1, 0.2, 0.6, 0.8, 0.3, 0.4, 0.4), b = RoverPhysics.slide(0.1, 0.2, 0.18, 0.24, 0.4);
        h.assertTrue(Math.abs(a[0] - b[0]) < 1e-9 && Math.abs(a[1] - b[1]) < 1e-9, "tyres = slide for equal grips");
        h.succeed();
    }

    /**
     * Off a one-block crest on the Moon at top speed the rover is airborne for about 13 ticks: at full lock it still
     * turns its flight path a few degrees (15 % of the ground rate), where it used to fly dead straight.
     */
    @GameTest(template = "empty")
    public static void airborneRoverSteersALittle(GameTestHelper h) {
        double speed = RoverPhysics.MAX_SPEED, yaw = 0, vx = 0, vz = speed, y = 0, vy = 0, groundYaw = 0;
        int air = 0;
        while (y > -1 && air < 200) {
            yaw += Math.toRadians(RoverPhysics.yawChange(speed, 1F, RoverPhysics.AIR_STEER));
            speed = RoverPhysics.speed(speed, 1, false, 1);
            double[] v = RoverPhysics.slide(vx, vz, -Math.sin(yaw) * speed, Math.cos(yaw) * speed, RoverPhysics.AIR_STEER);
            vx = v[0];
            vz = v[1];
            vy = RoverPhysics.fall(vy, MOON);
            y += vy;
            groundYaw += RoverPhysics.yawChange(RoverPhysics.MAX_SPEED, 1F, 1.0);
            air++;
        }
        double turned = Math.abs(Math.toDegrees(Math.atan2(vx, vz)));
        h.assertTrue(air >= 11 && air <= 15, "airtime " + air);
        h.assertTrue(turned > 3 && turned < Math.abs(groundYaw) * 0.15, "flight path turned " + turned + " deg (ground " + groundYaw + ")");
        h.succeed();
    }

    /**
     * Climbing a one-block step: the rover's feet jump a block in one tick, the drawn body (and the seats, so the
     * camera) does not: it rises no more than a third of a block per tick and is up within 8 ticks.
     */
    @GameTest(template = "empty")
    public static void stepUpEasesTheBodyUp(GameTestHelper h) {
        float lift = 0;
        double y = 0, shown = 0;
        double maxRise = 0;
        int settled = -1;
        for (int t = 0; t < 20; t++) {
            double dy = t == 1 ? 1.0 : 0;
            y += dy;
            lift = RoverPhysics.afterStep(lift, dy, true, 1F);
            lift = RoverPhysics.easeLift(lift, 0F);
            maxRise = Math.max(maxRise, y + lift - shown);
            shown = y + lift;
            if (settled < 0 && t > 1 && Math.abs(shown - y) < 0.06) {
                settled = t - 1;
            }
        }
        h.assertTrue(maxRise <= 0.34, "body rose " + maxRise + " in one tick");
        h.assertTrue(settled > 0 && settled <= 8, "body up the step after " + settled + " ticks");
        // a drop, a bounce or a teleport is left alone
        h.assertTrue(RoverPhysics.afterStep(0.2F, -1.0, true, 1F) == 0.2F, "drops are not eased");
        h.assertTrue(RoverPhysics.afterStep(0.2F, 0.15, true, 1F) == 0.2F, "small bumps follow the ground");
        h.assertTrue(RoverPhysics.afterStep(0.2F, 5.0, true, 1F) == 0.2F, "teleports are not eased");
        h.assertTrue(RoverPhysics.afterStep(0.2F, 1.0, false, 1F) == 0.2F, "airborne rises are real");
        h.succeed();
    }

    /** The HUD's battery percentage: hidden on a healthy battery, shown below 20 %, always in the detailed HUD. */
    @GameTest(template = "empty")
    public static void batteryPercentShowsWhenLow(GameTestHelper h) {
        h.assertTrue(RoverEntity.shownPercent(0.8F, false) == -1 && RoverEntity.shownPercent(0.2F, false) == -1, "hidden when healthy");
        h.assertTrue(RoverEntity.shownPercent(0.12F, false) == 12, "low: " + RoverEntity.shownPercent(0.12F, false));
        h.assertTrue(RoverEntity.shownPercent(0F, false) == 0, "empty reads 0");
        h.assertTrue(RoverEntity.shownPercent(0.001F, false) == 1, "a last drop is not 0");
        h.assertTrue(RoverEntity.shownPercent(0.8F, true) == 80 && RoverEntity.shownPercent(1F, true) == 100, "detailed HUD always");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lowGravityJumpsAreLongAndLandingsBounce(GameTestHelper h) {
        // airtime of a 0.3 b/t hop: about 6x longer on the Moon
        int earth = airtime(0.3, 1.0);
        int moon = airtime(0.3, MOON);
        h.assertTrue(moon > earth * 4, "airtime earth " + earth + " moon " + moon);
        h.assertTrue(RoverPhysics.bounce(-0.6) > 0.1, "a hard landing bounces");
        h.assertTrue(RoverPhysics.bounce(-0.1) == 0, "the springs soak up a gentle one");
        h.succeed();
    }

    private static int airtime(double vy, double g) {
        double y = 0;
        int t = 0;
        do {
            y += vy;
            vy = RoverPhysics.fall(vy, g);
            t++;
        } while (y > 0 && t < 10_000);
        return t;
    }

    @GameTest(template = "empty")
    public static void batteryLastsALongDrive(GameTestHelper h) {
        h.assertTrue(RoverPhysics.driveCost(1) == RoverPhysics.FE_PER_BLOCK, "cost per block");
        double range = RoverEntity.CAPACITY / (double) RoverPhysics.FE_PER_BLOCK;
        h.assertTrue(range >= 5000 && range <= 20000, "range on one charge " + range + " blocks");
        h.succeed();
    }

    private static RoverEntity spawn(GameTestHelper h, BlockPos at) {
        RoverEntity rover = h.spawn(VehiclesModule.ROVER.get(), Vec3.atBottomCenterOf(at));
        rover.setYRot(0F);
        return rover;
    }

    /** Driving tests run on the 10 x 8 x 34 "rover_strip" structure: their floors and the rover stay inside it. */
    @GameTest(template = "rover_strip", timeoutTicks = 200)
    public static void drivingUsesBattery(GameTestHelper h) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 5; x++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 2));
        rover.setEnergy(100_000);
        Vec3 start = rover.position();
        h.runAfterDelay(5, () -> rover.setSpeed(0.4));
        h.runAtTickTime(60, () -> {
            double moved = rover.position().distanceTo(start);
            h.assertTrue(moved > 3, "rover should coast forward, moved " + moved);
            int used = 100_000 - rover.energy();
            h.assertTrue(used > 0 && Math.abs(used - moved * RoverPhysics.FE_PER_BLOCK) < RoverPhysics.FE_PER_BLOCK * 2,
                    "used " + used + " FE for " + moved + " blocks");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void parkedRoverChargesFromABatteryBank(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(3, 1, 2), MachinesModule.BATTERY_BANK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        MachineBlockEntity bank = h.getBlockEntity(new BlockPos(3, 1, 2));
        bank.energy().generate(500_000);
        RoverEntity rover = spawn(h, new BlockPos(1, 1, 2));
        h.succeedWhen(() -> h.assertTrue(rover.energy() >= 10_000, "rover battery " + rover.energy()));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rtgKeepsTheRoverChargedInTheDark(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 2));
        rover.installSolar();
        rover.installRtg(new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        h.assertTrue(rover.hasSolar() && rover.hasRtg() && rover.hasPellet(), "upgrades fitted");
        h.succeedWhen(() -> h.assertTrue(rover.energy() >= RoverEntity.RTG_OUTPUT * 50, "energy " + rover.energy()));
    }

    /** Dust on the roof panel costs output like a tracker's; a dust whirl passing by scours it clean; dust is saved. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void dustyRoofPanelChargesSlowerUntilAWhirlPasses(GameTestHelper h) {
        h.getLevel().setDayTime(6000);
        floor(h, Blocks.STONE.defaultBlockState());
        RoverEntity clean = spawn(h, new BlockPos(1, 1, 1));
        RoverEntity dusty = spawn(h, new BlockPos(1, 1, 4));
        clean.installSolar();
        dusty.installSolar();
        dusty.setDust(1F);
        CompoundTag tag = dusty.saveWithoutId(new CompoundTag());
        h.assertTrue(Math.abs(tag.getFloat("dust") - 1F) < 1e-6, "dust is saved");
        h.runAfterDelay(100, () -> {
            double ratio = dusty.energy() / (double) clean.energy();
            h.assertTrue(clean.energy() > 0, "the clean panel charges");
            h.assertTrue(Math.abs(ratio - SolarPanelBlockEntity.dustFactor(1F)) < 0.05, "dusty / clean = " + ratio);
            // with no player near, a whirl dies away after a second; keep one passing until the rover's next check
            for (int k = 0; k < 4; k++) {
                h.runAfterDelay(k * 15L, () -> h.spawn(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.DUST_WHIRL.get(),
                        Vec3.atBottomCenterOf(new BlockPos(2, 1, 4))).setLife(100));
            }
            h.succeedWhen(() -> h.assertTrue(dusty.dust() == 0F, "the whirl swept the roof panel, dust " + dusty.dust()));
        });
    }

    @GameTest(template = "empty")
    public static void roverKeepsCargoEnergyAndUpgradesWhenSaved(GameTestHelper h) {
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 2));
        rover.setEnergy(123_456);
        rover.installRtg(new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        rover.cargo().setItem(5, new ItemStack(Items.DIAMOND, 7));
        CompoundTag tag = rover.saveWithoutId(new CompoundTag());
        RoverEntity copy = VehiclesModule.ROVER.get().create(h.getLevel());
        h.assertTrue(copy != null, "create rover");
        copy.load(tag);
        h.assertTrue(copy.energy() == 123_456, "energy " + copy.energy());
        h.assertTrue(copy.hasRtg() && copy.hasPellet() && !copy.hasSolar(), "upgrades");
        h.assertTrue(copy.cargo().getItem(5).is(Items.DIAMOND) && copy.cargo().getItem(5).getCount() == 7, "cargo");
        h.succeed();
    }

    @GameTest(template = "rover_strip", timeoutTicks = 200)
    public static void wheelsLeaveTracksInLunarRegolith(GameTestHelper h) {
        var regolith = BuiltInRegistries.BLOCK.get(Spacebreaker.id("lunar_regolith"));
        h.assertTrue(regolith != Blocks.AIR, "lunar regolith missing");
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 5; x++) {
                h.setBlock(new BlockPos(x, 0, z), regolith);
            }
        }
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 2));
        rover.setEnergy(50_000);
        h.runAfterDelay(5, () -> rover.setSpeed(0.3));
        h.succeedWhen(() -> {
            int printed = 0;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 5; x++) {
                    BlockState s = h.getBlockState(new BlockPos(x, 0, z));
                    for (var p : s.getProperties()) {
                        if (p.getName().equals("print") && !s.getValue(p).toString().equalsIgnoreCase("none")) {
                            printed++;
                        }
                    }
                }
            }
            h.assertTrue(printed >= 4, "wheel prints " + printed);
        });
    }

    /** Pluto's nitrogen ice (friction 0.99): the rover barely brakes, barely turns, and slides on the way it was going. */
    @GameTest(template = "empty")
    public static void nitrogenIceIsSlippery(GameTestHelper h) {
        double rock = RoverPhysics.grip(0.6F);
        double ice = RoverPhysics.grip(0.98F);
        double n2 = RoverPhysics.grip(0.99F);
        h.assertTrue(rock == 1.0 && ice < 0.1 && n2 < ice, "grip rock " + rock + " ice " + ice + " nitrogen " + n2);
        int stopRock = brakeTicks(RoverPhysics.traction(0.06) * rock);
        int stopN2 = brakeTicks(RoverPhysics.traction(0.06) * n2);
        h.assertTrue(stopN2 > stopRock * 5, "braking on nitrogen ice " + stopN2 + " vs rock " + stopRock + " ticks");
        h.assertTrue(Math.abs(RoverPhysics.yawChange(0.3, 1F, n2)) < Math.abs(RoverPhysics.yawChange(0.3, 1F, 1.0)) / 4, "weak steering on ice");
        // heading turned a quarter: on ice most of the old velocity remains after 10 ticks
        double vx = 0, vz = 0.3;
        for (int i = 0; i < 10; i++) {
            double[] v = RoverPhysics.slide(vx, vz, 0.3, 0, n2);
            vx = v[0];
            vz = v[1];
        }
        h.assertTrue(vz > 0.2, "still sliding forward: vz " + vz);
        double[] grippy = RoverPhysics.slide(0, 0.3, 0.3, 0, rock);
        h.assertTrue(grippy[1] == 0 && grippy[0] == 0.3, "on rock the rover goes where it points");
        h.succeed();
    }

    private static int brakeTicks(double traction) {
        int t = 0;
        for (double s = 0.4; s > 0 && t < 100_000; t++) {
            s = RoverPhysics.speed(s, -1, true, traction);
        }
        return t;
    }

    @GameTest(template = "rover_strip", timeoutTicks = 200)
    public static void roverCoastsFurtherOnNitrogenIce(GameTestHelper h) {
        var n2 = BuiltInRegistries.BLOCK.get(Spacebreaker.id("nitrogen_ice"));
        h.assertTrue(n2 != Blocks.AIR, "nitrogen ice missing");
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 9; x++) {
                h.setBlock(new BlockPos(x, 0, z), x < 4 ? Blocks.STONE : n2);
            }
        }
        RoverEntity onRock = spawn(h, new BlockPos(1, 1, 0));
        RoverEntity onIce = spawn(h, new BlockPos(6, 1, 0));
        onRock.setEnergy(50_000);
        onIce.setEnergy(50_000);
        Vec3 a = onRock.position();
        Vec3 b = onIce.position();
        h.runAfterDelay(5, () -> {
            onRock.setSpeed(0.2);
            onIce.setSpeed(0.2);
        });
        h.runAtTickTime(45, () -> {
            double rock = onRock.position().distanceTo(a);
            double ice = onIce.position().distanceTo(b);
            h.assertTrue(ice > rock * 1.5, "coasting: rock " + rock + " ice " + ice);
            h.succeed();
        });
    }

    /** Working sounds: the rover whine rises with speed; the electrolyser and crusher loop, quiet machines do not. */
    @GameTest(template = "empty")
    public static void workingSoundsAreWired(GameTestHelper h) {
        h.assertTrue(Math.abs(RoverPhysics.motorPitch(0) - 0.8F) < 1e-6 && Math.abs(RoverPhysics.motorPitch(RoverPhysics.MAX_SPEED) - 1.3F) < 1e-6,
                "pitch 0.8 .. 1.3");
        h.assertTrue(RoverPhysics.motorPitch(1.0) <= 1.3F, "pitch is capped");
        var electrolyzer = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock) MachinesModule.ELECTROLYZER.block().get();
        var crusher = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock) MachinesModule.CRUSHER.block().get();
        var press = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock) MachinesModule.PRESS.block().get();
        h.assertTrue(electrolyzer.loopSound() == com.pizzaparker07.spacebreaker.content.audio.SbSounds.ELECTROLYZER_LOOP.get(), "electrolyser hum");
        h.assertTrue(crusher.loopSound() == com.pizzaparker07.spacebreaker.content.audio.SbSounds.DRILL_LOOP.get(), "crusher grind");
        h.assertTrue(press.loopSound() == null, "the press has its own ram sounds");
        var centrifuge = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock) MachinesModule.GAS_CENTRIFUGE.block().get();
        var reactor = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock) MachinesModule.FISSION_REACTOR.block().get();
        h.assertTrue(centrifuge.loopSound() == com.pizzaparker07.spacebreaker.content.audio.SbSounds.CENTRIFUGE_LOOP.get(), "centrifuge whine");
        h.assertTrue(reactor.loopSound() == com.pizzaparker07.spacebreaker.content.audio.SbSounds.FISSION_REACTOR_LOOP.get(), "fission unit hum");
        h.assertTrue(centrifuge.loopShareRadius() > 0 && reactor.loopShareRadius() == 0, "a centrifuge cascade shares one loop");
        h.succeed();
    }

    /**
     * Getting in the way a player does, by using the rover with an empty hand: the first player takes the driver's
     * seat, the second the passenger's, a third is turned away. On every open model (and the cabin one) with its
     * deck fittings on, so no fitting swallows the click.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void playersBoardOpenRoversByUsingThem(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        var types = new java.util.ArrayList<net.minecraft.world.entity.EntityType<RoverEntity>>(java.util.List.of(
                VehiclesModule.ROVER.get(), VehiclesModule.SPRINT_ROVER.get(), VehiclesModule.PRESSURIZED_ROVER.get()));
        for (int i = 0; i < types.size(); i++) {
            RoverEntity rover = types.get(i).create(h.getLevel());
            Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1 + i * 3, 1, 2)));
            rover.moveTo(at.x, at.y, at.z, 0F, 0F);
            rover.installSolar();
            rover.installRtg(new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
            rover.cargo().setItem(0, new ItemStack(net.minecraft.world.item.Items.CHEST));
            h.getLevel().addFreshEntity(rover);
            var driver = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var passenger = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var third = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(types.get(i)).getPath();
            h.assertTrue(rover.interact(driver, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction() && driver.getVehicle() == rover,
                    id + ": the first player gets in");
            h.assertTrue(rover.interact(passenger, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction() && passenger.getVehicle() == rover,
                    id + ": the second player gets in");
            rover.interact(third, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(third.getVehicle() == null, id + ": a third is turned away");
            h.assertTrue(rover.getControllingPassenger() == driver, id + ": the first one drives");
        }
        h.succeed();
    }

    /**
     * Two riders: the driver in front, the passenger behind, both on their seats, and the deck solar panel well behind
     * the passenger's head (it used to stand over the rear seat). Getting off puts each rider beside the rover.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void twoRidersSitClearOfThePanel(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 2));
        rover.installSolar();
        net.minecraft.world.entity.player.Player driver = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player passenger = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(driver.startRiding(rover, true) && passenger.startRiding(rover, true), "both get on");
        h.assertTrue(!h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).startRiding(rover), "a third does not fit");
        h.runAfterDelay(2, () -> {
            h.assertTrue(rover.getControllingPassenger() == driver, "the first rider drives");
            // rover yaw 0 faces +z: the driver sits ahead of the centre, the passenger behind it
            double dz = driver.getZ() - rover.getZ();
            double pz = passenger.getZ() - rover.getZ();
            h.assertTrue(dz > 0.1 && pz < -0.3, "driver ahead, passenger behind: " + dz + " / " + pz);
            // the panel starts 15 px behind the centre: the passenger's head (4 px either side) stays clear of it
            double panelFront = -15 / 16.0;
            h.assertTrue(pz - 0.25 > panelFront, "passenger's head clear of the panel: " + (pz - 0.25) + " vs " + panelFront);
            driver.stopRiding();
            passenger.stopRiding();
            h.assertTrue(!driver.isPassenger() && !passenger.isPassenger(), "both get off");
            h.assertTrue(!h.getLevel().getBlockState(driver.blockPosition()).isSuffocating(h.getLevel(), driver.blockPosition()),
                    "dismounted into free space");
            // beside the rover on the ground, not on its roof or panel (QA R2)
            for (var p : new net.minecraft.world.entity.player.Player[]{driver, passenger}) {
                double horiz = Math.hypot(p.getX() - rover.getX(), p.getZ() - rover.getZ());
                h.assertTrue(p.getY() < rover.getY() + 0.5, "stepped down, not onto the roof: y " + (p.getY() - rover.getY()));
                h.assertTrue(horiz > rover.getBbWidth() / 2, "stepped out to the side: " + horiz);
            }
            h.succeed();
        });
    }

    /** One-block steps and slopes do not stop the rover: it climbs onto a raised floor. */
    @GameTest(template = "rover_strip", timeoutTicks = 120)
    public static void roverClimbsAOneBlockStep(GameTestHelper h) {
        for (int z = 0; z < 12; z++) {
            for (int x = 0; x < 5; x++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                if (z >= 5) {
                    h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                }
            }
        }
        RoverEntity rover = spawn(h, new BlockPos(2, 1, 1));
        rover.setEnergy(100_000);
        h.succeedWhen(() -> {
            rover.setSpeed(0.25);
            h.assertTrue(rover.getY() >= h.absolutePos(new BlockPos(2, 2, 7)).getY() - 0.01 && rover.getZ() > h.absolutePos(new BlockPos(2, 2, 6)).getZ(),
                    "up on the step: y " + rover.getY() + " z " + rover.getZ());
        });
    }

    /** The body leans with the ground under the wheels: level on flat ground, nose up onto a half step, never past the limit. */
    @GameTest(template = "empty")
    public static void bodyLeansWithTheGround(GameTestHelper h) {
        h.assertTrue(com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.tilt(0, 0, 1.5) == 0, "flat ground: level");
        float halfStep = com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.tilt(0.5, 0, 1.5);
        h.assertTrue(Math.abs(Math.toDegrees(halfStep) - 18.43) < 0.1, "half a block over the wheelbase: ~18 degrees, got " + Math.toDegrees(halfStep));
        h.assertTrue(com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.tilt(3, 0, 1.5)
                == com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.MAX_TILT, "a wall does not stand it on end");
        h.succeed();
    }

    /** The sprint rover: faster and further on a smaller battery, a small cargo bay, and it keeps its type when saved. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void sprintRoverIsFastLightAndQuiet(GameTestHelper h) {
        var sprint = com.pizzaparker07.spacebreaker.content.vehicles.RoverType.SPRINT;
        var standard = com.pizzaparker07.spacebreaker.content.vehicles.RoverType.STANDARD;
        double v1 = 0, v2 = 0;
        for (int i = 0; i < 400; i++) {
            v1 = com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.speed(v1, 1, true, 1, standard.maxSpeed(), standard.accel());
            v2 = com.pizzaparker07.spacebreaker.content.vehicles.RoverPhysics.speed(v2, 1, true, 1, sprint.maxSpeed(), sprint.accel());
        }
        h.assertTrue(Math.abs(v1 - standard.maxSpeed()) < 1e-9 && Math.abs(v2 - sprint.maxSpeed()) < 1e-9, "top speeds " + v1 + " / " + v2);
        h.assertTrue(sprint.maxSpeed() * 20 >= 13 - 1e-9, "13 m/s");
        h.assertTrue(sprint.range() > standard.range(), "goes further on less: " + sprint.range() + " vs " + standard.range());
        h.assertTrue(sprint.loudness() < standard.loudness() && sprint.grip() < standard.grip(), "quieter, skids sooner");
        floor(h, Blocks.STONE.defaultBlockState());
        RoverEntity rover = h.spawn(VehiclesModule.SPRINT_ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(2, 1, 2)));
        h.assertTrue(rover.roverType() == sprint, "the entity knows its type");
        h.assertTrue(rover.cargo().getContainerSize() == 9, "one row of cargo");
        h.assertTrue(rover.getPickResult().is(VehiclesModule.SPRINT_ROVER_ITEM.get()), "picks as a sprint rover");
        rover.setEnergy(1_000_000);
        h.assertTrue(rover.energy() == sprint.capacity(), "battery capped at its own capacity");
        CompoundTag tag = new CompoundTag();
        rover.save(tag);
        var copy = net.minecraft.world.entity.EntityType.create(tag, h.getLevel()).orElseThrow();
        h.assertTrue(copy instanceof RoverEntity r && r.roverType() == sprint, "loads back as a sprint rover");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(com.pizzaparker07.spacebreaker.Spacebreaker.id("sprint_rover")).isPresent(), "craftable");
        h.succeed();
    }

    /**
     * The pressurised rover: it fills its cabin tank from an oxygen tank it is parked beside, riders breathe from it,
     * and the cabin loses pressure when the oxygen or the power runs out. The cabin keeps out half the radiation.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void pressurizedRoverKeepsItsCrewBreathing(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        var type = com.pizzaparker07.spacebreaker.content.vehicles.RoverType.PRESSURIZED;
        h.assertTrue(type.cabin() && type.range() >= 15_000, "a cabin and a long range: " + type.range());
        h.setBlock(new BlockPos(0, 1, 2), MachinesModule.PRESSURIZED_TANK.get());
        com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity tank = h.getBlockEntity(new BlockPos(0, 1, 2));
        tank.tank().fill(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.OXYGEN.stack(8_000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        RoverEntity rover = h.spawn(VehiclesModule.PRESSURIZED_ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(2, 1, 2)));
        rover.setYRot(0F);
        rover.setEnergy(100_000);
        h.assertTrue(!rover.cabinPressurized(), "no air before filling");
        h.assertTrue(rover.radiationFactor() == 0.5, "the cabin walls stop half the radiation");
        h.runAfterDelay(30, () -> {
            h.assertTrue(rover.cabinOxygen() > 0 && rover.cabinPressurized(), "filled from the tank beside it: " + rover.cabinOxygen());
            h.assertTrue(tank.tank().getFluidAmount() < 8_000, "the tank gave its oxygen");
            var crew = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(crew.startRiding(rover, true), "gets in");
            int before = rover.cabinOxygen();
            h.assertTrue(rover.breathe(crew, 100), "breathes cabin air");
            h.assertTrue(rover.cabinOxygen() == before - 100, "used 100 breathing ticks");
            h.assertTrue(!rover.breathe(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), 10), "only riders breathe it");
            CompoundTag tag = new CompoundTag();
            rover.save(tag);
            var copy = net.minecraft.world.entity.EntityType.create(tag, h.getLevel()).orElseThrow();
            h.assertTrue(copy instanceof RoverEntity r && r.cabinOxygen() == rover.cabinOxygen(), "cabin oxygen is saved");
            rover.setEnergy(0);
            h.assertTrue(!rover.cabinPressurized(), "no power, no life support");
            rover.setEnergy(1_000);
            rover.setCabinOxygen(0);
            h.assertTrue(!rover.cabinPressurized() && !rover.breathe(crew, 1), "an empty tank means no cabin air");
            h.succeed();
        });
    }

    /**
     * Life support reads the pressurised rover's cabin: a rider without a helmet has air at 21 °C, half the outside
     * dose and normal pressure, breathing the cabin tank down; an empty tank or a plain rover is the outside again.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cabinLetsTheCrewBreatheWithoutHelmets(GameTestHelper h) {
        floor(h, Blocks.STONE.defaultBlockState());
        for (var type : new net.minecraft.world.entity.EntityType<?>[]{VehiclesModule.PRESSURIZED_ROVER.get(), VehiclesModule.AMPHIBIOUS_ROVER.get()}) {
            RoverEntity rover = (RoverEntity) h.spawn(type, Vec3.atBottomCenterOf(new BlockPos(2, 1, 2)));
            rover.setEnergy(100_000);
            rover.setCabinOxygen(1_000);
            var crew = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(crew.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(), "no helmet");
            h.assertTrue(crew.startRiding(rover, true), "boards");
            var env = com.pizzaparker07.spacebreaker.content.suit.LifeSupport.Env.at(crew, 100.0);
            h.assertTrue(env.breathable() && env.ambientC() == 21.0 && Math.abs(env.doseRateMsvDay() - 50.0) < 1e-9 && env.pressureKpa() > 100,
                    "cabin air, 21 C, half the dose: " + env);
            // a resting rider breathes a little under one tick's worth per tick: a few ticks use up whole units
            for (int i = 0; i < 5; i++) {
                com.pizzaparker07.spacebreaker.content.suit.LifeSupport.Env.at(crew, 100.0);
            }
            h.assertTrue(rover.cabinOxygen() < 1_000 && rover.cabinOxygen() >= 990, "breathing the cabin tank down: " + rover.cabinOxygen());
            rover.setCabinOxygen(0);
            h.assertTrue(com.pizzaparker07.spacebreaker.content.suit.LifeSupport.cabin(crew) == null, "an empty tank is the outside again");
            crew.stopRiding();
            rover.discard();
        }
        RoverEntity open = spawn(h, new BlockPos(2, 1, 2));
        open.setEnergy(100_000);
        var rider = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        rider.startRiding(open, true);
        h.assertTrue(com.pizzaparker07.spacebreaker.content.suit.LifeSupport.cabin(rider) == null, "the open rover has no cabin");
        h.succeed();
    }

    /** A pool of a liquid, 7 x 9 and 3 deep, on a stone floor. */
    /**
     * A walled pool that stays inside the 8x8 test structure: stone floor and rim, liquid 6x6x3 inside. Liquid must
     * not run past the structure, where it would flow into (and be shaped by) the neighbouring tests.
     */
    private static void pool(GameTestHelper h, BlockState liquid) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean rim = x == 0 || x == 7 || z == 0 || z == 7;
                for (int y = 1; y <= 3; y++) {
                    h.setBlock(new BlockPos(x, y, z), rim ? Blocks.STONE.defaultBlockState() : liquid);
                }
            }
        }
    }

    /** The amphibious rover floats at its waterline on water and sails across it under its own power. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void amphibiousRoverFloatsOnWater(GameTestHelper h) {
        pool(h, Blocks.WATER.defaultBlockState());
        RoverEntity boat = h.spawn(VehiclesModule.AMPHIBIOUS_ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(3, 2, 2)));
        boat.setYRot(0F);
        boat.setEnergy(500_000);
        double surface = h.absolutePos(new BlockPos(0, 4, 0)).getY();
        double[] z0 = new double[1];
        h.runAfterDelay(60, () -> {
            h.assertTrue(boat.afloat(), "afloat");
            double draft = surface - boat.getY();
            h.assertTrue(draft > 0.2 && draft < 0.8, "riding at its waterline, draft " + draft);
            z0[0] = boat.getZ();
            boat.setSpeed(0.2);
        });
        h.runAfterDelay(80, () -> {
            h.assertTrue(boat.getZ() - z0[0] > 1, "it sails on: moved " + (boat.getZ() - z0[0]));
            h.assertTrue(surface - boat.getY() < 0.8, "and stays up");
            h.succeed();
        });
    }

    /** A plain rover in the same pool sinks to the bottom. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void plainRoverSinks(GameTestHelper h) {
        pool(h, Blocks.WATER.defaultBlockState());
        RoverEntity sinker = h.spawn(VehiclesModule.ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(3, 3, 3)));
        double surface = h.absolutePos(new BlockPos(0, 4, 0)).getY();
        h.runAfterDelay(60, () -> {
            h.assertTrue(!sinker.afloat() && sinker.getY() < surface - 1.5, "a plain rover sinks: " + (surface - sinker.getY()));
            h.succeed();
        });
    }

    /** Driving out: afloat, it noses into the bank and climbs out onto dry ground. */
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void amphibiousRoverClimbsOutOntoTheShore(GameTestHelper h) {
        // water z 1..3 behind a rim, shore (stone up to y 3) from z 4 on, all inside the 8x8 structure
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean water = x > 0 && x < 7 && z > 0 && z < 4;
                for (int y = 1; y <= 3; y++) {
                    h.setBlock(new BlockPos(x, y, z), water ? Blocks.WATER : Blocks.STONE);
                }
            }
        }
        RoverEntity boat = h.spawn(VehiclesModule.AMPHIBIOUS_ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(3, 3, 2)));
        boat.setYRot(0F);
        boat.setEnergy(500_000);
        h.succeedWhen(() -> {
            boat.setSpeed(0.2);
            h.assertTrue(!boat.afloat() && boat.getY() >= h.absolutePos(new BlockPos(0, 4, 0)).getY() - 0.01
                    && boat.getZ() > h.absolutePos(new BlockPos(0, 4, 4)).getZ() + 1.0, "out on the shore: y " + boat.getY() + " z " + boat.getZ());
        });
    }

    /** Titan's methane lakes float it the same way. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void amphibiousRoverFloatsOnLiquidMethane(GameTestHelper h) {
        pool(h, com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.TITAN_METHANE.get().defaultBlockState());
        RoverEntity boat = h.spawn(VehiclesModule.AMPHIBIOUS_ROVER.get(), Vec3.atBottomCenterOf(new BlockPos(3, 2, 3)));
        boat.setEnergy(500_000);
        double surface = h.absolutePos(new BlockPos(0, 4, 0)).getY();
        h.runAfterDelay(60, () -> {
            double draft = surface - boat.getY();
            h.assertTrue(boat.afloat() && draft > 0.2 && draft < 0.8, "floats on methane, draft " + draft);
            var type = com.pizzaparker07.spacebreaker.content.vehicles.RoverType.AMPHIBIOUS;
            h.assertTrue(type.amphibious() && type.cabin() && type.swimSpeed() * 20 >= 5 - 1e-9, "pressurised, 5 m/s afloat");
            h.succeed();
        });
    }

    private static void floor(GameTestHelper h, BlockState state) {
        for (int z = 0; z < 5; z++) {
            for (int x = 0; x < 5; x++) {
                h.setBlock(new BlockPos(x, 0, z), state);
            }
        }
    }

    private RoverTests() {
    }
}
