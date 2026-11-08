package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The planetary rover on the Moon or Mars: parked with its solar panel and RTG, driving past in a dust cloud,
 * the wheel tracks it leaves, headlights at night and the driver's view with the battery readout.
 */
final class RoverScenario implements ShowcaseScenario {
    private static final int X0 = 24;
    private static final int Z0 = 24;
    private final ResourceKey<Level> dimension;
    private final boolean mars;
    private int gy;
    private RoverEntity parked;
    private RoverEntity driver;
    private RoverEntity cabin;

    RoverScenario(ResourceKey<Level> dimension, boolean mars) {
        this.dimension = dimension;
        this.mars = mars;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public void build(ServerLevel level) {
        PlanetTerrain t = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        if (mars) {
            com.pizzaparker07.spacebreaker.content.mars.MarsWeather.force(level, 0F, 0);
        }
        gy = (int) Math.floor(t.height(X0, Z0)) + 1;
        BlockState soil = level.getBlockState(new BlockPos(X0, gy - 1, Z0));
        if (soil.isAir()) {
            soil = Blocks.STONE.defaultBlockState();
        }
        // a level strip of the local soil so the rover can roll and print its tracks
        for (int x = X0 - 8; x <= X0 + 8; x++) {
            for (int z = Z0 - 6; z <= Z0 + 34; z++) {
                for (int y = gy - 3; y <= gy + 6; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < gy ? soil.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        parked = rover(level, X0 - 3.5, Z0 + 2.5, 35F);
        parked.installSolar();
        parked.installRtg(new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
        parked.cargo().setItem(0, new ItemStack(Items.CHEST));
        driver = rover(level, X0 + 3.5, Z0 + 30.5, 180F);
        // a rover nosing up onto a ledge: slabs under the front wheels, a full block under the left front one
        for (int x = X0 - 7; x <= X0 - 4; x++) {
            for (int z = Z0 + 11; z <= Z0 + 13; z++) {
                level.setBlock(new BlockPos(x, gy, z), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
            }
        }
        RoverEntity sprint = com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule.SPRINT_ROVER.get().create(level);
        sprint.moveTo(X0 + 2.5, gy, Z0 + 2.5, 35F, 0F);
        sprint.setEnergy(250_000);
        level.addFreshEntity(sprint);
        cabin = com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule.PRESSURIZED_ROVER.get().create(level);
        cabin.moveTo(X0 - 4.5, gy, Z0 + 20.5, 35F, 0F);
        cabin.setEnergy(900_000);
        cabin.setCabinOxygen(100_000);
        cabin.installSolar();
        level.addFreshEntity(cabin);
        RoverEntity tilted = rover(level, X0 - 5.5, Z0 + 10.3, 0F);
        tilted.installSolar();
    }

    private RoverEntity rover(ServerLevel level, double x, double z, float yaw) {
        RoverEntity r = VehiclesModule.ROVER.get().create(level);
        r.moveTo(x, gy, z, yaw, 0F);
        r.setEnergy(300_000);
        level.addFreshEntity(r);
        return r;
    }

    private static Consumer<ServerPlayer> at(double dayFraction, Consumer<ServerPlayer> more) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            more.accept(p);
        };
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        String w = mars ? "mars" : "moon";
        // shots are listed before the server has built the scene: work from the terrain, not the entities
        ServerLevel level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getLevel(dimension);
        gy = (int) Math.floor(((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain().height(X0, Z0)) + 1;
        double px = X0 - 3.5;
        double pz = Z0 + 2.5;
        // parked, three-quarter view from the front left
        s.add(new ShowcaseDriver.Shot(w + "_01_rover", 0, px + 3.2, gy + 1.9, pz + 4.2, MoonScenario.yaw(-3.2, -4.2), 16F, true, fp, 80,
                at(0.3, p -> { }), null));
        // driving past: the camera waits by the strip, the rover rolls towards it kicking up dust
        s.add(new ShowcaseDriver.Shot(w + "_02_driving", 0, X0 - 0.5, gy + 1.6, Z0 + 20.5, MoonScenario.yaw(4, 6), 8F, true, fp, 34,
                at(0.3, p -> {
                    driver.moveTo(X0 + 3.5, gy, Z0 + 30.5, 180F, 0F);
                    driver.setSpeed(0.42);
                }), null));
        // the tracks it left
        s.add(new ShowcaseDriver.Shot(w + "_03_tracks", 0, X0 + 3.5, gy + 4.5, Z0 + 32.5, 180F, 38F, true, fp, 40,
                at(0.3, p -> driver.setSpeed(0)), null));
        // night: headlights
        s.add(new ShowcaseDriver.Shot(w + "_04_night", 0, px + 1.5, gy + 1.7, pz + 7.5, MoonScenario.yaw(-1.5, -7.5) + 12F, 8F, true, fp, 80,
                at(0.78, p -> { }), null));
        // in the driver's seat with the battery readout
        s.add(new ShowcaseDriver.Shot(w + "_05_driver_view", 0, px, gy + 2, pz, 35F, 12F, false, CameraType.THIRD_PERSON_BACK, 60,
                at(0.3, p -> p.startRiding(parked, true)), null));
        // the pressurised rover with its crew behind the windows
        s.add(new ShowcaseDriver.Shot(w + "_09_pressurized", 0, X0 - 4.5 - 0.5, gy + 2.6, Z0 + 20.5 + 5.1, MoonScenario.yaw(0.5, -5.1), 18F, true, fp, 80,
                at(0.3, p -> crewIn(p.serverLevel(), cabin)), null));
        // the sprint rover beside the standard one
        s.add(new ShowcaseDriver.Shot(w + "_08_sprint", 0, X0 + 5.6, gy + 1.9, Z0 + 6.4, MoonScenario.yaw(-3.6, -4.4), 16F, true, fp, 80,
                at(0.3, p -> { }), null));
        // the rover leans with the ground under its wheels
        s.add(new ShowcaseDriver.Shot(w + "_07_slope", 0, X0 - 1.5, gy + 1.4, Z0 + 10.3, 90F, 8F, true, fp, 60, at(0.3, p -> { }), null));
        // two seats taken (suited stand-ins for the crew, sitting like players do), side view: the roof panel must clear their heads
        s.add(new ShowcaseDriver.Shot(w + "_06_two_seats", 0, px + 3.7, gy + 1.6, pz + 2.6, MoonScenario.yaw(-3.7, -2.6), 8F, true, fp, 60,
                at(0.3, p -> crew(p.serverLevel())), null));
        // what the crew see: first person from the driver's and the passenger's seat, and from the cabin rover's seat
        s.add(new ShowcaseDriver.Shot(w + "_10_driver_fp", 0, px, gy + 2, pz, 35F, 6F, false, fp, 60,
                at(0.3, p -> seat(p, parked, 0)), null));
        s.add(new ShowcaseDriver.Shot(w + "_11_passenger_fp", 0, px, gy + 2, pz, 35F, 6F, false, fp, 60,
                at(0.3, p -> seat(p, parked, 1)), null));
        s.add(new ShowcaseDriver.Shot(w + "_12_cabin_driver_fp", 0, X0 - 4.5, gy + 2, Z0 + 20.5, 35F, 6F, false, fp, 60,
                at(0.3, p -> seat(p, cabin, 0)), null));
        // the HUD on a low and on an empty battery: the percentage beside the bar, the blinking red battery icon
        s.add(new ShowcaseDriver.Shot(w + "_13_battery_low", 0, px, gy + 2, pz, 35F, 6F, false, fp, 60,
                at(0.3, p -> {
                    parked.setEnergy(parked.roverType().capacity() * 12 / 100);
                    seat(p, parked, 0);
                }), null));
        // (the rover without a panel or an RTG: nothing tops it up while the shot waits)
        s.add(new ShowcaseDriver.Shot(w + "_14_battery_empty", 0, X0 + 3.5, gy + 2, Z0 + 30.5, 180F, 6F, false, fp, 60,
                at(0.3, p -> {
                    driver.moveTo(X0 + 3.5, gy, Z0 + 30.5, 180F, 0F);
                    driver.setSpeed(0);
                    driver.setEnergy(0);
                    seat(p, driver, 0);
                }), null));
        // the driver-view shot leaves the player in the parked rover, where the driver's teleport cannot move them:
        // every shot that is not a seat view gets them out first and puts them at its own camera spot
        List<ShowcaseDriver.Shot> out = new ArrayList<>();
        for (ShowcaseDriver.Shot shot : s) {
            boolean seated = shot.name().endsWith("_05_driver_view") || shot.name().matches(".*_1[0-4]_.*");
            Consumer<ServerPlayer> setup = shot.serverSetup();
            out.add(seated ? shot : new ShowcaseDriver.Shot(shot.name(), shot.time(), shot.x(), shot.y(), shot.z(), shot.yaw(), shot.pitch(),
                    shot.hideGui(), shot.camera(), shot.minTicks(), p -> {
                if (p.isPassenger()) {
                    p.stopRiding();
                    p.connection.teleport(shot.x(), shot.y() - p.getEyeHeight(), shot.z(), shot.yaw(), shot.pitch());
                }
                if (setup != null) {
                    setup.accept(p);
                }
            }, shot.clientSetup()));
        }
        return out;
    }

    /** Puts the player in seat 0 (driver) or 1 (passenger, with a stand-in driving) of the rover, facing ahead. */
    private void seat(ServerPlayer p, RoverEntity target, int index) {
        p.stopRiding();
        for (net.minecraft.world.entity.Entity e : new ArrayList<>(target.getPassengers())) {
            e.stopRiding();
            if (!(e instanceof ServerPlayer)) {
                e.discard();
            }
        }
        if (index == 1) {
            p.serverLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.EASY, true);
            net.minecraft.world.entity.monster.Skeleton stand = net.minecraft.world.entity.EntityType.SKELETON.create(p.serverLevel());
            stand.setNoAi(true);
            stand.setPersistenceRequired();
            stand.moveTo(target.getX(), target.getY(), target.getZ());
            suitUp(stand);
            p.serverLevel().addFreshEntity(stand);
            stand.startRiding(target, true);
        }
        p.startRiding(target, true);
        p.setYRot(target.getYRot());
        p.setYHeadRot(target.getYRot());
        p.connection.teleport(p.getX(), p.getY(), p.getZ(), target.getYRot(), 6F);
    }

    /** Dresses a crew stand-in in a full EVA suit, so it reads as an astronaut (the thermal helmet's gold visor hides the face). */
    private static void suitUp(net.minecraft.world.entity.LivingEntity stand) {
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.THERMAL_HELMET.get()));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.EVA_CHESTPLATE.get()));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.EVA_LEGGINGS.get()));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.EVA_BOOTS.get()));
    }

    private void crew(ServerLevel level) {
        crewIn(level, parked);
    }

    private void crewIn(ServerLevel level, RoverEntity target) {
        if (target.getPassengers().size() >= 2) {
            return;
        }
        // peaceful removes monsters; the stand-ins (skeletons: arms at rest) have no AI and wear full suits, so nothing happens to the scene
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.EASY, true);
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.monster.Skeleton stand = net.minecraft.world.entity.EntityType.SKELETON.create(level);
            stand.setNoAi(true);
            stand.setPersistenceRequired();
            stand.moveTo(target.getX(), target.getY(), target.getZ());
            suitUp(stand);
            level.addFreshEntity(stand);
            stand.startRiding(target, true);
        }
    }
}
