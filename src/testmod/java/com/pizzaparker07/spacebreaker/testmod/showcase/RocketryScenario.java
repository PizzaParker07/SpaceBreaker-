package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.TowerAttachmentBlock;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** Rocketry showcase: assembler, launch facility with a Moon rocket, launch frames, orbit, landing and return. */
final class RocketryScenario implements ShowcaseScenario {
    private static final int F = ShowcaseBuilder.FLOOR;
    private static final double G = F + 1;
    private static final BlockPos PAD = new BlockPos(0, F, 0);
    private static final BlockPos ASSEMBLER = new BlockPos(7, F + 1, -6);
    private static final BlockPos CONSOLE = new BlockPos(5, F + 1, 6);
    private static final BlockPos LANDING = new BlockPos(40, F, 30);
    private static final BlockPos POOL = new BlockPos(-40, F, 30);

    private static RocketEntity padRocket;
    private static int moonGround = 100;
    private static final List<RocketEntity> scene = new ArrayList<>();

    @Override
    public void build(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -60; x <= 60; x++) {
            for (int z = -24; z <= 48; z++) {
                for (int y = F + 1; y <= F + 24; y++) {
                    level.setBlock(new BlockPos(x, y, z), air, Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, F, z), (Math.abs(x) <= 12 && Math.abs(z) <= 12 ? Blocks.SMOOTH_STONE : Blocks.GRASS_BLOCK).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // launch pad and flame trench
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                level.setBlock(PAD.offset(dx, 0, dz), (inner ? RocketryModule.FLAME_TRENCH : RocketryModule.LAUNCH_PAD).get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) >= 3) {
                    level.setBlock(PAD.offset(dx, 0, dz), RocketryModule.LAUNCH_PAD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        // service tower with the crew access arm at capsule height and the fuel umbilical low down
        for (int y = 1; y <= 18; y++) {
            level.setBlock(PAD.offset(-3, y, 0), RocketryModule.SERVICE_TOWER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(PAD.offset(-4, y, 0), RocketryModule.SERVICE_TOWER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(PAD.offset(-3, y, -1), RocketryModule.SERVICE_TOWER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(PAD.offset(-4, y, -1), RocketryModule.SERVICE_TOWER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(PAD.offset(-2, 15, 0), RocketryModule.CREW_ARM.get().defaultBlockState().setValue(TowerAttachmentBlock.FACING, Direction.EAST), Block.UPDATE_CLIENTS);
        level.setBlock(PAD.offset(-2, 3, 0), RocketryModule.FUEL_UMBILICAL.get().defaultBlockState().setValue(TowerAttachmentBlock.FACING, Direction.EAST), Block.UPDATE_CLIENTS);
        // control console and assembler
        level.setBlock(CONSOLE, RocketryModule.LAUNCH_CONSOLE.get().defaultBlockState().setValue(TowerAttachmentBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
        level.setBlock(ASSEMBLER, RocketryModule.ROCKET_ASSEMBLER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(ASSEMBLER) instanceof RocketAssemblerBlockEntity be) {
            fillAssembler(be);
        }
        // parts gallery: every part in an item frame on a white wall
        for (int x = 9; x <= 17; x++) {
            for (int y = 1; y <= 3; y++) {
                level.setBlock(new BlockPos(x, F + y, 12), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < RocketryModule.PARTS.size(); i++) {
            ItemFrame frame = new ItemFrame(level, new BlockPos(9 + i % 8, F + 3 - i / 8, 11), Direction.NORTH);
            frame.setItem(new ItemStack(RocketryModule.PARTS.get(i).get()), false);
            level.addFreshEntity(frame);
        }
        // the Moon rocket on the pad, hatch towards the crew arm
        padRocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, Vec3.atBottomCenterOf(PAD.above()), -90F);
        padRocket.fillCompletely();
        level.addFreshEntity(padRocket);
        // landing test site (regolith) and a splashdown pool
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                level.setBlock(LANDING.offset(dx, 0, dz), MoonModule.LUNAR_REGOLITH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(POOL.offset(dx, 0, dz), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(POOL.offset(dx, -1, dz), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void fillAssembler(RocketAssemblerBlockEntity be) {
        var items = be.items();
        items.setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.KEROSENE_ENGINE.get()));
        for (int t = AssemblerSlots.TANK_A; t <= AssemblerSlots.TANK_C; t++) {
            items.setStackInSlot(AssemblerSlots.stageSlot(0, t), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        }
        items.setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.RADIAL), new ItemStack(RocketryModule.FINS.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.INTERSTAGE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.DECOUPLER.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(2, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(2, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_SMALL.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(2, AssemblerSlots.RADIAL), new ItemStack(RocketryModule.LANDING_LEGS.get()));
        items.setStackInSlot(AssemblerSlots.HEAT_SHIELD, new ItemStack(RocketryModule.HEAT_SHIELD.get()));
        items.setStackInSlot(AssemblerSlots.CAPSULE, new ItemStack(RocketryModule.CREW_CAPSULE.get()));
        items.setStackInSlot(AssemblerSlots.TOP, new ItemStack(RocketryModule.PARACHUTE.get()));
    }

    /** Rides a fuelled rocket in Earth orbit with the star map open on a chosen destination. */
    private static void mapShot(ServerPlayer p, net.minecraft.resources.ResourceLocation body, boolean orbit) {
        clearScene(p);
        ServerLevel level = p.server.getLevel(Location.EARTH_ORBIT.dimension());
        RocketEntity r = spawn(level, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F, FlightState.ORBIT, 0, 0, false);
        toDimension(p, Location.EARTH_ORBIT, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 35F);
        p.startRiding(r, true);
        RocketryShowcaseClient.preselect = body;
        RocketryShowcaseClient.preselectOrbit = orbit;
        RocketryShowcaseClient.openMapWhenRiding = true;
    }

    private static void clearScene(ServerPlayer player) {
        player.stopRiding();
        for (RocketEntity r : scene) {
            r.discard();
        }
        scene.clear();
        RocketryShowcaseClient.openMapWhenRiding = false;
    }

    private static RocketEntity spawn(ServerLevel level, Vec3 pos, float yaw, FlightState state, int timer, int stage, boolean burning) {
        RocketEntity r = RocketEntity.create(level, RocketryModule.MOON_ROCKET, pos, yaw);
        r.fillCompletely();
        level.addFreshEntity(r);
        r.stageScene(state, timer, stage, burning);
        scene.add(r);
        return r;
    }

    private static void toDimension(ServerPlayer player, Location where, double x, double y, double z, float yaw, float pitch) {
        toDimension(player, where.dimension(), x, y, z, yaw, pitch);
    }

    private static void toDimension(ServerPlayer player, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
                                    double x, double y, double z, float yaw, float pitch) {
        ServerLevel level = player.server.getLevel(dim);
        if (level != null) {
            player.teleportTo(level, x, y - player.getEyeHeight(), z, yaw, pitch);
        }
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<Shot> s = new ArrayList<>();
        s.add(new Shot("01_pad_morning", 1500, 15.5, G + 5, 15.5, 135F, -6F, true, fp, 160, p -> clearScene(p), null));
        s.add(new Shot("02_pad_noon", 6000, -12.5, G + 4, 16.5, 215F, -8F, true, fp, 80, null, null));
        s.add(new Shot("03_tower_arm", 6000, -6.5, G + 16, 5.5, 230F, 6F, true, fp, 60, null, null));
        s.add(new Shot("04_parts_gallery", 6000, 13.0, G + 2, 7.0, 0F, 2F, true, fp, 40, null, null));
        s.add(new Shot("05_assembler_gui", 6000, 7.5, G + 1.6, -3.5, 180F, 20F, false, fp, 40,
                p -> {
                    p.getAbilities().flying = false;
                    if (p.serverLevel().getBlockEntity(ASSEMBLER) instanceof RocketAssemblerBlockEntity be) {
                        p.openMenu(be, ASSEMBLER);
                    }
                }, null));
        s.add(new Shot("06_console_gui", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 40,
                p -> {
                    p.closeContainer();
                    if (p.serverLevel().getBlockEntity(CONSOLE) instanceof LaunchConsoleBlockEntity be) {
                        be.refresh();
                        p.openMenu(be, CONSOLE);
                    }
                }, null));
        s.add(new Shot("07_ignition", 6000, 14.5, G + 3, -12.5, 50F, -24F, true, fp, 180,
                p -> {
                    p.closeContainer();
                    padRocket.fillCompletely();
                    padRocket.tryStartCountdown(null);
                }, null));
        s.add(new Shot("08_liftoff", 6000, 14.5, G + 3, -12.5, 50F, -30F, true, fp, 60, null, null));
        s.add(new Shot("09_ascent", 6000, 16.5, G + 2, -16.5, 45F, -62F, true, fp, 90, null, null));
        s.add(new Shot("10_earth_orbit", 6000, 0, 0, 0, 0F, 0F, true, fp, 120,
                p -> {
                    clearScene(p);
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    spawn(orbit, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F, FlightState.ORBIT, 0, 1, false);
                    toDimension(p, Location.EARTH_ORBIT, 7.5, RocketEntity.ORBIT_Y + 9, 9.5, 145F, 22F);
                }, null));
        s.add(new Shot("11_earth_orbit_wide", 6000, 0, 0, 0, 0F, 0F, true, fp, 120,
                p -> toDimension(p, Location.EARTH_ORBIT, 12.5, RocketEntity.ORBIT_Y + 2, 20.5, 150F, 38F), null));
        s.add(new Shot("12_orbit_hud", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    toDimension(p, Location.EARTH_ORBIT, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 35F);
                    p.startRiding(scene.get(0), true);
                }, null));
        s.add(new Shot("13_star_map", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    toDimension(p, Location.EARTH_ORBIT, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 35F);
                    p.startRiding(scene.get(0), true);
                    RocketryShowcaseClient.openMapWhenRiding = true;
                }, null));
        s.add(new Shot("14_moon_orbit", 6000, 0, 0, 0, 0F, 0F, true, fp, 120,
                p -> {
                    clearScene(p);
                    ServerLevel orbit = p.server.getLevel(Location.MOON_ORBIT.dimension());
                    spawn(orbit, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F, FlightState.ORBIT, 0, 2, false);
                    toDimension(p, Location.MOON_ORBIT, -6.5, RocketEntity.ORBIT_Y + 4, 8.5, 215F, 30F);
                }, null));
        s.add(new Shot("15_moon_descent", 6000, 0, 0, 0, 0F, 0F, true, fp, 30,
                p -> {
                    clearScene(p);
                    ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
                    if (moon == null) {
                        return;
                    }
                    int gx = 200, gz = 200;
                    moon.getChunk(gx >> 4, gz >> 4);
                    int ground = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
                    moonGround = ground;
                    spawn(moon, new Vec3(gx + 0.5, ground + 9, gz + 0.5), 0F, FlightState.DESCENT, 60, 2, true);
                    toDimension(p, Location.MOON_SURFACE, gx + 10.5, ground + 4, gz + 9.5, 133F, 6F);
                }, null));
        s.add(new Shot("16_moon_landed", 6000, 0, 0, 0, 0F, 0F, true, fp, 160,
                p -> toDimension(p, Location.MOON_SURFACE, 207.5, moonGround + 2.5, 206.5, 135F, 2F), null));
        s.add(new Shot("17_reentry", 6000, 0, 0, 0, 0F, 0F, false, fp, 60,
                p -> {
                    clearScene(p);
                    RocketEntity capsule = spawn(p.serverLevel(), new Vec3(-20.5, 700, 10.5), 0F, FlightState.REENTRY, 40, 3, false);
                    p.teleportTo(p.serverLevel(), -20.5, 701, 10.5, 0F, 60F);
                    p.startRiding(capsule, true);
                }, null));
        s.add(new Shot("18_parachutes", 7000, POOL.getX() + 14.5, G + 8, POOL.getZ() + 12.5, 135F, -32F, true, fp, 60,
                p -> {
                    clearScene(p);
                    spawn(p.serverLevel(), Vec3.atBottomCenterOf(POOL.above(34)), 0F, FlightState.PARACHUTE, 80, 3, false);
                }, null));
        s.add(new Shot("19_splashdown", 7000, POOL.getX() + 7.5, G + 3, POOL.getZ() + 6.5, 135F, 12F, true, fp, 400, null, null));
        s.add(new Shot("20_mars_orbit", 6000, 0, 0, 0, 0F, 0F, true, fp, 120,
                p -> {
                    clearScene(p);
                    ServerLevel orbit = p.server.getLevel(Location.Orbits.SHARED);
                    if (orbit == null) {
                        return;
                    }
                    double mx = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.sharedRegionX(Spacebreaker.id("mars"));
                    RocketEntity r = spawn(orbit, new Vec3(mx + 0.5, RocketEntity.ORBIT_Y, 0.5), 0F, FlightState.ORBIT, 0, 2, false);
                    r.setOrbitBody(Spacebreaker.id("mars"));
                    toDimension(p, Location.Orbits.SHARED, mx - 6.5, RocketEntity.ORBIT_Y + 4, 8.5, 215F, 30F);
                }, null));
        s.add(new Shot("21_star_map_mars", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    toDimension(p, Location.Orbits.SHARED, com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.sharedRegionX(Spacebreaker.id("mars")) + 0.5,
                            RocketEntity.ORBIT_Y + 1, 0.5, 30F, 35F);
                    p.startRiding(scene.get(0), true);
                    RocketryShowcaseClient.openMapWhenRiding = true;
                }, null));
        s.add(new Shot("22_mars_parachutes", 6000, 0, 0, 0, 0F, 0F, true, fp, 50,
                p -> {
                    clearScene(p);
                    ServerLevel mars = p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Spacebreaker.id("mars")));
                    if (mars == null) {
                        return;
                    }
                    int gx = 100, gz = 100;
                    mars.getChunk(gx >> 4, gz >> 4);
                    int ground = mars.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
                    spawn(mars, new Vec3(gx + 0.5, ground + 40, gz + 0.5), 0F, FlightState.PARACHUTE, 80, 2, false);
                    p.teleportTo(mars, gx + 14.5, ground + 12, gz + 12.5, 135F, -20F);
                }, null));
        // ---- boarding camera, transit view and the new star map (Turkish UI)
        s.add(new Shot("23_tr_setup", 6000, 0.5, G + 4, 0.5, 0F, 0F, true, fp, 200,
                p -> clearScene(p), RocketryShowcaseClient::turkish));
        s.add(new Shot("24_rocket_camera", 6000, 0.5, G + 4, 0.5, 120F, 22F, false, fp, 120,
                p -> {
                    clearScene(p);
                    if (padRocket != null) {
                        padRocket.discard();
                    }
                    RocketEntity r = spawn(p.serverLevel(), Vec3.atBottomCenterOf(PAD.above()), -90F, FlightState.IDLE, 0, 0, false);
                    p.teleportTo(p.serverLevel(), r.getX(), r.getY() + 8, r.getZ(), 120F, 22F);
                    p.startRiding(r, true);
                }, null));
        s.add(new Shot("25_cockpit", 6000, 0.5, G + 4, 0.5, 200F, 12F, false, fp, 80,
                p -> {
                    if (!scene.isEmpty()) {
                        p.teleportTo(p.serverLevel(), scene.get(0).getX(), scene.get(0).getY() + 8, scene.get(0).getZ(), 200F, 12F);
                        p.startRiding(scene.get(0), true);
                    }
                }, () -> RocketryShowcaseClient.cockpit = true));
        var mars = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false);
        var marsOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), true);
        s.add(new Shot("26_transit_ascent", 6000, 0.5, G + 4, 0.5, 0F, 0F, false, fp, 70,
                p -> clearScene(p), () -> {
                    RocketryShowcaseClient.cockpit = false;
                    com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(
                            com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.ASCENT, Location.EARTH_SURFACE, Location.EARTH_ORBIT);
                }));
        s.add(new Shot("27_transit_transfer", 6000, 0.5, G + 4, 0.5, 0F, 0F, false, fp, 110,
                null, () -> com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(
                        com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.TRANSFER, Location.EARTH_ORBIT,
                        marsOrbit.orElse(Location.MOON_ORBIT))));
        s.add(new Shot("28_transit_landing", 6000, 0.5, G + 4, 0.5, 0F, 0F, false, fp, 92,
                null, () -> com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(
                        com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit.Kind.LANDING, marsOrbit.orElse(Location.MOON_ORBIT),
                        mars.orElse(Location.MOON_SURFACE))));
        s.add(new Shot("29_star_map_system", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> mapShot(p, Spacebreaker.id("mars"), true),
                () -> com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(null, null, null)));
        s.add(new Shot("30_star_map_moon", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> mapShot(p, Location.MOON, false), null));
        s.add(new Shot("32_satellites", 6000, 0, 0, 0, 0F, 0F, true, fp, 120,
                p -> {
                    clearScene(p);
                    RocketryShowcaseClient.star = null;
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    orbit.addFreshEntity(com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity.create(orbit,
                            Spacebreaker.id("comm_satellite"), new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5)));
                    orbit.addFreshEntity(com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity.create(orbit,
                            Spacebreaker.id("observation_satellite"), new Vec3(5.5, RocketEntity.ORBIT_Y - 2, -3.5)));
                    toDimension(p, Location.EARTH_ORBIT, 2.5, RocketEntity.ORBIT_Y + 2.2, 5.5, 160F, 22F);
                }, () -> RocketryShowcaseClient.cockpit = false));
        s.add(new Shot("33_uncrewed_console", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 60,
                p -> {
                    clearScene(p);
                    if (padRocket != null) {
                        padRocket.discard();
                    }
                    RocketEntity probe = RocketEntity.create(p.serverLevel(), new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(List.of(
                            Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"),
                            Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_medium"), Spacebreaker.id("space_probe"),
                            Spacebreaker.id("nose_cone"))), Vec3.atBottomCenterOf(PAD.above()), -90F);
                    probe.fillCompletely();
                    probe.setOwner(p.getUUID());
                    com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).ifPresent(probe::setMissionTarget);
                    p.serverLevel().addFreshEntity(probe);
                    scene.add(probe);
                    p.getServer().execute(() -> {
                        if (p.serverLevel().getBlockEntity(CONSOLE) instanceof LaunchConsoleBlockEntity be) {
                            be.refresh();
                            p.openMenu(be, CONSOLE);
                        }
                    });
                }, null));
        s.add(new Shot("34_station", 6000, 0, 0, 0, 0F, 0F, true, fp, 140,
                p -> {
                    clearScene(p);
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    BlockPos c = BlockPos.containing(40, RocketEntity.ORBIT_Y, 0);
                    // a small pressurised module: hull box with a row of windows and a docking port on the side
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dy = 0; dy <= 4; dy++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                boolean shell = Math.abs(dx) == 3 || dy == 0 || dy == 4 || Math.abs(dz) == 2;
                                if (!shell) {
                                    continue;
                                }
                                var block = dy == 2 && dz == 2 && Math.abs(dx) < 3 ? RocketryModule.STATION_WINDOW.get() : RocketryModule.STATION_HULL.get();
                                orbit.setBlock(c.offset(dx, dy, dz), block.defaultBlockState(), 3);
                            }
                        }
                    }
                    orbit.setBlock(c.offset(3, 2, 0), RocketryModule.DOCKING_PORT.get().defaultBlockState()
                            .setValue(com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock.FACING, net.minecraft.core.Direction.EAST), 3);
                    RocketEntity r = spawn(orbit, Vec3.atBottomCenterOf(c.offset(7, -4, 0)), 0F, FlightState.ORBIT, 0, 2, false);
                    toDimension(p, Location.EARTH_ORBIT, 47.5, RocketEntity.ORBIT_Y + 3.5, 13.5, 160F, 8F);
                }, null));
        s.add(new Shot("31_nearby_stars", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    RocketryShowcaseClient.star = "Alpha Centauri";
                    mapShot(p, Location.MOON, false);
                }, null));
        return s;
    }

    static {
        Spacebreaker.LOGGER.debug("rocketry showcase scenario loaded");
    }
}
