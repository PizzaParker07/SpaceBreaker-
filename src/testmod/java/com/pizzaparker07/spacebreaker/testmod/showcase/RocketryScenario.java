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
        items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.TANK_B), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.DECOUPLER.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(2, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
        items.setStackInSlot(AssemblerSlots.stageSlot(2, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_MEDIUM.get()));
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

    /**
     * A habitat module with a solar panel on its roof, on the night or the day side of the orbit. After the panel
     * has run a while its output is written to the log and shown over it, so the shadow can be checked without the
     * picture.
     */
    private static void orbitPanelShot(ServerPlayer p, boolean night) {
        clearScene(p);
        ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
        BlockPos c = BlockPos.containing(night ? 200 : 260, RocketEntity.ORBIT_Y, 0);
        net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
        com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(orbit, c, east);
        orbit.setBlock(c.above(3), net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(), 3);
        BlockPos panel = c.offset(2, com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.HEIGHT, 2);
        orbit.setBlock(panel, com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        // move the clock to the far side of the orbit (Earth's shadow) or to orbital noon
        long now = p.server.overworld().getGameTime();
        long orbitTicks = (long) com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.ORBIT_TICKS;
        long time = (now / orbitTicks + 1) * orbitTicks + (night ? orbitTicks / 2 : 0);
        if (p.server.overworld().getLevelData() instanceof net.minecraft.world.level.storage.PrimaryLevelData data) {
            data.setGameTime(time);
        }
        toDimension(p, Location.EARTH_ORBIT, c.getX() + 6.5, RocketEntity.ORBIT_Y + 8.5, 7.5, 150F, 30F);
        later(p.server, 60, () -> {
            double sun = com.pizzaparker07.spacebreaker.core.env.Environment.solarFactor(orbit, panel);
            double out = orbit.getBlockEntity(panel) instanceof com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity be ? be.getExactOutput() : -1;
            Spacebreaker.LOGGER.info("[showcase] orbit {}: sunlit {} solarFactor {} panel {} FE/t", night ? "night" : "day",
                    String.format(java.util.Locale.ROOT, "%.3f", com.pizzaparker07.spacebreaker.content.rocketry.nav.OrbitDaylight.sunlit(orbit, panel)),
                    String.format(java.util.Locale.ROOT, "%.3f", sun), String.format(java.util.Locale.ROOT, "%.2f", out));
            var label = net.minecraft.world.entity.EntityType.TEXT_DISPLAY.create(orbit);
            if (label != null) {
                label.moveTo(panel.getX() + 0.5, panel.getY() + 1.4, panel.getZ() + 0.5);
                var tag = label.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                tag.putString("text", net.minecraft.network.chat.Component.Serializer.toJson(
                        net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT, "%.1f FE/t", out)), orbit.registryAccess()));
                tag.putString("billboard", "center");
                var bright = new net.minecraft.nbt.CompoundTag();
                bright.putInt("sky", 15);
                bright.putInt("block", 15);
                tag.put("brightness", bright);
                label.load(tag);
                orbit.addFreshEntity(label);
            }
        });
    }

    private static final java.util.List<long[]> LATER_AT = new java.util.ArrayList<>();
    private static final java.util.List<Runnable> LATER = new java.util.ArrayList<>();
    private static boolean laterHooked;

    /** Runs a server task this many ticks from now. */
    static void later(net.minecraft.server.MinecraftServer server, int ticks, Runnable task) {
        if (!laterHooked) {
            laterHooked = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
                for (int i = LATER.size() - 1; i >= 0; i--) {
                    if (e.getServer().getTickCount() >= LATER_AT.get(i)[0]) {
                        Runnable r = LATER.remove(i);
                        LATER_AT.remove(i);
                        r.run();
                    }
                }
            });
        }
        LATER_AT.add(new long[]{server.getTickCount() + ticks});
        LATER.add(task);
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
        s.add(new Shot("07_ignition", 6000, 14.5, G + 3, -12.5, 50F, -24F, true, fp, 190,
                p -> {
                    p.closeContainer();
                    padRocket.fillCompletely();
                    padRocket.tryStartCountdown(null);
                }, null));
        s.add(new Shot("08_liftoff", 6000, 14.5, G + 3, -12.5, 50F, -30F, true, fp, 60, null, null));
        s.add(new Shot("09_ascent", 6000, 16.5, G + 2, -16.5, 45F, -66F, true, fp, 35, null, null));
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
        // the climb out of the sky is seen in the world itself: high up the sky turns black (no full-screen effect)
        s.add(new Shot("26_transit_ascent", 6000, 0.5, 650, 0.5, 30F, 12F, true, fp, 60,
                p -> {
                    clearScene(p);
                    RocketryShowcaseClient.cockpit = false;
                }, () -> com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(null, null, null, 0F)));
        // a transfer keeps the orbit in view, with one quiet line on top; the screen only fades in its last second
        s.add(new Shot("27_transit_transfer", 6000, 0, 0, 0, 0F, 0F, false, fp, 60,
                p -> {
                    clearScene(p);
                    p.getInventory().clearContent();
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    RocketEntity r = spawn(orbit, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F, FlightState.TRANSFER, 0, 1, false);
                    r.stageDestination(marsOrbit.orElse(Location.MOON_ORBIT));
                    toDimension(p, Location.EARTH_ORBIT, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 30F, 35F);
                    p.startRiding(r, true);
                    later(p.server, 50, () -> net.minecraft.client.Minecraft.getInstance().execute(
                            () -> net.minecraft.client.Minecraft.getInstance().getToasts().clear()));
                }, null));
        s.add(new Shot("29_star_map_system", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> mapShot(p, Spacebreaker.id("mars"), true),
                () -> com.pizzaparker07.spacebreaker.client.rocketry.TransitView.showcase(null, null, null, 0F)));
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
        s.add(new Shot("42_cargo_route_console", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 80,
                p -> {
                    clearScene(p);
                    if (padRocket != null) {
                        padRocket.discard();
                    }
                    RocketEntity cargo = RocketEntity.create(p.serverLevel(), new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(List.of(
                            Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"),
                            Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_medium"), Spacebreaker.id("cargo_module"),
                            Spacebreaker.id("nose_cone"))), Vec3.atBottomCenterOf(PAD.above()), -90F);
                    cargo.fillCompletely();
                    cargo.setOwner(p.getUUID());
                    var moonOrbit = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Location.MOON, true).orElseThrow();
                    // a route between a Moon base and a station (shown here on the pad), loading for the next leg
                    cargo.startRoute(Location.MOON_SURFACE, moonOrbit);
                    cargo.setMissionTarget(moonOrbit);
                    cargo.cargo().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
                    cargo.cargo().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 32));
                    cargo.cargo().setItem(2, new net.minecraft.world.item.ItemStack(com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_TANK.get()));
                    cargo.scheduleRoute(6000, PAD);
                    p.serverLevel().addFreshEntity(cargo);
                    scene.add(cargo);
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
        s.add(new Shot("35_habitat_docking", 6000, 0, 0, 0, 0F, 0F, true, fp, 160,
                p -> {
                    clearScene(p);
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    BlockPos c = BlockPos.containing(120, RocketEntity.ORBIT_Y, 0);
                    net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
                    com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(orbit, c, east);
                    RocketEntity r = spawn(orbit, Vec3.atBottomCenterOf(c.offset(30, 0, 0)), 0F, FlightState.ORBIT, 0, 2, false);
                    r.approachPort(com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.port(c, east), east, 30);
                    toDimension(p, Location.EARTH_ORBIT, 136.5, RocketEntity.ORBIT_Y + 6.5, 17.5, 140F, 12F);
                }, null));
        s.add(new Shot("36_map_station", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
                    BlockPos c = BlockPos.containing(-120, RocketEntity.ORBIT_Y, 40);
                    net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
                    com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.unfold(orbit, c, east);
                    com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.get(orbit).setOwner(
                            com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem.port(c, east), "PizzaParker07");
                    mapShot(p, Location.EARTH_ORBIT.body(), true);
                }, null));
        s.add(new Shot("37_orbit_night", 6000, 0, 0, 0, 0F, 0F, true, fp, 160, p -> orbitPanelShot(p, true), null));
        s.add(new Shot("38_orbit_day", 6000, 0, 0, 0, 0F, 0F, true, fp, 160, p -> orbitPanelShot(p, false), null));
        s.add(new Shot("39_lunar_shuttle", 6000, 0, 0, 0, 0F, 0F, true, fp, 140,
                p -> {
                    clearScene(p);
                    ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
                    int gx = 40, gz = 40;
                    moon.getChunk(gx >> 4, gz >> 4);
                    int gy = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
                    // a small base pad: hull floor with the landing beacon in the middle
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            moon.setBlock(new BlockPos(gx + dx, gy - 1, gz + dz), RocketryModule.STATION_HULL.get().defaultBlockState(), 3);
                            for (int up = 0; up < 3; up++) {
                                moon.setBlock(new BlockPos(gx + dx, gy + up, gz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                    moon.setBlock(new BlockPos(gx, gy - 1, gz), RocketryModule.LANDING_BEACON.get().defaultBlockState(), 3);
                    RocketEntity r = RocketEntity.create(moon, RocketryModule.LUNAR_SHUTTLE, new Vec3(gx + 0.5, gy, gz + 0.5), 30F);
                    r.fillCompletely();
                    moon.addFreshEntity(r);
                    scene.add(r);
                    toDimension(p, Location.MOON_SURFACE, gx + 7.5, gy + 3.5, gz + 7.5, 135F, 12F);
                }, null));
        s.add(new Shot("40_map_base", 6000, 0, 0, 0, 0F, 0F, false, fp, 120,
                p -> {
                    ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
                    moon.getChunk(40 >> 4, 40 >> 4);
                    BlockPos b = new BlockPos(40, moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 40, 40) - 1, 40);
                    moon.setBlock(b, RocketryModule.LANDING_BEACON.get().defaultBlockState(), 3);
                    com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(moon).setOwner(b, "PizzaParker07");
                    mapShot(p, Location.MOON, false);
                }, null));
        s.add(new Shot("41_mars_lander", 6000, 0, 0, 0, 0F, 0F, true, fp, 140,
                p -> {
                    clearScene(p);
                    var marsSurface = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("mars"), false).orElseThrow();
                    ServerLevel level = p.server.getLevel(marsSurface.dimension());
                    if (level == null) {
                        return;
                    }
                    int gx = 60, gz = 60;
                    level.getChunk(gx >> 4, gz >> 4);
                    int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            level.setBlock(new BlockPos(gx + dx, gy - 1, gz + dz), RocketryModule.STATION_HULL.get().defaultBlockState(), 3);
                            for (int up = 0; up < 4; up++) {
                                level.setBlock(new BlockPos(gx + dx, gy + up, gz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                    level.setBlock(new BlockPos(gx, gy - 1, gz), RocketryModule.LANDING_BEACON.get().defaultBlockState(), 3);
                    RocketEntity r = RocketEntity.create(level, RocketryModule.MARS_LANDER, new Vec3(gx + 0.5, gy, gz + 0.5), 30F);
                    r.fillCompletely();
                    level.addFreshEntity(r);
                    scene.add(r);
                    toDimension(p, marsSurface, gx + 8.5, gy + 4.5, gz + 8.5, 135F, 14F);
                }, null));
        s.add(new Shot("43_map_asteroids", 6000, 0, 0, 0, 0F, 0F, false, fp, 120, p -> mapShot(p, Spacebreaker.id("bennu"), false), null));
        s.add(new Shot("44_map_system_asteroids", 6000, 0, 0, 0, 0F, 0F, false, fp, 120, p -> mapShot(p, Spacebreaker.id("mars"), true), null));
        s.add(new Shot("45_map_comets", 6000, 0, 0, 0, 0F, 0F, false, fp, 120, p -> mapShot(p, Spacebreaker.id("churyumov_gerasimenko"), false), null));
        s.add(new Shot("46_map_comet_landing", 6000, 0, 0, 0, 0F, 0F, false, fp, 120, p -> mapShot(p, Spacebreaker.id("churyumov_gerasimenko"), false), null));
        s.add(new Shot("47_comet_landing", 6000, 0, 0, 0, 0F, 0F, true, fp, 900, p -> {
            clearScene(p);
            var cometSurface = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id("churyumov_gerasimenko"), false).orElse(null);
            ServerLevel level = cometSurface == null ? null : p.server.getLevel(cometSurface.dimension());
            if (level == null) {
                Spacebreaker.LOGGER.info("[showcase] comet: no 67P world");
                return;
            }
            // a shuttle coming down from 67P orbit, as the arrival does; then a launch back to orbit
            int gx = 8, gz = 8;
            level.getChunk(gx >> 4, gz >> 4);
            int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
            RocketEntity r = RocketEntity.create(level, RocketryModule.LUNAR_SHUTTLE, new Vec3(gx + 0.5, ground + 60, gz + 0.5), 0F);
            r.fillCompletely();
            level.addFreshEntity(r);
            scene.add(r);
            r.stageScene(FlightState.DESCENT, 0, 0, true);
            toDimension(p, cometSurface, gx + 12.5, ground + 6, gz + 12.5, 135F, 10F);
            Spacebreaker.LOGGER.info("[showcase] comet: gravity {} m/s2, ground y {}", String.format(java.util.Locale.ROOT, "%.5f",
                    com.pizzaparker07.spacebreaker.core.env.Environment.gravity(level)), ground);
            later(p.server, 300, () -> {
                Spacebreaker.LOGGER.info("[showcase] comet: after descent state {} y {} (ground {}) vy {}", r.state(),
                        String.format(java.util.Locale.ROOT, "%.2f", r.getY()), ground, String.format(java.util.Locale.ROOT, "%.3f", r.getDeltaMovement().y));
                boolean go = r.tryStartCountdown(null);
                Spacebreaker.LOGGER.info("[showcase] comet: countdown started {} problem {}", go, r.launchProblem());
            });
            later(p.server, 560, () -> Spacebreaker.LOGGER.info("[showcase] comet: during ascent state {} y {} ascent {}", r.state(),
                    String.format(java.util.Locale.ROOT, "%.1f", r.getY()), String.format(java.util.Locale.ROOT, "%.2f", r.ascentProgress())));
            later(p.server, 880, () -> {
                RocketEntity now = scene.stream().filter(e -> !e.isRemoved()).findFirst().orElse(r);
                Spacebreaker.LOGGER.info("[showcase] comet: end state {} dim {} removed {}", now.state(), now.level().dimension().location(), r.isRemoved());
            });
        }, null));
        // warning lights at night, away from the console: lit (beams turning, light on the deck) and dark
        for (boolean on : new boolean[]{true, false}) {
            s.add(new Shot(on ? "48_warning_lights" : "48b_warning_lights_off", 18000, -1.5, G + 1.5, 12.5, 153F, 15F, true, fp, 60, p -> {
                clearScene(p);
                ServerLevel level = p.serverLevel();
                for (BlockPos a : new BlockPos[]{PAD.offset(-3, 1, 8), PAD.offset(-5, 1, 6)}) {
                    level.setBlock(a.below(), on ? net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()
                            : RocketryModule.LAUNCH_PAD.get().defaultBlockState(), 3);
                    level.setBlock(a, RocketryModule.WARNING_LIGHT.get().defaultBlockState(), 3);
                }
            }, null));
        }
        s.add(new Shot("49_base_blocks", 6000, 0, 0, 0, 0F, 0F, true, fp, 80, p -> {
            clearScene(p);
            ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
            int gx = 120, gz = 40;
            moon.getChunk(gx >> 4, gz >> 4);
            int gy = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
            var panel = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL;
            var deck = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK;
            // a short open corridor: titanium deck with a grate strip, a wall of panels with a porthole and pressure glass
            for (int dx = -1; dx <= 7; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    var floor = dz == 0 ? com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE.get() : deck.base().get();
                    moon.setBlock(new BlockPos(gx + dx, gy - 1, gz + dz), floor.defaultBlockState(), 3);
                    for (int up = 0; up < 4; up++) {
                        moon.setBlock(new BlockPos(gx + dx, gy + up, gz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                for (int up = 0; up < 3; up++) {
                    net.minecraft.world.level.block.Block w = panel.base().get();
                    if (up == 1 && dx == 2) w = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PORTHOLE.get();
                    if (up == 1 && (dx == 4 || dx == 5)) w = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PRESSURE_GLASS.get();
                    moon.setBlock(new BlockPos(gx + dx, gy + up, gz - 2), w.defaultBlockState(), 3);
                }
                moon.setBlock(new BlockPos(gx + dx, gy + 3, gz - 2), panel.slab().get().defaultBlockState(), 3);
            }
            // two sliding doors in the wall: one shut, one open
            var door = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get().defaultBlockState()
                    .setValue(com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.FACING, net.minecraft.core.Direction.SOUTH);
            com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(moon, new BlockPos(gx, gy, gz - 2), door);
            com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(moon, new BlockPos(gx + 6, gy, gz - 2),
                    door.setValue(com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN, true));
            moon.setBlock(new BlockPos(gx + 6, gy, gz + 2), panel.wall().get().defaultBlockState(), 3);
            moon.setBlock(new BlockPos(gx + 7, gy, gz + 2), deck.stairs().get().defaultBlockState(), 3);
            moon.setBlock(new BlockPos(gx + 1, gy, gz - 1), RocketryModule.WARNING_LIGHT.get().defaultBlockState(), 3);
            toDimension(p, Location.MOON_SURFACE, gx + 7.5, gy + 2.6, gz + 2.5, 135F, 28F);
        }, null));
        s.add(new Shot("50_base_fittings", 18000, 0, 0, 0, 0F, 0F, true, fp, 300, p -> {
            clearScene(p);
            ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
            int gx = 160, gz = 40;
            moon.getChunk(gx >> 4, gz >> 4);
            int gy = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
            var B = new Object() {
                net.minecraft.world.level.block.state.BlockState of(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
            };
            var panel = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get();
            var deck = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get();
            var stripe = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get();
            var led = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get();
            // a roofed corridor at night: LED panels in the ceiling, duct covers along one wall, hazard stripes at the edge
            for (int dx = -1; dx <= 9; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int up = -1; up <= 4; up++) {
                        BlockPos q = new BlockPos(gx + dx, gy + up, gz + dz);
                        net.minecraft.world.level.block.Block b = net.minecraft.world.level.block.Blocks.AIR;
                        if (up == -1) b = Math.abs(dz) == 2 ? stripe : Math.abs(dz) == 3 ? panel : deck;
                        else if (up == 4) b = dz == 0 && dx % 3 == 0 ? led : panel;
                        else if (dz == -3) b = up == 1 ? (dx % 2 == 0 ? com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT.get()
                                : com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT.get()) : panel;
                        else if (dz == 3 && dx < 6) b = panel;
                        if ((dx == -1 || dx == 9) && up >= 0 && up < 4) b = panel;
                        moon.setBlock(q, B.of(b), 3);
                    }
                }
            }
            // a raised grate platform with a railing, a ladder up to it and a floodlight on the deck
            for (int dx = 6; dx <= 8; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    moon.setBlock(new BlockPos(gx + dx, gy + 1, gz + dz), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM.get()
                            .defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP), 3);
                }
                moon.setBlock(new BlockPos(gx + dx, gy + 2, gz), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, dx < 8).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, dx > 6), 3);
            }
            // the ladder climbs a panel pillar under the platform's edge
            moon.setBlock(new BlockPos(gx + 6, gy, gz + 2), panel.defaultBlockState(), 3);
            moon.setBlock(new BlockPos(gx + 6, gy + 1, gz + 2), panel.defaultBlockState(), 3);
            for (int up = 0; up < 2; up++) {
                moon.setBlock(new BlockPos(gx + 5, gy + up, gz + 2), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.WEST), 3);
            }
            moon.setBlock(new BlockPos(gx + 3, gy, gz + 2), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT.get().defaultBlockState(), 3);
            moon.setBlock(new BlockPos(gx + 1, gy, gz - 2), RocketryModule.WARNING_LIGHT.get().defaultBlockState(), 3);
            toDimension(p, Location.MOON_SURFACE, gx + 0.5, gy + 1.8, gz + 1.5, -115F, 12F);
        }, null));
        s.add(new Shot("51_airlock", 6000, 0, 0, 0, 0F, 0F, true, fp, 80, p -> {
            clearScene(p);
            ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
            int gx = 200, gz = 40;
            moon.getChunk(gx >> 4, gz >> 4);
            int gy = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gx, gz);
            var panel = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL.base().get();
            var stripe = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE.base().get();
            var deck = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK.base().get();
            // an airlock chamber three blocks deep between two sliding doors; the outer one cycles open, the inner stays shut
            for (int dz = -1; dz <= 5; dz++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int up = -1; up <= 3; up++) {
                        boolean shell = Math.abs(dx) == 2 || up == -1 || up == 3;
                        net.minecraft.world.level.block.Block b = !shell ? net.minecraft.world.level.block.Blocks.AIR
                                : up == -1 ? (dz >= 1 && dz <= 3 && Math.abs(dx) < 2 ? stripe : deck) : panel;
                        moon.setBlock(new BlockPos(gx + dx, gy + up, gz + dz), b.defaultBlockState(), 3);
                    }
                }
            }
            var door = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR.get();
            BlockPos outer = new BlockPos(gx, gy, gz), inner = outer.south(4);
            for (BlockPos d : new BlockPos[]{outer, inner}) {
                for (int dx = -1; dx <= 1; dx += 2) {
                    for (int up = 0; up < 3; up++) {
                        moon.setBlock(d.offset(dx, up, 0), panel.defaultBlockState(), 3);
                    }
                }
                moon.setBlock(d.above(2), panel.defaultBlockState(), 3);
                com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.place(moon, d, door.defaultBlockState());
            }
            // the outer door sits open (the inner one shut); the outside beyond it is left as Moon
            moon.setBlock(outer, moon.getBlockState(outer).setValue(com.pizzaparker07.spacebreaker.content.rocketry.base.SlidingDoorBlock.OPEN, true), 3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int up = 0; up < 3; up++) {
                    moon.setBlock(new BlockPos(gx + dx, gy + up, gz - 1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
            moon.setBlock(new BlockPos(gx, gy + 2, gz + 2), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get().defaultBlockState(), 3);
            moon.setBlock(new BlockPos(gx - 1, gy + 2, gz + 1), RocketryModule.WARNING_LIGHT.get().defaultBlockState()
                    .setValue(com.pizzaparker07.spacebreaker.content.rocketry.pad.WarningLightBlock.FACING, net.minecraft.core.Direction.DOWN), 3);
            moon.setBlock(new BlockPos(gx - 1, gy + 3, gz + 1), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            toDimension(p, Location.MOON_SURFACE, gx + 0.5, gy + 0.9, gz - 1.2, 0F, 4F);
        }, null));
        s.add(new Shot("52_nuclear_thermal", 6000, 0, 0, 0, 0F, 0F, true, fp, 120, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            // a nuclear thermal transfer stage under a crew capsule, burning out of Earth orbit
            var design = new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(java.util.List.of(
                    Spacebreaker.id("nuclear_thermal_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("fuel_tank_large"),
                    Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));
            RocketEntity r = RocketEntity.create(orbit, design, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F);
            r.fillCompletely();
            orbit.addFreshEntity(r);
            r.stageScene(FlightState.ORBIT, 0, 0, true);
            scene.add(r);
            toDimension(p, Location.EARTH_ORBIT, 4.2, RocketEntity.ORBIT_Y + 1.5, 1.6, 107F, 14F);
        }, null));
        s.add(new Shot("53_assembler_nuclear", 6000, 7.5, G + 1.6, -3.5, 180F, 20F, false, fp, 40, p -> {
            clearScene(p);
            toDimension(p, Location.EARTH_SURFACE, 7.5, G + 1.6, -3.5, 180F, 20F);
            p.getAbilities().flying = false;
            if (p.serverLevel().getBlockEntity(ASSEMBLER) instanceof RocketAssemblerBlockEntity be) {
                // the second stage runs on a nuclear thermal engine: the assembler warns about it
                be.items().setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.NUCLEAR_THERMAL_ENGINE.get()));
                p.openMenu(be, ASSEMBLER);
            }
        }, null));
        s.add(new Shot("54_insulated_tanks", 6000, 0, 0, 0, 0F, 0F, true, fp, 120, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            // a hydrogen transfer stage with its tanks wrapped in gold multi-layer insulation, waiting in orbit
            var design = new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(java.util.List.of(
                    Spacebreaker.id("hydrolox_vacuum_engine"), Spacebreaker.id("fuel_tank_large_insulated"),
                    Spacebreaker.id("fuel_tank_medium_insulated"), Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"),
                    Spacebreaker.id("parachute")));
            RocketEntity r = RocketEntity.create(orbit, design, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F);
            r.fillCompletely();
            orbit.addFreshEntity(r);
            r.stageScene(FlightState.ORBIT, 0, 0, false);
            scene.add(r);
            toDimension(p, Location.EARTH_ORBIT, 5.6, RocketEntity.ORBIT_Y + 3.2, 2.4, 110F, 6F);
        }, null));
        // modular station, stage 1: a core unfolded in orbit with a rocket coming in to its roof port
        s.add(new Shot("55_station_core", 6000, 0, 0, 0, 0F, 0F, true, fp, 300, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(400, RocketEntity.ORBIT_Y - 6, 0);
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, net.minecraft.core.Direction.EAST, "PizzaParker07");
            RocketEntity r = spawn(orbit, Vec3.atBottomCenterOf(c.above(30)), 0F, FlightState.ORBIT, 0, 2, false);
            r.approachPort(com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.corePort(c), net.minecraft.core.Direction.UP, 14);
            toDimension(p, Location.EARTH_ORBIT, 416.5, RocketEntity.ORBIT_Y + 4.5, 15.5, 135F, 18F);
        }, null));
        // holding a docking module at a mouth: the outline shows where it will go (green: there is room)
        s.add(new Shot("56_station_preview", 6000, 0, 0, 0, 0F, 0F, false, fp, 300, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(500, RocketEntity.ORBIT_Y - 6, 0);
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, net.minecraft.core.Direction.EAST, "PizzaParker07");
            p.getInventory().clearContent();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(RocketryModule.DOCKING_MODULE.get()));
            BlockPos mouth = c.east(3).above(2);
            // well back from the mouth so the whole outline is in the picture (a long reach keeps the hatch targeted)
            var reach = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
            if (reach != null) {
                reach.setBaseValue(16);
            }
            toDimension(p, Location.EARTH_ORBIT, mouth.getX() + 7.5, mouth.getY() + 6.5, mouth.getZ() + 0.5, 90F, 39F);
        }, null));
        // the docking module attached to the core's front mouth, a rocket coming in to its port
        s.add(new Shot("57_station_docking_module", 6000, 0, 0, 0, 0F, 0F, true, fp, 300, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(600, RocketEntity.ORBIT_Y - 6, 0);
            net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, east, "PizzaParker07");
            BlockPos mouth = c.east(3).above(2);
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.DOCKING.attach(orbit, mouth, "PizzaParker07");
            RocketEntity r = spawn(orbit, Vec3.atBottomCenterOf(mouth.east(30)), 0F, FlightState.ORBIT, 0, 2, false);
            r.approachPort(mouth.east(7), east, 16);
            toDimension(p, Location.EARTH_ORBIT, 622.5, RocketEntity.ORBIT_Y + 6.5, 20.5, 145F, 14F);
        }, null));
        // inside the core: the console and the air distributor, the passage into the docking tunnel
        s.add(new Shot("58_station_inside", 6000, 0, 0, 0, 0F, 0F, true, fp, 200, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(700, RocketEntity.ORBIT_Y - 6, 0);
            net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, east, "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.DOCKING.attach(orbit, c.east(3).above(2), "PizzaParker07");
            toDimension(p, Location.EARTH_ORBIT, c.getX() - 1.5, c.getY() + 2.6, c.getZ() + 0.5, -90F, 8F);
        }, null));
        // modular station, stage 2: core + docking tunnel + living quarters + airlock + solar wings
        s.add(new Shot("59_station_stage2", 6000, 0, 0, 0, 0F, 0F, true, fp, 300, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(800, RocketEntity.ORBIT_Y - 6, 0);
            net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, east, "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.DOCKING.attach(orbit, c.east(3).above(2), "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.HABITATION.attach(orbit, c.west(3).above(2), "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.AIRLOCK.attach(orbit, c.north(3).above(2), "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.SOLAR_WINGS.attach(orbit, c.south(3).above(2), "PizzaParker07");
            RocketEntity r = spawn(orbit, Vec3.atBottomCenterOf(c.east(10).above(2)), 0F, FlightState.ORBIT, 0, 2, false);
            r.approachPort(c.east(10).above(2), east, 0.5);
            toDimension(p, Location.EARTH_ORBIT, c.getX() + 14.5, c.getY() + 14.5, c.getZ() + 22.5, 150F, 30F);
        }, null));
        // inside the living quarters
        s.add(new Shot("60_station_quarters", 6000, 0, 0, 0, 0F, 0F, true, fp, 320, p -> {
            clearScene(p);
            ServerLevel orbit = p.server.getLevel(Location.EARTH_ORBIT.dimension());
            BlockPos c = BlockPos.containing(900, RocketEntity.ORBIT_Y - 6, 0);
            net.minecraft.core.Direction east = net.minecraft.core.Direction.EAST;
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(orbit, c, east, "PizzaParker07");
            com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.HABITATION.attach(orbit, c.east(3).above(2), "PizzaParker07");
            toDimension(p, Location.EARTH_ORBIT, c.getX() + 2.5, c.getY() + 2.6, c.getZ() + 0.5, -90F, 10F);
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
