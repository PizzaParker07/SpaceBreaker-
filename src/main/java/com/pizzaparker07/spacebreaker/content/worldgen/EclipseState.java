package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.BodySystem;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Eclipses on a moon's surface, from {@link Eclipses}: while the planet covers the sun, sunlight (solar panels) drops
 * with the covered share and the warmth of the day fades slowly; mission control warns about a minute ahead and
 * announces the eclipse once. Works for any moon of a planet (the Moon, Phobos, the moons of the giants).
 */
public final class EclipseState {
    /** Real seconds of warning before an eclipse. */
    static final int WARN_SECONDS = 60;
    private static final Map<Level, Entry> STATES = new WeakHashMap<>();

    static final class Entry {
        String observer;
        String parent;
        boolean none = true;
        long resolvedAt = Long.MIN_VALUE;
        long tick = Long.MIN_VALUE;
        double coverage;
        double smoothed;
        double nextStart = Double.NaN;
        double announced = Double.NaN;
        boolean warned;
    }

    private EclipseState() {
    }

    static void register() {
        Environment.addSunlightModifier((level, pos) -> 1 - coverage(level));
        Environment.addTemperatureModifier((level, pos) -> {
            double s = smoothed(level);
            if (s <= 0) {
                return 0;
            }
            return Environment.body(level).map(b -> {
                double sun = Math.max(0.0, Math.sin(Environment.sunElevation(level)));
                return -(b.climate().dayTempC() - b.climate().nightTempC()) * Math.sqrt(sun) * s;
            }).orElse(0.0);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0) {
                announce(level);
            }
        });
    }

    /** The observer and parent ids of a moon's level ({@code none} when the level is not on a moon of a planet). */
    private static Entry entry(Level level) {
        Entry e;
        synchronized (STATES) {
            e = STATES.computeIfAbsent(level, l -> new Entry());
        }
        // resolve lazily (registries may not be ready when the level is first touched), retrying now and then
        if (e.observer == null && (e.resolvedAt == Long.MIN_VALUE || level.getGameTime() - e.resolvedAt >= 100 || level.getGameTime() < e.resolvedAt)) {
            e.resolvedAt = level.getGameTime();
            e.none = true;
            var access = level.registryAccess();
            var body = Environment.body(level);
            var reg = Bodies.registry(access);
            if (body.isPresent() && reg.isPresent() && !Bodies.isOrbit(level) && body.get().parent().isPresent()) {
                var parentId = body.get().parent().get();
                var parent = reg.get().getOptional(parentId);
                if (parent.isPresent() && parent.get().type() != BodyType.STAR) {
                    e.observer = reg.get().getKey(body.get()).toString();
                    e.parent = parentId.toString();
                    e.none = false;
                }
            }
        }
        return e;
    }

    private static BodySystem system(Level level) {
        return BodyAdapter.system(level.registryAccess()).orElse(null);
    }

    /** Share of the sun hidden by the planet right now (0 when not on a moon). */
    public static double coverage(Level level) {
        Entry e = entry(level);
        if (e.none) {
            return 0;
        }
        long tick = level.getGameTime();
        if (tick != e.tick) {
            BodySystem sys = system(level);
            double c = 0;
            if (sys != null) {
                try {
                    c = Eclipses.coverage(sys, e.observer, e.parent, BodyAdapter.simTime(level));
                } catch (RuntimeException ex) {
                    c = 0;
                }
            }
            long gap = e.tick == Long.MIN_VALUE ? 1 : Math.max(1, tick - e.tick);
            // warmth fades and returns over about ten seconds
            e.smoothed += (c - e.smoothed) * Math.min(1, gap / 200.0);
            e.coverage = c;
            e.tick = tick;
        }
        return e.coverage;
    }

    /** The coverage eased over about ten seconds (for warmth and light). */
    public static double smoothed(Level level) {
        coverage(level);
        return entry(level).smoothed;
    }

    /** Real seconds until the next eclipse on this level (searching about a year of simulation ahead), or NaN. */
    public static double secondsToNext(ServerLevel level) {
        Entry e = entry(level);
        BodySystem sys = system(level);
        if (e.none || sys == null) {
            return Double.NaN;
        }
        double t = BodyAdapter.simTime(level);
        if (Double.isNaN(e.nextStart) || e.nextStart < t - 6 * 3600) {
            e.nextStart = Eclipses.next(sys, e.observer, e.parent, t, 3.2e7, 3 * 3600);
        }
        if (Double.isNaN(e.nextStart)) {
            return Double.NaN;
        }
        double perTick = com.pizzaparker07.spacebreaker.core.astro.GameClock.simSecondsPerTick(BodyAdapter.timeScale());
        return (e.nextStart - t) / perTick / 20.0;
    }

    private static void announce(ServerLevel level) {
        Entry e = entry(level);
        if (e.none || level.players().isEmpty()) {
            return;
        }
        double secs = secondsToNext(level);
        if (Double.isNaN(secs)) {
            return;
        }
        if (secs > 0 && secs <= WARN_SECONDS && !e.warned) {
            e.warned = true;
            tell(level, Component.translatable("message.spacebreaker.eclipse.soon", parentName(e)));
        }
        if (coverage(level) > 0 && e.announced != e.nextStart) {
            e.announced = e.nextStart;
            e.warned = false;
            tell(level, Component.translatable("message.spacebreaker.eclipse.now", parentName(e)));
        }
    }

    private static Component parentName(Entry e) {
        String path = e.parent.substring(e.parent.indexOf(':') + 1);
        return Component.translatable("body.spacebreaker." + path);
    }

    private static void tell(ServerLevel level, Component msg) {
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(msg.copy().withStyle(ChatFormatting.GOLD));
        }
    }
}
