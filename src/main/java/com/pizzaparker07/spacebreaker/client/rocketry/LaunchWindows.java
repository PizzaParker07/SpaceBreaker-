package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.Ephemeris;
import com.pizzaparker07.spacebreaker.core.astro.GameClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;


/**
 * Launch window text for the star map. Uses the same ephemeris as the server's OrbitService (body data
 * and simulated clock are synced), so client and server agree on when a window opens.
 */
final class LaunchWindows {
    private static Ephemeris ephemeris;
    private static ClientLevel forLevel;

    private LaunchWindows() {
    }

    @Nullable
    static Component describe(ClientLevel level, Location from, Location to) {
        if (from.body() == to.body() || level == null) {
            return null;
        }
        try {
            if (ephemeris == null || forLevel != level) {
                var sys = BodyAdapter.system(level.registryAccess()).orElse(null);
                if (sys == null) {
                    return null;
                }
                ephemeris = new Ephemeris(sys, () -> BodyAdapter.simTime(level));
                forLevel = level;
            }
            String a = from.body().toString();
            String b = to.body().toString();
            Ephemeris.Window w = ephemeris.window(a, b);
            if (w.open()) {
                return Component.translatable("gui.spacebreaker.star_map.window_open");
            }
            long ticks = GameClock.ticks(w.waitFrom(ephemeris.now()), BodyAdapter.timeScale());
            long secs = ticks / 20;
            Component wait = secs >= 3600 ? Component.translatable("gui.spacebreaker.star_map.wait_hours", secs / 3600, secs / 60 % 60)
                    : Component.translatable("gui.spacebreaker.star_map.wait_minutes", Math.max(1, secs / 60));
            return Component.translatable("gui.spacebreaker.star_map.window_wait", wait);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
