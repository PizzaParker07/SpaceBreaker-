package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Automated client scenario (dev only, never shipped): creates a fresh flat world, builds a
 * showcase of the mod's content, walks a scripted camera through it and saves screenshots to
 * run-showcase/screenshots. Enabled with -Dspacebreaker.scenario=style_sample.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class ShowcaseDriver {
    private static final String SCENARIO = System.getProperty("spacebreaker.scenario", "");
    private static final String PREFIX = System.getProperty("spacebreaker.shotPrefix", "vanilla");
    /** Optional regex: only shots whose name matches are taken (-Pshots=...). */
    private static final String FILTER = System.getProperty("spacebreaker.shots", "");
    /** -PguiScale=N (0 = auto): the GUI scale for every shot, for HUD and screen layout checks. */
    private static final int GUI_SCALE = Integer.getInteger("spacebreaker.guiScale", -1);
    /** -Poffhand=namespace:item: an item in the off hand for every shot (its slot sits left of the hotbar). */
    private static final String OFFHAND = System.getProperty("spacebreaker.offhand", "");
    /**
     * -Pretreat=N: after each shot, a second one from N blocks further back along the view (and N/3 higher, looking at
     * the same spot) with a 6-chunk view distance, saved as {@code <name>_far}: what was in the first shot is then
     * drawn by an LOD mod (Distant Horizons) if one is installed.
     */
    private static final int RETREAT = Integer.getInteger("spacebreaker.retreat", 0);
    /** -PmainHand=left and -PattackIndicator=hotbar|crosshair|off: player options that move HUD elements. */
    private static final String MAIN_HAND = System.getProperty("spacebreaker.mainHand", "");
    private static final String ATTACK_INDICATOR = System.getProperty("spacebreaker.attackIndicator", "");
    private static boolean retreating;
    private static int savedRenderDistance;
    private static final String WORLD = "sb_showcase";
    private static final int TIMEOUT_TICKS = 20 * 60 * 12;

    private enum Phase { TITLE, LOADING, BUILD, SHOTS, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks;
    private static int totalTicks;
    private static int shotIndex;
    private static boolean shotApplied;
    private static List<Shot> shots;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = net.minecraft.world.level.Level.OVERWORLD;

    /**
     * One screenshot. eye = camera position; serverSetup runs on the integrated server thread,
     * clientSetup on the client before waiting.
     */
    record Shot(String name, long time, double x, double y, double z, float yaw, float pitch, boolean hideGui,
                CameraType camera, int minTicks, Consumer<ServerPlayer> serverSetup, Runnable clientSetup) {
        /**
         * A shot that leaves the player where they are (no teleport, no camera hold): for sequences where the player
         * rides something that moves, which a teleport would throw them out of.
         */
        static Shot stay(String name, long time, boolean hideGui, CameraType camera, int minTicks,
                         Consumer<ServerPlayer> serverSetup, Runnable clientSetup) {
            return new Shot(name, time, Double.NaN, Double.NaN, Double.NaN, 0F, 0F, hideGui, camera, minTicks, serverSetup, clientSetup);
        }

        boolean stays() {
            return Double.isNaN(x);
        }
    }

    private ShowcaseDriver() {
    }

    private static boolean enabled() {
        return !SCENARIO.isEmpty();
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Init.Post event) {
        if (!enabled() || phase != Phase.TITLE || !(event.getScreen() instanceof TitleScreen title)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        log("creating world for scenario '" + SCENARIO + "'");
        deleteWorld(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD));
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, null);
        rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(20261106L, false, false);
        phase = Phase.LOADING;
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, options,
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                title);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!enabled() || phase == Phase.TITLE || phase == Phase.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (++totalTicks > TIMEOUT_TICKS * (QaUiProbe.active() ? 4 : 1)) {
            log("timed out, stopping");
            finish(mc);
            return;
        }
        switch (phase) {
            case LOADING -> {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                    if (++ticks > 40) {
                        ShowcaseScenario scenario = ShowcaseScenarios.get(SCENARIO);
                        var server = mc.getSingleplayerServer();
                        server.execute(() -> {
                            ServerLevel level = server.getLevel(scenario.dimension());
                            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                            if (level != null && player.level() != level) {
                                player.teleportTo(level, 0.5, 100, 0.5, 0F, 0F);
                            }
                            scenario.build(level != null ? level : server.overworld());
                        });
                        dimension = scenario.dimension();
                        shots = scenario.shots();
                        if (!FILTER.isEmpty()) {
                            shots = shots.stream().filter(sh -> sh.name().matches(FILTER)).toList();
                        }
                        phase = Phase.BUILD;
                        ticks = 0;
                        log("world ready, building showcase (" + shots.size() + " shots)");
                    }
                }
            }
            case BUILD -> {
                if (++ticks > 60) {
                    phase = Phase.SHOTS;
                    ticks = 0;
                }
            }
            case SHOTS -> runShots(mc);
            default -> {
            }
        }
    }

    private static void runShots(Minecraft mc) {
        if (shotIndex >= shots.size()) {
            log("all shots taken");
            finish(mc);
            return;
        }
        Shot shot = shots.get(shotIndex);
        if (!shotApplied) {
            mc.setScreen(null);
            if (!MAIN_HAND.isEmpty()) {
                mc.options.mainHand().set(MAIN_HAND.equalsIgnoreCase("left")
                        ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT);
                mc.options.broadcastOptions();
            }
            if (!ATTACK_INDICATOR.isEmpty()) {
                mc.options.attackIndicator().set(switch (ATTACK_INDICATOR.toLowerCase(java.util.Locale.ROOT)) {
                    case "hotbar" -> net.minecraft.client.AttackIndicatorStatus.HOTBAR;
                    case "off" -> net.minecraft.client.AttackIndicatorStatus.OFF;
                    default -> net.minecraft.client.AttackIndicatorStatus.CROSSHAIR;
                });
            }
            if (GUI_SCALE >= 0 && mc.options.guiScale().get() != GUI_SCALE) {
                mc.options.guiScale().set(GUI_SCALE);
                mc.resizeDisplay();
            }
            mc.options.hideGui = shot.hideGui();
            mc.options.setCameraType(shot.camera());
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ServerLevel level = server.getLevel(dimension) != null ? server.getLevel(dimension) : server.overworld();
                level.setDayTime(shot.time());
                level.setWeatherParameters(12000, 0, false, false);
                if (shot.stays()) {
                    if (shot.serverSetup() != null) {
                        shot.serverSetup().accept(player);
                    }
                    return;
                }
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                // hold the camera still until the terrain around it is loaded and drawn: without this the player
                // falls during the first frames after a teleport (chunks not there yet, flying not synced yet)
                player.setNoGravity(true);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.fallDistance = 0F;
                if (player.level() != level) {
                    player.teleportTo(level, shot.x(), shot.y() - player.getEyeHeight(), shot.z(), shot.yaw(), shot.pitch());
                } else {
                    player.connection.teleport(shot.x(), shot.y() - player.getEyeHeight(), shot.z(), shot.yaw(), shot.pitch());
                }
                if (shot.serverSetup() != null) {
                    shot.serverSetup().accept(player);
                }
                if (!OFFHAND.isEmpty()) {
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.parse(OFFHAND))
                            .ifPresent(item -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new net.minecraft.world.item.ItemStack(item)));
                }
            });
            if (shot.clientSetup() != null) {
                shot.clientSetup().run();
            }
            shotApplied = true;
            ticks = 0;
            log("shot " + shot.name());
            return;
        }
        ticks++;
        if (mc.player != null && !mc.player.isPassenger() && !shot.stays()) {
            // client side of the hold: no drift while the server's teleport and the chunks arrive
            mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (retreating) {
            if (ticks >= 1500 && (mc.levelRenderer.hasRenderedAllSections() || ticks > 1900)) {
                String file = PREFIX + "_" + shot.name() + "_far.png";
                Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> log("saved " + file));
                mc.options.renderDistance().set(savedRenderDistance);
                retreating = false;
                var server = mc.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayers().get(0).setNoGravity(false));
                shotIndex++;
                shotApplied = false;
            }
            return;
        }
        boolean ready = ticks >= shot.minTicks() && mc.levelRenderer.hasRenderedAllSections() && terrainLoaded(mc);
        if (ready && !QaUiProbe.finished(mc, shot.name())) {
            return; // QA screen probe: tooltips on this shot's screen first
        }
        if (ready || ticks > shot.minTicks() + (QaUiProbe.active() ? 12000 : 400)) {
            String file = PREFIX + "_" + shot.name() + ".png";
            Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> log("saved " + file));
            var server = mc.getSingleplayerServer();
            if (RETREAT > 0 && mc.player != null && !mc.player.isPassenger()) {
                // look at the same spot from far back: the target is where the view meets the ground, or 40 blocks out
                var look = mc.player.getViewVector(1F);
                var eye = mc.player.getEyePosition();
                var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(80)),
                        net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
                var target = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? eye.add(look.scale(40)) : hit.getLocation();
                double hx = look.x, hz = look.z, hl = Math.max(1.0E-4, Math.hypot(hx, hz));
                double fx = target.x - hx / hl * RETREAT, fz = target.z - hz / hl * RETREAT, fy = target.y + RETREAT / 3.0;
                float yaw = mc.player.getYRot();
                float pitch = (float) Math.toDegrees(Math.atan2(fy - target.y, RETREAT));
                savedRenderDistance = mc.options.renderDistance().get();
                mc.options.renderDistance().set(6);
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    player.setNoGravity(true);
                    player.connection.teleport(fx, fy - player.getEyeHeight(), fz, yaw, pitch);
                });
                retreating = true;
                ticks = 0;
                log("shot " + shot.name() + " from " + RETREAT + " blocks back");
                return;
            }
            server.execute(() -> server.getPlayerList().getPlayers().get(0).setNoGravity(false));
            shotIndex++;
            shotApplied = false;
        }
    }

    /**
     * The chunks around the camera are loaded on the client. Checked where the player is now, not at the shot's spot:
     * a shot's setup may move the player (into a vehicle, to another world).
     */
    private static boolean terrainLoaded(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        int cx = net.minecraft.core.SectionPos.blockToSectionCoord(mc.player.getX());
        int cz = net.minecraft.core.SectionPos.blockToSectionCoord(mc.player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!mc.level.getChunkSource().hasChunk(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Lowest field of view the game accepts: {@code options.fov().set} silently ignores anything below it. */
    static final int MIN_FOV = 30;

    /**
     * Sets the camera's field of view for a shot. Values below {@link #MIN_FOV} (which the game would silently ignore)
     * are clamped to it, with a warning in the log, so a narrow "telephoto" request still narrows the view.
     */
    static void fov(int degrees) {
        int value = degrees;
        if (degrees < MIN_FOV) {
            value = MIN_FOV;
            log("fov " + degrees + " is below the game's minimum, using " + MIN_FOV);
        }
        Minecraft.getInstance().options.fov().set(value);
    }

    /** Opens the creative inventory on the Spacebreaker tab (dev-only reflection into a private method). */
    static void openCreativeTab() {
        Minecraft mc = Minecraft.getInstance();
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, SbTabs.EQUIPMENT.holder.get());
        } catch (ReflectiveOperationException e) {
            log("could not select tab: " + e);
        }
    }

    private static void finish(Minecraft mc) {
        phase = Phase.DONE;
        mc.execute(mc::stop);
    }

    private static void deleteWorld(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            log("could not delete old world: " + e);
        }
    }

    static void log(String msg) {
        Spacebreaker.LOGGER.info("[showcase] {}", msg);
    }
}
