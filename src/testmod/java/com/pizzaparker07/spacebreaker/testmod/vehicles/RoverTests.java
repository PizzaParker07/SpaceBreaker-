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

    @GameTest(template = "empty", timeoutTicks = 200)
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

    @GameTest(template = "empty", timeoutTicks = 200)
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

    @GameTest(template = "empty", timeoutTicks = 200)
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
        h.succeed();
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
