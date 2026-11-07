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
        // the rover leans with the ground under its wheels
        s.add(new ShowcaseDriver.Shot(w + "_07_slope", 0, X0 - 1.5, gy + 1.4, Z0 + 10.3, 90F, 8F, true, fp, 60, at(0.3, p -> { }), null));
        // two seats taken (helmeted stand-ins for the crew, sitting like players do), side view: the roof panel must clear their heads
        s.add(new ShowcaseDriver.Shot(w + "_06_two_seats", 0, px + 3.7, gy + 1.6, pz + 2.6, MoonScenario.yaw(-3.7, -2.6), 8F, true, fp, 60,
                at(0.3, p -> crew(p.serverLevel())), null));
        return s;
    }

    private void crew(ServerLevel level) {
        if (parked.getPassengers().size() >= 2) {
            return;
        }
        // peaceful removes zombies; the stand-ins have no AI and wear helmets, so nothing happens to the scene
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.EASY, true);
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.monster.Zombie stand = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            stand.setNoAi(true);
            stand.setPersistenceRequired();
            stand.moveTo(parked.getX(), parked.getY(), parked.getZ());
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            level.addFreshEntity(stand);
            stand.startRiding(parked, true);
        }
    }
}
