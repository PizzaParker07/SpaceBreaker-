package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cold traps: the floors of permanently shadowed polar craters never see the sun, so they stay far colder than
 * the night side, day and night. Which worlds have them and how cold they are comes from the astro library's
 * {@link com.pizzaparker07.spacebreaker.core.astro.PolarIllumination} (Moon about 40 K = -233 °C, Mercury about
 * 60 K; none where ice is not stable, e.g. worlds with air); without that data the floor sits {@link #BELOW_NIGHT_C}
 * under the night temperature. On the Moon a first-tier suit cannot cope, a heated suit (or enough thermal
 * modules) can.
 */
public final class ColdTraps {
    /** How much colder than the night side a shadowed crater floor is. */
    public static final double BELOW_NIGHT_C = 60;
    /** Height above the crater floor still inside the cold trap. */
    public static final int DEPTH = 8;
    private static final Map<Level, Map<Long, Integer>> FLOOR = new java.util.WeakHashMap<>();

    private ColdTraps() {
    }

    static void register() {
        Environment.addTemperatureModifier(ColdTraps::offsetC);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.tickCount % 20 == 0) {
                notify(player);
            }
        });
    }

    /** Tells a player on the action bar when they step into or out of a cold trap. */
    private static void notify(net.minecraft.server.level.ServerPlayer player) {
        boolean in = inColdTrap(player.level(), player.blockPosition());
        var tag = player.getPersistentData();
        if (tag.getBoolean("spacebreaker_cold_trap") == in) {
            return;
        }
        tag.putBoolean("spacebreaker_cold_trap", in);
        player.displayClientMessage(in
                ? net.minecraft.network.chat.Component.translatable("message.spacebreaker.cold_trap.enter",
                        Math.round(Environment.temperatureC(player.level(), player.blockPosition())))
                : net.minecraft.network.chat.Component.translatable("message.spacebreaker.cold_trap.leave"), true);
    }

    /** Binds the astro polar data of the level's body to its planet generator (on level load). */
    static void bind(ServerLevel level) {
        if (!(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            return;
        }
        var access = level.registryAccess();
        var polar = Environment.body(level).flatMap(b -> com.pizzaparker07.spacebreaker.core.body.Bodies.registry(access).map(r -> r.getKey(b)))
                .flatMap(id -> com.pizzaparker07.spacebreaker.core.astro.BodyAdapter.polarIllumination(access, id.toString()));
        gen.bindPolar(polar.orElse(null));
        synchronized (FLOOR) {
            FLOOR.remove(level);
        }
    }

    /** Height of the shadowed crater floor at this column, or {@code Integer.MIN_VALUE} outside cold traps. */
    public static int floor(ServerLevel level, int x, int z) {
        if (!(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            return Integer.MIN_VALUE;
        }
        Map<Long, Integer> cache;
        synchronized (FLOOR) {
            cache = FLOOR.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        }
        if (cache.size() > 8192) {
            cache.clear();
        }
        return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> gen.coldTrapFloor(x, z));
    }

    /** True if this position is inside a cold trap (shadowed crater floor, up to {@link #DEPTH} above it). */
    public static boolean inColdTrap(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        int floor = floor(server, pos.getX(), pos.getZ());
        return floor != Integer.MIN_VALUE && pos.getY() <= floor + DEPTH;
    }

    static double offsetC(Level level, BlockPos pos) {
        if (!inColdTrap(level, pos)) {
            return 0;
        }
        var gen = (PlanetChunkGenerator) ((ServerLevel) level).getChunkSource().getGenerator();
        var polar = gen.polar();
        return Environment.body(level).map(b -> polar != null
                ? offsetToC(b.climate().dayTempC(), b.climate().nightTempC(), Environment.sunElevation(level), polar.coldTrapTempK() - 273.15)
                : offsetC(b.climate().dayTempC(), b.climate().nightTempC(), Environment.sunElevation(level))).orElse(0.0);
    }

    /** Temperature offset that brings the sun-warmed surface temperature to {@code trapC}. */
    public static double offsetToC(double dayC, double nightC, float sunElevation, double trapC) {
        double sun = Math.max(0.0, Math.sin(sunElevation));
        return trapC - (nightC + (dayC - nightC) * Math.sqrt(sun));
    }

    /** Fallback without astro data: cancels the sun's warming and drops {@link #BELOW_NIGHT_C} below the night. */
    public static double offsetC(double dayC, double nightC, float sunElevation) {
        double sun = Math.max(0.0, Math.sin(sunElevation));
        return -(dayC - nightC) * Math.sqrt(sun) - BELOW_NIGHT_C;
    }
}
