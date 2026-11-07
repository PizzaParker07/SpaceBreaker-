package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renderer compatibility showcase: the same sky checks in every space world (Moon by day and night with Earth,
 * Earth orbit, Moon orbit, Mars), plus a small prop stage with block entity renderers, emissive and translucent
 * blocks and a suited player, so vanilla, Sodium and Sodium + Iris + shader pack runs can be compared shot by
 * shot (-Pscenario=compat with -PshotPrefix=vanilla/sodium/iris).
 */
final class CompatScenario implements ShowcaseScenario {
    private static final double ORBIT_Y = RocketEntity.ORBIT_Y;
    private PlanetTerrain moonTerrain;
    private static volatile BlockPos stageOrigin;
    private PlanetTerrain marsTerrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    public void build(ServerLevel moon) {
        moonTerrain = terrain(moon);
        int gy = (int) ground(moonTerrain, 0.5, 0.5);
        stageOrigin = new BlockPos(4, gy, -6);
        stage(moon, stageOrigin);
        // a small platform in each orbit so terrain/sky depth sorting can be judged against real blocks
        for (Location where : new Location[]{Location.EARTH_ORBIT, Location.MOON_ORBIT}) {
            ServerLevel orbit = moon.getServer().getLevel(where.dimension());
            if (orbit != null) {
                platform(orbit, new BlockPos(0, (int) ORBIT_Y - 1, 0));
            }
        }
    }

    private static PlanetTerrain terrain(ServerLevel level) {
        return ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
    }

    private static double ground(PlanetTerrain t, double x, double z) {
        return Math.floor(t.height(x, z)) + 1;
    }

    private PlanetTerrain moon() {
        if (moonTerrain == null) {
            moonTerrain = terrain(Minecraft.getInstance().getSingleplayerServer().getLevel(MoonModule.MOON));
        }
        return moonTerrain;
    }

    private PlanetTerrain mars() {
        if (marsTerrain == null) {
            marsTerrain = terrain(Minecraft.getInstance().getSingleplayerServer().getLevel(MarsModule.MARS));
        }
        return marsTerrain;
    }

    /** Props with block entity renderers, emissive parts and translucent blocks on a steel floor. */
    private static void stage(ServerLevel level, BlockPos origin) {
        BlockState floor = EarthModule.STEEL.block.get().defaultBlockState();
        for (int dx = -1; dx <= 9; dx++) {
            for (int dz = -1; dz <= 5; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), floor, 3);
                for (int dy = 0; dy < 6; dy++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        place(level, origin.offset(0, 0, 0), MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        place(level, origin.offset(2, 0, 0), MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        place(level, origin.offset(4, 0, 0), MachinesModule.FLUID_TANK.get().defaultBlockState());
        place(level, origin.offset(5, 0, 0), MachinesModule.CRYOGENIC_TANK.get().defaultBlockState());
        place(level, origin.offset(7, 0, 0), facing(SuitModule.OXYGEN_FILLER.get().defaultBlockState(), Direction.SOUTH));
        place(level, origin.offset(8, 0, 0), facing(RocketryModule.LAUNCH_CONSOLE.get().defaultBlockState(), Direction.SOUTH));
        // translucent and emissive vanilla references
        place(level, origin.offset(0, 0, 3), Blocks.GLASS.defaultBlockState());
        place(level, origin.offset(1, 0, 3), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        place(level, origin.offset(2, 0, 3), Blocks.SEA_LANTERN.defaultBlockState());
        place(level, origin.offset(3, 0, 3), Blocks.LANTERN.defaultBlockState());
    }

    private static BlockState facing(BlockState state, Direction dir) {
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, dir);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
        }
        return state;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }

    private static void platform(ServerLevel level, BlockPos center) {
        BlockState floor = EarthModule.STEEL.block.get().defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(center.offset(dx, 0, dz), floor, 3);
            }
        }
        level.setBlock(center.offset(-3, 1, -3), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        level.setBlock(center.offset(3, 1, -3), Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(3, 2, -3), Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, -3), Blocks.SEA_LANTERN.defaultBlockState(), 3);
    }

    private static Consumer<ServerPlayer> go(ResourceKey<Level> dim, double x, double y, double z, float yaw, float pitch,
                                             double dayFraction, Consumer<ServerPlayer> more) {
        return p -> {
            ServerLevel level = p.server.getLevel(dim);
            if (level == null) {
                return;
            }
            p.teleportTo(level, x, y - p.getEyeHeight(), z, yaw, pitch);
            if (dayFraction >= 0) {
                PlanetTime.setDayFraction(level, dayFraction);
            }
            level.setWeatherParameters(12000, 0, false, false);
            if (more != null) {
                more.accept(p);
            }
        };
    }

    private static void suitUp(ServerPlayer p) {
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SuitModule.EVA_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
    }

    private static void unsuit(ServerPlayer p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            p.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        PlanetTerrain t = moon();
        double vx = 0.5;
        double vz = 0.5;
        double gy = ground(t, vx, vz);
        Vector3d earth = SkyMath.parentDirection(t.latitude(vz), t.longitude(vx), 0.25);
        float earthYaw = MoonScenario.yaw(earth.x, earth.z);
        float earthPitch = MoonScenario.pitch(earth.x, earth.y, earth.z);
        ResourceKey<Level> moonDim = MoonModule.MOON;

        s.add(new ShowcaseDriver.Shot("01_moon_vista_day", 0, 0, 0, 0, 0F, 0F, true, fp, 260,
                go(moonDim, vx, gy + 14, vz, earthYaw + 25F, 6F, 0.12, CompatScenario::unsuit), MoonScenario::farView));
        s.add(new ShowcaseDriver.Shot("02_moon_earth_day", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, vx, gy + 3, vz, earthYaw, earthPitch * 0.85F, 0.05, null), null));
        s.add(new ShowcaseDriver.Shot("03_moon_sun_shadows", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, vx, gy + 10, vz, -90F, 12F, 0.97, null), null));
        s.add(new ShowcaseDriver.Shot("04_moon_night_earth", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, vx, gy + 3, vz, earthYaw, earthPitch * 0.7F, 0.75, null), null));
        s.add(new ShowcaseDriver.Shot("05_moon_props", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, 8.5, ground(t, 8, -6) + 2.2, 2.5, 180F, 22F, 0.15, null), null));
        s.add(new ShowcaseDriver.Shot("06_moon_suit_visor", 0, 0, 0, 0, 0F, 0F, true, CameraType.THIRD_PERSON_FRONT, 80,
                go(moonDim, 6.5, ground(t, 6, -2) + 1.62, -1.5, 180F, 8F, 0.15, CompatScenario::suitUp), null));
        s.add(new ShowcaseDriver.Shot("07_earth_orbit", 0, 0, 0, 0, 0F, 0F, true, fp, 140,
                go(Location.EARTH_ORBIT.dimension(), 2.5, ORBIT_Y + 1.6, 3.5, 160F, 35F, -1, CompatScenario::unsuit), null));
        s.add(new ShowcaseDriver.Shot("08_earth_orbit_up", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(Location.EARTH_ORBIT.dimension(), 2.5, ORBIT_Y + 1.6, 3.5, 30F, -25F, -1, null), null));
        s.add(new ShowcaseDriver.Shot("09_moon_orbit", 0, 0, 0, 0, 0F, 0F, true, fp, 140,
                go(Location.MOON_ORBIT.dimension(), 2.5, ORBIT_Y + 1.6, 3.5, 160F, 35F, -1, null), null));
        PlanetTerrain m = mars();
        double my = ground(m, 0.5, 0.5);
        s.add(new ShowcaseDriver.Shot("10_mars_day", 0, 0, 0, 0, 0F, 0F, true, fp, 260,
                go(MarsModule.MARS, 0.5, my + 12, 0.5, 30F, 6F, 0.2, null), null));
        s.add(new ShowcaseDriver.Shot("11_mars_sunset", 0, 0, 0, 0, 0F, 0F, true, fp, 140,
                go(MarsModule.MARS, 0.5, my + 12, 0.5, 90F, 4F, 0.49, null), null));
        s.add(new ShowcaseDriver.Shot("12_mars_night", 0, 0, 0, 0, 0F, 0F, true, fp, 140,
                go(MarsModule.MARS, 0.5, my + 12, 0.5, 30F, -20F, 0.75, null), null));
        // a hologram-style ghost structure (compat.client.GhostBlocks) between the prop rows, then hidden again
        s.add(new ShowcaseDriver.Shot("14_ghost_blocks", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, 8.5, ground(t, 8, -6) + 2.2, 2.5, 180F, 22F, 0.15, null),
                () -> CompatShowcaseClient.ghostOrigin = stageOrigin == null ? null : stageOrigin.offset(3, 0, 1)));
        // habitat module (rocketry HabitatKitItem): translucent window band seen from inside and outside, roof solar row
        double hy = ground(t, 40.5, 0.5);
        s.add(new ShowcaseDriver.Shot("15_habitat_inside", 0, 0, 0, 0, 0F, 0F, true, fp, 120,
                go(moonDim, 40.5, hy + 1 + 1.62, 0.5, -90F, 4F, 0.15, p -> habitat(p, hy)), null));
        s.add(new ShowcaseDriver.Shot("16_habitat_outside", 0, 0, 0, 0, 0F, 0F, true, fp, 80,
                go(moonDim, 51.5, hy + 5.5, 8.5, 125F, 18F, 0.15, null), null));
        // docking jolt (ViewportEvent.ComputeCameraAngles roll/pitch) seen through the pack: horizon tilts in both
        s.add(new ShowcaseDriver.Shot("17_docking_jolt", 0, 0, 0, 0, 0F, 0F, false, fp, 80,
                go(moonDim, 60.5, ground(t, 60.5, 0.5) + 2, 0.5, 0F, 0F, 0.15, p -> board(p, ground(t, 60.5, 0.5))),
                () -> CompatShowcaseClient.joltLoop = true));
        // permanently shadowed polar crater floor (cold trap)
        s.add(new ShowcaseDriver.Shot("18_cold_trap", 0, 0, 0, 0, 0F, 0F, true, fp, 260, CompatScenario::coldTrap,
                () -> CompatShowcaseClient.joltLoop = false));
        // reentry plasma (GUI layer) over the pack's frame; a short view distance so the frame is ready while the
        // capsule is still in the heating phase (ticks keep running: ridden entities are never frozen)
        s.add(new ShowcaseDriver.Shot("13_reentry_plasma", 6000, 0, 0, 0, 0F, 0F, false, fp, 50, CompatScenario::reentry,
                () -> Minecraft.getInstance().options.renderDistance().set(4)));
        return s;
    }

    private static void reentry(ServerPlayer p) {
        CompatShowcaseClient.ghostOrigin = null;
        // the driver first brings the player back to the scenario's dimension, so change worlds now and board the
        // capsule a second later (riding across a dimension change in the same tick leaves the client without a view)
        ServerLevel earth = p.server.overworld();
        p.teleportTo(earth, -20.5, 701, 10.5, 0F, 60F);
        var server = p.server;
        int now = server.getTickCount();
        server.tell(new net.minecraft.server.TickTask(now + 20, () -> {
            RocketEntity capsule = RocketEntity.create(earth, RocketryModule.MOON_ROCKET, new net.minecraft.world.phys.Vec3(-20.5, 700, 10.5), 0F);
            capsule.fillCompletely();
            earth.addFreshEntity(capsule);
            capsule.stageScene(com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState.REENTRY, 0, 3, false);
            p.startRiding(capsule, true);
        }));
    }

    /** Unfolds a habitat module with its floor at the ground (terrain cleared first). */
    private static void habitat(ServerPlayer p, double groundY) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos centre = BlockPos.containing(40.5, groundY, 0.5);
        for (BlockPos q : BlockPos.betweenClosed(centre.offset(-4, 0, -4), centre.offset(4, 8, 4))) {
            level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
        }
        for (BlockPos q : BlockPos.betweenClosed(centre.offset(-4, -1, -4), centre.offset(4, -1, 4))) {
            level.setBlock(q, EarthModule.STEEL.block.get().defaultBlockState(), 3);
        }
        com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(level, centre, Direction.NORTH);
        p.teleportTo(level, 40.5, groundY + 1, 0.5, -90F, 4F);
    }

    /** Boards a landed capsule a second after arriving (as for the reentry shot). */
    private static void board(ServerPlayer p, double groundY) {
        ServerLevel level = (ServerLevel) p.level();
        var server = p.server;
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 20, () -> {
            RocketEntity capsule = RocketEntity.create(level, RocketryModule.MOON_ROCKET, new net.minecraft.world.phys.Vec3(64.5, groundY, 0.5), 0F);
            level.addFreshEntity(capsule);
            p.startRiding(capsule, true);
        }));
    }

    /** Rim of the first cold-trap crater near the Moon's north pole, looking down at its floor at local noon. */
    private static void coldTrap(ServerPlayer p) {
        p.stopRiding();
        ServerLevel moon = p.server.getLevel(MoonModule.MOON);
        if (moon == null || !(moon.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            return;
        }
        PlanetTerrain t = gen.terrain();
        double pole = t.profile().poleDistance();
        for (var c : t.cratersNear(-1500, pole * 0.9, 1500, pole)) {
            int floor = gen.coldTrapFloor((int) c.x(), (int) c.z());
            if (c.radius() >= 10 && floor != Integer.MIN_VALUE) {
                double x = c.x() + c.radius() * 1.1;
                p.teleportTo(moon, x, t.height(x, c.z()) + 6, c.z(), 90F, 28F);
                PlanetTime.setDayFraction(moon, 0.25);
                return;
            }
        }
        Spacebreaker.LOGGER.warn("showcase: no cold trap crater found");
    }
}
