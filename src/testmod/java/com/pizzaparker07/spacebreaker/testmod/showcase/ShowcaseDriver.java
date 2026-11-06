package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.registry.ModCreativeTabs;
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
    private static final String WORLD = "sb_showcase";
    private static final int TIMEOUT_TICKS = 20 * 60 * 12;

    private enum Phase { TITLE, LOADING, BUILD, SHOTS, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks;
    private static int totalTicks;
    private static int shotIndex;
    private static boolean shotApplied;
    private static List<Shot> shots;

    /**
     * One screenshot. eye = camera position; serverSetup runs on the integrated server thread,
     * clientSetup on the client before waiting.
     */
    record Shot(String name, long time, double x, double y, double z, float yaw, float pitch, boolean hideGui,
                CameraType camera, int minTicks, Consumer<ServerPlayer> serverSetup, Runnable clientSetup) {
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
        if (++totalTicks > TIMEOUT_TICKS) {
            log("timed out, stopping");
            finish(mc);
            return;
        }
        switch (phase) {
            case LOADING -> {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                    if (++ticks > 40) {
                        ServerLevel level = mc.getSingleplayerServer().overworld();
                        mc.getSingleplayerServer().execute(() -> ShowcaseBuilder.build(level));
                        shots = ShowcaseShots.forScenario(SCENARIO);
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
            mc.options.hideGui = shot.hideGui();
            mc.options.setCameraType(shot.camera());
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ServerLevel level = server.overworld();
                level.setDayTime(shot.time());
                level.setWeatherParameters(12000, 0, false, false);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(shot.x(), shot.y() - player.getEyeHeight(), shot.z(), shot.yaw(), shot.pitch());
                if (shot.serverSetup() != null) {
                    shot.serverSetup().accept(player);
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
        boolean ready = ticks >= shot.minTicks() && mc.levelRenderer.hasRenderedAllSections();
        if (ready || ticks > shot.minTicks() + 400) {
            String file = PREFIX + "_" + shot.name() + ".png";
            Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> log("saved " + file));
            shotIndex++;
            shotApplied = false;
        }
    }

    /** Opens the creative inventory on the Spacebreaker tab (dev-only reflection into a private method). */
    static void openCreativeTab() {
        Minecraft mc = Minecraft.getInstance();
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, ModCreativeTabs.MAIN.get());
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
