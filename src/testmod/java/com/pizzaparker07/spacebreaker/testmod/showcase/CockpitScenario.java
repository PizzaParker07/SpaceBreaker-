package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.client.hud.CockpitHud;
import com.pizzaparker07.spacebreaker.client.hud.CockpitReadout;
import com.pizzaparker07.spacebreaker.client.hud.HudConfig;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/**
 * Draft of the spacecraft cockpit HUD with made-up flight numbers (the craft itself does not exist yet): seated in a
 * capsule in Earth orbit, the simple HUD on a transfer to the Moon, the detailed HUD with orbit, event and node, and a
 * fast close approach.
 */
final class CockpitScenario implements ShowcaseScenario {
    @Override
    public ResourceKey<Level> dimension() {
        return Location.Orbits.EARTH_ORBIT;
    }

    @Override
    public void build(ServerLevel level) {
    }

    private static void seated(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        ServerLevel level = p.serverLevel();
        RocketEntity r = RocketEntity.create(level, RocketryModule.MOON_ROCKET, new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F);
        r.fillCompletely();
        level.addFreshEntity(r);
        r.stageScene(FlightState.ORBIT, 0, 1, false);
        SuitScenario.dress(p, SuitScenario.fullSuit(false, false));
        p.startRiding(r, true);
    }

    /** The transfer to the Moon: 3.1 km/s outbound, 52,000 km out, most of the Δv left. */
    private static final CockpitReadout TRANSFER = new CockpitReadout(Component.literal("Moon"), -1240, 3.3e8, false, 2650, 3900,
            false, 1.8e5, 3.9e8, Component.translatable("hud.spacebreaker.cockpit.event.soi_enter", "Moon"), 61_200, 815, 58_900);
    /** Close to a station with assist on and closing fast. */
    private static final CockpitReadout DOCKING = new CockpitReadout(Component.literal("Station"), -4.6, 38, false, 410, 3900,
            true, 4.1e5, 4.2e5, null, Double.NaN, Double.NaN, Double.NaN);

    private static Runnable show(CockpitReadout r, boolean detailed) {
        return () -> {
            SuitShots.CLEAR_TOASTS.run();
            HudConfig.DETAILED.set(detailed);
            CockpitHud.setSource(() -> r);
        };
    }

    /** A point {@code dist} blocks from the eye in the direction of yaw/pitch (degrees, Minecraft convention). */
    private static Vec3 ahead(double eyeY, float yaw, float pitch, double dist) {
        return new Vec3(0.5, eyeY, 0.5).add(Vec3.directionFromRotation(pitch, yaw).scale(dist));
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        double y = RocketEntity.ORBIT_Y + 1;
        return List.of(
                new Shot("cockpit_01_transfer", 6000, 0.5, y, 0.5, 30F, 20F, false, fp, 160, CockpitScenario::seated, show(TRANSFER, false)),
                new Shot("cockpit_02_transfer_detailed", 6000, 0.5, y, 0.5, 30F, 20F, false, fp, 60, CockpitScenario::seated, show(TRANSFER, true)),
                new Shot("cockpit_03_docking", 6000, 0.5, y, 0.5, 30F, 20F, false, fp, 60, CockpitScenario::seated, show(DOCKING, false)),
                // the target marked on screen: brackets around it when in view, an edge arrow when behind
                new Shot("cockpit_04_target_marker", 6000, 0.5, y, 0.5, 30F, 20F, false, fp, 60, CockpitScenario::seated,
                        show(TRANSFER.withTarget(ahead(y, 30F, 14F, 300), 24), false)),
                new Shot("cockpit_05_target_behind", 6000, 0.5, y, 0.5, 30F, 20F, false, fp, 60, CockpitScenario::seated,
                        show(TRANSFER.withTarget(ahead(y, 30F + 140F, 0F, 300), 24), false)));
    }
}
