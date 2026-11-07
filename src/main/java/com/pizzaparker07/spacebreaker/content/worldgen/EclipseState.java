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
    /** How far ahead (simulated seconds, about a year) an eclipse search looks. */
    static final double SEARCH_HORIZON = 3.2e7;
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
        Eclipses.Event event;
        long searchedAt = Long.MIN_VALUE;
        /** After an empty search: simulation time until which no eclipse needs looking for, and its time scale. */
        double quietUntil = Double.NaN;
        double quietFrom = Double.NaN;
        double quietScale = Double.NaN;
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

    /** Simulated seconds per real second at the server's time scale. */
    static double simPerReal() {
        return com.pizzaparker07.spacebreaker.core.astro.GameClock.simSecondsPerTick(BodyAdapter.timeScale()) * 20.0;
    }

    /** The current or next eclipse of this level (cached until its presentation window has passed). */
    static Eclipses.Event event(Level level, Entry e, BodySystem sys, double t) {
        double spr = simPerReal();
        if (e.event == null || Eclipses.shownTo(e.event, spr) < t || e.event.start() > t + SEARCH_HORIZON) {
            if (e.searchedAt != Long.MIN_VALUE && level.getGameTime() - e.searchedAt < 100 && e.event == null) {
                return null;
            }
            // an empty search covered a year ahead: skip until halfway through it (unless time jumped back or
            // the time scale changed). Most moons of the outer planets have no eclipse for years.
            if (e.event == null && !Double.isNaN(e.quietUntil) && t >= e.quietFrom && t < e.quietUntil && spr == e.quietScale) {
                return null;
            }
            e.searchedAt = level.getGameTime();
            try {
                Eclipses.Event ev = Eclipses.event(sys, e.observer, e.parent, t, Eclipses.MIN_SHOWN_SECONDS * spr, SEARCH_HORIZON, 3 * 3600);
                if (ev != null && Eclipses.shownTo(ev, spr) < t) {
                    // that one is already over: the next after it
                    ev = Eclipses.event(sys, e.observer, e.parent, ev.end() + 60, 0, SEARCH_HORIZON, 3 * 3600);
                }
                e.event = ev;
                if (ev == null) {
                    e.quietFrom = t;
                    e.quietUntil = t + SEARCH_HORIZON / 2;
                    e.quietScale = spr;
                } else {
                    e.quietUntil = Double.NaN;
                }
            } catch (RuntimeException ex) {
                e.event = null;
            }
        }
        return e.event;
    }

    /**
     * Share of the sun hidden by the planet as shown right now (0 when not on a moon). Follows the geometry, but each
     * eclipse is shown for at least a minute of real time (see {@link Eclipses#presentation}); server and client
     * compute the same window from the same simulation time.
     */
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
                double t = BodyAdapter.simTime(level);
                c = Eclipses.presentation(event(level, e, sys, t), t, simPerReal());
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

    /** Real seconds until the next eclipse is shown on this level (0 while one is showing), or NaN. */
    public static double secondsToNext(ServerLevel level) {
        Entry e = entry(level);
        BodySystem sys = system(level);
        if (e.none || sys == null) {
            return Double.NaN;
        }
        double t = BodyAdapter.simTime(level);
        Eclipses.Event ev = event(level, e, sys, t);
        if (ev == null) {
            return Double.NaN;
        }
        e.nextStart = ev.start();
        return Math.max(0, (Eclipses.shownFrom(ev, simPerReal()) - t) / simPerReal());
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
