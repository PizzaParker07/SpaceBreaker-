package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * QA: the launch from the pad to Earth orbit as the crew sees it. The player sits in the capsule of the showcase pad
 * rocket, the countdown runs, and a recorder logs every client tick (screen on top, dimension, height) to
 * run-showcase/qa_launch.tsv and saves a frame every 20 ticks, every 2 ticks around a dimension change or a screen.
 * Shows whether the switch to orbit passes through a loading screen or cuts hard.
 */
final class QaLaunchScenario implements ShowcaseScenario {
    private static final int RECORD_TICKS = 2400;
    private final ShowcaseScenario rocketry;

    QaLaunchScenario(ShowcaseScenario rocketry) {
        this.rocketry = rocketry;
    }

    @Override
    public void build(ServerLevel level) {
        rocketry.build(level);
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        s.add(new Shot("launch_end", 6000, 0.5, 120, 0.5, 0F, 0F, false, CameraType.FIRST_PERSON, RECORD_TICKS,
                p -> {
                    ServerLevel level = p.serverLevel();
                    List<RocketEntity> rockets = level.getEntitiesOfClass(RocketEntity.class, new AABB(p.blockPosition()).inflate(256));
                    if (rockets.isEmpty()) {
                        ShowcaseDriver.log("qa_launch: no rocket found");
                        return;
                    }
                    RocketEntity r = rockets.get(0);
                    p.getAbilities().flying = false;
                    p.setNoGravity(false);
                    p.onUpdateAbilities();
                    p.teleportTo(r.getX(), r.getY() + 2, r.getZ());
                    r.fillCompletely();
                    boolean seated = p.startRiding(r, true);
                    boolean go = r.tryStartCountdown(p);
                    ShowcaseDriver.log("qa_launch: seated " + seated + ", countdown " + go + ", rocket at " + r.blockPosition());
                }, Recorder::start));
        return s;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return rocketry.dimension();
    }

    /** Logs every client tick and saves frames, for {@link #RECORD_TICKS} ticks. */
    static final class Recorder {
        private static boolean registered;
        private static int tick = -1;
        private static String lastDim = "";
        private static String lastScreen = "";
        private static int denseUntil = 0;
        private static final StringBuilder LOG = new StringBuilder("tick\tscreen\tdimension\tplayer_y\tvehicle\tframe\n");

        static void start() {
            tick = 0;
            if (!registered) {
                registered = true;
                NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> onTick());
            }
        }

        private static void onTick() {
            if (tick < 0) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            String screen = mc.screen == null ? "-" : mc.screen.getClass().getSimpleName();
            String dim = mc.level == null ? "(none)" : mc.level.dimension().location().toString();
            if (!dim.equals(lastDim) || !screen.equals(lastScreen)) {
                denseUntil = tick + 60; // a change: frames every 2 ticks for the next 3 s
                lastDim = dim;
                lastScreen = screen;
            }
            String frame = "";
            if (tick % 20 == 0 || (tick <= denseUntil && tick % 2 == 0)) {
                frame = String.format("qa_launch_%04d.png", tick);
                String f = frame;
                Screenshot.grab(mc.gameDirectory, f, mc.getMainRenderTarget(), msg -> { });
            }
            LOG.append(tick).append('\t').append(screen).append('\t').append(dim).append('\t')
                    .append(mc.player == null ? "" : String.format("%.1f", mc.player.getY())).append('\t')
                    .append(mc.player == null || mc.player.getVehicle() == null ? "-" : mc.player.getVehicle().getType().toShortString())
                    .append('\t').append(frame).append('\n');
            tick++;
            if (tick % 100 == 0 || tick >= RECORD_TICKS) {
                try {
                    Files.writeString(Path.of(mc.gameDirectory.getPath(), "qa_launch.tsv"), LOG.toString());
                } catch (IOException ex) {
                    ShowcaseDriver.log("qa_launch: could not write log " + ex);
                }
            }
            if (tick >= RECORD_TICKS) {
                tick = -1;
            }
        }
    }
}
